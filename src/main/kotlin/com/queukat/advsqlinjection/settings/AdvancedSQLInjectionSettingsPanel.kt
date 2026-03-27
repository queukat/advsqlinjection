package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.injection.InjectionRuleMatcher
import com.queukat.advsqlinjection.injection.RuleMatchInput
import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.queukat.advsqlinjection.model.InjectionRule
import com.queukat.advsqlinjection.model.RuleScope
import com.queukat.advsqlinjection.model.RuleTargetType
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.AbstractTableModel

class AdvancedSQLInjectionSettingsPanel(private val project: Project?) {

    private val rulesTableModel = RulesTableModel()
    private val rulesTable = JTable(rulesTableModel).apply {
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        fillsViewportHeight = true
        autoCreateRowSorter = false
    }

    private val injectionEnabledCheck = com.intellij.ui.components.JBCheckBox(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.enableInjection")
    )
    private val injectAllOccurrencesCheck = com.intellij.ui.components.JBCheckBox(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.injectAllOccurrences")
    )
    private val caseInsensitivePrefixCheck = com.intellij.ui.components.JBCheckBox(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.caseInsensitivePrefix")
    )

    private val addExampleButton = JButton(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.addExampleButton")
    )
    private val previewCurrentFileButton = JButton(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewCurrentFileButton")
    )
    private val openReadmeButton = JButton(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.openReadmeButton")
    )
    private val emptyStateLabel = JBLabel(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.emptyState")
    ).apply {
        border = JBUI.Borders.empty(4, 0, 0, 0)
    }

    private val rulesPanel: JComponent = JPanel(BorderLayout()).apply {
        val decorator = ToolbarDecorator.createDecorator(rulesTable)
            .setAddAction { openRuleDialog(null)?.let(::appendRule) }
            .setEditAction { editSelectedRule() }
            .setRemoveAction { removeSelectedRule() }
            .setMoveUpAction { moveSelectedRuleUp() }
            .setMoveDownAction { moveSelectedRuleDown() }
        add(decorator.createPanel(), BorderLayout.CENTER)
    }

