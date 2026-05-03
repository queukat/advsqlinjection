package com.queukat.advsqlinjection.injection

import com.queukat.advsqlinjection.model.InjectionRule
import com.intellij.lang.Language

data class PlannedInjection(
    val rule: InjectionRule,
    val ranges: List<RelativeMatchRange>
)

data class ExecutableInjectionRule(
    val preparedRule: PreparedInjectionRule,
    val language: Language
)

data class ExecutablePlannedInjection(
    val rule: InjectionRule,
    val language: Language,
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

            val preparedRule = InjectionRuleMatcher.prepareRule(rule)
            val ranges = InjectionRuleMatcher.findRanges(
                preparedRule = preparedRule,
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

    fun planFirstMatchingExecutableRule(
        rules: List<ExecutableInjectionRule>,
        input: RuleMatchInput,
        caseInsensitivePrefix: Boolean,
        injectAllOccurrences: Boolean
    ): ExecutablePlannedInjection? {
        for (candidate in rules) {
            val ranges = InjectionRuleMatcher.findRanges(
                preparedRule = candidate.preparedRule,
                input = input,
                caseInsensitivePrefix = caseInsensitivePrefix,
                injectAllOccurrences = injectAllOccurrences
            )
            if (ranges.isNotEmpty()) {
                return ExecutablePlannedInjection(
                    rule = candidate.preparedRule.rule,
                    language = candidate.language,
                    ranges = ranges
                )
            }
        }

        return null
    }
}
