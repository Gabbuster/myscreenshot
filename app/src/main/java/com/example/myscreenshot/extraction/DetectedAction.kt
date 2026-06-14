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
    val airlineName: String? = null,
    val flightNumber: String? = null,
    val originAirport: String? = null,
    val destinationAirport: String? = null,
    val originCity: String? = null,
    val destinationCity: String? = null,
    val arrivalDateTime: Long? = null,
)
