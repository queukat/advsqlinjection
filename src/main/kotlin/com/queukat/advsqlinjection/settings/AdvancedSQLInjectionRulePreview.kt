package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.injection.InjectionRuleMatcher
import com.queukat.advsqlinjection.injection.RuleMatchInput
import com.queukat.advsqlinjection.injection.StructuralInjectionPrefixExtractor
import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.queukat.advsqlinjection.model.InjectionRule
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.annotations.TestOnly

internal object AdvancedSQLInjectionRulePreview {
    @TestOnly
    fun buildResult(
        project: Project?,
        selectedRule: InjectionRule,
        caseInsensitivePrefix: Boolean,
        injectAllOccurrences: Boolean
    ): Result {
        val previewProject = project ?: return messageResult(
            selectedRule.languageId,
            AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewNoProject")
        )
        unavailableLanguageResult(selectedRule)?.let { return it }
        val context = currentFileContext(previewProject) ?: return messageResult(
            selectedRule.languageId,
            AdvancedSqlInjectionBundle.message(NO_EDITOR_MESSAGE_KEY)
        )

        return ApplicationManager.getApplication().runReadAction<Result> {
            buildResultInReadAction(
                project = previewProject,
                context = context,
                selectedRule = selectedRule,
                caseInsensitivePrefix = caseInsensitivePrefix,
                injectAllOccurrences = injectAllOccurrences
            )
        }
    }

    fun buildMessageAsync(
        project: Project?,
        selectedRule: InjectionRule,
        caseInsensitivePrefix: Boolean,
        injectAllOccurrences: Boolean,
        parentDisposable: Disposable? = project,
        onMessageReady: (String) -> Unit
    ) {
        buildResultAsync(
            project = project,
            selectedRule = selectedRule,
            caseInsensitivePrefix = caseInsensitivePrefix,
            injectAllOccurrences = injectAllOccurrences,
            parentDisposable = parentDisposable
        ) { result ->
            onMessageReady(result.toCopyableText())
        }
    }

    fun buildResultAsync(
        project: Project?,
        selectedRule: InjectionRule,
        caseInsensitivePrefix: Boolean,
        injectAllOccurrences: Boolean,
        parentDisposable: Disposable? = project,
        onResultReady: (Result) -> Unit
    ) {
        val previewProject = project ?: run {
            onResultReady(
                messageResult(
                    selectedRule.languageId,
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewNoProject")
                )
            )
            return
        }
        unavailableLanguageResult(selectedRule)?.let { result ->
            onResultReady(result)
            return
        }
        val context = currentFileContext(previewProject) ?: run {
            onResultReady(messageResult(selectedRule.languageId, AdvancedSqlInjectionBundle.message(NO_EDITOR_MESSAGE_KEY)))
            return
        }

        ReadAction.nonBlocking<Result> {
            ProgressManager.checkCanceled()
            buildResultInReadAction(
                project = previewProject,
                context = context,
                selectedRule = selectedRule,
                caseInsensitivePrefix = caseInsensitivePrefix,
                injectAllOccurrences = injectAllOccurrences
            )
        }
            .withDocumentsCommitted(previewProject)
            .let { readAction ->
                if (parentDisposable != null) {
                    readAction.expireWith(parentDisposable)
                } else {
                    readAction
                }
            }
            .finishOnUiThread(ModalityState.defaultModalityState()) { result ->
                onResultReady(result)
            }
            .submit(AppExecutorUtil.getAppExecutorService())
    }

    fun messageResult(languageId: String, message: String): Result =
        Result(languageId = languageId, message = message)

