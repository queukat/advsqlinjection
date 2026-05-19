package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.model.InjectionRule
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixture4TestCase
import org.junit.Test
import kotlin.test.assertEquals

class AdvancedSQLInjectionRuleDialogTest : LightPlatformCodeInsightFixture4TestCase() {

    @Test
    fun testNewRuleDialogCanBeConstructed() {
        val dialog = AdvancedSQLInjectionRuleDialog(
            project = project,
            initialRule = InjectionRule.emptyRule(),
            isEditMode = false
        )

        dialog.close(DialogWrapper.CANCEL_EXIT_CODE)
    }

    @Test
    fun testSqlExampleRuleDialogCanBeConstructed() {
        val dialog = AdvancedSQLInjectionRuleDialog(
            project = project,
            initialRule = InjectionRule.exampleSqlRule(),
            isEditMode = false
        )

        dialog.close(DialogWrapper.CANCEL_EXIT_CODE)
    }

    @Test
    fun testUnavailableLanguageIdSurvivesDialogRoundTrip() {
        val unavailableLanguageId = "DefinitelyUnavailableLanguageForDialogTest"
        val dialog = AdvancedSQLInjectionRuleDialog(
            project = project,
            initialRule = InjectionRule(
                prefix = "dsl:",
                languageId = unavailableLanguageId,
                filePattern = "*.yaml"
            ),
            isEditMode = true
        )

        assertEquals(unavailableLanguageId, dialog.buildRule().languageId)

        dialog.close(DialogWrapper.CANCEL_EXIT_CODE)
    }

}
