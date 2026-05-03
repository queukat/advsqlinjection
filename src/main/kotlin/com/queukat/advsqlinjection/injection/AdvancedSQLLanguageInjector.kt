package com.queukat.advsqlinjection.injection

import com.queukat.advsqlinjection.model.InjectionRule
import com.queukat.advsqlinjection.settings.AdvancedSQLInjectionSettingsState
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

class AdvancedSQLLanguageInjector : MultiHostInjector {

    private val log = Logger.getInstance(AdvancedSQLLanguageInjector::class.java)
    private val warnedInvalidLanguageRules = ConcurrentHashMap.newKeySet<String>()
    private val snapshotCache = AtomicReference<CachedSnapshot>()

    override fun elementsToInjectIn(): List<Class<out PsiElement>> =
        listOf(PsiLanguageInjectionHost::class.java)

    override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
        val host = context as? PsiLanguageInjectionHost ?: return
        val file = host.containingFile?.virtualFile ?: return
        val project = host.project

        val settingsService = AdvancedSQLInjectionSettingsState.getInstance(project)
        val settings = settingsService.state

        if (!settings.sqlInjectionEnabled) {
            return
        }

        val valueTextRange = ElementManipulators.getValueTextRange(host)
        if (valueTextRange.startOffset >= valueTextRange.endOffset) {
            return
        }

        val hostText = host.text
        val valueText = hostText.substring(valueTextRange.startOffset, valueTextRange.endOffset)
        if (valueText.isEmpty()) {
            return
        }

        val snapshot = snapshotFor(settings)
        val input = RuleMatchInput(
            valueText = valueText,
            fileName = file.name,
            fullPath = InjectionRuleMatcher.normalizePath(file.path),
            relativePath = InjectionRuleMatcher.toRelativePath(project.basePath, file.path),
            structuralPrefixes = StructuralInjectionPrefixExtractor.extract(host)
        )

        val plannedInjection = InjectionExecutionPlanner.planFirstMatchingExecutableRule(
            rules = snapshot.executableRules,
            input = input,
            caseInsensitivePrefix = snapshot.caseInsensitivePrefix,
            injectAllOccurrences = snapshot.injectAllOccurrences
        ) ?: return

        registrar.startInjecting(plannedInjection.language)
        plannedInjection.ranges.forEach { relativeRange ->
            val hostRange = TextRange(
                valueTextRange.startOffset + relativeRange.startOffset,
                valueTextRange.startOffset + relativeRange.endOffset
            )
            registrar.addPlace(null, null, host, hostRange)
        }
        registrar.doneInjecting()
    }

    private fun snapshotFor(settings: AdvancedSQLInjectionSettingsState.State): RuleExecutionSnapshot {
        val cacheKey = SettingsCacheKey(
            injectAllOccurrences = settings.injectAllOccurrences,
            caseInsensitivePrefix = settings.caseInsensitivePrefix,
            rules = settings.rules.map(InjectionRule::normalized)
        )

        snapshotCache.get()?.takeIf { it.key == cacheKey }?.let { return it.snapshot }

        val executableRules = cacheKey.rules.mapNotNull { rule ->
            val language = Language.findLanguageByID(rule.languageId)
            if (language == null) {
                if (rule.languageId.isNotBlank()) {
                    warnInvalidLanguageRule(rule.languageId, rule.prefix)
                }
                null
            } else if (!rule.enabled || rule.prefix.isEmpty()) {
                null
            } else {
                ExecutableInjectionRule(
                    preparedRule = InjectionRuleMatcher.prepareRule(rule),
                    language = language
                )
            }
        }

        val snapshot = RuleExecutionSnapshot(
            executableRules = executableRules,
            injectAllOccurrences = cacheKey.injectAllOccurrences,
            caseInsensitivePrefix = cacheKey.caseInsensitivePrefix
        )
        snapshotCache.set(CachedSnapshot(cacheKey, snapshot))
        return snapshot
    }

    private fun warnInvalidLanguageRule(languageId: String, prefix: String) {
        val warningKey = "$languageId::$prefix"
        if (warnedInvalidLanguageRules.add(warningKey)) {
            log.warn("Unknown language id '$languageId' in injection rule for prefix '$prefix'")
        }
    }

    private data class SettingsCacheKey(
        val injectAllOccurrences: Boolean,
        val caseInsensitivePrefix: Boolean,
        val rules: List<InjectionRule>
    )

    private data class RuleExecutionSnapshot(
        val executableRules: List<ExecutableInjectionRule>,
        val injectAllOccurrences: Boolean,
        val caseInsensitivePrefix: Boolean
    )

    private data class CachedSnapshot(
        val key: SettingsCacheKey,
        val snapshot: RuleExecutionSnapshot
    )
}
