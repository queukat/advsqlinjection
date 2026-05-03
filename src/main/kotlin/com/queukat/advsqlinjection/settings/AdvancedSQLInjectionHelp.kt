package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.nio.charset.StandardCharsets
import javax.swing.JComponent
import javax.swing.JEditorPane
import javax.swing.JPanel

object AdvancedSQLInjectionHelp {
    private const val SETUP_GUIDE_RESOURCE = "help/advanced-language-injection-setup.html"

    fun openSetupGuide(project: Project?): Boolean {
        val html = AdvancedSQLInjectionHelp::class.java.classLoader
            .getResourceAsStream(SETUP_GUIDE_RESOURCE)
            ?.bufferedReader(StandardCharsets.UTF_8)
            ?.use { it.readText() }
            ?: return false

        SetupGuideDialog(project, html).show()
        return true
    }

    private class SetupGuideDialog(
        project: Project?,
        private val html: String
    ) : DialogWrapper(project) {

        init {
            title = AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.helpDialogTitle")
            init()
        }

        override fun createCenterPanel(): JComponent {
            val editorPane = JEditorPane("text/html", html).apply {
                isEditable = false
                putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
                border = JBUI.Borders.empty(12)
            }

            return JPanel(BorderLayout()).apply {
                preferredSize = JBUI.size(640, 500)
                add(JBScrollPane(editorPane), BorderLayout.CENTER)
            }
        }
    }
}
