package com.queukat.advsqlinjection.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@Service
@State(
    name = "com.queukat.advsqlinjection.settings.AdvancedSQLInjectionSettingsState",
    storages = [Storage("AdvancedSqlInjectionPluginSettings.xml")]
)
class AdvancedSQLInjectionSettingsState :
    PersistentStateComponent<AdvancedSQLInjectionSettingsState.State> {

    class State {
        var sqlInjectionEnabled: Boolean = true
        var prefixLanguagePatterns: MutableList<String> = mutableListOf()
        var injectAllOccurrences: Boolean = false
        var caseInsensitivePrefix: Boolean = false
    }

    private var myState: State = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        fun getInstance(project: Project): AdvancedSQLInjectionSettingsState {
            return project.getService(AdvancedSQLInjectionSettingsState::class.java)
        }
    }
}
