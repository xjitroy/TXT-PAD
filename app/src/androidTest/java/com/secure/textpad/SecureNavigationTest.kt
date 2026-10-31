package com.secure.textpad

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.secure.textpad.data.PadFile
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class SecureNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun screenshotProtectionCoversTheEntireEncryptedExitAnimation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun shell(command: String) {
            instrumentation.uiAutomation.executeShellCommand(command).use {
                android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
            }
        }
        val old = Settings.Global.getString(compose.activity.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE) ?: "1"
        val model = ViewModelProvider(compose.activity)[PadViewModel::class.java]
        fun protected() = compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0
        try {
            shell("settings put global animator_duration_scale 1")
            Thread.sleep(150)
            compose.waitUntil(15000) { !model.busy }
            compose.waitForIdle()
            compose.mainClock.autoAdvance = false
            compose.runOnIdle {
                model.newFile()
                val document = checkNotNull(model.editor)
                document.edit(TextFieldValue("PRIVATE FRAME"))
                document.saved(PadFile(Uri.parse("file:///qa/private-frame.stxt"), "private-frame.stxt", true), "PRIVATE FRAME", true)
            }
            compose.mainClock.advanceTimeBy(800)
            compose.onNodeWithText("PRIVATE FRAME").assertExists()
            compose.runOnIdle { assertTrue(protected()) }
            compose.runOnIdle { model.navigate {} }
            compose.mainClock.advanceTimeBy(32)
            compose.onNodeWithText("private-frame.stxt").assertExists()
            compose.runOnIdle { assertTrue("The exiting encrypted page must remain protected", protected()) }
            compose.mainClock.advanceTimeBy(1000)
            compose.mainClock.autoAdvance = true
            compose.onNodeWithText("PRIVATE FRAME").assertDoesNotExist()
            compose.waitUntil(10000) { !protected() }
        } finally {
            compose.mainClock.autoAdvance = true
            shell("settings put global animator_duration_scale $old")
        }
    }
}

class ExternalIntentRecreationTest {
    @get:Rule val compose = createEmptyComposeRule()

    @Test fun consumedExternalOpenIsNotReplayedAfterActivityRecreation() {
        val intent = Intent(Intent.ACTION_VIEW,
            Uri.parse("content://com.secure.textpad.qa.missing/file.txt"),
            ApplicationProvider.getApplicationContext<android.content.Context>(), MainActivity::class.java)
        ActivityScenario.launch<MainActivity>(intent).use { scenario ->
            lateinit var model: PadViewModel
            scenario.onActivity { model = ViewModelProvider(it)[PadViewModel::class.java] }
            compose.waitUntil(15000) { model.notice != null && !model.busy }
            compose.onNodeWithText("OK").performClick()
            compose.runOnIdle { assertNull(model.notice) }
            scenario.recreate()
            compose.waitForIdle()
            // Allow a wrongly replayed asynchronous URI request to report its error.
            Thread.sleep(1000)
            compose.runOnIdle {
                assertNull("A consumed external intent must not be opened again", model.notice)
                assertNull(model.editor)
            }
        }
    }
}
