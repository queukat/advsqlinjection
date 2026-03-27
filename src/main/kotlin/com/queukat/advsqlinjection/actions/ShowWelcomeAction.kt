package com.queukat.advsqlinjection.actions

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.queukat.advsqlinjection.settings.AdvancedSQLInjectionHelp
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class ShowWelcomeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        if (AdvancedSQLInjectionHelp.openReadme(project)) {
            return
        }

        Messages.showWarningDialog(
            project,
            AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.readmeMissing"),
            AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.welcomeAction.title")
        )
    }
}
