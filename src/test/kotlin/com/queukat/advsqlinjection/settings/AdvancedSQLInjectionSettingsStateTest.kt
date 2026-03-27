package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.model.InjectionRule
import com.queukat.advsqlinjection.model.RuleScope
import com.queukat.advsqlinjection.model.RuleTargetType
import com.intellij.util.xmlb.XmlSerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdvancedSQLInjectionSettingsStateTest {

    @Test
    fun `loadState migrates legacy raw rules into typed rules`() {
        val service = AdvancedSQLInjectionSettingsState()
        val state = AdvancedSQLInjectionSettingsState.State().apply {
            prefixLanguagePatterns = mutableListOf("sql:=SQL=*.yaml")
        }

        service.loadState(state)

        val migratedRule = service.state.rules.single()
        assertEquals("sql:", migratedRule.prefix)
        assertEquals("SQL", migratedRule.languageId)
        assertEquals("*.yaml", migratedRule.filePattern)
        assertEquals(RuleScope.FILE_NAME_ONLY, migratedRule.scope)
        assertEquals(RuleTargetType.VALUE_CONTAINS_PREFIX, migratedRule.targetType)
        assertTrue(service.state.prefixLanguagePatterns.isEmpty())
    }

    @Test
    fun `typed rules survive persistence round trip`() {
        val state = AdvancedSQLInjectionSettingsState.State().apply {
            sqlInjectionEnabled = false
            injectAllOccurrences = true
            caseInsensitivePrefix = true
            rules = mutableListOf(
                InjectionRule(
                    enabled = true,
                    prefix = "sql:",
                    languageId = "SQL",
                    filePattern = "*.yaml",
                    pathPattern = "config/**/*.yaml",
                    scope = RuleScope.PATH_AWARE,
                    targetType = RuleTargetType.VALUE_STARTS_WITH_PREFIX
                )
            )
        }

        val serialized = XmlSerializer.serialize(state)
        val restored = XmlSerializer.deserialize(serialized, AdvancedSQLInjectionSettingsState.State::class.java)

        assertEquals(state.sqlInjectionEnabled, restored.sqlInjectionEnabled)
        assertEquals(state.injectAllOccurrences, restored.injectAllOccurrences)
        assertEquals(state.caseInsensitivePrefix, restored.caseInsensitivePrefix)
        assertEquals(state.rules, restored.rules)
    }
}
