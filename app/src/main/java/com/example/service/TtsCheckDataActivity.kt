package com.example.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import com.example.data.model.VoiceCatalog
import java.util.ArrayList

class TtsCheckDataActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val returnIntent = Intent()
        val availableLangs = ArrayList<String>()
        val unavailableLangs = ArrayList<String>()

        VoiceCatalog.ALL_VOICES.forEach { voice ->
            val langTag = voice.locale.replace("-", "_")
            if (!availableLangs.contains(langTag)) {
                availableLangs.add(langTag)
            }
        }

        returnIntent.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_AVAILABLE_VOICES, availableLangs)
        returnIntent.putStringArrayListExtra(TextToSpeech.Engine.EXTRA_UNAVAILABLE_VOICES, unavailableLangs)
        setResult(TextToSpeech.Engine.CHECK_VOICE_DATA_PASS, returnIntent)
        finish()
    }
}
