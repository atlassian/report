package com.atlassian.performance.tools.report.api.parser.jfr

import org.junit.Test
import org.openjdk.jmc.common.item.*
import org.openjdk.jmc.common.unit.UnitLookup
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit
import org.openjdk.jmc.flightrecorder.jdk.JdkFilters
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs
import org.openjdk.jmc.flightrecorder.parser.IParserExtension
import org.openjdk.jmc.flightrecorder.parser.ParserExtensionRegistry
import org.openjdk.jmc.flightrecorder.parser.filter.FilterExtension
import org.openjdk.jmc.flightrecorder.parser.filter.OnLoadFilters
import java.util.*
import kotlin.streams.toList

class JfrParserTest {
    //    private fun filterByThreadId(attribute: IAccessorKey<*>, givenThreadId: Long): IItemFilter {
//        return IItemFilter {
//            val threadId: Long = it.getValue(attribute).toString().toLong()
//            threadId == givenThreadId
//        }
//    }
    private fun profilerResult() = JfrParserTest::class.java.getResourceAsStream("/profiler-result.jfr")
    private val onLoadFilter = OnLoadFilters.includeEvents(listOf(JdkTypeIDs.EXECUTION_SAMPLE))

    private fun allEvents(): IItemCollection {
        return JfrLoaderToolkit.loadEvents(profilerResult())
    }

    private fun alreadyFilteredEvents(): IItemCollection {
        val extensions: MutableList<IParserExtension> = ArrayList(ParserExtensionRegistry.getParserExtensions())
        extensions.add(FilterExtension(onLoadFilter))
        return JfrLoaderToolkit.loadEvents(profilerResult(), extensions)
    }

    private fun eventsFilteredAfterwards(): IItemCollection {
        return JfrLoaderToolkit.loadEvents(profilerResult()).apply(JdkFilters.EXECUTION_SAMPLE)
    }

    @Test
    fun shouldAccessJavaThreadId() {
        val events = alreadyFilteredEvents()
        val javaThreadIdAttribute = Attribute.attr("javaThreadId", null, UnitLookup.RAW_LONG)
        val samplesWithThreadId = events.apply(ItemFilters.equals(javaThreadIdAttribute, 1))
        val firstItem = events.first()
        firstItem.forEach { it ->
            println(it)
        }
    }

    @Test
    fun should() {
        val events = alreadyFilteredEvents()
//        val javaSampledThreadAttribute = Attribute.attr("sampledThread", null, UnitLookup.RAW_LONG)
        val javaThreadIdAttribute = Attribute.attr("javaThreadId", null, UnitLookup.RAW_LONG)
        val samplesWithThreadId = events.apply(ItemFilters.equals(javaThreadIdAttribute, 1))
//        val samplesWithThreadId = events.apply(ItemFilters.equals(JdkAttributes.JAVA_THREAD, object: IMCThread {
//            override fun getThreadId(): Long {
//                return 1
//            }
//
//            override fun getThreadName(): String {
//                TODO("Not yet implemented")
//            }
//
//            override fun getThreadGroup(): IMCThreadGroup {
//                TODO("Not yet implemented")
//            }
//
//        }))
        val sample = events.stream().toList()
        val executionSample = sample[8]
        val sample1 = sample[1000]
        val sample2 = sample[1200]
        val sample3 = sample[9900]
        val sample4 = sample[9990]
    }
}
