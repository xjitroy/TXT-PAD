package com.secure.textpad

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import com.secure.textpad.ui.theme.GlassButton as Button
import com.secure.textpad.ui.theme.GlassTextButton as TextButton
import com.secure.textpad.ui.theme.GlassOutlinedButton as OutlinedButton
import com.secure.textpad.ui.theme.GlassTonalButton as FilledTonalButton
import com.secure.textpad.ui.theme.GlassIconButton as IconButton
import com.secure.textpad.ui.theme.GlassFilterChip as FilterChip

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import com.secure.textpad.data.*
import com.secure.textpad.ui.editor.*
import com.secure.textpad.ui.home.HomeScreen
import com.secure.textpad.ui.home.TrashScreen
import com.secure.textpad.ui.theme.LiquidGlassTheme
import com.secure.textpad.ui.theme.liquidGlass
import com.secure.textpad.ui.theme.LocalGlassMotionEnabled
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.secure.textpad.security.SessionPassword
import java.security.KeyStore
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory

class PadViewModel(app: Application) : AndroidViewModel(app) {
    val repository = StorageRepository(app)
    var files by mutableStateOf<List<PadFile>>(emptyList()); private set
    var trash by mutableStateOf<List<TrashItem>>(emptyList()); private set
    var showTrash by mutableStateOf(false)
    var editor by mutableStateOf<EditorDocument?>(null); private set
    var busy by mutableStateOf(false); private set
    var saving by mutableStateOf(false); private set
    var savingProtected by mutableStateOf(false); private set
    var notice by mutableStateOf<String?>(null)
    var unlockFile by mutableStateOf<PadFile?>(null)
    var workspace by mutableStateOf(repository.workspace != null); private set
    var defaultPrivate by mutableStateOf(repository.defaultPrivate)
    var pendingNavigation by mutableStateOf<(() -> Unit)?>(null)

