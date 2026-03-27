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
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import javax.swing.ComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent

private data class LanguageChoice(
    val id: String,
    val displayName: String
) {
    override fun toString(): String = "$displayName ($id)"
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
        renderer = SimpleListCellRenderer.create { label, value, _ ->
            label.text = when (value) {
                RuleScope.FILE_NAME_ONLY ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.scope.fileNameOnly")

                RuleScope.PATH_AWARE ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.scope.pathAware")

                null -> ""
            }
        }
    }
    private val targetTypeCombo = JComboBox(RuleTargetType.values()).apply {
        selectedItem = initialRule.targetType
        renderer = SimpleListCellRenderer.create { label, value, _ ->
            label.text = when (value) {
                RuleTargetType.VALUE_STARTS_WITH_PREFIX ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.targetType.startsWith")

                RuleTargetType.VALUE_CONTAINS_PREFIX ->
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.targetType.contains")

                null -> ""
            }
        }
    }

    private val languageChoices: List<LanguageChoice> = Language.getRegisteredLanguages()
        .mapNotNull { language ->
            language.id.takeIf { it.isNotBlank() }?.let {
                LanguageChoice(id = it, displayName = language.displayName.ifBlank { it })
            }
        }
        .distinctBy(LanguageChoice::id)
        .sortedWith(compareBy(LanguageChoice::displayName, LanguageChoice::id))

    private val languageCombo = JComboBox(languageChoices.toTypedArray()).apply {
        renderer = SimpleListCellRenderer.create { label, value, _ ->
            label.text = value?.toString().orEmpty()
        }
        selectLanguage(initialRule.languageId)
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

    private fun selectLanguage(languageId: String) {
        val model: ComboBoxModel<LanguageChoice> = languageCombo.model
        for (index in 0 until model.size) {
            val choice = model.getElementAt(index)
            if (choice.id == languageId) {
                languageCombo.selectedItem = choice
                return
            }
        }
        languageCombo.selectedItem = model.getElementAt(0)
    }
}
