package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.model.InjectionRule
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import org.junit.Test
import kotlin.test.assertTrue

class AdvancedSQLInjectionRulePreviewTest : LightPlatformCodeInsightFixture4TestCase() {

    @Test
    fun testPreviewReportsWhenHostScanIsTruncated() {
        val yamlText = (1..505).joinToString(separator = "\n") { index ->
            "query$index: \"dsl:class User$index {}\""
        }
        myFixture.configureByText("many.yaml", yamlText)

        val result = AdvancedSQLInjectionRulePreview.buildResult(
            project = project,
            selectedRule = InjectionRule(
                prefix = "dsl:",
                languageId = "JAVA",
                filePattern = "*.yaml"
            ),
            caseInsensitivePrefix = false,
            injectAllOccurrences = false
        )

        assertTrue(result.truncated, result.toString())
        assertTrue(
            result.toCopyableText().contains("Preview scanned the first 500 value hosts only"),
            result.toCopyableText()
        )
    }

    @Test
    fun testUnavailableLanguagePreviewIsStructuredAndCopyable() {
        val result = AdvancedSQLInjectionRulePreview.buildResult(
            project = project,
            selectedRule = InjectionRule(
                prefix = "dsl:",
                languageId = "DefinitelyUnavailablePreviewLanguage",
                filePattern = "*.yaml"
            ),
            caseInsensitivePrefix = false,
            injectAllOccurrences = false
        )

        assertTrue(result.unavailableLanguageId == "DefinitelyUnavailablePreviewLanguage", result.toString())
        assertTrue(
            result.toCopyableText().contains("DefinitelyUnavailablePreviewLanguage"),
            result.toCopyableText()
        )
    }
}
