package com.queukat.advsqlinjection.settings

import com.queukat.advsqlinjection.model.InjectionRule
import com.queukat.advsqlinjection.model.RuleScope
import com.queukat.advsqlinjection.model.RuleTargetType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AdvancedSQLInjectionRulesTableModelTest {

    @Test
    fun `setRules normalizes rules and exposes column values`() {
        val model = AdvancedSQLInjectionRulesTableModel()

        model.setRules(
            listOf(
                rule(
                    prefix = " sql: ",
                    languageId = " SQL ",
                    filePattern = "",
                    pathPattern = "config\\queries\\*.yaml",
                    scope = RuleScope.PATH_AWARE,
                    targetType = RuleTargetType.VALUE_CONTAINS_PREFIX
                )
            )
        )

        assertEquals(1, model.rowCount)
        assertEquals(7, model.columnCount)
        assertTrue(model.getColumnName(0).isNotBlank())
        assertEquals(java.lang.Boolean::class.java, model.getColumnClass(0))
        assertEquals(Any::class.java, model.getColumnClass(1))
        assertTrue(model.isCellEditable(0, 0))
        assertFalse(model.isCellEditable(0, 1))

        assertEquals(true, model.getValueAt(0, 0))
        assertEquals("sql:", model.getValueAt(0, 1))
        assertEquals("SQL", model.getValueAt(0, 2))
        assertEquals("*", model.getValueAt(0, 3))
        assertEquals("config/queries/*.yaml", model.getValueAt(0, 4))
        assertEquals(RuleScope.PATH_AWARE, model.getValueAt(0, 5))
        assertEquals(RuleTargetType.VALUE_CONTAINS_PREFIX, model.getValueAt(0, 6))
        assertEquals("", model.getValueAt(0, 99))
    }

    @Test
    fun `blank path pattern is displayed as dash`() {
        val model = AdvancedSQLInjectionRulesTableModel()

        model.setRules(listOf(rule(pathPattern = "")))

        assertEquals("-", model.getValueAt(0, 4))
    }

    @Test
    fun `enabled column can be toggled safely`() {
        val model = AdvancedSQLInjectionRulesTableModel()
        model.setRules(listOf(rule(enabled = true)))

        model.setValueAt(false, 0, 0)
        assertFalse(model.ruleAt(0)!!.enabled)

        model.setValueAt(true, 0, 1)
        assertFalse(model.ruleAt(0)!!.enabled)

        model.setValueAt("not a boolean", 0, 0)
        assertFalse(model.ruleAt(0)!!.enabled)

        model.setValueAt(true, -1, 0)
        model.setValueAt(true, 1, 0)
        assertFalse(model.ruleAt(0)!!.enabled)
    }

    @Test
    fun `rules can be added duplicated updated removed and copied`() {
        val model = AdvancedSQLInjectionRulesTableModel()

        assertEquals(0, model.addRule(rule(prefix = " first ", pathPattern = "a\\b")))
        assertEquals("first", model.ruleAt(0)!!.prefix)
        assertEquals("a/b", model.ruleAt(0)!!.pathPattern)
        assertNull(model.duplicateRule(-1))

        assertEquals(1, model.duplicateRule(0))
        assertEquals(2, model.rowCount)

        model.updateRule(-1, rule(prefix = "ignored"))
        model.updateRule(1, rule(prefix = "second"))
        assertEquals(listOf("first", "second"), model.snapshot().map { it.prefix })

        val copiedRule = model.ruleAt(1)!!
        copiedRule.prefix = "changed copy"
        assertEquals("second", model.ruleAt(1)!!.prefix)

        val snapshot = model.snapshot()
        snapshot[1].prefix = "changed snapshot"
        assertEquals("second", model.ruleAt(1)!!.prefix)

        model.removeRule(-1)
        assertEquals(2, model.rowCount)

        model.removeRule(0)
        assertEquals(listOf("second"), model.snapshot().map { it.prefix })
        assertNull(model.ruleAt(10))
    }

    @Test
    fun `rules can be moved with boundary guards`() {
        val model = AdvancedSQLInjectionRulesTableModel()
        model.setRules(
            listOf(
                rule(prefix = "one"),
                rule(prefix = "two"),
                rule(prefix = "three")
            )
        )

        assertEquals(-1, model.moveUp(-1))
        assertEquals(0, model.moveUp(0))
        assertEquals(1, model.moveUp(2))
        assertEquals(listOf("one", "three", "two"), model.snapshot().map { it.prefix })

        assertEquals(1, model.moveDown(0))
        assertEquals(listOf("three", "one", "two"), model.snapshot().map { it.prefix })
        assertEquals(2, model.moveDown(2))
        assertEquals(3, model.moveDown(3))
    }

    private fun rule(
        enabled: Boolean = true,
        prefix: String = "sql:",
        languageId: String = "SQL",
        filePattern: String = "*.yaml",
        pathPattern: String = "",
        scope: RuleScope = RuleScope.FILE_NAME_ONLY,
        targetType: RuleTargetType = RuleTargetType.VALUE_STARTS_WITH_PREFIX
    ): InjectionRule =
        InjectionRule(
            enabled = enabled,
            prefix = prefix,
            languageId = languageId,
            filePattern = filePattern,
            pathPattern = pathPattern,
            scope = scope,
            targetType = targetType
        )
}
