package com.example.myscreenshot.extraction

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.mlkit.nl.entityextraction.Entity
import com.google.mlkit.nl.entityextraction.EntityExtraction
import com.google.mlkit.nl.entityextraction.EntityExtractionParams
import com.google.mlkit.nl.entityextraction.EntityExtractorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class EntityExtractor(private val context: Context) {
    suspend fun extract(text: String): List<ExtractedEntity> {
        if (text.isBlank()) return emptyList()
        return runCatching {
            val extractor = EntityExtraction.getClient(
                EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build(),
            )
            suspendTask(extractor.downloadModelIfNeeded())
            val params = EntityExtractionParams.Builder(text).build()
            suspendTask(extractor.annotate(params)).flatMap { annotation ->
                annotation.entities.map { entity ->
                    ExtractedEntity(entity.typeName(), annotation.annotatedText)
                }
            }
        }.getOrElse { emptyList() }
    }

    private fun Entity.typeName(): String = when (type) {
        Entity.TYPE_DATE_TIME -> "date_time"
        Entity.TYPE_MONEY -> "money"
        Entity.TYPE_ADDRESS -> "address"
        Entity.TYPE_PHONE -> "phone"
        Entity.TYPE_URL -> "url"
        else -> "other"
    }

    private suspend fun <T> suspendTask(task: Task<T>): T =
        suspendCancellableCoroutine { continuation ->
            task.addOnSuccessListener { continuation.resume(it) }
            task.addOnFailureListener { continuation.resumeWith(Result.failure(it)) }
        }
}
