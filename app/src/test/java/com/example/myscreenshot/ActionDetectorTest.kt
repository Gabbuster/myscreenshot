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
        val action = actions.single()
        assertEquals("Qatar Airways • Doha → Paris", action.title)
        assertEquals("Qatar Airways", action.airlineName)
        assertEquals("QR041", action.flightNumber)
        assertEquals("DOH", action.originAirport)
        assertEquals("CDG", action.destinationAirport)
        assertEquals("Doha", action.originCity)
        assertEquals("Paris", action.destinationCity)
        assertTrue(action.rawTextEvidence.contains("Qatar Airways flight QR041 detected."))
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
        assertEquals("QR704", actions.single().flightNumber)
    }

    @Test
    fun flightConfirmation_ignoresMarketingWhenBuildingTitle() {
        val text = """
            Qatar Airways
            Join Privilege Club
            Claim your Avios
            Download our app
            Booking confirmed
            Doha DOH → Paris CDG
            Flight QR041
            Departure 22 May 2026 01:35
            Arrival 22 May 2026 07:45
        """.trimIndent()

        val action = ActionDetector().detect(text, emptyList(), now, zoneId).single()

        assertEquals("Travel", action.type)
        assertEquals("Qatar Airways • Doha → Paris", action.title)
        assertTrue(!action.title.contains("Join", ignoreCase = true))
        assertTrue(!action.title.contains("Avios", ignoreCase = true))
        assertEquals("High confidence", action.confidence)
        assertEquals(Instant.parse("2026-05-21T22:35:00Z").toEpochMilli(), action.dateTime)
        assertEquals(Instant.parse("2026-05-22T04:45:00Z").toEpochMilli(), action.arrivalDateTime)
    }

    @Test
    fun globalTitleSanitizer_rejectsMarketingTitleText() {
        val text = """
            Subscribe to newsletter
            Offer expires 20 June 2026
            Amount due QAR 120.00
        """.trimIndent()

        val action = ActionDetector().detect(text, emptyList(), now, zoneId).single { it.type == "Bill" }

        assertTrue(!action.title.contains("Subscribe", ignoreCase = true))
        assertTrue(!action.title.contains("Offer", ignoreCase = true))
        assertTrue(action.title.startsWith("Bill"))
    }
}
