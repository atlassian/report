package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.io.api.ensureDirectory
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.zip.ZipFile

class CompressedResult(
    private val relativeResourcePath: Path
) {
    fun extractDirectory(tempFolder: TemporaryFolder): Path {
        val file = File(
            this::class
                .java
                .getResource("real-results/$relativeResourcePath")
                .toURI()
        )
        return if (file.name.endsWith(".zip")) {
            unzip(file, tempFolder)
        } else {
            file.toPath()
        }
    }

    companion object {

        fun unzip(clazz: Class<*>, resource: String, tempFolder: TemporaryFolder) = clazz
            .getResource(resource)!!
            .toURI()
            .let { File(it) }
            .let { unzip(it, tempFolder) }

        fun unzip(
            file: File,
            tempFolder: TemporaryFolder
        ): Path {
            val unpacked = tempFolder.newFolder(UUID.randomUUID().toString() + file.name)
            unpacked.mkdirs()
            val zip = ZipFile(file)
            zip.stream().forEach { entry ->
                val unpackedEntry = unpacked.resolve(entry.name)
                if (entry.isDirectory) {
                    unpackedEntry.ensureDirectory()
                } else {
                    zip.getInputStream(entry).use { packedStream ->
                        unpackedEntry.outputStream().use { unpackedStream ->
                            packedStream.copyTo(unpackedStream)
                        }
                    }
                }
            }
            return unpacked.toPath()
        }
    }
}
