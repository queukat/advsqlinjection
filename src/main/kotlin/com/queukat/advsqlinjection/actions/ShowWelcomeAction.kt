package com.queukat.advsqlinjection.actions

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages

class ShowWelcomeAction : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project
        val message = AdvancedSqlInjectionBundle.message(
            "msg.AdvancedSqlInjection.welcomeAction.text"
        )
        val title = AdvancedSqlInjectionBundle.message(
            "msg.AdvancedSqlInjection.welcomeAction.title"
        )
        Messages.showMessageDialog(
            project,
            message,
            title,
            Messages.getInformationIcon()
        )
    }
}
