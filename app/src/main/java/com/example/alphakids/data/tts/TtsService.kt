package com.example.alphakids.data.tts

interface TtsService {
    fun speak(id: String, msg: String)
}
