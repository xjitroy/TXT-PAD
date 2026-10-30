package com.secure.textpad

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import android.view.WindowManager
import org.junit.Before
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.geometry.Offset
import com.secure.textpad.security.SessionPassword
import android.app.Application
import androidx.core.graphics.ColorUtils
import androidx.test.platform.app.InstrumentationRegistry
import com.secure.textpad.data.PadFile
import com.secure.textpad.data.StorageRepository
import com.secure.textpad.ui.editor.EditorDocument
import com.secure.textpad.ui.editor.EditorScreen
import com.secure.textpad.ui.home.HomeScreen
import com.secure.textpad.ui.theme.LiquidGlassTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.UUID

class ThemeAndFileActionsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Before fun configureEditorWindow() {
        compose.activity.runOnUiThread {
            WindowCompat.setDecorFitsSystemWindows(compose.activity.window, false)
            compose.activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }
    private val sample = PadFile(Uri.parse("file:///qa/Meeting-notes.txt"), "Meeting notes.txt", true, 640, 1793337600000, "Meeting-notes.txt", false)
    private fun home(dark: Boolean, enlarged: Boolean = false) {
        compose.setContent {
            LiquidGlassTheme(darkTheme = dark, dynamicColor = false, animateBackdrop = false) {
                val density = LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(density.density, if (enlarged) 1.6f else 1f)) {
                    HomeScreen(listOf(sample), false, true, false, false,
                        onOpen = {}, onFolder = {}, onCreate = {}, onMove = {}, onBackup = {}, onRestore = {},
                        onPrivateDefault = {}, onAppLock = {}, onRefresh = {}, onRename = { _, _ -> }, onTrash = {}, onShowTrash = {})
                }
            }
        }
    }
    private fun capture(name: String) {
        compose.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val additionalOutput = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val destination = (additionalOutput?.let(::File) ?: File(context.filesDir, "qa")).apply { mkdirs() }
        File(destination, "$name-semantics.txt").writeText(compose.onRoot().printToString())
        // Capture the actual device compositor, including GPU RenderNode blur and the IME.
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        val bitmap = checkNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        try { File(destination, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
        finally { bitmap.recycle() }
    }
    @Test fun darkHomeIsReadableAndHasFileActions() {
        home(true)
        compose.onNodeWithText("TXT").assertIsDisplayed()
        compose.onNodeWithText("Private Vault").assertIsDisplayed()
        capture("home-dark")
        compose.onNodeWithContentDescription("File options: Meeting notes.txt").performScrollTo().performClick()
        compose.onNodeWithText("Rename").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Move to…").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Move to Trash").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Rename").performScrollTo().performClick()
        compose.onNodeWithText("Rename file").assertIsDisplayed()
    }
    @Test fun lightHomeScreenshot() { home(false); capture("home-light") }
    @Test fun enlargedTextKeepsFiltersAndActionsAccessible() {
        home(true, true)
        compose.onNodeWithText("Private Vault").assertIsDisplayed()
        capture("home-large-text")
        println("QA New File bounds: " + compose.onNodeWithText("New File").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithText("New File").assertIsDisplayed()
    }
    @Test fun rootContentColorTracksSurfaceInBothThemes() {
        var dark by androidx.compose.runtime.mutableStateOf(true)
        var ink = Color.Unspecified
        var surface = Color.Unspecified
        var expected = Color.Unspecified
        compose.setContent {
            LiquidGlassTheme(darkTheme = dark, dynamicColor = false, animateBackdrop = false) {
                val local = LocalContentColor.current
                val scheme = MaterialTheme.colorScheme
                SideEffect { ink = local; surface = scheme.surface; expected = scheme.onSurface }
                Box(Modifier.fillMaxSize())
            }
        }
        compose.runOnIdle {
            assertEquals(expected, ink)
            assertTrue(ColorUtils.calculateContrast(ink.toArgb(), surface.toArgb()) >= 7)
            dark = false
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(expected, ink)
            assertTrue(ColorUtils.calculateContrast(ink.toArgb(), surface.toArgb()) >= 7)
        }
    }
    @Test fun editorToolsRemainSharpAndAccessibleInDarkMode() {
        val document = EditorDocument(sample, "Meeting notes\nA clear, comfortable place to write.\nবাংলা লেখাও পরিষ্কার থাকবে।", false)
        compose.setContent {
            LiquidGlassTheme(darkTheme = true, dynamicColor = false, animateBackdrop = false) {
                EditorScreen(document, false, onBack = {}, onSave = { _, _ -> }, onMove = {}, onSaveAs = { _, _ -> }, onSensitivePrompt = {})
            }
        }
        compose.onNodeWithText("Save").assertIsDisplayed()
        capture("editor-dark")
        compose.onNodeWithText("Tools ⋯").performClick()
        compose.onNodeWithText("Comfort font size · 16sp").assertIsDisplayed()
    }
    @Test fun privateFileRenameTrashRestoreAndPermanentDelete() = runBlocking {
        val repo = StorageRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        val token = "qa-${UUID.randomUUID()}"
        val text = "Offline file actions\nবাংলা"
        var file = repo.create("$token.txt", true)
        file = repo.save(file, text, false)
        file = repo.rename(file, "$token-renamed.txt")
        assertEquals(text, repo.readText(file))
        repo.moveToTrash(file)
        val trashed = repo.listTrash().single { it.path == file.path }
        assertFalse(File(file.uri.path!!).exists())
        file = repo.restoreTrash(trashed.id)
        assertEquals(text, repo.readText(file))
        assertFalse(repo.listTrash().any { it.id == trashed.id })
        repo.moveToTrash(file)
        val again = repo.listTrash().single { it.path == file.path }
        repo.deleteTrashPermanently(again.id)
        assertFalse(repo.listTrash().any { it.id == again.id })
    }
    @Test fun encryptedFileKeepsPasswordWhenRestoredFromTrash() = runBlocking {
        val repo = StorageRepository(InstrumentationRegistry.getInstrumentation().targetContext)
        var file = repo.create("qa-${UUID.randomUUID()}.stxt", true)
        file = repo.save(file, "Secret", true, "long unique password".toCharArray())
        repo.moveToTrash(file)
        val item = repo.listTrash().single { it.path == file.path }
        file = repo.restoreTrash(item.id)
        assertTrue(repo.detect(file))
        assertEquals("Secret", repo.readText(file, "long unique password".toCharArray()))
        repo.moveToTrash(file)
        repo.deleteTrashPermanently(repo.listTrash().single { it.path == file.path }.id)
    }
    private fun assertCaretInsideViewport(document: EditorDocument) {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag("editor.input").performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val node = compose.onNodeWithTag("editor.input").fetchSemanticsNode()
        val viewport = compose.onNodeWithTag("editor.viewport").fetchSemanticsNode().boundsInRoot
        val caret = layouts.single().getCursorRect(document.value.selection.end).translate(node.positionInRoot)
        assertTrue("Caret bottom $caret must stay above viewport bottom $viewport", caret.bottom <= viewport.bottom)
        assertTrue("Caret top $caret must remain visible in $viewport", caret.top >= viewport.top)
    }
    @Test fun keyboardAndTypingKeepTheLastLineVisible() {
        val text = (1..145).joinToString("\n") { "Line $it — বাংলা লিখছি। Typing stays comfortable and visible." }
        val document = EditorDocument(sample, text, false).apply { select(text.length, text.length) }
        compose.setContent {
            LiquidGlassTheme(darkTheme = true, dynamicColor = false, animateBackdrop = false) {
                EditorScreen(document, false, onBack = {}, onSave = { _, _ -> }, onMove = {}, onSaveAs = { _, _ -> }, onSensitivePrompt = {})
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Tools ⋯").performClick()
        compose.onNodeWithTag("editor.lineNumbers").performScrollTo().performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("editor.gutter").assertIsDisplayed()
        val beforeKeyboard = compose.onNodeWithTag("editor.viewport").fetchSemanticsNode().boundsInRoot.height
        compose.onNodeWithTag("editor.input").performClick()
        compose.runOnIdle { document.select(document.value.text.length, document.value.text.length) }
        compose.waitUntil(15000) {
            compose.onNodeWithTag("editor.viewport").fetchSemanticsNode().boundsInRoot.height < beforeKeyboard - 100
        }
        compose.onNodeWithTag("editor.input").performTextInput("\nশেষ লাইনে লিখছি")
        compose.waitForIdle()
        assertTrue(document.value.text.endsWith("শেষ লাইনে লিখছি"))
        assertCaretInsideViewport(document)
        capture("editor-keyboard")
        val extra = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        if (extra != null) {
            val screenshot = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            if (screenshot != null) File(extra, "editor-keyboard-full.png").outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
    }
    @Test fun undoRedoSelectsAndRevealsTheChangedLocation() {
        val original = (1..80).joinToString("\n") { "Original line $it" }
        val document = EditorDocument(sample, original, false)
        document.edit(TextFieldValue(original + "\nVISIBLE CHANGE"))
        compose.setContent {
            LiquidGlassTheme(darkTheme = true, dynamicColor = false, animateBackdrop = false) {
                EditorScreen(document, false, onBack = {}, onSave = { _, _ -> }, onMove = {}, onSaveAs = { _, _ -> }, onSensitivePrompt = {})
            }
        }
        compose.onNodeWithTag("editor.undo").performClick()
        compose.runOnIdle { assertEquals(original, document.value.text); assertEquals(original.length, document.value.selection.start) }
        compose.onNodeWithTag("editor.redo").performClick()
        compose.runOnIdle { assertEquals("\nVISIBLE CHANGE", document.value.text.substring(document.value.selection.start, document.value.selection.end)) }
        compose.waitForIdle()
        assertCaretInsideViewport(document)
    }
    @Test fun typingContinuesWhileAnAutoSaveIsRunning() {
        val document = EditorDocument(sample, "Note", false)
        compose.setContent {
            LiquidGlassTheme(animateBackdrop = false) {
                EditorScreen(document, busy = false, saving = true, onBack = {}, onSave = { _, _ -> }, onMove = {}, onSaveAs = { _, _ -> }, onSensitivePrompt = {})
            }
        }
        compose.onNodeWithTag("editor.input").performClick().performTextInput("typing")
        compose.runOnIdle { assertTrue(document.value.text.contains("typing")) }
    }
    @Test fun saveAsAcceptsPhpAndCustomFilename() {
        val document = EditorDocument(sample, "<?php echo 'offline'; ?>", false)
        var requestedName = ""
        compose.setContent {
            LiquidGlassTheme(animateBackdrop = false) {
                EditorScreen(document, false, onBack = {}, onSave = { _, _ -> }, onMove = {},
                    onSaveAs = { name, _ -> requestedName = name }, onSensitivePrompt = {})
            }
        }
        compose.onNodeWithText("Tools ⋯").performClick()
        compose.onNodeWithText("Save As…").performScrollTo().performClick()
        compose.onAllNodes(hasSetTextAction()).onLast().performTextReplacement("index.php")
        compose.onNodeWithText("Choose location").performClick()
        compose.runOnIdle { assertEquals("index.php", requestedName) }
    }
    @Test fun sessionPasswordIsSealedAndConsumedWithoutPersistingPlaintext() {
        val password = "session-only password".toCharArray()
        val sealed = SessionPassword.seal(password)
        assertTrue(password.all { it == '\u0000' })
        val opened = sealed.open()
        assertArrayEquals("session-only password".toCharArray(), opened)
        opened.fill('\u0000'); sealed.clear()
        assertTrue(runCatching { sealed.open() }.isFailure)
    }
    @Test fun defaultAutoSavePersistsEditsThroughTheViewModel() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val vm = PadViewModel(context.applicationContext as Application)
        compose.waitUntil(15000) { !vm.busy }
        compose.runOnIdle { vm.newFile() }
        val document = vm.editor!!
        compose.setContent {
            LiquidGlassTheme(animateBackdrop = false) {
                EditorScreen(document, vm.busy, saving = vm.saving, onBack = {}, onSave = { encrypted, password -> vm.save(encrypted, password) },
                    onMove = {}, onSaveAs = { _, _ -> }, onSensitivePrompt = {})
            }
        }
        compose.onNodeWithTag("editor.input").performTextInput("Auto-save is on by default")
        compose.waitUntil(15000) { !document.fresh && !document.dirty && !vm.saving }
        runBlocking {
            assertEquals(document.value.text, vm.repository.readText(document.file))
            document.clear()
            vm.repository.moveToTrash(document.file)
            vm.repository.deleteTrashPermanently(vm.repository.listTrash().single { it.path == document.file.path }.id)
        }
    }

    @Test fun unlockedEncryptedDocumentAutoSavesWithoutAnotherPasswordPrompt() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val vm = PadViewModel(context.applicationContext as Application)
        val file = runBlocking {
            val created = vm.repository.create("qa-${UUID.randomUUID()}.stxt", true)
            vm.repository.save(created, "Locked note", true, "auto-save test password".toCharArray())
        }
        compose.waitUntil(15000) { !vm.busy }
        compose.runOnIdle { vm.open(file) }
        compose.waitUntil(15000) { vm.unlockFile != null && !vm.busy }
        compose.runOnIdle { vm.unlock("auto-save test password".toCharArray()) }
        compose.waitUntil(15000) { vm.editor != null && !vm.busy }
        val document = vm.editor!!
        compose.setContent {
            LiquidGlassTheme(animateBackdrop = false) {
                EditorScreen(document, vm.busy, saving = vm.saving, onBack = {}, onSave = { encrypted, password -> vm.save(encrypted, password) },
                    onMove = {}, onSaveAs = { _, _ -> }, onSensitivePrompt = {})
            }
        }
        compose.onNodeWithTag("editor.input").performTextInput("EDITED ")
        compose.waitUntil(45000) { (!document.dirty && !vm.saving) || vm.notice != null }
        compose.runOnIdle { assertNull("Encrypted auto-save failed: ${vm.notice}", vm.notice) }
        runBlocking {
            assertTrue(vm.repository.detect(document.file))
            assertTrue(vm.repository.readText(document.file, "auto-save test password".toCharArray()).contains("EDITED"))
            document.clear()
            vm.repository.moveToTrash(document.file)
            vm.repository.deleteTrashPermanently(vm.repository.listTrash().single { it.path == document.file.path }.id)
        }
    }
    @Test fun saveAsWritesPhpAndArbitraryExtensionAsUtf8() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repository = StorageRepository(context)
        val text = "<?php echo 'বাংলা'; ?>"
        listOf("php", "html", "custom-extension").forEach { extension ->
            val file = File(context.filesDir, "qa-${UUID.randomUUID()}.$extension").apply { createNewFile() }
            try {
                val destination = repository.external(Uri.fromFile(file))
                repository.save(destination, text, false)
                assertEquals(text, file.readText(Charsets.UTF_8))
            } finally { file.delete() }
        }
    }

}
