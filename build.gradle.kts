val kotlinVersion = "1.2.30"

plugins {
    kotlin("jvm").version("1.2.30")
    id("com.atlassian.performance.tools.gradle-release").version("0.0.2")
}

dependencies {
    listOf(
        "org.jetbrains.kotlin:kotlin-stdlib-jre8:$kotlinVersion",
        "com.atlassian.performance.tools:workspace:0.0.1",
        "com.atlassian.performance.tools:io:0.0.1",
        "com.atlassian.performance.tools:infrastructure:0.0.2",
        "org.eclipse.jgit:org.eclipse.jgit:4.11.0.201803080745-r",
        "org.apache.commons:commons-csv:1.4",
        "org.apache.commons:commons-math3:3.6.1"
    ).plus(
        log4jCore()
    ).forEach { compile(it) }

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

task<Wrapper>("wrapper") {
    gradleVersion = "4.9"
    distributionType = Wrapper.DistributionType.ALL
}