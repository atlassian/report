package com.atlassian.performance.tools.report

import com.atlassian.performance.tools.infrastructure.metric.Dimension
import com.atlassian.performance.tools.infrastructure.metric.SystemMetric
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.time.Duration
import java.time.Instant
import java.util.*

class SystemMetricsGenerator {
    private val logger: Logger = LogManager.getLogger(this::class.java)

    fun asList(): List<SystemMetric> {
        val dimensions = Dimension.values()
        val systems = arrayOf(
            "jira-node-1",
            "jira-node-2",
            "jira-node-3",
            "jira-node-4",
            "vu-1",
            "vu-2",
            "vu-3",
            "vu-4",
            "jira-home"
        )

        val random = Random()
        val seed = random.nextLong()
        logger.info("Metrics generation seed: $seed")
        random.setSeed(seed)

        return dimensions.flatMap { dim ->
            systems.flatMap { sys ->
                var time = Instant.parse("2017-12-20T14:57:43.997Z")
                var value = 100 * random.nextDouble()
                (1..25).map {
                    time += Duration.ofMinutes(1)
                    value = Math.abs(value + random.nextDouble() * if(random.nextBoolean()) -10 else 10)
                    SystemMetric(
                        dimension = dim,
                        start = time,
                        system = sys,
                        value = value
                    )
                }
            }
        }
    }
}