    init { refresh() }
    fun report(error: Throwable) { notice = error.message ?: "Operation failed. Your editor text is still available." }
    fun runOperation(block: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch { try { block() } catch (e: Exception) { report(e) } finally { busy = false } }
    }
    fun refresh() = runOperation { reload() }
    private suspend fun reload() {
        trash = repository.listTrash()
        files = repository.scan()
        // Publish the directory cursor results first; inspect magic bytes on I/O afterwards.
        val detected = ArrayList<PadFile>()
        for (f in files) detected.add(f.copy(encrypted = runCatching { repository.detect(f) }.getOrNull()))
        files = detected
    }
    fun navigate(action: () -> Unit) {
        if (busy || saving) return
        if (editor?.dirty == true) pendingNavigation = action else { editor?.clear(); editor = null; action() }
    }
    fun discardAndNavigate() {
        val action = pendingNavigation; pendingNavigation = null
        editor?.clear(); editor = null; action?.invoke()
    }
    fun open(file: PadFile) = navigate {
        runOperation {
            if (repository.detect(file)) unlockFile = file
            else editor = EditorDocument(file, repository.readText(file), false)
        }
    }
    fun unlock(password: CharArray) {
        val file = unlockFile ?: run { password.fill('\u0000'); return }
        unlockFile = null
        runOperation {
            var sealed: SessionPassword? = null
            try {
                sealed = withContext(Dispatchers.IO) { SessionPassword.seal(password.copyOf()) }
                val text = repository.readText(file, password)
                editor = EditorDocument(file, text, true).also { it.setSession(sealed) }
                sealed = null
            } finally { sealed?.clear(); password.fill('\u0000') }
        }
    }
    fun newFile() {
        if (busy) return
        editor = EditorDocument(PadFile(Uri.EMPTY, "New document", repository.defaultPrivate || !workspace, path = ""), "", false, fresh = true)
    }
    fun back() {
        if (busy || saving) return
        val doc = editor
        if (doc?.fresh == true && !doc.encrypted) { pendingNavigation = { refresh() }; save(false, null, true) }
        else navigate { refresh() }
    }
    fun saveAs(uri: Uri, password: CharArray?) {
        val doc = editor
        if (busy || doc == null) { password?.fill('\u0000'); return }
        val text = doc.value.text
        runOperation {
            var sealed: SessionPassword? = null
            try {
                // Finish an in-flight auto-save before switching the active destination.
                while (saving) kotlinx.coroutines.delay(20)
                if (password != null) sealed = withContext(Dispatchers.IO) { SessionPassword.seal(password.copyOf()) }
                val destination = repository.external(uri)
                val saved = repository.save(destination, text, password != null, password)
                doc.saved(saved, text, password != null)
                doc.setSession(sealed); sealed = null
                notice = "Saved as ${destination.name}. Auto-save now uses this file."
                reload()
            } finally { sealed?.clear(); password?.fill('\u0000') }
        }
    }
    fun save(encrypted: Boolean, password: CharArray?, navigateAfter: Boolean = false) {
        val doc = editor
        if (busy || saving || doc == null) { password?.fill('\u0000'); return }
        val text = doc.value.text
        saving = true
        savingProtected = encrypted
        viewModelScope.launch {
            var sealed: SessionPassword? = null
            var actualPassword: CharArray? = password
            try {
                if (encrypted && password != null) sealed = withContext(Dispatchers.IO) { SessionPassword.seal(password.copyOf()) }
                if (encrypted && actualPassword == null) actualPassword = withContext(Dispatchers.IO) { doc.sessionPassword?.open() }
                val target = if (doc.fresh) repository.create(repository.autoTitle(text, if (encrypted) "stxt" else "txt"), doc.file.vault) else doc.file
                val saved = repository.save(target, text, encrypted, actualPassword)
                doc.saved(saved, text, encrypted)
                if (!encrypted) doc.setSession(null)
                else if (sealed != null) { doc.setSession(sealed); sealed = null }
                files = (files.filterNot { it.uri == saved.uri } + saved).sortedByDescending { it.modified }
                if (navigateAfter && !doc.dirty) {
                    val action = pendingNavigation; pendingNavigation = null
                    doc.clear(); editor = null
                    viewModelScope.launch { kotlinx.coroutines.yield(); action?.invoke() }
                }
            } catch (e: Exception) {
                doc.saveFailure = true
                if (encrypted && password == null) doc.setSession(null)
                report(e)
            }
            finally { sealed?.clear(); actualPassword?.fill('\u0000'); password?.fill('\u0000'); saving = false; savingProtected = false }
            // An IME can commit text after Back/Save has taken its snapshot.
            // Save that newer revision too before leaving the document.
            if (navigateAfter && editor === doc && doc.dirty && !doc.saveFailure) save(encrypted, null, true)
        }
    }
    fun move(file: PadFile) = runOperation {
        val moved = repository.move(file, !file.vault)
        editor?.takeIf { it.file.uri == file.uri }?.let { it.file = moved }
        reload(); notice = if (moved.vault) "Moved to private vault." else "Moved to workspace."
    }
    fun rename(file: PadFile, name: String) = runOperation {
        val renamed = repository.rename(file, name)
        editor?.takeIf { it.file.uri == file.uri }?.let { it.file = renamed }
        reload()
    }
    fun trash(file: PadFile) = runOperation { repository.moveToTrash(file); reload(); notice = "Moved to Trash. You can restore it later." }
    fun restoreTrash(item: TrashItem) = runOperation { repository.restoreTrash(item.id); reload(); notice = "Restored ${item.name}." }
    fun deleteTrash(item: TrashItem) = runOperation { repository.deleteTrashPermanently(item.id); reload() }
    fun folder(uri: Uri) = runOperation { repository.chooseWorkspace(uri); workspace = true; reload() }
    fun backup(uri: Uri, password: CharArray) = runOperation {
        try { repository.backup(uri, password); notice = "Offline backup created. Keep it and its master password safe." }
        finally { password.fill('\u0000') }
    }
    fun restore(uri: Uri, password: CharArray, policy: ConflictPolicy) = runOperation {
        try { val r = repository.restore(uri, password, policy); reload(); notice = "Restored ${r.restored} files. Skipped ${r.skipped}." }
        finally { password.fill('\u0000') }
    }
    suspend fun external(uri: Uri) = repository.external(uri)
    override fun onCleared() { editor?.clear() }
}

