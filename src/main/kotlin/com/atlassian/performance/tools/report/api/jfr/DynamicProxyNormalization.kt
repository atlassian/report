package com.atlassian.performance.tools.report.api.jfr

import java.nio.ByteBuffer
import java.util.function.Consumer

class DynamicProxyNormalization : Consumer<MutableJvmSymbol> {

    private val proxyClassName = Regex("\\\$Proxy[0-9]")
    private val proxyPackageName = Regex("proxy[0-9]")

    override fun accept(symbol: MutableJvmSymbol) {
        val symbolString = symbol.toString()
        if (symbolString.contains(proxyClassName) || symbolString.contains(proxyPackageName)) {
            val newSymbol = "PROXY".padEnd(symbol.payload.size, '_')
            ByteBuffer.wrap(symbol.payload).put(newSymbol.toByteArray())
        }
    }
}


