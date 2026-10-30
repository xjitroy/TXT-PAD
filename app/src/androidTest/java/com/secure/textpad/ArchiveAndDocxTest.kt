package com.secure.textpad

import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import com.secure.textpad.data.ConflictPolicy
import com.secure.textpad.data.DocxHandler
import com.secure.textpad.data.StorageRepository
import com.secure.textpad.security.CryptoEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ArchiveAndDocxTest {
    private fun isolated(block: suspend (StorageRepository, File) -> Unit) = runBlocking {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val id = UUID.randomUUID().toString()
        val root = File(base.cacheDir, "qa-archive-$id").apply { mkdirs() }
        val context = object : ContextWrapper(base) {
            override fun getFilesDir() = File(root, "files").apply { mkdirs() }
            override fun getNoBackupFilesDir() = File(root, "no-backup").apply { mkdirs() }
            override fun getSharedPreferences(name: String, mode: Int) = base.getSharedPreferences("$name-$id", Context.MODE_PRIVATE)
        }
        try { block(StorageRepository(context), root) }
        finally { root.deleteRecursively(); base.deleteSharedPreferences("txt-$id") }
    }
    @Test fun fullBackupRetainsInnerEncryptionAndHandlesEveryConflictPolicy() = isolated { repo, root ->
        val plain = repo.save(repo.create("paragraph.txt", true), "Original বাংলা", false)
        val locked = repo.save(repo.create("locked.stxt", true), "Inner secret", true, "inner password for QA".toCharArray())
        val binary = File(root, "files/secure_vault/payload.bin").apply {
            writeBytes(ByteArray(300000).also { java.security.SecureRandom().nextBytes(it) })
        }
        val expected = java.security.MessageDigest.getInstance("SHA-256").digest(binary.readBytes())
        val backup = File(root, "full.tpbak"); val uri = Uri.fromFile(backup)
        val password = "master password for QA".toCharArray()
        repo.backup(uri, password)
        assertTrue(password.all { it == '\u0000' })
        repo.save(plain, "Newer contents", false)
        assertEquals(3, repo.restore(uri, "master password for QA".toCharArray(), ConflictPolicy.SKIP).skipped)
        assertEquals("Newer contents", repo.readText(plain))
        assertEquals(3, repo.restore(uri, "master password for QA".toCharArray(), ConflictPolicy.KEEP_BOTH).restored)
        assertEquals("Original বাংলা", repo.readText(repo.scan().single { it.name == "paragraph (1).txt" }))
        val protectedCopy = repo.scan().single { it.name == "locked (1).stxt" }
        assertTrue(repo.detect(protectedCopy))
        assertEquals("Inner secret", repo.readText(protectedCopy, "inner password for QA".toCharArray()))
        assertEquals(3, repo.restore(uri, "master password for QA".toCharArray(), ConflictPolicy.OVERWRITE).restored)
        assertEquals("Original বাংলা", repo.readText(plain))
        assertEquals("Inner secret", repo.readText(locked, "inner password for QA".toCharArray()))
        assertArrayEquals(expected, java.security.MessageDigest.getInstance("SHA-256").digest(binary.readBytes()))
    }
    @Test fun damagedOrWrongPasswordBackupCannotChangeExistingFiles() = isolated { repo, root ->
        val file = repo.save(repo.create("keep.txt", true), "Keep this safely", false)
        val backup = File(root, "bad.tpbak"); val uri = Uri.fromFile(backup)
        repo.backup(uri, "correct master password".toCharArray())
        val wrong = "wrong master password".toCharArray()
        assertTrue(runCatching { repo.restore(uri, wrong, ConflictPolicy.OVERWRITE) }.isFailure)
        assertTrue(wrong.all { it == '\u0000' })
        val bytes = backup.readBytes(); bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte(); backup.writeBytes(bytes)
        assertTrue(runCatching { repo.restore(uri, "correct master password".toCharArray(), ConflictPolicy.OVERWRITE) }.isFailure)
        assertEquals("Keep this safely", repo.readText(file))
        assertEquals(1, repo.scan().size)
    }
    @Test fun authenticatedArchiveWithBadManifestIsRejectedBeforeAnyWrite() = isolated { repo, root ->
        repo.save(repo.create("existing.txt", true), "Untouched", false)
        listOf("safe.txt", "../escape.txt").forEach { path ->
            val archive = File(root, "manifest.tpbak")
            archive.outputStream().use { output -> CryptoEngine.writeBackup(output, "manifest QA password".toCharArray()) { stream ->
                ZipOutputStream(stream).use { zip ->
                    zip.putNextEntry(ZipEntry("files/0")); zip.write("data".toByteArray()); zip.closeEntry()
                    zip.putNextEntry(ZipEntry("manifest.json"))
                    zip.write("""{"version":1,"files":[{"entry":"files/0","path":"$path","vault":true,"modified":0,"size":4,"sha256":"invalid"}]}""".toByteArray())
                    zip.closeEntry()
                }
            } }
            assertTrue(runCatching { repo.restore(Uri.fromFile(archive), "manifest QA password".toCharArray(), ConflictPolicy.OVERWRITE) }.isFailure)
            assertEquals(1, repo.scan().size)
            assertEquals("Untouched", repo.readText(repo.scan().single()))
            assertFalse(File(root, "files/escape.txt").exists())
        }
    }
    @Test fun nativeDocxRoundTripsUnicodeParagraphsAndRejectsDeclarations() {
        val text = "A <clear> & useful page\nবাংলা লেখা 🔐\n\nLast paragraph"
        val out = ByteArrayOutputStream(); DocxHandler.write(text, out)
        assertEquals(text, DocxHandler.read(ByteArrayInputStream(out.toByteArray())))
        val bad = ByteArrayOutputStream()
        ZipOutputStream(bad).use { zip ->
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write("""<!DOCTYPE document [<!ENTITY x SYSTEM "file:///data/local/tmp/secret">]><document>&x;</document>""".toByteArray())
            zip.closeEntry()
        }
        assertTrue(runCatching { DocxHandler.read(ByteArrayInputStream(bad.toByteArray())) }.isFailure)
    }
}