class MainActivity : FragmentActivity() {
    private val prefs by lazy { getSharedPreferences("txt", MODE_PRIVATE) }
    private var animateGlass by mutableStateOf(true)
    private var appLock by mutableStateOf(false)
    private var unlocked by mutableStateOf(true)
    private var incoming by mutableStateOf<Uri?>(null)
    private var gateMessage by mutableStateOf<String?>(null)
    private var prompt: BiometricPrompt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        animateGlass = prefs.getBoolean("animated_glass", true)
        appLock = prefs.getBoolean("app_lock", false)
        unlocked = !appLock
        incoming = externalUri(intent)
        setContent { LiquidGlassTheme(animateBackdrop = animateGlass) { App() } }
    }
    override fun onNewIntent(intent: Intent) { super.onNewIntent(intent); setIntent(intent); incoming = externalUri(intent) }
    private fun externalUri(intent: Intent?): Uri? = if (intent?.action == Intent.ACTION_VIEW || intent?.action == Intent.ACTION_EDIT) intent?.data else null
    override fun onStop() {
        super.onStop()
        if (appLock && !isChangingConfigurations) unlocked = false
    }
    override fun onDestroy() { prompt?.cancelAuthentication(); prompt = null; super.onDestroy() }

    private fun authenticate(onSuccess: () -> Unit) {
        try {
            val auth = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                if (Build.VERSION.SDK_INT >= 30) BiometricManager.Authenticators.DEVICE_CREDENTIAL else 0
            check(BiometricManager.from(this).canAuthenticate(auth) == BiometricManager.BIOMETRIC_SUCCESS) {
                "Set up a strong fingerprint${if (Build.VERSION.SDK_INT >= 30) " or device PIN/password" else ""} in Android settings first."
            }
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val alias = "txt.app.authentication.v1"
            val key = (store.getKey(alias, null) as? SecretKey) ?: run {
                val builder = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256).setUserAuthenticationRequired(true).setInvalidatedByBiometricEnrollment(false)
                if (Build.VERSION.SDK_INT >= 30) builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
                else builder.setUserAuthenticationValidityDurationSeconds(-1)
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(builder.build()) }.generateKey()
            }
            val info = SecretKeyFactory.getInstance(key.algorithm, "AndroidKeyStore").getKeySpec(key, KeyInfo::class.java) as KeyInfo
            check(info.isInsideSecureHardware) { "Hardware-backed authentication is unavailable on this device." }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
            prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this), object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    try {
                        val challenge = ByteArray(32).also { SecureRandom().nextBytes(it) }
                        try { checkNotNull(result.cryptoObject?.cipher).doFinal(challenge).fill(0) } finally { challenge.fill(0) }
                        gateMessage = null; onSuccess()
                    } catch (e: Exception) { gateMessage = "Authentication could not be verified. ${e.message.orEmpty()}" }
                }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { gateMessage = errString.toString() }
            })
            val builder = BiometricPrompt.PromptInfo.Builder().setTitle("Unlock TXT").setSubtitle("Verify with Android to access your documents").setAllowedAuthenticators(auth)
            if (Build.VERSION.SDK_INT < 30) builder.setNegativeButtonText("Cancel")
            prompt?.authenticate(builder.build(), BiometricPrompt.CryptoObject(cipher))
        } catch (e: Exception) { gateMessage = e.message ?: "Authentication unavailable."
        }
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun App(vm: PadViewModel = viewModel()) {
        var sensitive by remember { mutableStateOf(false) }
        var backupUri by remember { mutableStateOf<Uri?>(null) }
        var restoreUri by remember { mutableStateOf<Uri?>(null) }
        var restorePassword by remember { mutableStateOf(false) }
        var conflict by remember { mutableStateOf(ConflictPolicy.KEEP_BOTH) }
        var saveAsUri by remember { mutableStateOf<Uri?>(null) }
        var saveAsEncrypted by remember { mutableStateOf(false) }
        var saveBeforeLeaving by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()
        val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { it?.let(vm::folder) }
        val backupPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { backupUri = it }
        val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { restoreUri = it }
        val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
            if (uri != null) { if (saveAsEncrypted) saveAsUri = uri else vm.saveAs(uri, null) }
        }
        val secure = appLock || vm.savingProtected || saveAsUri != null || vm.busy || sensitive || vm.editor?.encrypted == true || vm.unlockFile != null || !unlocked || backupUri != null || restorePassword
        SideEffect {
            if (secure) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        LaunchedEffect(incoming, unlocked, vm.busy, vm.saving) {
            val uri = incoming
            if (uri != null && unlocked && !vm.busy && !vm.saving) {
                incoming = null
                scope.launch {
                    try {
                        val file = vm.external(uri)
                        if (file.name.endsWith(".tpbak", true)) vm.navigate { restoreUri = uri }
                        else vm.open(file)
                    } catch (e: Exception) { vm.report(e) }
                }
            }
        }
        if (!unlocked) {
            LaunchedEffect(Unit) { authenticate { unlocked = true } }
            Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(Modifier.liquidGlass().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("TXT", style = MaterialTheme.typography.displayMedium)
                    Text("Your documents are locked.", Modifier.padding(vertical = 16.dp))
                    gateMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { authenticate { unlocked = true } }) { Text("Unlock with Android") }
                }
            }
            return
        }
        val navigationMotion = LocalGlassMotionEnabled.current
        AnimatedContent(targetState = vm.editor, contentKey = { it?.sessionId ?: "home" }, label = "document expansion",
            transitionSpec = { if (!navigationMotion) EnterTransition.None.togetherWith(ExitTransition.None) else (fadeIn(tween(180)) + slideInVertically(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.88f, stiffness = 420f), initialOffsetY = { it / 24 })).togetherWith(fadeOut(tween(120))) }) { doc ->
            if (doc == null && vm.showTrash) TrashScreen(vm.trash, vm.busy,
                onBack = { vm.showTrash = false }, onRestore = vm::restoreTrash, onDelete = vm::deleteTrash)
            else if (doc == null) HomeScreen(vm.files, vm.busy, vm.workspace, vm.defaultPrivate, appLock,
                onOpen = { if (it.name.endsWith(".tpbak", true)) restoreUri = it.uri else vm.open(it) },
                onFolder = { folderPicker.launch(vm.repository.workspace) }, onCreate = vm::newFile, onMove = vm::move,
                onBackup = { backupPicker.launch("TXT_Backup_${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.tpbak") },
                onRestore = { restorePicker.launch(arrayOf("*/*")) },
                onPrivateDefault = { vm.defaultPrivate = it; vm.repository.defaultPrivate = it },
                onAppLock = { enable -> authenticate { appLock = enable; unlocked = true; prefs.edit().putBoolean("app_lock", enable).apply() } },
                onRefresh = vm::refresh, onRename = vm::rename, onTrash = vm::trash, onShowTrash = { vm.showTrash = true; vm.refresh() },
                motionEnabled = animateGlass, onMotionChange = { animateGlass = it; prefs.edit().putBoolean("animated_glass", it).apply() })
            else EditorScreen(doc, vm.busy, saving = vm.saving, onBack = vm::back,
                onSave = { e, p -> vm.save(e, p) }, onSaveAs = { name, encrypted -> saveAsEncrypted = encrypted; exportPicker.launch(name) }, onMove = { vm.move(doc.file) }, onSensitivePrompt = { sensitive = it })
        }
        saveAsUri?.let { uri -> PasswordDialog("Password for saved copy", confirm = true,
            onDismiss = { saveAsUri = null; scope.launch { runCatching { android.provider.DocumentsContract.deleteDocument(contentResolver, uri) } } },
            onSensitive = { sensitive = it }, onPassword = { saveAsUri = null; vm.saveAs(uri, it) }) }
        vm.unlockFile?.let { f -> PasswordDialog("Unlock ${f.name}", onDismiss = { vm.unlockFile = null }, onSensitive = { sensitive = it }, onPassword = vm::unlock) }
        backupUri?.let { uri -> PasswordDialog("Backup master password", confirm = true,
            onDismiss = { backupUri = null; scope.launch { runCatching { android.provider.DocumentsContract.deleteDocument(contentResolver, uri) } } },
            onSensitive = { sensitive = it }, onPassword = { backupUri = null; vm.backup(uri, it) }) }
        restoreUri?.let { uri ->
            if (!restorePassword) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { restoreUri = null }, title = { Text("Restore offline backup") }, text = {
                Column {
                    Text("Choose how to handle matching filenames. The backup is verified before files are changed.")
                    ConflictPolicy.entries.forEach { policy ->
                        TextButton(onClick = { conflict = policy }) { Text((if (conflict == policy) "✓ " else "") + when (policy) {
                            ConflictPolicy.KEEP_BOTH -> "Keep both (auto-rename)"; ConflictPolicy.OVERWRITE -> "Overwrite existing"; ConflictPolicy.SKIP -> "Skip existing"
                        }) }
                    }
                    if (!vm.workspace) OutlinedButton(onClick = { folderPicker.launch(null) }) { Text("Choose workspace for public files") }
                }
            }, confirmButton = { TextButton(onClick = { restorePassword = true }, enabled = !vm.busy) { Text("Enter master password") } }, dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Cancel") } })
            else PasswordDialog("Backup master password", onDismiss = { restorePassword = false; restoreUri = null }, onSensitive = { sensitive = it },
                onPassword = { restorePassword = false; restoreUri = null; vm.restore(uri, it, conflict) })
        }
        if (vm.pendingNavigation != null && !saveBeforeLeaving && !vm.saving) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { vm.pendingNavigation = null }, title = { Text("Save your changes?") },
            text = { Text("Your latest edits have not been saved.") },
            confirmButton = { TextButton(onClick = { if (vm.editor?.encrypted == true) saveBeforeLeaving = true else vm.save(false, null, true) }) { Text("Save") } },
            dismissButton = { Row {
                TextButton(onClick = vm::discardAndNavigate) { Text("Discard") }
                TextButton(onClick = { vm.pendingNavigation = null }) { Text("Keep editing") }
            } })
        if (saveBeforeLeaving) PasswordDialog("Password to save", onDismiss = { saveBeforeLeaving = false }, onSensitive = { sensitive = it },
            onPassword = { saveBeforeLeaving = false; vm.save(true, it, true) })
        vm.notice?.let { notice -> AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { vm.notice = null }, title = { Text("TXT") }, text = { Text(notice) },
            confirmButton = { TextButton(onClick = { vm.notice = null }) { Text("OK") } }) }
        if (gateMessage != null && unlocked) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { gateMessage = null }, title = { Text("Android authentication") }, text = { Text(gateMessage.orEmpty()) },
            confirmButton = { TextButton(onClick = { gateMessage = null }) { Text("OK") } })
    }
}
