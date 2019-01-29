package com.atlassian.performance.tools.report.api.result

import com.atlassian.performance.tools.io.api.ensureDirectory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

class CompressedResult(
    private val relativeResourcePath: Path
) {
    fun extractDirectory(): Path {
        val file = File(
            this::class
                .java
                .getResource("real-results/$relativeResourcePath")
                .toURI()
        )
        return if (file.name.endsWith(".zip")) {
            unzip(file)
        } else {
            file.toPath()
        }
    }

    private fun unzip(
        file: File
    ): Path {
        val unpacked = Files.createTempDirectory(file.name)
        val zip = ZipFile(file)
        zip.stream().forEach { entry ->
            val unpackedEntry = unpacked.resolve(entry.name)
            if (entry.isDirectory) {
                unpackedEntry.ensureDirectory()
            } else {
                zip.getInputStream(entry).use { packedStream ->
                    unpackedEntry.toFile().outputStream().use { unpackedStream ->
                        packedStream.copyTo(unpackedStream)
                    }
                }
            }
        }
        return unpacked
    }
}