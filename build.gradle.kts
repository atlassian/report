import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

val kotlinVersion = "1.2.70"

plugins {
    kotlin("jvm").version("1.2.70")
    id("com.atlassian.performance.tools.gradle-release").version("0.7.1")
    `java-library`
}

configurations.all {
    resolutionStrategy {
        activateDependencyLocking()
        failOnVersionConflict()
        eachDependency {
            when (requested.module.toString()) {
                "com.google.guava:guava" -> useVersion("23.6-jre")
                "org.apache.commons:commons-csv" -> useVersion("1.4")
                "org.apache.httpcomponents:httpclient" -> useVersion("4.5.5")
                "org.apache.httpcomponents:httpcore" -> useVersion("4.4.9")
                "org.codehaus.plexus:plexus-utils" -> useVersion("3.1.0")
                "org.slf4j:slf4j-api" -> useVersion("1.8.0-alpha2")
                "com.jcraft:jzlib" -> useVersion("1.1.3")
                "com.google.code.gson:gson" -> useVersion("2.8.2")
                "org.jsoup:jsoup" -> useVersion("1.10.2")
                "com.numericalmethod:suanshu-20120606" -> useVersion("1.0.1-atlassian-hosted")
            }
            when (requested.group) {
                "org.jetbrains.kotlin" -> useVersion(kotlinVersion)
            }
        }
    }
}

dependencies {

    api("com.atlassian.performance.tools:jira-actions:[3.10.0,4.0.0)")
    api("com.atlassian.performance.tools:infrastructure:[2.0.0,5.0.0)")
    api("com.atlassian.performance.tools:workspace:[2.0.0,3.0.0)")
    api("com.atlassian.performance.tools:virtual-users:[1.0.0,4.0.0)")

    listOf(
        "com.atlassian.performance.tools:io:[1.0.0,2.0.0)",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion",
        "org.eclipse.jgit:org.eclipse.jgit:4.11.0.201803080745-r",
        "org.apache.commons:commons-csv:1.4",
        "org.apache.commons:commons-math3:3.6.1",
        "org.apache.commons:commons-lang3:3.5",
        "com.numericalmethod:suanshu-20120606:1.0.1-atlassian-hosted"
    ).plus(
        log4jCore()
    ).forEach { implementation(it) }

    listOf(
        "junit:junit:4.12",
        "org.hamcrest:hamcrest-library:1.3",
        "org.assertj:assertj-core:3.10.0"
    ).forEach { testCompile(it) }
}

fun log4jCore(): List<String> = log4j(
    "api",
    "core",
    "slf4j-impl"
)

fun log4j(
    vararg modules: String
): List<String> = modules.map { module ->
    "org.apache.logging.log4j:log4j-$module:2.10.0"
}

tasks.getByName("test", Test::class).apply {
    maxHeapSize = "2g" // Work around https://ecosystem.atlassian.net/browse/JPERF-395

    testLogging {
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.getByName("wrapper", Wrapper::class).apply {
    gradleVersion = "5.0"
    distributionType = Wrapper.DistributionType.ALL
}
