package com.queukat.advsqlinjection.injection

import com.queukat.advsqlinjection.model.InjectionRule
import com.queukat.advsqlinjection.model.RuleScope
import com.queukat.advsqlinjection.model.RuleTargetType

data class RuleMatchInput(
    val valueText: String,
    val fileName: String,
    val fullPath: String,
    val relativePath: String? = null,
    val structuralPrefixes: List<String> = emptyList()
)

data class RelativeMatchRange(
    val startOffset: Int,
    val endOffset: Int
)

data class PreparedInjectionRule(
    val rule: InjectionRule,
    val filePatternRegex: Regex,
    val pathPatternRegex: Regex
)

object InjectionRuleMatcher {
    fun prepareRule(rawRule: InjectionRule): PreparedInjectionRule {
        val rule = rawRule.normalized()
        val pathPattern = rule.pathPattern.ifBlank { "**" }
        return PreparedInjectionRule(
            rule = rule,
            filePatternRegex = globToRegex(rule.filePattern),
            pathPatternRegex = globToRegex(pathPattern)
        )
    }

    fun findRanges(
        rawRule: InjectionRule,
        input: RuleMatchInput,
        caseInsensitivePrefix: Boolean,
        injectAllOccurrences: Boolean
    ): List<RelativeMatchRange> {
        return findRanges(
            preparedRule = prepareRule(rawRule),
            input = input,
            caseInsensitivePrefix = caseInsensitivePrefix,
            injectAllOccurrences = injectAllOccurrences
        )
    }

    fun findRanges(
        preparedRule: PreparedInjectionRule,
        input: RuleMatchInput,
        caseInsensitivePrefix: Boolean,
        injectAllOccurrences: Boolean
    ): List<RelativeMatchRange> {
        val rule = preparedRule.rule
        if (!rule.enabled || rule.prefix.isEmpty() || rule.languageId.isEmpty()) {
            return emptyList()
        }

        if (!matchesFile(preparedRule, input)) {
            return emptyList()
        }

        val occurrenceOffsets = findOccurrenceOffsets(rule, input.valueText, caseInsensitivePrefix)
        if (occurrenceOffsets.isNotEmpty()) {
            val effectiveOffsets = if (injectAllOccurrences) occurrenceOffsets else listOf(occurrenceOffsets.first())
            return effectiveOffsets.mapIndexedNotNull { index, occurrenceOffset ->
                val startOffset = occurrenceOffset + rule.prefix.length
                val endOffset = when {
                    !injectAllOccurrences -> input.valueText.length
                    index + 1 < effectiveOffsets.size -> effectiveOffsets[index + 1]
                    else -> input.valueText.length
                }

                if (startOffset < endOffset) {
                    RelativeMatchRange(startOffset, endOffset)
                } else {
                    null
                }
            }
        }

        return findStructuralPrefixRange(rule, input, caseInsensitivePrefix)
    }

    fun matchesFile(rawRule: InjectionRule, input: RuleMatchInput): Boolean {
        return matchesFile(prepareRule(rawRule), input)
    }

    fun matchesFile(preparedRule: PreparedInjectionRule, input: RuleMatchInput): Boolean {
        if (!preparedRule.filePatternRegex.matches(input.fileName)) {
            return false
        }

        val rule = preparedRule.rule
        if (rule.scope == RuleScope.FILE_NAME_ONLY) {
            return true
        }

        return buildList {
            add(input.fullPath)
            input.relativePath?.let(::add)
        }.distinct().any { candidate ->
            preparedRule.pathPatternRegex.matches(InjectionRule.normalizePath(candidate))
        }
    }

    fun normalizePath(path: String): String = InjectionRule.normalizePath(path)

    fun toRelativePath(projectBasePath: String?, fullPath: String): String? {
        val base = projectBasePath?.takeIf { it.isNotBlank() }?.let(::normalizePath) ?: return null
        val candidate = normalizePath(fullPath)
        return when {
            candidate == base -> ""
            candidate.startsWith("$base/") -> candidate.removePrefix("$base/")
            else -> null
        }
    }

    private fun findOccurrenceOffsets(
        rule: InjectionRule,
        valueText: String,
        caseInsensitivePrefix: Boolean
    ): List<Int> {
        return when (rule.targetType) {
            RuleTargetType.VALUE_STARTS_WITH_PREFIX ->
                if (valueText.startsWith(rule.prefix, ignoreCase = caseInsensitivePrefix)) listOf(0) else emptyList()

            RuleTargetType.VALUE_CONTAINS_PREFIX -> {
                val offsets = mutableListOf<Int>()
                var searchStart = 0

                while (searchStart < valueText.length) {
                    val occurrenceOffset = valueText.indexOf(
                        rule.prefix,
                        startIndex = searchStart,
                        ignoreCase = caseInsensitivePrefix
                    )
                    if (occurrenceOffset < 0) {
                        break
                    }
                    offsets += occurrenceOffset
                    searchStart = occurrenceOffset + rule.prefix.length.coerceAtLeast(1)
                }

                offsets
            }
        }
    }

    private fun findStructuralPrefixRange(
        rule: InjectionRule,
        input: RuleMatchInput,
        caseInsensitivePrefix: Boolean
    ): List<RelativeMatchRange> {
        if (input.valueText.isEmpty()) {
            return emptyList()
        }

        val matches = input.structuralPrefixes.any { structuralPrefix ->
            when (rule.targetType) {
                RuleTargetType.VALUE_STARTS_WITH_PREFIX ->
                    structuralPrefix.startsWith(rule.prefix, ignoreCase = caseInsensitivePrefix)

                RuleTargetType.VALUE_CONTAINS_PREFIX ->
                    structuralPrefix.indexOf(rule.prefix, ignoreCase = caseInsensitivePrefix) >= 0
            }
        }

        return if (matches) {
            listOf(RelativeMatchRange(0, input.valueText.length))
        } else {
            emptyList()
        }
    }

    private fun globToRegex(glob: String): Regex {
        val builder = StringBuilder(glob.length * 2)
        var index = 0
        while (index < glob.length) {
            index = appendGlobToken(glob, index, builder)
        }
        return Regex("^$builder$")
    }

    private fun appendGlobToken(glob: String, index: Int, builder: StringBuilder): Int =
        if (glob[index] == '*') {
            appendWildcardToken(glob, index, builder)
        } else {
            appendLiteralToken(glob[index], builder)
            index + 1
        }

    private fun appendWildcardToken(glob: String, index: Int, builder: StringBuilder): Int {
        if (glob.getOrNull(index + 1) != '*') {
            builder.append("[^/]*")
            return index + 1
        }

        return if (glob.getOrNull(index + 2) == '/') {
            builder.append("(?:.*/)?")
            index + 3
        } else {
            builder.append(".*")
            index + 2
        }
    }

    private fun appendLiteralToken(current: Char, builder: StringBuilder) {
        when (current) {
            '?', '.', '(', ')', '[', ']', '{', '}', '+',
            '$', '^', '|', '\\' -> builder.append('\\').append(current)

            else -> builder.append(current)
        }
    }
}
