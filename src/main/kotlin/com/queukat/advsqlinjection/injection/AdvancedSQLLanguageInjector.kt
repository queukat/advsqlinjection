package com.queukat.advsqlinjection.injection

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

class AdvancedSQLLanguageInjector : MultiHostInjector {

    private val log = Logger.getInstance(AdvancedSQLLanguageInjector::class.java)
    private val warnedInvalidLanguageRules = ConcurrentHashMap.newKeySet<String>()

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

        val input = RuleMatchInput(
            valueText = valueText,
            fileName = file.name,
            fullPath = InjectionRuleMatcher.normalizePath(file.path),
            relativePath = InjectionRuleMatcher.toRelativePath(project.basePath, file.path)
        )

        settings.rules.forEach { rule ->
            if (Language.findLanguageByID(rule.languageId.trim()) == null && rule.languageId.isNotBlank()) {
                warnInvalidLanguageRule(rule.languageId.trim(), rule.prefix.trim())
            }
        }

        val plannedInjection = InjectionExecutionPlanner.planFirstMatchingRule(
            rules = settings.rules,
            input = input,
            caseInsensitivePrefix = settings.caseInsensitivePrefix,
            injectAllOccurrences = settings.injectAllOccurrences,
            isLanguageSupported = { languageId -> Language.findLanguageByID(languageId) != null }
        ) ?: return

        val language = Language.findLanguageByID(plannedInjection.rule.languageId) ?: return
        registrar.startInjecting(language)
        plannedInjection.ranges.forEach { relativeRange ->
            val hostRange = TextRange(
                valueTextRange.startOffset + relativeRange.startOffset,
                valueTextRange.startOffset + relativeRange.endOffset
            )
            registrar.addPlace(null, null, host, hostRange)
        }
        registrar.doneInjecting()
    }

    private fun warnInvalidLanguageRule(languageId: String, prefix: String) {
        val warningKey = "$languageId::$prefix"
        if (warnedInvalidLanguageRules.add(warningKey)) {
            log.warn("Unknown language id '$languageId' in injection rule for prefix '$prefix'")
        }
    }
}
