package com.queukat.advsqlinjection.injection

import com.queukat.advsqlinjection.model.InjectionRule
import com.queukat.advsqlinjection.model.RuleScope
import com.queukat.advsqlinjection.model.RuleTargetType
import com.queukat.advsqlinjection.settings.AdvancedSQLInjectionSettingsState
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import junit.framework.TestCase.assertEquals
import org.junit.Test

class AdvancedSQLLanguageInjectorFixtureTest : LightPlatformCodeInsightFixture4TestCase() {

    @Test
    fun testYamlInjection() {
        assertJavaInjected("queries.yaml", "query: \"sql:c<caret>lass User {}\"")
    }

    @Test
    fun testJsonInjection() {
        assertJavaInjected("queries.json", """{"query": "sql:c<caret>lass User {}"}""")
    }

    @Test
    fun testPropertiesInjection() {
        assertJavaInjected("queries.properties", "query=sql:c<caret>lass User {}")
    }

    @Test
    fun testYamlBlockScalarHeaderInjection() {
        assertJavaInjected(
            fileName = "queries.yaml",
            text = """
                snippet: |
                  ---
                      c<caret>lass User {}
            """.trimIndent(),
            prefix = "snippet: |",
            filePattern = "*.yaml",
            scope = RuleScope.PATH_AWARE
        )
    }

    private fun assertJavaInjected(
        fileName: String,
        text: String,
        prefix: String = "sql:",
        filePattern: String = "*",
        scope: RuleScope = RuleScope.FILE_NAME_ONLY
    ) {
        AdvancedSQLInjectionSettingsState.getInstance(project).loadState(
            AdvancedSQLInjectionSettingsState.State().apply {
                rules = mutableListOf(
                    InjectionRule(
                        prefix = prefix,
                        languageId = "JAVA",
                        filePattern = filePattern,
                        scope = scope,
                        targetType = RuleTargetType.VALUE_STARTS_WITH_PREFIX
                    )
                )
            }
        )

        myFixture.configureByText(fileName, text)
        myFixture.doHighlighting()

        assertEquals("JAVA", myFixture.file.language.id)
    }
}
