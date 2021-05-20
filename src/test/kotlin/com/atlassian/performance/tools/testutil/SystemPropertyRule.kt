package com.atlassian.performance.tools.testutil

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement


annotation class WithSystemProperty(val key:String, val value:String)

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
