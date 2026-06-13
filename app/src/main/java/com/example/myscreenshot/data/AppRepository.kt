package com.example.myscreenshot.data

import android.content.Context
import android.net.Uri
import com.example.myscreenshot.extraction.DetectedAction
import com.example.myscreenshot.extraction.ReminderSuggestion
import com.example.myscreenshot.reminders.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.util.UUID

class AppRepository(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.get(appContext)
    private val scheduler = ReminderScheduler(appContext)

    fun observeReminders(): Flow<List<Reminder>> = database.reminderDao().observeActive()

    fun observeReminder(id: String): Flow<Reminder?> = database.reminderDao().observeById(id)

    suspend fun deleteReminder(id: String) {
        database.reminderDao().updateStatus(id, "deleted", System.currentTimeMillis())
    }

    suspend fun saveAction(
        action: DetectedAction,
        title: String,
        notes: String,
        sourceType: String,
        sourceUri: String?,
        ocrText: String,
    ): Reminder {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val storedSourceUri = persistSourceImage(sourceUri, sourceType, id)
        val reminder = Reminder(
            id = id,
            title = title.ifBlank { action.title },
            type = action.type,
            sourceType = sourceType,
            sourceImageUri = storedSourceUri,
            ocrText = ocrText,
            dateTime = action.dateTime,
            endDateTime = action.endDateTime,
            amount = action.amount,
            currency = action.currency,
            location = action.location,
            notes = notes,
            confidence = action.confidence,
            status = "active",
            createdAt = now,
            updatedAt = now,
        )
        database.reminderDao().upsert(reminder)
        database.sourceDocumentDao().upsert(
            SourceDocument(
                id = UUID.randomUUID().toString(),
                localUri = storedSourceUri,
                mimeType = sourceType,
                extractedText = ocrText,
                createdAt = now,
            ),
        )
        val alerts = action.reminderSuggestions.toAlerts(id, action.dateTime)
        database.reminderDao().upsertAlerts(alerts)
        alerts.forEach { scheduler.schedule(reminder, it) }
        return reminder
    }

    private fun persistSourceImage(sourceUri: String?, sourceType: String, reminderId: String): String? {
        if (sourceUri == null || !sourceType.equals("image", ignoreCase = true)) return sourceUri
        val uri = Uri.parse(sourceUri)
        val outputDir = File(appContext.filesDir, "source_images").apply { mkdirs() }
        val outputFile = File(outputDir, "$reminderId.jpg")
        return runCatching {
            appContext.contentResolver.openInputStream(uri)?.use { input ->
                outputFile.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching sourceUri
            Uri.fromFile(outputFile).toString()
        }.getOrDefault(sourceUri)
    }

    private fun List<ReminderSuggestion>.toAlerts(reminderId: String, dateTime: Long?): List<ReminderAlert> {
        val base = dateTime ?: return emptyList()
        return mapNotNull { suggestion ->
            val alertTime = base - suggestion.offsetMinutes * 60_000
            if (alertTime <= System.currentTimeMillis()) return@mapNotNull null
            ReminderAlert(
                id = UUID.randomUUID().toString(),
                reminderId = reminderId,
                alertDateTime = alertTime,
                alertLabel = suggestion.label,
                isScheduled = false,
                isTriggered = false,
            )
        }
    }
}
