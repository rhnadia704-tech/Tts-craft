package com.example

import com.example.data.model.VoiceCatalog
import com.example.data.remote.DRM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DrmAndVoiceTest {

    @Test
    fun testSecMsGecGeneration() {
        val secMsGec = DRM.generateSecMsGec()
        assertNotNull(secMsGec)
        assertEquals(64, secMsGec.length)
        assertTrue(secMsGec.matches(Regex("^[0-9A-F]{64}$")))
    }

    @Test
    fun testMuidGeneration() {
        val muid = DRM.generateMuid()
        assertNotNull(muid)
        assertEquals(32, muid.length)
        assertTrue(muid.matches(Regex("^[0-9A-F]{32}$")))
    }

    @Test
    fun testVoiceCatalogAndSampleSentences() {
        assertTrue(VoiceCatalog.ALL_VOICES.isNotEmpty())
        VoiceCatalog.ALL_VOICES.forEach { voice ->
            val sample = voice.getSampleSentence()
            assertTrue(sample.isNotBlank())
            assertTrue(sample.contains(voice.name))
        }
    }
}
