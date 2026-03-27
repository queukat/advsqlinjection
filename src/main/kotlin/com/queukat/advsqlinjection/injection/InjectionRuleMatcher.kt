package com.queukat.advsqlinjection.injection

import com.queukat.advsqlinjection.model.InjectionRule
import com.queukat.advsqlinjection.model.RuleScope
import com.queukat.advsqlinjection.model.RuleTargetType

data class RuleMatchInput(
    val valueText: String,
    val fileName: String,
    val fullPath: String,
    val relativePath: String? = null
)

data class RelativeMatchRange(
    val startOffset: Int,
    val endOffset: Int
)

object InjectionRuleMatcher {
    fun findRanges(
        rawRule: InjectionRule,
        input: RuleMatchInput,
        caseInsensitivePrefix: Boolean,
        injectAllOccurrences: Boolean
    ): List<RelativeMatchRange> {
        val rule = rawRule.normalized()
        if (!rule.enabled || rule.prefix.isEmpty() || rule.languageId.isEmpty()) {
            return emptyList()
        }

        if (!matchesFile(rule, input)) {
            return emptyList()
        }

        val occurrenceOffsets = findOccurrenceOffsets(rule, input.valueText, caseInsensitivePrefix)
        if (occurrenceOffsets.isEmpty()) {
            return emptyList()
        }

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

    fun matchesFile(rawRule: InjectionRule, input: RuleMatchInput): Boolean {
        val rule = rawRule.normalized()
        if (!globMatches(rule.filePattern, input.fileName)) {
            return false
        }

        if (rule.scope == RuleScope.FILE_NAME_ONLY) {
            return true
        }

        val pathPattern = rule.pathPattern.ifBlank { "**" }
        return buildList {
            add(input.fullPath)
            input.relativePath?.let(::add)
        }.distinct().any { candidate ->
            globMatches(pathPattern, InjectionRule.normalizePath(candidate))
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

    private fun globMatches(glob: String, value: String): Boolean =
        globToRegex(glob).matches(value)

    private fun globToRegex(glob: String): Regex {
        val builder = StringBuilder(glob.length * 2)
        var index = 0
        while (index < glob.length) {
            val current = glob[index]
            if (current == '*') {
                val nextIsWildcard = index + 1 < glob.length && glob[index + 1] == '*'
                if (nextIsWildcard) {
                    builder.append(".*")
                    index++
                } else {
                    builder.append("[^/]*")
                }
            } else {
                when (current) {
                    '?', '.', '(', ')', '[', ']', '{', '}', '+',
                    '$', '^', '|', '\\' -> builder.append('\\').append(current)
                    else -> builder.append(current)
                }
            }
            index++
        }
        return Regex("^$builder$")
    }
}
