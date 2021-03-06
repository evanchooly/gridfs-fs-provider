package com.antwerkz.gridfs

import com.mongodb.MongoClient
import org.bson.types.ObjectId
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class LoadTest {
    companion object {
        val fileCount = 10000
        val fileSize = 10000
        val provider = GridFSFileSystemProvider(MongoClient())
    }

    @Test
    fun load() {
        val database = MongoClient().getDatabase("gridfs")
        database.drop()

        val dir = File(System.getProperty("java.io.tmpdir"), "${ObjectId()}")
        try {
            dir.mkdirs()
            val file = buildFile()
            (0..fileCount).forEach {
                val temp = File.createTempFile("load-$it", ".random", dir)
                temp.writeBytes(file)
            }
            val targetDir = provider.getPath(URI("gridfs://localhost/gridfs.load/"))

            dir.walk()
                    .filter { !it.isDirectory }
                    .forEach {
                        val local = Paths.get(it.absolutePath)
                        val second = targetDir.resolve(it.name)
                        Files.move(local, second, StandardCopyOption.REPLACE_EXISTING)
                    }
            Assert.assertEquals(database.getCollection("load.files").count().toInt(), fileCount + 1)
        } finally {
            dir.deleteRecursively()
            database.drop()
        }
    }

    fun buildFile(): ByteArray {
        return (0..fileSize)
                .map { (it % 0xFF).toByte() }
                .toTypedArray().toByteArray()
    }
}