package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.VoiceCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("EdgeTTS Pro", appName)
    }

    @Test
    fun `verify voice catalog`() {
        val voices = VoiceCatalog.ALL_VOICES
        assertTrue("Voice catalog should have voices", voices.isNotEmpty())
        val defaultVoice = VoiceCatalog.getDefaultVoice()
        assertNotNull("Default voice should exist", defaultVoice)
        assertEquals("fr-FR-DeniseNeural", defaultVoice.id)
    }
}
