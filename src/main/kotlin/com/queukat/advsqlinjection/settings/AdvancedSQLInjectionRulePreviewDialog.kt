package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.messages.AdvancedSqlInjectionBundle
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import javax.swing.Action
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTextArea

internal class AdvancedSQLInjectionRulePreviewDialog(
    project: Project?,
    private val result: AdvancedSQLInjectionRulePreview.Result
) : DialogWrapper(project) {

    private val copyAction: Action = object : DialogWrapperAction(
        AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewCopyButton")
    ) {
        override fun doAction(event: ActionEvent?) {
            CopyPasteManager.getInstance().setContents(StringSelection(result.toCopyableText()))
        }
    }

    init {
        title = AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewDialogTitle")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val content = JPanel(BorderLayout(0, JBUI.scale(8))).apply {
            preferredSize = Dimension(JBUI.scale(640), JBUI.scale(420))
            border = JBUI.Borders.empty(8)
        }
        content.add(createSummaryPanel(), BorderLayout.NORTH)
        content.add(createCopyableTextPanel(), BorderLayout.CENTER)
        return content
    }

    override fun createActions(): Array<Action> =
        arrayOf(copyAction, okAction)

    private fun createSummaryPanel(): JComponent =
        JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            addSummaryRow(
                AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewStatusLabel"),
                statusText()
            )
            result.currentFilePath?.let {
                addSummaryRow(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewFileLabel"), it)
            }
            addSummaryRow(
                AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewLanguageLabel"),
                languageText()
            )
            result.fileMatched?.let {
                addSummaryRow(
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewFileMatchedLabel"),
                    booleanText(it)
                )
            }
            result.matchedHosts?.let {
                addSummaryRow(
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewHostsMatchedLabel"),
                    it.toString()
                )
            }
            result.matchedSegments?.let {
                addSummaryRow(
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewSegmentsLabel"),
                    it.toString()
                )
            }
            if (result.examples.isNotEmpty()) {
                addSummaryRow(
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewExamplesLabel"),
                    result.examples.size.toString()
                )
            }
            if (result.truncated) {
                addSummaryRow(
                    AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewTruncatedLabel"),
                    AdvancedSqlInjectionBundle.message(
                        "msg.AdvancedSqlInjection.previewTruncatedShort",
                        result.hostScanLimit
                    )
                )
            }
        }

    private fun JPanel.addSummaryRow(label: String, value: String) {
        add(JBLabel("$label $value").apply {
            border = JBUI.Borders.emptyBottom(2)
        })
    }

    private fun createCopyableTextPanel(): JComponent =
        JPanel(BorderLayout(0, JBUI.scale(4))).apply {
            add(
                JBLabel(AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewCopyableTextLabel")),
                BorderLayout.NORTH
            )
            add(
                JScrollPane(
                    JTextArea(result.toCopyableText()).apply {
                        isEditable = false
                        lineWrap = true
                        wrapStyleWord = true
                        caretPosition = 0
                    }
                ),
                BorderLayout.CENTER
            )
        }

    private fun statusText(): String =
        when {
            result.unavailableLanguageId != null ->
                AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewStatusUnavailableLanguage")

            result.message != null -> result.message

            else -> AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.previewStatusReady")
        }

    private fun languageText(): String =
        when {
            result.unavailableLanguageId != null ->
                AdvancedSqlInjectionBundle.message(
                    "msg.AdvancedSqlInjection.languageUnavailableInTable",
                    result.unavailableLanguageId
                )

            result.languageId.isBlank() ->
                AdvancedSqlInjectionBundle.message("msg.AdvancedSqlInjection.languagePlaceholder")

            else -> result.languageId
        }

    private fun booleanText(value: Boolean): String =
        AdvancedSqlInjectionBundle.message(
            if (value) {
                "msg.AdvancedSqlInjection.previewBooleanYes"
            } else {
                "msg.AdvancedSqlInjection.previewBooleanNo"
            }
        )
}
