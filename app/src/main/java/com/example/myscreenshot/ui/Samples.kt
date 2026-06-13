package com.example.myscreenshot.ui

import com.example.myscreenshot.data.Reminder
import com.example.myscreenshot.extraction.DetectedAction
import com.example.myscreenshot.extraction.ReminderSuggestion
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

object Samples {
    val flightText = """
        Qatar Airways
        Booking confirmed
        Doha (DOH) to Paris (CDG)
        Flight QR041
        Date 18 June 2026
        Departure 08:30
        Booking reference ABC123
    """.trimIndent()

    val billText = """
        Electricity Bill
        Total amount due QAR 340.50
        Payment due date 27 June 2026
    """.trimIndent()

    val appointmentText = """
        Doctor Appointment
        20 June 2026
        10:30
        Doha Clinic
    """.trimIndent()

    fun reminders(): List<Reminder> = listOf(
        reminder("Flight QR041 to Paris", "Travel", "Check-in opens in 1 day", "Doha (DOH) to Paris (CDG)", date(2026, 6, 18, 8, 30)),
        reminder("Electricity Bill", "Bill", "In 3 days", "QAR 340.50", date(2026, 6, 27, 9, 0), 340.50, "QAR"),
        reminder("Doctor Appointment", "Appointment", "In 5 days", "Doha Clinic", date(2026, 6, 20, 10, 30)),
        reminder("Delivery from iHerb", "Delivery", "Expected 24 June", "Tracking: 123456789", date(2026, 6, 24, 9, 0)),
    )

    fun actions(): List<DetectedAction> = listOf(
        DetectedAction(
            id = UUID.randomUUID().toString(),
            type = "Travel",
            title = "Flight QR041 to Paris",
            dateTime = date(2026, 6, 18, 8, 30),
            amount = null,
            currency = null,
            location = "Doha (DOH) to Paris (CDG)",
            confidence = "High confidence",
            reminderSuggestions = listOf(
                ReminderSuggestion("Check-in reminder 24h before", 1440),
                ReminderSuggestion("Airport reminder 3h before", 180),
            ),
            needsConfirmation = true,
            rawTextEvidence = flightText,
            calendarEvent = true,
        ),
    )

    private fun reminder(
        title: String,
        type: String,
        alert: String,
        notes: String,
        dateTime: Long,
        amount: Double? = null,
        currency: String? = null,
    ) = Reminder(
        id = UUID.randomUUID().toString(),
        title = title,
        type = type,
        sourceType = "sample",
        sourceImageUri = null,
        ocrText = "",
        dateTime = dateTime,
        amount = amount,
        currency = currency,
        location = null,
        notes = "$alert\n$notes",
        confidence = "High confidence",
        status = "active",
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
    )

    private fun date(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

