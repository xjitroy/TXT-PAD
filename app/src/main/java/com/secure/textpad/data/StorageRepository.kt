package com.secure.textpad.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import android.system.Os
import com.secure.textpad.security.CryptoEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.nio.charset.CodingErrorAction
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class PadFile(
    val uri: Uri, val name: String, val vault: Boolean, val size: Long = 0,
    val modified: Long = 0, val path: String = name, val encrypted: Boolean? = null
)
enum class ConflictPolicy { OVERWRITE, KEEP_BOTH, SKIP }
@Serializable data class BackupItem(val entry: String, val path: String, val vault: Boolean, val modified: Long, val size: Long, val sha256: String)
@Serializable data class BackupManifest(val version: Int = 1, val files: List<BackupItem>)
data class RestoreResult(val restored: Int, val skipped: Int)
@Serializable data class TrashItem(val id: String, val name: String, val path: String, val vault: Boolean,
    val modified: Long, val deleted: Long, val size: Long, val sha256: String)

class StorageRepository(private val context: Context) {
    private val resolver = context.contentResolver
    private val prefs = context.getSharedPreferences("txt", Context.MODE_PRIVATE)
    private val vaultRoot = File(context.filesDir, "secure_vault").apply { mkdirs() }
    private val trashRoot = File(context.filesDir, "trash").apply { mkdirs() }
    private val workRoot = File(context.noBackupFilesDir, "operations").apply { mkdirs() }
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = false }
    val workspace: Uri? get() = prefs.getString("tree", null)?.let(Uri::parse)
    var defaultPrivate: Boolean
        get() = prefs.getBoolean("private", false)
        set(value) { prefs.edit().putBoolean("private", value).apply() }

    suspend fun chooseWorkspace(uri: Uri) = withContext(Dispatchers.IO) {
        resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        val root = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        val files = children(uri, root)
        if (files.none { it.name == ".nomedia" }) {
            val created = DocumentsContract.createDocument(resolver, root, "application/octet-stream", ".nomedia")
                ?: error("Folder provider could not create .nomedia.")
            resolver.openOutputStream(created, "wt")?.use { it.flush() } ?: error("Folder is not writable.")
        }
        prefs.edit().putString("tree", uri.toString()).commit()
    }

    private data class Child(val uri: Uri, val name: String, val size: Long, val time: Long, val directory: Boolean)
    private fun children(tree: Uri, parent: Uri): List<Child> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(parent))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED, DocumentsContract.Document.COLUMN_SIZE, DocumentsContract.Document.COLUMN_MIME_TYPE)
        return resolver.query(uri, columns, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    require(size < 10000) { "Folder exceeds 10,000 entries." }
                    add(Child(DocumentsContract.buildDocumentUriUsingTree(tree, cursor.getString(0)), cursor.getString(1) ?: "Untitled",
                        cursor.getLong(3), cursor.getLong(2), cursor.getString(4) == DocumentsContract.Document.MIME_TYPE_DIR))
                }
            }
        } ?: error("Folder permission was lost. Select the workspace again.")
    }

    private fun supported(name: String) = name.substringAfterLast('.', "").lowercase() in
        setOf("txt", "md", "log", "json", "xml", "csv", "html", "css", "js", "kt", "py", "ini", "cfg", "docx", "stxt", "enc", "tpbak")

    suspend fun scan(all: Boolean = false): List<PadFile> = withContext(Dispatchers.IO) { scanNow(all) }
    private fun scanNow(all: Boolean): List<PadFile> {
        val result = mutableListOf<PadFile>()
        fun walkPublic(tree: Uri, parent: Uri, prefix: String, depth: Int) {
            require(depth <= 32) { "Workspace nesting exceeds 32 folders." }
            for (child in children(tree, parent)) {
                validateName(child.name)
                if (child.name == ".nomedia") continue
                val path = prefix + child.name
                if (child.directory) walkPublic(tree, child.uri, "$path/", depth + 1)
                else if (all || supported(child.name)) result.add(PadFile(child.uri, child.name, false, child.size, child.time, path))
                require(result.size <= 10000) { "Too many files." }
            }
        }
        workspace?.let { tree -> walkPublic(tree, DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree)), "", 0) }
        vaultRoot.walkTopDown().maxDepth(33).filter { it.isFile }.forEach { f ->
            if (all || supported(f.name)) result.add(PadFile(Uri.fromFile(f), f.name, true, f.length(), f.lastModified(), f.relativeTo(vaultRoot).invariantSeparatorsPath))
            require(result.size <= 10000)
        }
        return result.sortedWith(compareByDescending<PadFile> { it.modified }.thenBy { it.name.lowercase() })
    }

    suspend fun external(uri: Uri): PadFile = withContext(Dispatchers.IO) {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: "Untitled.txt"
        var size = 0L
        if (uri.scheme == "content") resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use {
            if (it.moveToFirst()) { name = it.getString(0) ?: name; size = it.getLong(1) }
        }
        PadFile(uri, name, false, size)
    }
    private fun input(file: PadFile): InputStream = if (file.vault) privateFile(file.path).inputStream()
        else resolver.openInputStream(file.uri) ?: error("Cannot open ${file.name}.")

    suspend fun detect(file: PadFile): Boolean = withContext(Dispatchers.IO) {
        input(file).use { stream ->
            val h = ByteArray(4); var n = 0
            while (n < 4) { val r = stream.read(h, n, 4 - n); if (r < 0) break; n += r }
            CryptoEngine.isEncrypted(h)
        }
    }

    suspend fun readText(file: PadFile, password: CharArray? = null): String = withContext(Dispatchers.IO) {
        try {
            val raw = input(file).use { limitedBytes(it, 12 * 1024 * 1024) }
            var plain: ByteArray? = null
            try {
                val encoded = CryptoEngine.isEncrypted(raw)
                plain = if (encoded) CryptoEngine.decrypt(raw, password ?: error("Password required.")) else raw
                val docx = plain.size >= 4 && plain[0] == 80.toByte() && plain[1] == 75.toByte()
                if (docx) DocxHandler.read(ByteArrayInputStream(plain))
                else {
                    val decoder = Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    val text = decoder.decode(java.nio.ByteBuffer.wrap(plain)).toString().removePrefix("\uFEFF")
                    require(text.length <= DocxHandler.MAX_TEXT) { "Editor limit: 2 million characters." }
                    require(!text.contains('\u0000')) { "Binary files cannot be edited as text." }
                    text
                }
            } finally { raw.fill(0); plain?.fill(0) }
        } finally { password?.fill('\u0000') }
    }

    suspend fun create(name: String, private: Boolean): PadFile = withContext(Dispatchers.IO) {
        lock.withLock { createNow(name, private, unique = true) }
    }

    private fun createNow(path: String, private: Boolean, unique: Boolean): PadFile {
        validatePath(path)
        var chosen = path
        var count = 1
        while (find(chosen, private) != null) {
            require(unique) { "File already exists." }
            val extension = path.substringAfterLast('.', "")
            val stem = if (extension.isEmpty()) path else path.dropLast(extension.length + 1)
            chosen = "$stem (${count++})" + if (extension.isEmpty()) "" else ".$extension"
        }
        val name = chosen.substringAfterLast('/')
        if (private) {
            val f = privateFile(chosen); f.parentFile?.mkdirs(); check(f.createNewFile())
            return PadFile(Uri.fromFile(f), name, true, path = chosen)
        }
        val tree = workspace ?: error("Select a workspace folder first.")
        var parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        chosen.substringBeforeLast('/', "").split('/').filter { it.isNotEmpty() }.forEach { segment ->
            parent = children(tree, parent).find { it.name == segment && it.directory }?.uri
                ?: DocumentsContract.createDocument(resolver, parent, DocumentsContract.Document.MIME_TYPE_DIR, segment)
                ?: error("Cannot create folder $segment.")
        }
        val uri = DocumentsContract.createDocument(resolver, parent, mime(name), name) ?: error("Cannot create file.")
        return PadFile(uri, name, false, path = chosen)
    }

    private fun find(path: String, private: Boolean): PadFile? {
        if (private) {
            val f = privateFile(path)
            return if (f.isFile) PadFile(Uri.fromFile(f), f.name, true, f.length(), f.lastModified(), path) else null
        }
        val tree = workspace ?: return null
        var parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val segments = path.split('/')
        segments.forEachIndexed { i, s ->
            val c = children(tree, parent).find { it.name == s } ?: return null
            if (i == segments.lastIndex) return if (!c.directory) PadFile(c.uri, c.name, false, c.size, c.time, path) else null
            require(c.directory) { "A file blocks folder $s." }; parent = c.uri
        }
        return null
    }

    suspend fun save(file: PadFile, text: String, encrypted: Boolean, password: CharArray? = null): PadFile = withContext(Dispatchers.IO) {
        lock.withLock {
            val stage = temp("save")
            var bytes: ByteArray? = null
            try {
                require(text.length <= DocxHandler.MAX_TEXT)
                bytes = if (file.name.endsWith(".docx", true)) ByteArrayOutputStream().use { out -> DocxHandler.write(text, out); out.toByteArray() }
                    else text.toByteArray(Charsets.UTF_8)
                if (encrypted) {
                    val pass = password ?: error("Password required for each encrypted save.")
                    val existing = input(file).use { limitedBytes(it, 12 * 1024 * 1024) }
                    try { if (CryptoEngine.isEncrypted(existing)) CryptoEngine.decrypt(existing, pass.copyOf()).fill(0) }
                    finally { existing.fill(0) }
                    val protected = CryptoEngine.encrypt(bytes, pass)
                    try { stage.writeBytes(protected) } finally { protected.fill(0) }
                } else stage.writeBytes(bytes)
                writeNow(file, stage)
                file.copy(size = stage.length(), modified = System.currentTimeMillis(), encrypted = encrypted)
            } finally { bytes?.fill(0); password?.fill('\u0000'); stage.delete() }
        }
    }

    private fun writeNow(file: PadFile, source: File) {
        if (file.vault) {
            val target = privateFile(file.path)
            val sibling = File(target.parentFile, ".${UUID.randomUUID()}.pending")
            try {
                source.inputStream().use { src -> FileOutputStream(sibling).use { dst -> src.copyTo(dst, 65536); dst.fd.sync() } }
                Os.rename(sibling.absolutePath, target.absolutePath)
            } finally { sibling.delete() }
            return
        }
        val rollback = temp("rollback")
        try {
            input(file).use { src -> rollback.outputStream().use { src.copyTo(it, 65536) } }
            try { writeUri(file.uri, source) }
            catch (failure: Exception) {
                try { writeUri(file.uri, rollback) }
                catch (recovery: Exception) { throw IOException("Save failed; recovery copy retained at ${rollback.name} in app storage.", recovery) }
                rollback.delete(); throw failure
            }
            rollback.delete()
        } catch (e: Exception) { throw e }
    }
    private fun writeUri(uri: Uri, source: File) {
        resolver.openOutputStream(uri, "rwt")?.use { out -> source.inputStream().use { it.copyTo(out, 65536) }; out.flush() }
            ?: error("This provider did not grant write access. Move a copy into your workspace.")
    }

    suspend fun move(file: PadFile, private: Boolean): PadFile = withContext(Dispatchers.IO) {
        lock.withLock {
            if (file.vault == private) return@withLock file
            val stage = temp("move")
            var dest: PadFile? = null
            try {
                input(file).use { src -> stage.outputStream().use { src.copyTo(it, 65536) } }
                dest = createNow(file.path, private, true)
                writeNow(dest, stage)
                val expected = digest(stage)
                val actual = input(dest).use { hashStream(it) }
                check(expected == actual) { "Copy verification failed; original was kept." }
                deleteNow(file)
                dest.copy(size = stage.length(), encrypted = file.encrypted)
            } catch (e: Exception) { dest?.let { runCatching { deleteNow(it) } }; throw e }
            finally { stage.delete() }
        }
    }
    suspend fun rename(file: PadFile, newName: String): PadFile = withContext(Dispatchers.IO) {
        lock.withLock {
            validateName(newName)
            // Changing extensions can change the editor's export format; keep it explicit via Save As.
            require(newName.substringAfterLast('.', "").equals(file.name.substringAfterLast('.', ""), true)) {
                "Keep the same file extension when renaming. Use Save As to change format."
            }
            if (newName == file.name) return@withLock file
            val path = file.path.substringBeforeLast('/', "").let { if (it.isEmpty()) newName else "$it/$newName" }
            require(find(path, file.vault) == null) { "A file with that name already exists." }
            val stage = temp("rename")
            try {
                input(file).use { src -> stage.outputStream().use { src.copyTo(it, 65536) } }
                val target = createNow(path, file.vault, false)
                writeNow(target, stage)
                check(input(target).use(::hashStream) == digest(stage)) { "Rename verification failed. Original kept." }
                deleteNow(file)
                target.copy(size = stage.length(), encrypted = file.encrypted)
            } finally { stage.delete() }
        }
    }

    suspend fun listTrash(): List<TrashItem> = withContext(Dispatchers.IO) {
        trashRoot.listFiles().orEmpty().filter { it.isDirectory && File(it, "item.json").isFile }.map { dir ->
            val item = json.decodeFromString<TrashItem>(File(dir, "item.json").readText())
            require(item.id == dir.name && File(dir, "content").isFile) { "Damaged Trash record. Keep app data for recovery." }
            item
        }.sortedByDescending { it.deleted }
    }
    private fun trashDirectory(id: String): File {
        require(id.matches(Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")))
        return File(trashRoot, id)
    }
    suspend fun moveToTrash(file: PadFile) = withContext(Dispatchers.IO) {
        lock.withLock {
            val id = UUID.randomUUID().toString()
            val dir = trashDirectory(id).apply { check(mkdir()) }
            val content = File(dir, "content")
            var recorded = false
            try {
                input(file).use { src -> FileOutputStream(content).use { dst -> src.copyTo(dst, 65536); dst.fd.sync() } }
                val hash = digest(content)
                check(input(file).use(::hashStream) == hash) { "File changed during copy. Original kept." }
                val item = TrashItem(id, file.name, file.path, file.vault, file.modified, System.currentTimeMillis(), content.length(), hash)
                FileOutputStream(File(dir, "item.json")).use { it.write(json.encodeToString(item).toByteArray()); it.fd.sync() }
                recorded = true
                // The verified recovery copy and metadata exist before removing the original.
                deleteNow(file)
            } finally { if (!recorded) dir.deleteRecursively() }
        }
    }
    suspend fun restoreTrash(id: String): PadFile = withContext(Dispatchers.IO) {
        lock.withLock {
            val dir = trashDirectory(id)
            val item = json.decodeFromString<TrashItem>(File(dir, "item.json").readText())
            require(item.id == id)
            val content = File(dir, "content")
            require(content.length() == item.size && digest(content) == item.sha256) { "Trash copy is damaged; original recovery data kept." }
            val target = createNow(item.path, item.vault, true)
            writeNow(target, content)
            check(input(target).use(::hashStream) == item.sha256) { "Restore verification failed. Trash copy kept." }
            if (item.vault) privateFile(target.path).setLastModified(item.modified.coerceAtLeast(0))
            check(dir.deleteRecursively()) { "Restored file, but could not remove its Trash copy." }
            target.copy(size = item.size)
        }
    }
    suspend fun deleteTrashPermanently(id: String) = withContext(Dispatchers.IO) {
        lock.withLock { check(trashDirectory(id).deleteRecursively()) { "Could not permanently delete this Trash item." } }
    }

    private fun deleteNow(file: PadFile) {
        check(if (file.vault) privateFile(file.path).delete() else DocumentsContract.deleteDocument(resolver, file.uri)) { "Could not remove original file." }
    }

    suspend fun backup(uri: Uri, password: CharArray) = withContext(Dispatchers.IO) {
        lock.withLock {
            try {
                val files = scanNow(true).filterNot { sameDocument(it.uri, uri) }
                resolver.openOutputStream(uri, "rwt")?.use { out ->
                    CryptoEngine.writeBackup(out, password) { encrypted ->
                        ZipOutputStream(encrypted).use { zip ->
                            val manifest = files.mapIndexed { index, f ->
                                val path = "files/$index"
                                zip.putNextEntry(ZipEntry(path).apply { time = f.modified.coerceAtLeast(0) })
                                val digest = MessageDigest.getInstance("SHA-256")
                                var size = 0L
                                input(f).use { src ->
                                    val buffer = ByteArray(65536)
                                    try { while (true) { val n = src.read(buffer); if (n < 0) break; zip.write(buffer, 0, n); digest.update(buffer, 0, n); size += n } }
                                    finally { buffer.fill(0) }
                                }
                                zip.closeEntry()
                                BackupItem(path, f.path, f.vault, f.modified, size, hex(digest.digest()))
                            }
                            zip.putNextEntry(ZipEntry("manifest.json"))
                            zip.write(json.encodeToString(BackupManifest(files = manifest)).toByteArray(Charsets.UTF_8)); zip.closeEntry()
                        }
                    }
                } ?: error("Cannot write backup destination.")
            } catch (e: Exception) { runCatching { DocumentsContract.deleteDocument(resolver, uri) }; throw e }
            finally { password.fill('\u0000') }
        }
    }

    suspend fun restore(uri: Uri, password: CharArray, policy: ConflictPolicy): RestoreResult = withContext(Dispatchers.IO) {
        lock.withLock {
            val dir = File(workRoot, "restore-${UUID.randomUUID()}").apply { mkdirs() }
            try {
                var manifest: BackupManifest? = null
                val staged = mutableMapOf<String, File>()
                resolver.openInputStream(uri)?.use { source ->
                    CryptoEngine.readBackup(source, password) { decrypted ->
                        ZipInputStream(decrypted).use { zip ->
                            var total = 0L
                            while (true) {
                                val e = zip.nextEntry ?: break
                                require(!e.isDirectory && (e.name == "manifest.json" || e.name.matches(Regex("files/[0-9]{1,5}")))) { "Unsafe backup entry." }
                                require(e.name !in staged && !(e.name == "manifest.json" && manifest != null)) { "Duplicate backup entry." }
                                if (e.name == "manifest.json") {
                                    manifest = json.decodeFromString<BackupManifest>(limitedBytes(zip, 8 * 1024 * 1024).toString(Charsets.UTF_8))
                                } else {
                                    require(staged.size < 10000) { "Too many backup entries." }
                                    val f = File(dir, staged.size.toString())
                                    f.outputStream().use { dst ->
                                        val buffer = ByteArray(65536)
                                        try { while (true) {
                                            val n = zip.read(buffer); if (n < 0) break
                                            total += n
                                            require(total <= 100L * 1024 * 1024 * 1024 && dir.usableSpace > n + 16L * 1024 * 1024) { "Not enough space for staged restore." }
                                            dst.write(buffer, 0, n)
                                        } } finally { buffer.fill(0) }
                                    }
                                    staged[e.name] = f
                                }
                                zip.closeEntry()
                            }
                        }
                    }
                } ?: error("Cannot open backup.")
                val m = manifest ?: error("Backup manifest missing.")
                require(m.version == 1 && m.files.size == staged.size && m.files.map { it.entry }.toSet().size == m.files.size) { "Invalid manifest." }
                val paths = mutableSetOf<String>()
                m.files.forEach { item ->
                    validatePath(item.path)
                    require(paths.add("${item.vault}:${item.path}")) { "Duplicate destination." }
                    val f = staged[item.entry] ?: error("Missing backup file.")
                    require(item.size == f.length() && item.sha256 == digest(f)) { "Backup checksum mismatch." }
                    if (!item.vault) require(workspace != null) { "Select a workspace before restoring public files." }
                }
                // Authentication and every checksum have passed before the first destination write.
                data class Undo(val target: PadFile, val original: File?)
                val undo = mutableListOf<Undo>()
                var skipped = 0
                try {
                    m.files.forEach { item ->
                        val existing = find(item.path, item.vault)
                        if (existing != null && policy == ConflictPolicy.SKIP) { skipped++; return@forEach }
                        val target = if (existing != null && policy == ConflictPolicy.OVERWRITE) existing else createNow(item.path, item.vault, true)
                        val original = if (target == existing) File(dir, "undo-${undo.size}").also { copy -> input(target).use { src -> copy.outputStream().use { src.copyTo(it, 65536) } } } else null
                        undo.add(Undo(target, original))
                        writeNow(target, staged.getValue(item.entry))
                        if (target.vault) privateFile(target.path).setLastModified(item.modified.coerceAtLeast(0))
                    }
                } catch (failure: Exception) {
                    var failed = false
                    undo.asReversed().forEach { u -> runCatching { if (u.original == null) deleteNow(u.target) else writeNow(u.target, u.original) }.onFailure { failed = true } }
                    if (failed) { File(dir, "retain").writeText("Recovery data: do not delete."); error("Restore interrupted; some rollback files remain in ${dir.name}. Keep app data and retry from the backup.") }
                    throw failure
                }
                RestoreResult(undo.size, skipped)
            } finally { password.fill('\u0000'); if (!File(dir, "retain").exists()) dir.deleteRecursively() }
        }
    }

    fun autoTitle(text: String, extension: String = "txt"): String {
        val line = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        val clean = line.filter { it.isLetterOrDigit() || it == ' ' || it == '-' || it == '_' }.trim().take(60)
        val title = clean.ifBlank { java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date()) }
        return "$title.$extension"
    }
    private fun sameDocument(a: Uri, b: Uri): Boolean = a == b || runCatching {
        a.authority == b.authority && DocumentsContract.getDocumentId(a) == DocumentsContract.getDocumentId(b)
    }.getOrDefault(false)

    private fun privateFile(path: String): File {
        validatePath(path)
        val f = File(vaultRoot, path).canonicalFile
        require(f.path.startsWith(vaultRoot.canonicalPath + File.separator))
        return f
    }
    private fun validateName(name: String) { require(name.isNotBlank() && name != "." && name != ".." && name.length <= 240 && name.none { it == '/' || it == '\\' || it.code < 32 }) { "Invalid filename." } }
    private fun validatePath(path: String) { require(path.length <= 4096 && !path.startsWith('/')); path.split('/').forEach(::validateName) }
    private fun temp(prefix: String) = File.createTempFile(prefix, ".tmp", workRoot)
    private fun mime(name: String) = if (name.endsWith(".docx", true)) "application/vnd.openxmlformats-officedocument.wordprocessingml.document" else if (name.endsWith(".stxt", true) || name.endsWith(".enc", true)) "application/octet-stream" else "text/plain"
    private fun limitedBytes(input: InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        try { while (true) { val n = input.read(buffer); if (n < 0) break; require(output.size() + n <= limit) { "File exceeds safe size limit." }; output.write(buffer, 0, n) }; return output.toByteArray() }
        finally { buffer.fill(0); output.reset() }
    }
    private fun digest(file: File) = file.inputStream().use(::hashStream)
    private fun hashStream(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256"); val b = ByteArray(65536)
        try { while (true) { val n = input.read(b); if (n < 0) break; digest.update(b, 0, n) }; return hex(digest.digest()) }
        finally { b.fill(0) }
    }
    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}
