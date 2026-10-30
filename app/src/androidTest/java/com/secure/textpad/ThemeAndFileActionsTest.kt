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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
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
    @get:Rule val compose = createComposeRule()
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
        val destination = File(context.filesDir, "qa").apply { mkdirs() }
        File(destination, "$name-semantics.txt").writeText(compose.onRoot().printToString())
        File(destination, "$name.png").outputStream().use {
            check(compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }
    @Test fun darkHomeIsReadableAndHasFileActions() {
        home(true)
        compose.onNodeWithText("TXT").assertIsDisplayed()
        compose.onNodeWithText("Private Vault").assertIsDisplayed()
        capture("home-dark")
        compose.onNodeWithContentDescription("File options: Meeting notes.txt").performClick()
        compose.onNodeWithText("Rename").assertIsDisplayed()
        compose.onNodeWithText("Move to…").assertIsDisplayed()
        compose.onNodeWithText("Move to Trash").assertIsDisplayed()
        compose.onNodeWithText("Rename").performClick()
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
}
