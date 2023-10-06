package com.atlassian.performance.tools.report

import javax.json.spi.JsonProvider

internal object JsonProviderSingleton {

    val JSON: JsonProvider = JsonProvider.provider()
}
