package com.example.myscreenshot.extraction

import android.content.Context
import java.time.LocalDateTime
import java.time.ZoneId

interface LocalAiExtractor {
    suspend fun extractActions(text: String): List<DetectedAction>
}

data class OnDeviceAiResult(
    val actions: List<DetectedAction>,
    val entities: List<ExtractedEntity>,
    val note: String?,
)

class OnDeviceAiExtractor(
    private val context: Context,
    private val entityExtractor: EntityExtractor = EntityExtractor(context),
    private val actionDetector: ActionDetector = ActionDetector(),
) : LocalAiExtractor {
    override suspend fun extractActions(text: String): List<DetectedAction> =
        analyze(text, LocalDateTime.now(), ZoneId.systemDefault()).actions

    suspend fun analyze(
        text: String,
        now: LocalDateTime,
        zoneId: ZoneId,
    ): OnDeviceAiResult {
        val entities = entityExtractor.extract(text)
        val actions = actionDetector.detect(
            text = text,
            entities = entities,
            now = now,
            zoneId = zoneId,
        )
        val note = when {
            actions.isEmpty() -> null
            entities.isNotEmpty() -> "Screen4U used OCR and local language detection"
            else -> "Screen4U used local screenshot rules"
        }
        return OnDeviceAiResult(actions = actions, entities = entities, note = note)
    }
}
