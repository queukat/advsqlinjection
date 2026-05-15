package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.injection.InjectionRuleMatcher
import com.queukat.advsqlinjection.injection.RuleMatchInput
import com.queukat.advsqlinjection.injection.StructuralInjectionPrefixExtractor
import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.queukat.advsqlinjection.model.InjectionRule
import com.intellij.lang.Language
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil

internal object AdvancedSQLInjectionRulePreview {
    fun buildMessage(
        project: Project?,
        selectedRule: InjectionRule,
        caseInsensitivePrefix: Boolean,
        injectAllOccurrences: Boolean
    ): String {
        if (project == null) {
            return AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewNoProject")
        }
        if (Language.findLanguageByID(selectedRule.languageId) == null) {
            return AdvancedSqlInjectionBundle.message(
                "msg.AdvancedSqlInjection.previewLanguageUnavailable",
                selectedRule.languageId
            )
        }

        val editor = FileEditorManager.getInstance(project).selectedTextEditor
            ?: return AdvancedSqlInjectionBundle.message(NO_EDITOR_MESSAGE_KEY)
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document)
            ?: return AdvancedSqlInjectionBundle.message(NO_EDITOR_MESSAGE_KEY)
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            ?: return AdvancedSqlInjectionBundle.message(NO_EDITOR_MESSAGE_KEY)

        val fullPath = InjectionRuleMatcher.normalizePath(virtualFile.path)
        val relativePath = InjectionRuleMatcher.toRelativePath(project.basePath, virtualFile.path)
        val fileMatch = InjectionRuleMatcher.matchesFile(
            selectedRule,
            RuleMatchInput(valueText = "", fileName = virtualFile.name, fullPath = fullPath, relativePath = relativePath)
        )

        val hosts = PsiTreeUtil.collectElementsOfType(psiFile, PsiLanguageInjectionHost::class.java).toList()
        var matchedHosts = 0
        var matchedSegments = 0
        val previews = mutableListOf<String>()

        hosts.forEach { host ->
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
                    fullPath = fullPath,
                    relativePath = relativePath,
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

        return buildString {
            appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewFileHeader", virtualFile.path))
            appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewFileMatch", fileMatch))
            appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewHostMatchCount", matchedHosts))
            appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewSegmentMatchCount", matchedSegments))
            if (previews.isNotEmpty()) {
                appendLine()
                appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewExamplesHeader"))
                previews.forEach { preview ->
                    appendLine("- $preview")
                }
            }
        }
    }

    private const val NO_EDITOR_MESSAGE_KEY = "msg.AdvancedSqlInjection.previewNoEditor"
    private const val PREVIEW_LIMIT = 3
    private const val PREVIEW_TEXT_LIMIT = 120
}