    val panel: DialogPanel = panel {
        group(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.settingsTitle")) {
            row { cell(injectionEnabledCheck) }
            row { cell(injectAllOccurrencesCheck) }
            row { cell(caseInsensitivePrefixCheck) }
        }

        group(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.rulesGroupTitle")) {
            row {
                cell(rulesPanel).align(AlignX.FILL).resizableColumn()
            }.layout(RowLayout.PARENT_GRID)
            row { cell(emptyStateLabel) }
            row {
                cell(addExampleButton)
                cell(previewCurrentFileButton)
                cell(openReadmeButton)
            }
            row {
                text(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.rulesHelp"))
            }
        }
    }.apply {
        border = JBUI.Borders.empty(8)
    }

    init {
        configureRulesTable()
        initButtons()
        updateEmptyState()
    }

    fun isModified(state: AdvancedSQLInjectionSettingsState.State): Boolean =
        injectionEnabledCheck.isSelected != state.sqlInjectionEnabled ||
            injectAllOccurrencesCheck.isSelected != state.injectAllOccurrences ||
            caseInsensitivePrefixCheck.isSelected != state.caseInsensitivePrefix ||
            rulesTableModel.snapshot() != state.rules

    fun apply(state: AdvancedSQLInjectionSettingsState.State) {
        state.sqlInjectionEnabled = injectionEnabledCheck.isSelected
        state.injectAllOccurrences = injectAllOccurrencesCheck.isSelected
        state.caseInsensitivePrefix = caseInsensitivePrefixCheck.isSelected
        state.rules = rulesTableModel.snapshot().toMutableList()
        state.prefixLanguagePatterns = mutableListOf()
    }

    fun reset(state: AdvancedSQLInjectionSettingsState.State) {
        injectionEnabledCheck.isSelected = state.sqlInjectionEnabled
        injectAllOccurrencesCheck.isSelected = state.injectAllOccurrences
        caseInsensitivePrefixCheck.isSelected = state.caseInsensitivePrefix
        rulesTableModel.setRules(state.rules)
        updateEmptyState()
    }

    private fun configureRulesTable() {
        rulesTable.columnModel.getColumn(0).preferredWidth = 60
        rulesTable.columnModel.getColumn(1).preferredWidth = 140
        rulesTable.columnModel.getColumn(2).preferredWidth = 120
        rulesTable.columnModel.getColumn(3).preferredWidth = 120
        rulesTable.columnModel.getColumn(4).preferredWidth = 180
        rulesTable.columnModel.getColumn(5).preferredWidth = 100
        rulesTable.columnModel.getColumn(6).preferredWidth = 150
        rulesTable.setDefaultRenderer(Any::class.java, RulesCellRenderer())
        rulesTable.setDefaultRenderer(
            java.lang.Boolean::class.java,
            rulesTable.getDefaultRenderer(java.lang.Boolean::class.java)
        )
        rulesTable.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(event: java.awt.event.MouseEvent) {
                if (event.clickCount == 2 && rulesTable.selectedRow >= 0) {
                    editSelectedRule()
                }
            }
        })
    }

    private fun initButtons() {
        addExampleButton.addActionListener {
            openRuleDialog(InjectionRule.exampleSqlRule())?.let(::appendRule)
        }
        previewCurrentFileButton.addActionListener {
            previewSelectedRuleAgainstCurrentFile()
        }
        openReadmeButton.addActionListener {
            if (!AdvancedSQLInjectionHelp.openReadme(project)) {
                Messages.showWarningDialog(
                    panel,
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.readmeMissing"),
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.helpDialogTitle")
                )
            }
        }
    }

    private fun appendRule(rule: InjectionRule) {
        val rowIndex = rulesTableModel.addRule(rule)
        selectRow(rowIndex)
        updateEmptyState()
    }

    private fun editSelectedRule() {
        val selectedRow = rulesTable.selectedRow
        if (selectedRow < 0) {
            return
        }

        val rule = rulesTableModel.ruleAt(selectedRow) ?: return
        val editedRule = openRuleDialog(rule)
        if (editedRule != null) {
            rulesTableModel.updateRule(selectedRow, editedRule)
            selectRow(selectedRow)
            updateEmptyState()
        }
    }

    private fun removeSelectedRule() {
        val selectedRow = rulesTable.selectedRow
        if (selectedRow < 0) {
            return
        }

        rulesTableModel.removeRule(selectedRow)
        if (rulesTableModel.rowCount > 0) {
            selectRow(selectedRow.coerceAtMost(rulesTableModel.rowCount - 1))
        }
        updateEmptyState()
    }

    private fun moveSelectedRuleUp() {
        val selectedRow = rulesTable.selectedRow
        if (selectedRow <= 0) {
            return
        }

        val newIndex = rulesTableModel.moveUp(selectedRow)
        selectRow(newIndex)
    }

    private fun moveSelectedRuleDown() {
        val selectedRow = rulesTable.selectedRow
        if (selectedRow < 0 || selectedRow >= rulesTableModel.rowCount - 1) {
            return
        }

        val newIndex = rulesTableModel.moveDown(selectedRow)
        selectRow(newIndex)
    }

    private fun openRuleDialog(seedRule: InjectionRule?): InjectionRule? {
        val dialog = AdvancedSQLInjectionRuleDialog(
            project = project,
            initialRule = seedRule?.copy() ?: InjectionRule.exampleSqlRule(),
            isEditMode = seedRule != null
        )
        return if (dialog.showAndGet()) dialog.buildRule() else null
    }

    private fun previewSelectedRuleAgainstCurrentFile() {
        val project = project ?: run {
            showPreviewMessage(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewNoProject"))
            return
        }
        val selectedRule = rulesTableModel.ruleAt(rulesTable.selectedRow) ?: run {
            showPreviewMessage(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewSelectRule"))
            return
        }

        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: run {
            showPreviewMessage(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewNoEditor"))
            return
        }
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: run {
            showPreviewMessage(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewNoEditor"))
            return
        }
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile) ?: run {
            showPreviewMessage(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewNoEditor"))
            return
        }

        val fullPath = InjectionRuleMatcher.normalizePath(virtualFile.path)
        val relativePath = InjectionRuleMatcher.toRelativePath(project.basePath, virtualFile.path)
        val fileMatch = InjectionRuleMatcher.matchesFile(
            selectedRule,
            RuleMatchInput(valueText = "", fileName = virtualFile.name, fullPath = fullPath, relativePath = relativePath)
        )

        val hosts = PsiTreeUtil.collectElementsOfType(psiFile, PsiLanguageInjectionHost::class.java)
        var matchedHosts = 0
        var matchedSegments = 0
        val previews = mutableListOf<String>()

        hosts.forEach { host ->
            val valueTextRange = ElementManipulators.getValueTextRange(host)
            if (valueTextRange.startOffset >= valueTextRange.endOffset) {
                return@forEach
            }

            val hostText = host.text
            val valueText = hostText.substring(valueTextRange.startOffset, valueTextRange.endOffset)
            val ranges = InjectionRuleMatcher.findRanges(
                rawRule = selectedRule,
                input = RuleMatchInput(
                    valueText = valueText,
                    fileName = virtualFile.name,
                    fullPath = fullPath,
                    relativePath = relativePath
                ),
                caseInsensitivePrefix = caseInsensitivePrefixCheck.isSelected,
                injectAllOccurrences = injectAllOccurrencesCheck.isSelected
            )

            if (ranges.isNotEmpty()) {
                matchedHosts++
                matchedSegments += ranges.size
                if (previews.size < 3) {
                    previews += valueText.take(120).replace('\n', ' ')
                }
            }
        }

        val message = buildString {
            appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewFileHeader", virtualFile.path))
            appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewFileMatch", fileMatch))
            appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewHostMatchCount", matchedHosts))
            appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewSegmentMatchCount", matchedSegments))
            if (previews.isNotEmpty()) {
                appendLine()
                appendLine(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewExamplesHeader"))
                previews.forEach { preview ->
                    appendLine("- $preview")
                }
            }
        }

        showPreviewMessage(message)
    }

    private fun showPreviewMessage(message: String) {
        Messages.showInfoMessage(
            panel,
            message,
            AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewDialogTitle")
        )
    }

    private fun selectRow(index: Int) {
        if (index in 0 until rulesTableModel.rowCount) {
            rulesTable.selectionModel.setSelectionInterval(index, index)
        }
    }

    private fun updateEmptyState() {
        emptyStateLabel.isVisible = rulesTableModel.rowCount == 0
    }

    private inner class RulesCellRenderer : javax.swing.table.DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): java.awt.Component {
            val component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
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

                else -> value?.toString().orEmpty()
            }
            return component
        }
    }

    private class RulesTableModel : AbstractTableModel() {
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
}
