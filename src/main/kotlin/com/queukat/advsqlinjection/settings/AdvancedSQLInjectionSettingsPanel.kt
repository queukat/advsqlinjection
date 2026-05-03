package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.queukat.advsqlinjection.model.InjectionRule
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBLabel
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

class AdvancedSQLInjectionSettingsPanel(private val project: Project?) {

    private val rulesTableModel = AdvancedSQLInjectionRulesTableModel()
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
    private val duplicateRuleButton = JButton(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.duplicateRuleButton")
    )
    private val previewCurrentFileButton = JButton(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewCurrentFileButton")
    )
    private val openSetupGuideButton = JButton(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.openSetupGuideButton")
    )
    private val emptyStateLabel = JBLabel(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.emptyState")
    ).apply {
        border = JBUI.Borders.empty(4, 0, 0, 0)
    }
    private val addRuleButton = JButton(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.addRuleButton")
    )

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
                cell(addRuleButton)
                cell(addExampleButton)
                cell(duplicateRuleButton)
                cell(previewCurrentFileButton)
                cell(openSetupGuideButton)
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
        rulesTable.setDefaultRenderer(Any::class.java, AdvancedSQLInjectionRulesCellRenderer())
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
        addRuleButton.addActionListener {
            openRuleDialog(null)?.let(::appendRule)
        }
        addExampleButton.addActionListener {
            openRuleDialog(InjectionRule.exampleSqlRule())?.let(::appendRule)
        }
        duplicateRuleButton.addActionListener {
            duplicateSelectedRule()
        }
        previewCurrentFileButton.addActionListener {
            previewSelectedRuleAgainstCurrentFile()
        }
        openSetupGuideButton.addActionListener {
            if (!AdvancedSQLInjectionHelp.openSetupGuide(project)) {
                Messages.showWarningDialog(
                    panel,
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.setupGuideMissing"),
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

    private fun duplicateSelectedRule() {
        val selectedRow = rulesTable.selectedRow
        if (selectedRow < 0) {
            return
        }

        val newIndex = rulesTableModel.duplicateRule(selectedRow) ?: return
        selectRow(newIndex)
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
            initialRule = seedRule?.copy() ?: InjectionRule.emptyRule(),
            isEditMode = seedRule != null
        )
        return if (dialog.showAndGet()) dialog.buildRule() else null
    }

    private fun previewSelectedRuleAgainstCurrentFile() {
        val selectedRule = rulesTableModel.ruleAt(rulesTable.selectedRow) ?: run {
            showPreviewMessage(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewSelectRule"))
            return
        }
        showPreviewMessage(
            AdvancedSQLInjectionRulePreview.buildMessage(
                project = project,
                selectedRule = selectedRule,
                caseInsensitivePrefix = caseInsensitivePrefixCheck.isSelected,
                injectAllOccurrences = injectAllOccurrencesCheck.isSelected
            )
        )
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

}
