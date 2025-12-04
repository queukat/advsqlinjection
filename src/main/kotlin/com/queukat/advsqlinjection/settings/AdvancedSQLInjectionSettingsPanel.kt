package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.intellij.lang.Language
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import javax.swing.*
import javax.swing.event.ListSelectionEvent

class AdvancedSQLInjectionSettingsPanel(private val project: Project?) {

    private val rulesModel = DefaultListModel<String>()
    private val rulesList = JBList(rulesModel).apply {
        visibleRowCount = 6
    }

    private val injectionEnabledCheck =
        JCheckBox(
            AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.enableInjection")
        )
    private val injectAllOccurrencesCheck = JCheckBox(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.injectAllOccurrences")
    )
    private val caseInsensitivePrefixCheck = JCheckBox(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.caseInsensitivePrefix")
    )

    private val addRuleButton = JButton(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.addRuleButton")
    )
    private val removeRuleButton = JButton(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.removeRuleButton")
    ).apply { isEnabled = false }
    private val testInjectionButton = JButton(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.testInjectionButton")
    )

    private val allLangIds: Array<String> =
        Language.getRegisteredLanguages()
            .mapNotNull { it.id }
            .distinct()
            .sorted()
            .toTypedArray()

    val panel: DialogPanel = panel {
        group(
            AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.settingsTitle")
        ) {
            row { cell(injectionEnabledCheck) }
            row { cell(injectAllOccurrencesCheck) }
            row { cell(caseInsensitivePrefixCheck) }
        }

        group(
            AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.prefixLanguagePatternGroup")
        ) {
            row {
                scrollCell(JScrollPane(rulesList).apply {
                    border = JBUI.Borders.empty()
                    minimumSize = JBUI.size(200, 120)
                    preferredSize = JBUI.size(400, 160)
                })
                    .resizableColumn()
                    .align(AlignX.FILL)
            }
            row {
                cell(addRuleButton)
                cell(removeRuleButton)
                cell(testInjectionButton)
            }
            row {
                label(
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.injectionScopeHint")
                )
                    .applyToComponent {
                        font = font.deriveFont(font.size2D - 1f)
                    }
                    .comment("") // чтобы оставить компактным, можно и без comment
            }
        }
    }.apply {
        border = JBUI.Borders.empty(8)
    }

    init {
        initButtons()
    }

    private fun initButtons() {
        addRuleButton.addActionListener {
            val prefixField = JTextField(10)
            val languageCombo = JComboBox(allLangIds)
            val patternField = JTextField("*.yaml", 10)

            val form = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(JLabel(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.prefixLabel")))
                add(prefixField)
                add(JLabel(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.languageLabel")))
                add(languageCombo)
                add(JLabel(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.filePatternLabel")))
                add(patternField)            }

            val result = JOptionPane.showConfirmDialog(
                panel,
                form,
                AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.newRuleDialogTitle"),
                JOptionPane.OK_CANCEL_OPTION
            )


            if (result == JOptionPane.OK_OPTION) {
                val prefix = prefixField.text.trim()
                val lang = (languageCombo.selectedItem as? String)?.trim().orEmpty()
                val pattern = patternField.text.trim()

                if (prefix.isNotEmpty() && lang.isNotEmpty() && pattern.isNotEmpty()) {
                    rulesModel.addElement("$prefix=$lang=$pattern")
                }
            }
        }

        removeRuleButton.addActionListener {
            val index = rulesList.selectedIndex
            if (index >= 0) {
                rulesModel.remove(index)
            }
        }

        rulesList.addListSelectionListener { e: ListSelectionEvent ->
            if (!e.valueIsAdjusting) {
                removeRuleButton.isEnabled = rulesList.selectedIndex >= 0
            }
        }

        testInjectionButton.addActionListener {
            JOptionPane.showMessageDialog(
                panel,
                AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.testInjectionDialogText"),
                AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.testInjectionDialogTitle"),
                JOptionPane.INFORMATION_MESSAGE
            )

        }
    }

    fun isModified(state: AdvancedSQLInjectionSettingsState.State): Boolean =
        injectionEnabledCheck.isSelected != state.sqlInjectionEnabled ||
            injectAllOccurrencesCheck.isSelected != state.injectAllOccurrences ||
            caseInsensitivePrefixCheck.isSelected != state.caseInsensitivePrefix ||
            currentRules() != state.prefixLanguagePatterns

    fun apply(state: AdvancedSQLInjectionSettingsState.State) {
        state.sqlInjectionEnabled = injectionEnabledCheck.isSelected
        state.injectAllOccurrences = injectAllOccurrencesCheck.isSelected
        state.caseInsensitivePrefix = caseInsensitivePrefixCheck.isSelected
        state.prefixLanguagePatterns.apply {
            clear()
            addAll(currentRules())
        }
    }

    fun reset(state: AdvancedSQLInjectionSettingsState.State) {
        injectionEnabledCheck.isSelected = state.sqlInjectionEnabled
        injectAllOccurrencesCheck.isSelected = state.injectAllOccurrences
        caseInsensitivePrefixCheck.isSelected = state.caseInsensitivePrefix

        rulesModel.apply {
            clear()
            state.prefixLanguagePatterns.forEach(::addElement)
        }
    }

    private fun currentRules(): List<String> =
        (0 until rulesModel.size()).map(rulesModel::getElementAt)
}
