package com.atlassian.performance.tools.testutil

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Used to mark test methods as requiring a System Property available and cleaned up.
 */
annotation class WithSystemProperty(val key:String, val value:String)

/**
 * An instance of this Rule must be present in a test class and annotated with @Rule.
 * It does the work of processing the WithSystemProperty annotations - setting up the
 * desired properties for the test case and restoring them to their previous values
 * afterwards.
 */
class SystemPropertyRule: TestRule {
    override fun apply(statement: Statement, description: Description): Statement {
        val annotations = description
            .annotations
            .filterIsInstance<WithSystemProperty>()

        return object : Statement() {
            override fun evaluate() {
                val oldValues = annotations.map {
                    it.key to System.getProperty(it.key)
                }.toMap()

                for (newEntry in annotations) {
                    System.setProperty(newEntry.key, newEntry.value)
                }

                try {
                    statement.evaluate()
                } finally {
                    for (oldEntry in oldValues) {
                        if (oldEntry.value == null) {
                            System.clearProperty(oldEntry.key)
                        } else {
                            System.setProperty(oldEntry.key, oldEntry.value)
                        }
                    }
                }
            }
        }
    }
}
