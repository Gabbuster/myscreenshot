package com.example.myscreenshot.extraction

class DummyLocalAiExtractor : LocalAiExtractor {
    override suspend fun extractActions(text: String): List<DetectedAction> = emptyList()
}

