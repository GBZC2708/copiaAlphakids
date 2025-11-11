package com.example.alphakids.data.tts

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.core.os.bundleOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidTtsService @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsService, TextToSpeech.OnInitListener {

    private val tts: TextToSpeech = TextToSpeech(context, this)
    private val utteranceParams = ConcurrentHashMap<String, BundleParams>()
    @Volatile
    private var ready = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        } else {
            ready = false
        }
    }

    override fun speak(id: String, msg: String) {
        if (!ready) return
        val params = utteranceParams.getOrPut(id) { BundleParams(id) }
        tts.speak(msg, TextToSpeech.QUEUE_FLUSH, params.bundle, params.utteranceId)
    }

    private data class BundleParams(val utteranceId: String) {
        val bundle: Bundle = bundleOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId)
    }

    @Suppress("deprecation")
    protected fun finalize() {
        tts.shutdown()
    }
}
