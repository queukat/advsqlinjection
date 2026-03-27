package com.queukat.advsqlinjection.injection

import com.queukat.advsqlinjection.model.InjectionRule

data class PlannedInjection(
    val rule: InjectionRule,
    val ranges: List<RelativeMatchRange>
)

object InjectionExecutionPlanner {
    fun planFirstMatchingRule(
        rules: List<InjectionRule>,
        input: RuleMatchInput,
        caseInsensitivePrefix: Boolean,
        injectAllOccurrences: Boolean,
        isLanguageSupported: (String) -> Boolean
    ): PlannedInjection? {
        for (rawRule in rules) {
            val rule = rawRule.normalized()
            if (!rule.enabled || rule.prefix.isEmpty() || rule.languageId.isEmpty()) {
                continue
            }
            if (!isLanguageSupported(rule.languageId)) {
                continue
            }

            val ranges = InjectionRuleMatcher.findRanges(
                rawRule = rule,
                input = input,
                caseInsensitivePrefix = caseInsensitivePrefix,
                injectAllOccurrences = injectAllOccurrences
            )
            if (ranges.isNotEmpty()) {
                return PlannedInjection(rule = rule, ranges = ranges)
            }
        }

        return null
    }
}