    private fun buildResultInReadAction(
        project: Project,
        context: PreviewFileContext,
        selectedRule: InjectionRule,
        caseInsensitivePrefix: Boolean,
        injectAllOccurrences: Boolean
    ): Result {
        val virtualFile = context.virtualFile
        val psiFile = PsiManager.getInstance(project).findFile(context.virtualFile)
            ?: return messageResult(selectedRule.languageId, AdvancedSqlInjectionBundle.message(NO_EDITOR_MESSAGE_KEY))

        val fileMatch = InjectionRuleMatcher.matchesFile(
            selectedRule,
            RuleMatchInput(
                valueText = "",
                fileName = virtualFile.name,
                fullPath = context.fullPath,
                relativePath = context.relativePath
            )
        )

        val hostScan = collectPreviewHosts(psiFile)
        var matchedHosts = 0
        var matchedSegments = 0
        val previews = mutableListOf<String>()

        hostScan.hosts.forEach { host ->
            ProgressManager.checkCanceled()
            val valueTextRange = ElementManipulators.getValueTextRange(host)
            if (valueTextRange.startOffset >= valueTextRange.endOffset) {
                return@forEach
            }

            val hostText = host.text
            val valueText = hostText.substring(valueTextRange.startOffset, valueTextRange.endOffset)
            val ranges = InjectionRuleMatcher.findRanges(
                rawRule = selectedRule,
                input = RuleMatchInput(
                    valueText = valueText,
                    fileName = virtualFile.name,
                    fullPath = context.fullPath,
                    relativePath = context.relativePath,
                    structuralPrefixes = StructuralInjectionPrefixExtractor.extract(host)
                ),
                caseInsensitivePrefix = caseInsensitivePrefix,
                injectAllOccurrences = injectAllOccurrences
            )

            if (ranges.isNotEmpty()) {
                matchedHosts++
                matchedSegments += ranges.size
                if (previews.size < PREVIEW_LIMIT) {
                    previews += valueText.take(PREVIEW_TEXT_LIMIT).replace('\n', ' ')
                }
            }
        }

        return Result(
            languageId = selectedRule.languageId,
            currentFilePath = virtualFile.path,
            fileMatched = fileMatch,
            matchedHosts = matchedHosts,
            matchedSegments = matchedSegments,
            examples = previews,
            truncated = hostScan.truncated,
            hostScanLimit = PREVIEW_HOST_SCAN_LIMIT
        )
    }

    private fun unavailableLanguageResult(selectedRule: InjectionRule): Result? =
        if (Language.findLanguageByID(selectedRule.languageId) == null) {
            Result(
                languageId = selectedRule.languageId,
                unavailableLanguageId = selectedRule.languageId
            )
        } else {
            null
        }

    private fun currentFileContext(project: Project): PreviewFileContext? {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return null
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return null
        return PreviewFileContext(
            virtualFile = virtualFile,
            fullPath = InjectionRuleMatcher.normalizePath(virtualFile.path),
            relativePath = InjectionRuleMatcher.toRelativePath(project.basePath, virtualFile.path)
        )
    }

    private fun collectPreviewHosts(psiFile: PsiFile): PreviewHostScan {
        val hosts = mutableListOf<PsiLanguageInjectionHost>()
        var truncated = false

        psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                ProgressManager.checkCanceled()
                if (element is PsiLanguageInjectionHost) {
                    if (hosts.size >= PREVIEW_HOST_SCAN_LIMIT) {
                        truncated = true
                        stopWalking()
                        return
                    }
                    hosts += element
                }
                super.visitElement(element)
            }
        })

        return PreviewHostScan(hosts = hosts, truncated = truncated)
    }

    private data class PreviewFileContext(
        val virtualFile: VirtualFile,
        val fullPath: String,
        val relativePath: String?
    )

    private data class PreviewHostScan(
        val hosts: List<PsiLanguageInjectionHost>,
        val truncated: Boolean
    )

    internal data class Result(
        val languageId: String,
        val currentFilePath: String? = null,
        val fileMatched: Boolean? = null,
        val matchedHosts: Int? = null,
        val matchedSegments: Int? = null,
        val examples: List<String> = emptyList(),
        val unavailableLanguageId: String? = null,
        val message: String? = null,
        val truncated: Boolean = false,
        val hostScanLimit: Int = 0
    ) {
        fun toCopyableText(): String = buildString {
            message?.let { appendLine(it) }
            unavailableLanguageId?.let {
                appendLine(
                    AdvancedSqlInjectionBundle.message(
                        "msg.AdvancedSqlInjection.previewLanguageUnavailable",
                        it
                    )
                )
            }
            currentFilePath?.let {
                appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewFileHeader", it))
            }
            fileMatched?.let {
                appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewFileMatch", it))
            }
            matchedHosts?.let {
                appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewHostMatchCount", it))
            }
            matchedSegments?.let {
                appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewSegmentMatchCount", it))
            }
            if (examples.isNotEmpty()) {
                appendLine()
                appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewExamplesHeader"))
                examples.forEach { preview ->
                    appendLine("- $preview")
                }
            }
            if (truncated) {
                appendLine()
                appendLine(
                    AdvancedSqlInjectionBundle.message(
                        "msg.AdvancedSqlInjection.previewTruncated",
                        hostScanLimit
                    )
                )
            }
        }.trimEnd()
    }

    private const val NO_EDITOR_MESSAGE_KEY = "msg.AdvancedSqlInjection.previewNoEditor"
    private const val PREVIEW_HOST_SCAN_LIMIT = 500
    private const val PREVIEW_LIMIT = 3
    private const val PREVIEW_TEXT_LIMIT = 120
}
