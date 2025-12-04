package com.queukat.advsqlinjection.injection

import com.queukat.advsqlinjection.settings.AdvancedSQLInjectionSettingsState
import com.intellij.lang.Language
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost

class AdvancedSQLLanguageInjector : MultiHostInjector {

    private val log = Logger.getInstance(AdvancedSQLLanguageInjector::class.java)

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

        val text = host.text
        val fileName = file.name

        for (rule in settings.prefixLanguagePatterns) {
            val parts = rule.split("=", limit = 3)
            if (parts.size != 3) continue

            val prefix = parts[0].trim()
            val languageId = parts[1].trim()
            val pattern = parts[2].trim()

            if (prefix.isEmpty() || languageId.isEmpty() || pattern.isEmpty()) {
                continue
            }

            val fileNameRegex = globToRegex(pattern)
            if (!fileName.matches(fileNameRegex)) {
                continue
            }

            val language = Language.findLanguageByID(languageId)
            if (language == null) {
                log.warn("Unknown language id '$languageId' in rule '$rule'")
                continue
            }

            var injected = false
            var searchStart = 0

            while (true) {
                val prefixIdx = text.indexOf(
                    prefix,
                    startIndex = searchStart,
                    ignoreCase = settings.caseInsensitivePrefix
                )
                if (prefixIdx < 0) break

                val range = TextRange(prefixIdx + prefix.length, text.length)
                if (range.startOffset < range.endOffset) {
                    registrar
                        .startInjecting(language)
                        .addPlace(null, null, host, range)
                        .doneInjecting()
                    injected = true
                }

                if (!settings.injectAllOccurrences) {
                    // Keep behavior: inject only first occurrence and stop completely
                    return
                }

                searchStart = prefixIdx + 1
            }

            if (injected) {
                // First matching rule wins
                return
            }
        }
    }

    private fun globToRegex(pattern: String): Regex {
        val builder = StringBuilder(pattern.length * 2)
        for (ch in pattern) {
            when (ch) {
                '*' -> builder.append(".*")
                '?' -> builder.append('.')
                '.', '(', ')', '[', ']', '{', '}', '+',
                '$', '^', '|', '\\' -> {
                    builder.append('\\').append(ch)
                }
                else -> builder.append(ch)
            }
        }
        return builder.toString().toRegex()
    }
}
