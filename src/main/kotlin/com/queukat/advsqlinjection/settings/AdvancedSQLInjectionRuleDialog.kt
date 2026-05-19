package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.queukat.advsqlinjection.model.InjectionRule
import com.queukat.advsqlinjection.model.RuleScope
import com.queukat.advsqlinjection.model.RuleTargetType
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import java.awt.Component
import javax.swing.ComboBoxModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.ListCellRenderer

private data class LanguageChoice(
    val id: String,
    val displayName: String,
    val unavailable: Boolean = false
) {
    override fun toString(): String =
        when {
            id.isBlank() -> displayName
            unavailable -> AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.languageUnavailableInTable", id)
            else -> "$displayName ($id)"
        }
}

class AdvancedSQLInjectionRuleDialog(
    project: Project?,
    private val initialRule: InjectionRule,
    private val isEditMode: Boolean
) : DialogWrapper(project) {

    private val enabledCheck = JBCheckBox(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.ruleEnabled")
    ).apply {
        isSelected = initialRule.enabled
    }

    private val prefixField = JBTextField(initialRule.prefix, 24)
    private val filePatternField = JBTextField(initialRule.filePattern.ifBlank { "*" }, 24)
    private val pathPatternField = JBTextField(initialRule.pathPattern, 24)
    private val scopeCombo = JComboBox(RuleScope.values()).apply {
        selectedItem = initialRule.scope
        renderer = textListCellRenderer { value ->
            when (value) {
                RuleScope.FILE_NAME_ONLY ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.scope.fileNameOnly")

                RuleScope.PATH_AWARE ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.scope.pathAware")

                null -> ""
                else -> value.toString()
            }
        }
    }
    private val targetTypeCombo = JComboBox(RuleTargetType.values()).apply {
        selectedItem = initialRule.targetType
        renderer = textListCellRenderer { value ->
            when (value) {
                RuleTargetType.VALUE_STARTS_WITH_PREFIX ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.targetType.startsWith")

                RuleTargetType.VALUE_CONTAINS_PREFIX ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.targetType.contains")

                null -> ""
                else -> value.toString()
            }
        }
    }

    private val languageChoices: List<LanguageChoice> = buildList {
        val initialLanguageId = initialRule.languageId.trim()
        val registeredChoices = Language.getRegisteredLanguages()
            .mapNotNull { language ->
                language.id.takeIf { it.isNotBlank() }?.let {
                    LanguageChoice(id = it, displayName = language.displayName.ifBlank { it })
                }
            }
            .distinctBy(LanguageChoice::id)
            .sortedWith(compareBy(LanguageChoice::displayName, LanguageChoice::id))

        add(LanguageChoice(id = "", displayName = AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.languagePlaceholder")))
        if (initialLanguageId.isNotBlank() && registeredChoices.none { it.id == initialLanguageId }) {
            add(LanguageChoice(id = initialLanguageId, displayName = initialLanguageId, unavailable = true))
        }
        addAll(registeredChoices)
    }

    private val languageCombo = JComboBox(languageChoices.toTypedArray()).apply {
        renderer = textListCellRenderer { value -> value?.toString().orEmpty() }
        selectLanguage(this, initialRule.languageId)
    }

    private val contentPanel: DialogPanel = panel {
        row { cell(enabledCheck) }
        row(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.prefixLabel")) {
            cell(prefixField).align(AlignX.FILL)
        }
        row(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.languageLabel")) {
            cell(languageCombo).align(AlignX.FILL)
        }
        row(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.filePatternLabel")) {
            cell(filePatternField).align(AlignX.FILL)
        }
        row(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.pathPatternLabel")) {
            cell(pathPatternField).align(AlignX.FILL)
        }
        row(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.scopeLabel")) {
            cell(scopeCombo).align(AlignX.FILL)
        }
        row(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.targetTypeLabel")) {
            cell(targetTypeCombo).align(AlignX.FILL)
        }
        row {
            text(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.ruleDialogHint"))
        }
    }.apply {
        border = JBUI.Borders.empty(8)
    }

    init {
        title = AdvancedSqlInjectionBundle.message(
            if (isEditMode) {
                "msg.AdvancedSqlInjection.editRuleDialogTitle"
            } else {
                "msg.AdvancedSqlInjection.newRuleDialogTitle"
            }
        )
        init()
    }

    override fun createCenterPanel(): JComponent = contentPanel

    override fun doValidate(): ValidationInfo? {
        if (prefixField.text.trim().isEmpty()) {
            return ValidationInfo(
                AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.validation.prefix"),
                prefixField
            )
        }
        if (selectedLanguageId().isEmpty()) {
            return ValidationInfo(
                AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.validation.language"),
                languageCombo
            )
        }
        if (filePatternField.text.trim().isEmpty()) {
            return ValidationInfo(
                AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.validation.filePattern"),
                filePatternField
            )
        }
        return null
    }

    fun buildRule(): InjectionRule =
        InjectionRule(
            enabled = enabledCheck.isSelected,
            prefix = prefixField.text.trim(),
            languageId = selectedLanguageId(),
            filePattern = filePatternField.text.trim().ifBlank { "*" },
            pathPattern = pathPatternField.text.trim(),
            scope = scopeCombo.selectedItem as? RuleScope ?: RuleScope.FILE_NAME_ONLY,
            targetType = targetTypeCombo.selectedItem as? RuleTargetType ?: RuleTargetType.VALUE_STARTS_WITH_PREFIX
        ).normalized()

    private fun selectedLanguageId(): String =
        (languageCombo.selectedItem as? LanguageChoice)?.id.orEmpty()

    private fun textListCellRenderer(textProvider: (Any?) -> String): ListCellRenderer<Any?> =
        object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>?,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean
            ): Component {
                val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                text = textProvider(value)
                return component
            }
        }

    private fun selectLanguage(comboBox: JComboBox<LanguageChoice>, languageId: String) {
        val model: ComboBoxModel<LanguageChoice> = comboBox.model
        for (index in 0 until model.size) {
            val choice = model.getElementAt(index)
            if (choice.id == languageId) {
                comboBox.selectedItem = choice
                return
            }
        }
        if (model.size > 0) {
            comboBox.selectedItem = model.getElementAt(0)
        }
    }
}
