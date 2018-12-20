package com.atlassian.performance.tools.report.chart.waterfall

internal enum class Phase(
    val label: String,
    val color: String
) {
    P0_IDLE(
        label = "Idle",
        color = "rgba(255, 255, 255, 0.0)"
    ),
    P1_REDIRECT(
        label = "Redirect",
        color = "rgba(255, 0, 0, 0.7)"
    ),
    P2_IDLE(
        label = "?",
        color = "rgba(255, 255, 255, 0.0)"
    ),
    P3_APPCHACHE(
        label = "AppCache",
        color = "rgba(66, 150, 88, 0.7)"
    ),
    P4_DNS(
        label = "DNS Lookup",
        color = "rgba(0, 139, 125, 0.7)"
    ),
    P5_IDLE(
        label = "?",
        color = "rgba(255, 255, 255, 0.0)"
    ),
    P6_TCP(
        label = "Initial connection",
        color = "rgba(255, 141, 2, 0.7)"
    ),
    P7_SSL(
        label = "SSL",
        color = "rgba(152, 19, 177, 0.7)"
    ),
    P8_IDLE(
        label = "Stalled",
        color = "rgba(160, 160, 160, 0.7)"
    ),
    P9_REQUEST(
        label = "Waiting (TTFB)",
        color = "rgba(87, 217, 163, 0.7)"
    ),
    P10_RESPONSE(
        label = "Content Download",
        color = "rgba(38, 132, 255, 0.7)"
    ),
    P11_DOM_PARSING(
        label = "DOM Parsing",
        color = "rgba(255, 196, 0, 0.7)"
    ),
    P12_PROCESSING(
        label = "DOM Processing",
        color = "rgba(135, 119, 217, 0.7)"
    ),
    P13_IDLE(
        label = "?",
        color = "rgba(255, 255, 255, 0.0)"
    ),
    P14_LOAD(
        label = "Load",
        color = "rgba(0, 199, 229, 0.7)"
    )
}