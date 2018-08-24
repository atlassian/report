package com.atlassian.performance.tools.report.vmstat

internal enum class VmstatHeader {
    /**
     * The number of processes waiting for run time
     */
    R,
    /**
     * The number of processes in uninterruptible sleep
     */
    B,
    /**
     * The amount of virtual memory used
     */
    SWPD,
    /**
     * The amount of idle memory
     */
    FREE,
    /**
     * The amount of memory used as buffers
     */
    BUFF,
    /**
     * The amount of memory used as cache
     */
    CACHE,
    /**
     * Amount of memory swapped in from disk (/s)
     */
    SI,
    /**
     * Amount of memory swapped to disk (/s)
     */
    SO,
    /**
     * Blocks received from a block device (blocks/s)
     */
    BI,
    /**
     * Blocks sent to a block device (blocks/s)
     */
    BO,
    /**
     * The number of interrupts per second, including the clock
     */
    IN,
    /**
     * The number of context switches per second
     */
    CS,
    /**
     * Time spent running non-kernel code. (user time, including nice time)
     */
    US,
    /**
     * Time spent running kernel code. (system time)
     */
    SY,
    /**
     * Time spent idle. Prior to Linux 2.5.41, this includes IO-wait time
     */
    ID,
    /**
     * Time spent waiting for IO. Prior to Linux 2.5.41, included in idle
     */
    WA,
    /**
     * Time stolen from a virtual machine. Prior to Linux 2.6.11, unknown
     */
    ST,
    /**
     * Date at which stats were captured in the ISO-8601 format.
     */
    DATE,
    /**
     * Time at which stats were captured in the ISO-8601 format.
     */
    TIME
}