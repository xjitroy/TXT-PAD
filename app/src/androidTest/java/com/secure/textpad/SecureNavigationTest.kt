package com.secure.textpad

import android.net.Uri
import android.provider.Settings
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.secure.textpad.data.PadFile
import org.junit.Assert.assertTrue
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
