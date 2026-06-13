package com.example.myscreenshot.extraction

interface LocalAiExtractor {
    suspend fun extractActions(text: String): List<DetectedAction>
}

