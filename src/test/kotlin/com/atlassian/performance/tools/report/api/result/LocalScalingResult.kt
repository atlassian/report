package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.report.api.FullTimeline
import java.io.File
import java.io.FileFilter
import java.nio.file.Path
import java.nio.file.Paths

internal class LocalScalingResult(
    private val path: Path
) {
    fun loadCohorts(): List<DataScalingCohortActionResult> {
        return loadAll()
            .flatMap { edibleResult ->
                val cohort = edibleResult.cohort

                edibleResult.actionStats.centers!!.map { (action, duration) ->
                    DataScalingCohortActionResult(
                        parseDataset(cohort),
                        parseDeployment(cohort),
                        parseVersion(cohort),
                        action,
                        duration.toMillis().toDouble()
                    )
                }
            }
    }

    fun loadAll(): List<EdibleResult> = CompressedResult(path)
        .extractDirectory()
        .toFile()
        .listDirectories()
        .map { loadEdible(it) }
        .sortedBy { it.cohort }

    private fun loadEdible(
        cohortDirectory: File
    ): EdibleResult = loadRaw(cohortDirectory).prepareForJudgement(FullTimeline())

    private fun loadRaw(
        cohortDirectory: File
    ): RawCohortResult = RawCohortResult.Factory().fullResult(
        cohort = cohortDirectory.name,
        results = cohortDirectory.toPath()
    )

    private fun parseVersion(cohort: String): String {
        return when {
            cohort.startsWith("7.6.10") -> "7.6.10"
            cohort.startsWith("7.13.") -> "7.13.1"
            else -> throw IllegalStateException("Unknown version in $cohort.")
        }
    }

    private fun parseDeployment(cohort: String): String {
        return when {
            cohort.contains("Standalone") -> "Server"
            cohort.contains("Data Center") -> "Data Center"
            else -> throw IllegalStateException("Unknown deployment in $cohort.")
        }
    }

    private fun parseDataset(cohort: String): String {
        return cohort.substring(startIndex = cohort.lastIndexOf(": ") + 2, endIndex = cohort.length).trim()
    }

}

private fun File.listDirectories() = this.listFiles(FileFilter { it.isDirectory })

data class DataScalingCohortActionResult(
    val dataset: String,
    val deployment: String,
    val version: String,
    val action: String,
    val value: Double
)