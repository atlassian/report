package com.atlassian.performance.tools.report.jstat

/**
 * https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jstat.html
 */
enum class JstatGcutilHeader {
    /**
     * @see com.atlassian.jira.test.performance.infrastructure.Jstat.Companion.TIME
     */
    DATE,

    /**
     * The Timestamp column contains the elapsed time, in seconds, since the target JVM started.
     */
    TIMESTAMP,

    /**
     * Survivor space 0 utilization as a percentage of the space's current capacity.
     */
    S0,

    /**
     * Survivor space 1 utilization as a percentage of the space's current capacity.
     */
    S1,

    /**
     * Eden space utilization as a percentage of the space's current capacity.
     */
    E,

    /**
     * Old space utilization as a percentage of the space's current capacity.
     */
    O,

    /**
     * Metaspace utilization as a percentage of the space's current capacity
     */
    M,

    /**
     * Compressed class space utilization as a percentage.
     */
    CCS,

    /**
     * Number of young generation GC events.
     */
    YGC,

    /**
     * Young generation garbage collection time.
     */
    YGCT,

    /**
     * Number of full GC events.
     */
    FGC,

    /**
     * Full garbage collection time.
     */
    FGCT,

    /**
     * Total garbage collection time.
     */
    GCT
}