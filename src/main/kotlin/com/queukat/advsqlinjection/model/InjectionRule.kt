package com.queukat.advsqlinjection.model

enum class RuleScope {
    FILE_NAME_ONLY,
    PATH_AWARE
}

enum class RuleTargetType {
    VALUE_STARTS_WITH_PREFIX,
    VALUE_CONTAINS_PREFIX
}

data class InjectionRule(
    var enabled: Boolean = true,
    var prefix: String = "",
    var languageId: String = "SQL",
    var filePattern: String = "*",
    var pathPattern: String = "",
    var scope: RuleScope = RuleScope.FILE_NAME_ONLY,
    var targetType: RuleTargetType = RuleTargetType.VALUE_STARTS_WITH_PREFIX
) {
    fun normalized(): InjectionRule =
        copy(
            prefix = prefix.trim(),
            languageId = languageId.trim(),
            filePattern = filePattern.trim().ifEmpty { "*" },
            pathPattern = normalizePath(pathPattern.trim())
        )

    companion object {
        fun exampleSqlRule(): InjectionRule =
            InjectionRule(
                prefix = "sql:",
                languageId = "SQL",
                filePattern = "*.yaml",
                pathPattern = "config/**/*.yaml",
                scope = RuleScope.PATH_AWARE,
                targetType = RuleTargetType.VALUE_STARTS_WITH_PREFIX
            )

        fun normalizePath(value: String): String = value.replace('\\', '/')
    }
}
