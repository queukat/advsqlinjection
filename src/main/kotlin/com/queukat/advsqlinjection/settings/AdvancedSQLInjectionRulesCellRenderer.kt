package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.queukat.advsqlinjection.model.RuleScope
import com.queukat.advsqlinjection.model.RuleTargetType
import com.intellij.lang.Language
import com.intellij.ui.JBColor
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

internal class AdvancedSQLInjectionRulesCellRenderer : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable?,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        toolTipText = null
        foreground = if (isSelected) table?.selectionForeground else table?.foreground

        text = when (value) {
            is RuleScope -> when (value) {
                RuleScope.FILE_NAME_ONLY ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.scope.fileNameOnly")

                RuleScope.PATH_AWARE ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.scope.pathAware")
            }

            is RuleTargetType -> when (value) {
                RuleTargetType.VALUE_STARTS_WITH_PREFIX ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.targetType.startsWith")

                RuleTargetType.VALUE_CONTAINS_PREFIX ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.targetType.contains")
            }

            is String -> renderString(value, column, isSelected)
            else -> value?.toString().orEmpty()
        }
        return component
    }

    private fun renderString(value: String, column: Int, isSelected: Boolean): String {
        if (column != LANGUAGE_COLUMN || value.isBlank() || Language.findLanguageByID(value) != null) {
            return value
        }

        toolTipText = AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.languageUnavailableTooltip", value)
        if (!isSelected) {
            foreground = JBColor.RED
        }
        return AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.languageUnavailableInTable", value)
    }

    private companion object {
        const val LANGUAGE_COLUMN = 2
    }
}
