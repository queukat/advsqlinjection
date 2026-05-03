package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.queukat.advsqlinjection.model.InjectionRule
import javax.swing.table.AbstractTableModel

internal class AdvancedSQLInjectionRulesTableModel : AbstractTableModel() {
    private val columns = listOf(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.column.enabled"),
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.column.prefix"),
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.column.language"),
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.column.filePattern"),
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.column.pathPattern"),
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.column.scope"),
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.column.targetType")
    )
    private val rules = mutableListOf<InjectionRule>()

    override fun getRowCount(): Int = rules.size

    override fun getColumnCount(): Int = columns.size

    override fun getColumnName(column: Int): String = columns[column]

    override fun getColumnClass(columnIndex: Int): Class<*> =
        if (columnIndex == 0) java.lang.Boolean::class.java else Any::class.java

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == 0

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val rule = rules[rowIndex]
        return when (columnIndex) {
            0 -> rule.enabled
            1 -> rule.prefix
            2 -> rule.languageId
            3 -> rule.filePattern
            4 -> rule.pathPattern.ifBlank { "-" }
            5 -> rule.scope
            6 -> rule.targetType
            else -> ""
        }
    }

    override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
        if (columnIndex == 0 && rowIndex in rules.indices) {
            rules[rowIndex] = rules[rowIndex].copy(enabled = aValue as? Boolean ?: false)
            fireTableRowsUpdated(rowIndex, rowIndex)
        }
    }

    fun setRules(newRules: List<InjectionRule>) {
        rules.clear()
        rules.addAll(newRules.map(InjectionRule::normalized))
        fireTableDataChanged()
    }

    fun addRule(rule: InjectionRule): Int {
        val newIndex = rules.size
        rules += rule.normalized()
        fireTableRowsInserted(newIndex, newIndex)
        return newIndex
    }

    fun duplicateRule(index: Int): Int? {
        val rule = rules.getOrNull(index)?.copy() ?: return null
        val newIndex = index + 1
        rules.add(newIndex, rule)
        fireTableRowsInserted(newIndex, newIndex)
        return newIndex
    }

    fun updateRule(index: Int, rule: InjectionRule) {
        if (index !in rules.indices) {
            return
        }
        rules[index] = rule.normalized()
        fireTableRowsUpdated(index, index)
    }

    fun removeRule(index: Int) {
        if (index !in rules.indices) {
            return
        }
        rules.removeAt(index)
        fireTableRowsDeleted(index, index)
    }

    fun moveUp(index: Int): Int {
        if (index !in rules.indices || index == 0) {
            return index
        }
        val rule = rules.removeAt(index)
        val newIndex = index - 1
        rules.add(newIndex, rule)
        fireTableDataChanged()
        return newIndex
    }

    fun moveDown(index: Int): Int {
        if (index !in rules.indices || index == rules.lastIndex) {
            return index
        }
        val rule = rules.removeAt(index)
        val newIndex = index + 1
        rules.add(newIndex, rule)
        fireTableDataChanged()
        return newIndex
    }

    fun ruleAt(index: Int): InjectionRule? = rules.getOrNull(index)?.copy()

    fun snapshot(): List<InjectionRule> = rules.map(InjectionRule::copy)
}
