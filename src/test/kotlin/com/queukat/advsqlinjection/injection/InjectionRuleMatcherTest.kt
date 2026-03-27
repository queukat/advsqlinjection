package com.queukat.advsqlinjection.injection

import com.queukat.advsqlinjection.model.InjectionRule
import com.queukat.advsqlinjection.model.RuleScope
import com.queukat.advsqlinjection.model.RuleTargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InjectionRuleMatcherTest {

    @Test
    fun `value starts with prefix matches from prefix end to value end`() {
        val rule = InjectionRule(
            prefix = "sql:",
            languageId = "SQL",
            filePattern = "*.yaml",
            targetType = RuleTargetType.VALUE_STARTS_WITH_PREFIX
        )

        val ranges = InjectionRuleMatcher.findRanges(
            rawRule = rule,
            input = RuleMatchInput(
                valueText = "sql:select * from users",
                fileName = "queries.yaml",
                fullPath = "/repo/queries.yaml"
            ),
            caseInsensitivePrefix = false,
            injectAllOccurrences = false
        )

        assertEquals(listOf(RelativeMatchRange(4, 23)), ranges)
    }

    @Test
    fun `contains mode respects case insensitive matching`() {
        val rule = InjectionRule(
            prefix = "sql:",
            languageId = "SQL",
            filePattern = "*.yaml",
            targetType = RuleTargetType.VALUE_CONTAINS_PREFIX
        )

        val ranges = InjectionRuleMatcher.findRanges(
            rawRule = rule,
            input = RuleMatchInput(
                valueText = "prefix SQL:select 1",
                fileName = "queries.yaml",
                fullPath = "/repo/queries.yaml"
            ),
            caseInsensitivePrefix = true,
            injectAllOccurrences = false
        )

        assertEquals(listOf(RelativeMatchRange(11, 19)), ranges)
    }

    @Test
    fun `path aware scope matches project relative paths`() {
        val rule = InjectionRule(
            prefix = "sql:",
            languageId = "SQL",
            filePattern = "*.yaml",
            pathPattern = "config/**/*.yaml",
            scope = RuleScope.PATH_AWARE,
            targetType = RuleTargetType.VALUE_STARTS_WITH_PREFIX
        )

        val matches = InjectionRuleMatcher.matchesFile(
            rawRule = rule,
            input = RuleMatchInput(
                valueText = "",
                fileName = "main.yaml",
                fullPath = "/repo/config/sql/main.yaml",
                relativePath = "config/sql/main.yaml"
            )
        )

        assertTrue(matches)
    }

    @Test
    fun `multiple occurrences are split into non overlapping ranges`() {
        val rule = InjectionRule(
            prefix = "sql:",
            languageId = "SQL",
            filePattern = "*.yaml",
            targetType = RuleTargetType.VALUE_CONTAINS_PREFIX
        )

        val ranges = InjectionRuleMatcher.findRanges(
            rawRule = rule,
            input = RuleMatchInput(
                valueText = "sql:one sql:two",
                fileName = "queries.yaml",
                fullPath = "/repo/queries.yaml"
            ),
            caseInsensitivePrefix = false,
            injectAllOccurrences = true
        )

        assertEquals(
            listOf(
                RelativeMatchRange(4, 8),
                RelativeMatchRange(12, 15)
            ),
            ranges
        )
    }

    @Test
    fun `planner keeps first matching rule order`() {
        val rules = listOf(
            InjectionRule(prefix = "sql:", languageId = "SQL", filePattern = "*.yaml"),
            InjectionRule(prefix = "sql:", languageId = "RegExp", filePattern = "*.yaml")
        )

        val plan = InjectionExecutionPlanner.planFirstMatchingRule(
            rules = rules,
            input = RuleMatchInput(
                valueText = "sql:select 1",
                fileName = "queries.yaml",
                fullPath = "/repo/queries.yaml"
            ),
            caseInsensitivePrefix = false,
            injectAllOccurrences = false,
            isLanguageSupported = { true }
        )

        assertNotNull(plan)
        assertEquals("SQL", plan.rule.languageId)
    }

    @Test
    fun `planner skips invalid language ids`() {
        val rules = listOf(
            InjectionRule(prefix = "sql:", languageId = "DefinitelyMissingLanguage", filePattern = "*.yaml"),
            InjectionRule(prefix = "sql:", languageId = "SQL", filePattern = "*.yaml")
        )

        val plan = InjectionExecutionPlanner.planFirstMatchingRule(
            rules = rules,
            input = RuleMatchInput(
                valueText = "sql:select 1",
                fileName = "queries.yaml",
                fullPath = "/repo/queries.yaml"
            ),
            caseInsensitivePrefix = false,
            injectAllOccurrences = false,
            isLanguageSupported = { languageId -> languageId == "SQL" }
        )

        assertNotNull(plan)
        assertEquals("SQL", plan.rule.languageId)
    }
}
