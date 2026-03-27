package com.queukat.advsqlinjection.model

object LegacyRuleMigration {
    fun migrate(rawRules: List<String>): List<InjectionRule> =
        rawRules.mapNotNull(::migrateRule)

    fun migrateRule(rawRule: String): InjectionRule? {
        val parts = rawRule.split("=", limit = 3)
        if (parts.size != 3) {
            return null
        }

        val prefix = parts[0].trim()
        val languageId = parts[1].trim()
        val filePattern = parts[2].trim().ifEmpty { "*" }
        if (prefix.isEmpty() || languageId.isEmpty()) {
            return null
        }

        return InjectionRule(
            enabled = true,
            prefix = prefix,
            languageId = languageId,
            filePattern = filePattern,
            pathPattern = "",
            scope = RuleScope.FILE_NAME_ONLY,
            targetType = RuleTargetType.VALUE_CONTAINS_PREFIX
        )
    }
}
