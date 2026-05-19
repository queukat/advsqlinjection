package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import javax.swing.JComponent

class AdvancedSQLInjectionSettingsConfigurable(
    private val project: Project
) : SearchableConfigurable {

    private var settingsPanel: AdvancedSQLInjectionSettingsPanel? = null

    override fun getDisplayName(): String =
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.settingsTitle")

    override fun getId(): String = "com.queukat.advsqlinjection.settings"

    override fun enableSearch(option: String?): Runnable? = null

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
        val shouldRefreshHighlighting = settingsPanel?.isModified(state) == true
        settingsPanel?.apply(state)
        if (shouldRefreshHighlighting) {
            DaemonCodeAnalyzer.getInstance(project).settingsChanged()
        }
    }

    override fun reset() {
        val state = AdvancedSQLInjectionSettingsState.getInstance(project).state
        settingsPanel?.reset(state)
    }

    override fun disposeUIResources() {
        settingsPanel?.let(Disposer::dispose)
        settingsPanel = null
    }

    override fun getHelpTopic(): String? = null
}
