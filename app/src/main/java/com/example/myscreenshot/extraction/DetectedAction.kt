package com.example.myscreenshot.extraction

data class DetectedAction(
    val id: String,
    val type: String,
    val title: String,
    val dateTime: Long?,
    val amount: Double?,
    val currency: String?,
    val location: String?,
    val endDateTime: Long? = null,
    val confidence: String,
    val reminderSuggestions: List<ReminderSuggestion>,
    val needsConfirmation: Boolean,
    val rawTextEvidence: String,
    val calendarEvent: Boolean = false,
)

