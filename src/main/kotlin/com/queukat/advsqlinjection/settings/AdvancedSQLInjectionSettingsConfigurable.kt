package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class AdvancedSQLInjectionSettingsConfigurable(
    private val project: Project
) : Configurable {

    private var settingsPanel: AdvancedSQLInjectionSettingsPanel? = null

    override fun getDisplayName(): String =
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.settingsTitle")

    override fun createComponent(): JComponent {
        val panel = AdvancedSQLInjectionSettingsPanel(project)
        val state = AdvancedSQLInjectionSettingsState.getInstance(project).state
        panel.reset(state)
        settingsPanel = panel
        return panel.panel
    }

    override fun isModified(): Boolean {
        val state = AdvancedSQLInjectionSettingsState.getInstance(project).state
        return settingsPanel?.isModified(state) ?: false
    }

    override fun apply() {
        val service = AdvancedSQLInjectionSettingsState.getInstance(project)
        val state = service.state
        settingsPanel?.apply(state)
    }

    override fun reset() {
        val state = AdvancedSQLInjectionSettingsState.getInstance(project).state
        settingsPanel?.reset(state)
    }

    override fun disposeUIResources() {
        settingsPanel = null
    }

    override fun getHelpTopic(): String? = null
}
