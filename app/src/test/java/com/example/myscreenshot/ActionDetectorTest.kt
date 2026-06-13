package com.example.myscreenshot

import com.example.myscreenshot.extraction.ActionDetector
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionDetectorTest {
    private val zoneId: ZoneId = ZoneId.of("Asia/Qatar")
    private val now = LocalDateTime.of(2026, 6, 12, 12, 0)

    @Test
    fun hotelBooking_usesLabeledCheckInAndCheckOutDates() {
        val text = """
            Booking confirmed
            Pearl Resort Hotel
            Check-in 18 June 2026 15:00
            Check-out 21 June 2026 11:00
            3 nights
        """.trimIndent()

        val action = ActionDetector().detect(text, emptyList(), now, zoneId).single { it.type == "Hotel" }

        assertEquals("High confidence", action.confidence)
        assertEquals("Hotel Stay in Pearl Resort Hotel from Jun 18 to Jun 21", action.title)
        assertEquals(Instant.parse("2026-06-18T12:00:00Z").toEpochMilli(), action.dateTime)
        assertEquals(Instant.parse("2026-06-21T08:00:00Z").toEpochMilli(), action.endDateTime)
        assertTrue(action.reminderSuggestions.any { it.label.contains("7 days") })
    }

    @Test
    fun hotelBooking_supportsMonthFirstDateRangesWithoutYear() {
        val text = """
            Hotel reservation
            Check in Jun 18
            Check out Jun 20
            2 nights
        """.trimIndent()

        val action = ActionDetector().detect(text, emptyList(), now, zoneId).single { it.type == "Hotel" }

        assertNotNull(action.dateTime)
        assertEquals(Instant.parse("2026-06-18T12:00:00Z").toEpochMilli(), action.dateTime)
        assertEquals(Instant.parse("2026-06-20T08:00:00Z").toEpochMilli(), action.endDateTime)
    }

    @Test
    fun hotelBooking_supportsCompactSharedMonthDateRange() {
        val text = """
            The Grove Hotel
            Stay 18 - 21 June 2026
            Guest room confirmed
        """.trimIndent()

        val action = ActionDetector().detect(text, emptyList(), now, zoneId).single { it.type == "Hotel" }

        assertEquals("High confidence", action.confidence)
        assertEquals(Instant.parse("2026-06-18T12:00:00Z").toEpochMilli(), action.dateTime)
        assertEquals(Instant.parse("2026-06-21T08:00:00Z").toEpochMilli(), action.endDateTime)
        assertTrue(action.reminderSuggestions.any { it.label == "Reminder 1 day before check-in" })
    }

    @Test
    fun qatarAirwaysConfirmation_returnsOnlyFlightAction() {
        val text = """
            Qatar Airways
            Booking confirmed
            Booking reference ABC123
            Doha DOH to Paris CDG
            Flight QR041
            Departure 18 June 2026 08:30
            Terminal 1
        """.trimIndent()

        val actions = ActionDetector().detect(text, emptyList(), now, zoneId)

        assertEquals(listOf("Travel"), actions.map { it.type })
        assertTrue(actions.single().title.contains("QR041"))
    }

    @Test
    fun flightConfirmationWithFlightNumber_suppressesOtherCategoriesEvenWithHotelWords() {
        val text = """
            Qatar Airways
            Booking confirmed
            Flight QR041
            Departure 18 June 2026 08:30

            Booking details
            Passenger room preference
            Documents required at check-in
        """.trimIndent()

        val actions = ActionDetector().detect(text, emptyList(), now, zoneId)

        assertEquals(listOf("Travel"), actions.map { it.type })
    }

    @Test
    fun qatarAirwaysFlightNumberWithSpace_returnsOnlyFlightAction() {
        val text = """
            Qatar Airways confirmation
            Flight Number QR 704
            Departure Doha
            Arrival London
            22 June 2026 14:10
        """.trimIndent()

        val actions = ActionDetector().detect(text, emptyList(), now, zoneId)

        assertEquals(listOf("Travel"), actions.map { it.type })
        assertTrue(actions.single().title.contains("QR704"))
    }
}
