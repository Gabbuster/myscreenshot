package com.example.myscreenshot.extraction

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID

class ActionDetector {
    fun detect(
        text: String,
        entities: List<ExtractedEntity>,
        now: LocalDateTime,
        zoneId: ZoneId,
    ): List<DetectedAction> {
        val cleaned = text.trim()
        if (cleaned.isBlank()) return emptyList()

        val actions = mutableListOf<DetectedAction>()
        detectFlight(cleaned, now, zoneId)?.let(actions::add)
        detectHotel(cleaned, now, zoneId)?.let(actions::add)
        detectBill(cleaned, entities, now, zoneId)?.let(actions::add)
        detectAppointment(cleaned, now, zoneId)?.let(actions::add)
        detectDelivery(cleaned, now, zoneId)?.let(actions::add)
        detectExpiry(cleaned, now, zoneId)?.let(actions::add)

        if (actions.isEmpty()) {
            detectGeneric(cleaned, now, zoneId)?.let(actions::add)
        }
        return actions
            .suppressWeakNonTravelActionsForClearFlight(cleaned)
            .map { it.withSafeGeneratedTitle(zoneId) }
            .distinctBy { it.type + it.title + it.dateTime }
    }

    private fun detectFlight(text: String, now: LocalDateTime, zoneId: ZoneId): DetectedAction? {
        val travel = extractTravelInfo(text, now, zoneId) ?: return null
        if (!travel.hasTravelSignal) return null
        val title = travel.buildTitle().rejectMarketingTitleOrRebuild(travel)
        val confidenceScore = listOf(
            travel.airlineName != null,
            travel.flightNumber != null,
            travel.hasRoute,
            travel.departureDateTime != null,
            travel.originAirport != null && travel.destinationAirport != null,
        ).count { it } * 20
        return DetectedAction(
            id = UUID.randomUUID().toString(),
            type = "Travel",
            title = title,
            dateTime = travel.departureDateTime,
            endDateTime = travel.arrivalDateTime,
            amount = null,
            currency = null,
            location = travel.routeDisplay,
            confidence = when {
                confidenceScore >= 80 -> "High confidence"
                confidenceScore >= 40 -> "Review needed"
                else -> "Low confidence"
            },
            reminderSuggestions = listOf(
                ReminderSuggestion("Check-in reminder 24h before", 24 * 60),
                ReminderSuggestion("Airport reminder 3h before", 3 * 60),
                ReminderSuggestion("Flight departure", 0),
            ),
            needsConfirmation = true,
            rawTextEvidence = travel.summary(zoneId),
            calendarEvent = true,
            airlineName = travel.airlineName,
            flightNumber = travel.flightNumber,
            originAirport = travel.originAirport,
            destinationAirport = travel.destinationAirport,
            originCity = travel.originCity,
            destinationCity = travel.destinationCity,
            arrivalDateTime = travel.arrivalDateTime,
        )
    }

    private fun detectHotel(text: String, now: LocalDateTime, zoneId: ZoneId): DetectedAction? {
        val hasHotelKeyword = text.containsAny("hotel", "resort", "inn", "suites", "accommodation", "room", "guest", "stay")
        val hasStayKeyword = text.containsAny("check-in", "check in", "arrival", "check-out", "check out", "departure", "nights")
        if (!hasHotelKeyword && !hasStayKeyword) return null

        val stayDates = findHotelStayDates(text, now.toLocalDate())
        val checkIn = stayDates.checkIn
        val nightsCount = Regex("(\\d+)\\s*nights?", RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)?.toLongOrNull()
        val finalCheckOut = stayDates.checkOut ?: if (checkIn != null && nightsCount != null) checkIn.plusDays(nightsCount) else null

        if (checkIn == null && finalCheckOut == null && !hasHotelKeyword) return null

        val checkInTime = findTimeNear(text, "check") ?: findTimeNear(text, "arrival") ?: LocalTime.of(15, 0)
        val checkOutTime = findTimeNear(text, "check-out") ?: findTimeNear(text, "checkout") ?: findTimeNear(text, "departure") ?: LocalTime.of(11, 0)

        val dateTime = checkIn?.let { LocalDateTime.of(it, checkInTime).atZone(zoneId).toInstant().toEpochMilli() } ?: findDateTime(text, now, zoneId)
        val endDateTime = finalCheckOut?.let { LocalDateTime.of(it, checkOutTime).atZone(zoneId).toInstant().toEpochMilli() }

        val nights = nightsCount?.toString()
            ?: if (checkIn != null && finalCheckOut != null) ChronoUnit.DAYS.between(checkIn, finalCheckOut).coerceAtLeast(1).toString() else null

        val hotelName = findHotelName(text)

        val title = buildString {
            append("Hotel Stay in $hotelName")
            if (checkIn != null && finalCheckOut != null) {
                append(" from ${checkIn.format(shortDateFormatter)} to ${finalCheckOut.format(shortDateFormatter)}")
            } else if (checkIn != null) {
                append(" from ${checkIn.format(shortDateFormatter)}")
            }
        }

        return DetectedAction(
            id = UUID.randomUUID().toString(),
            type = "Hotel",
            title = title,
            dateTime = dateTime,
            endDateTime = endDateTime,
            amount = null,
            currency = null,
            location = hotelName,
            confidence = confidence(dateTime != null, nights != null, true),
            reminderSuggestions = listOf(
                ReminderSuggestion("Hotel prep reminder 7 days before", 7 * 24 * 60),
                ReminderSuggestion("Reminder 1 day before check-in", 24 * 60),
                ReminderSuggestion("Check-in reminder", 0),
            ),
            needsConfirmation = true,
            rawTextEvidence = text.take(500),
            calendarEvent = true,
        )
    }

    private fun detectBill(text: String, entities: List<ExtractedEntity>, now: LocalDateTime, zoneId: ZoneId): DetectedAction? {
        val hasBillKeyword = text.containsAny("due", "payment", "total amount", "amount due", "invoice", "bill", "balance")
        val hasMoneyEntity = entities.any { it.type == "money" }
        if (!hasBillKeyword && !hasMoneyEntity) return null
        val money = Regex("\\b(QAR|USD|EUR|GBP|AED|SAR)\\s*([0-9]+(?:[.,][0-9]{2})?)\\b", RegexOption.IGNORE_CASE).find(text)
        if (!hasBillKeyword && money == null) return null
        val dateTime = findDateTime(text, now, zoneId)
        val amount = money?.groupValues?.getOrNull(2)?.replace(",", ".")?.toDoubleOrNull()
        val currency = money?.groupValues?.getOrNull(1)?.uppercase(Locale.US)
        val title = buildString {
            append("Bill")
            if (currency != null && amount != null) append(" $currency ${String.format(Locale.US, "%.2f", amount)}")
            dateTime?.let { append(" due ${formatEpochDate(it, zoneId)}") }
        }
        return DetectedAction(
            id = UUID.randomUUID().toString(),
            type = "Bill",
            title = title,
            dateTime = dateTime,
            amount = amount,
            currency = currency,
            location = null,
            confidence = confidence(dateTime != null, money != null || hasMoneyEntity, text.containsAny("due date", "amount due")),
            reminderSuggestions = listOf(
                ReminderSuggestion("Reminder 3 days before", 3 * 24 * 60),
                ReminderSuggestion("Reminder on due date", 0),
            ),
            needsConfirmation = true,
            rawTextEvidence = text.take(500),
        )
    }

    private fun detectAppointment(text: String, now: LocalDateTime, zoneId: ZoneId): DetectedAction? {
        if (!text.containsAny("doctor", "clinic", "dentist", "hospital", "appointment", "meeting", "consultation", "booking")) return null
        val dateTime = findDateTime(text, now, zoneId)
        val place = Regex("([A-Za-z ]+Clinic|[A-Za-z ]+Hospital)", RegexOption.IGNORE_CASE).find(text)?.value?.trim()
        val appointmentKind = Regex("(Doctor|Dentist|Clinic|Hospital|Meeting|Consultation)[A-Za-z ]*", RegexOption.IGNORE_CASE)
            .find(text)?.value?.trim()?.replaceFirstChar { it.titlecase(Locale.US) } ?: "Appointment"
        val title = buildString {
            append(appointmentKind)
            place?.let { append(" at $it") }
            dateTime?.let { append(" on ${formatEpochDate(it, zoneId)}") }
        }
        return DetectedAction(
            id = UUID.randomUUID().toString(),
            type = "Appointment",
            title = title,
            dateTime = dateTime,
            amount = null,
            currency = null,
            location = place,
            confidence = confidence(dateTime != null, text.containsTime(), true),
            reminderSuggestions = listOf(
                ReminderSuggestion("Reminder 1 day before", 24 * 60),
                ReminderSuggestion("Reminder 2 hours before", 2 * 60),
            ),
            needsConfirmation = true,
            rawTextEvidence = text.take(500),
            calendarEvent = true,
        )
    }

    private fun detectDelivery(text: String, now: LocalDateTime, zoneId: ZoneId): DetectedAction? {
        if (!text.containsAny("delivery", "shipment", "tracking", "expected", "arriving", "dhl", "aramex", "fedex", "ups", "iherb", "amazon")) return null
        val dateTime = findDateTime(text, now, zoneId)
        val tracking = Regex("\\b\\d{8,20}\\b").find(text)?.value
        val title = buildString {
            append("Delivery")
            tracking?.let { append(" $it") }
            dateTime?.let { append(" by ${formatEpochDate(it, zoneId)}") }
        }
        return DetectedAction(
            id = UUID.randomUUID().toString(),
            type = "Delivery",
            title = title,
            dateTime = dateTime,
            amount = null,
            currency = null,
            location = tracking?.let { "Tracking: $it" },
            confidence = confidence(dateTime != null, tracking != null, true),
            reminderSuggestions = listOf(ReminderSuggestion("Reminder on delivery day", 0)),
            needsConfirmation = true,
            rawTextEvidence = text.take(500),
        )
    }

    private fun detectExpiry(text: String, now: LocalDateTime, zoneId: ZoneId): DetectedAction? {
        if (!text.containsAny("expires", "expiry", "valid until", "warranty", "passport", "visa", "insurance", "residence permit", "driving license")) return null
        val dateTime = findDateTime(text, now, zoneId)
        val title = buildString {
            append("Document expiry")
            dateTime?.let { append(" on ${formatEpochDate(it, zoneId)}") }
        }
        return DetectedAction(
            id = UUID.randomUUID().toString(),
            type = "Documents",
            title = title,
            dateTime = dateTime,
            amount = null,
            currency = null,
            location = null,
            confidence = confidence(dateTime != null, false, true),
            reminderSuggestions = listOf(
                ReminderSuggestion("Reminder 30 days before", 30 * 24 * 60),
                ReminderSuggestion("Reminder 7 days before", 7 * 24 * 60),
                ReminderSuggestion("Reminder 1 day before", 24 * 60),
            ),
            needsConfirmation = true,
            rawTextEvidence = text.take(500),
        )
    }

    private fun detectGeneric(text: String, now: LocalDateTime, zoneId: ZoneId): DetectedAction? {
        if (!text.containsAny("remember", "don't forget", "please bring", "submit", "send", "book", "call", "remind me", "tomorrow", "next week")) return null
        val dateTime = findDateTime(text, now, zoneId)
        return DetectedAction(
            id = UUID.randomUUID().toString(),
            type = "Documents",
            title = "Review reminder",
            dateTime = dateTime,
            amount = null,
            currency = null,
            location = null,
            confidence = if (dateTime == null) "Low confidence" else "Review needed",
            reminderSuggestions = listOf(ReminderSuggestion("Reminder at selected time", 0)),
            needsConfirmation = true,
            rawTextEvidence = text.take(500),
        )
    }

    private fun findDateTime(text: String, now: LocalDateTime, zoneId: ZoneId): Long? {
        val date = findDate(text, now.toLocalDate()) ?: return null
        val time = findTime(text) ?: LocalTime.of(9, 0)
        return LocalDateTime.of(date, time).atZone(zoneId).toInstant().toEpochMilli()
    }

    private fun findTime(text: String): LocalTime? {
        return Regex("\\b([01]?\\d|2[0-3])[:.]([0-5]\\d)\\b").find(text)?.let {
            LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        }
    }

    private fun findTimeNear(text: String, keyword: String): LocalTime? {
        val index = text.indexOf(keyword, ignoreCase = true)
        if (index < 0) return null
        val window = text.substring(index, minOf(text.length, index + 80))
        return findTime(window)
    }

    private data class TravelInfo(
        val airlineName: String?,
        val flightNumber: String?,
        val originAirport: String?,
        val destinationAirport: String?,
        val originCity: String?,
        val destinationCity: String?,
        val departureDateTime: Long?,
        val arrivalDateTime: Long?,
    ) {
        val hasRoute: Boolean = (originAirport != null || originCity != null) && (destinationAirport != null || destinationCity != null)
        val hasTravelSignal: Boolean = airlineName != null || flightNumber != null || hasRoute || departureDateTime != null
        val routeDisplay: String? = if (hasRoute) {
            "${originCity ?: originAirport.orEmpty()}${originAirport?.let { " ($it)" }.orEmpty()} → ${destinationCity ?: destinationAirport.orEmpty()}${destinationAirport?.let { " ($it)" }.orEmpty()}"
        } else {
            null
        }

        fun buildTitle(): String = when {
            airlineName != null && routeDisplay != null -> "$airlineName • ${routeDisplay.replace(Regex("\\s*\\([A-Z]{3}\\)"), "")}"
            flightNumber != null && routeDisplay != null -> "$flightNumber • ${routeDisplay.replace(Regex("\\s*\\([A-Z]{3}\\)"), "")}"
            routeDisplay != null -> routeDisplay.replace(Regex("\\s*\\([A-Z]{3}\\)"), "")
            airlineName != null && flightNumber != null -> "$airlineName • $flightNumber"
            flightNumber != null -> "Flight $flightNumber"
            airlineName != null -> "$airlineName flight"
            else -> "Flight reminder"
        }

        fun summary(zoneId: ZoneId): String = buildList {
            val firstLine = when {
                airlineName != null && flightNumber != null -> "$airlineName flight $flightNumber detected."
                airlineName != null -> "$airlineName flight detected."
                flightNumber != null -> "Flight $flightNumber detected."
                else -> "Flight booking detected."
            }
            add(firstLine)
            routeDisplay?.let { add("Flight from ${originCity ?: originAirport} (${originAirport.orEmpty()}) to ${destinationCity ?: destinationAirport} (${destinationAirport.orEmpty()}).") }
            departureDateTime?.let {
                add("")
                add("Departure:")
                add(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), zoneId).format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH)))
            }
            arrivalDateTime?.let {
                add("")
                add("Arrival:")
                add(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it), zoneId).format(DateTimeFormatter.ofPattern("d MMM yyyy HH:mm", Locale.ENGLISH)))
            }
        }.joinToString("\n")
    }

    private data class AirlineInfo(
        val name: String,
        val codes: Set<String>,
        val markers: Set<String>,
    )

    private data class AirportInfo(
        val code: String,
        val city: String,
        val names: Set<String>,
    )

    private fun extractTravelInfo(text: String, now: LocalDateTime, zoneId: ZoneId): TravelInfo? {
        val travelText = text.withoutMarketingLines()
        val airline = travelText.findAirline()
        val flightNumber = travelText.findFlightNumber()
        val route = travelText.findStructuredRoute()
        val departure = findDateTimeNearAny(travelText, now, zoneId, "departure", "depart", "flight", "boarding", "take off")
            ?: findDateTime(travelText, now, zoneId)
        val arrival = findDateTimeNearAny(travelText, now, zoneId, "arrival", "arrive", "landing")
            ?: findSecondDateTime(travelText, now, zoneId, departure)

        if (airline == null && flightNumber == null && route == null && departure == null) return null
        val airlineName = airline?.name ?: flightNumber?.take(2)?.uppercase(Locale.US)?.let { code ->
            airlines.firstOrNull { code in it.codes }?.name
        }
        return TravelInfo(
            airlineName = airlineName,
            flightNumber = flightNumber,
            originAirport = route?.first?.code,
            destinationAirport = route?.second?.code,
            originCity = route?.first?.city,
            destinationCity = route?.second?.city,
            departureDateTime = departure,
            arrivalDateTime = arrival,
        )
    }

    private fun String.withoutMarketingLines(): String =
        lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.isMarketingNoise() }
            .joinToString("\n")

    private fun String.isMarketingNoise(): Boolean =
        marketingNoiseRegex.containsMatchIn(this)

    private fun String.findAirline(): AirlineInfo? {
        val clean = lowercase(Locale.US)
        return airlines.firstOrNull { airline ->
            airline.markers.any { clean.contains(it) }
        }
    }

    private fun String.findStructuredRoute(): Pair<AirportInfo, AirportInfo>? {
        val clean = withoutMarketingLines()
        Regex("\\b([A-Z]{3})\\b\\s*(?:→|->|to|-|–|—)\\s*\\b([A-Z]{3})\\b", RegexOption.IGNORE_CASE)
            .find(clean)?.let { match ->
                val origin = airportForCode(match.groupValues[1])
                val destination = airportForCode(match.groupValues[2])
                if (origin != null && destination != null && origin != destination) return origin to destination
            }

        Regex("([A-Za-z][A-Za-z .'-]{1,34})\\s*\\(?([A-Z]{3})\\)?\\s*(?:→|->|to|-|–|—)\\s*([A-Za-z][A-Za-z .'-]{1,34})\\s*\\(?([A-Z]{3})\\)?", RegexOption.IGNORE_CASE)
            .find(clean)?.let { match ->
                val origin = airportForCode(match.groupValues[2])?.copy(city = match.groupValues[1].cleanTravelCity())
                val destination = airportForCode(match.groupValues[4])?.copy(city = match.groupValues[3].cleanTravelCity())
                if (origin != null && destination != null && origin != destination) return origin to destination
            }

        val codeMatches = Regex("\\b[A-Z]{3}\\b").findAll(clean)
            .map { it.value.uppercase(Locale.US) }
            .mapNotNull { airportForCode(it) }
            .distinctBy { it.code }
            .toList()
        if (codeMatches.size >= 2) return codeMatches[0] to codeMatches[1]

        val cityMatches = airports.filter { airport ->
            airport.names.any { Regex("\\b${Regex.escape(it)}\\b", RegexOption.IGNORE_CASE).containsMatchIn(clean) }
        }.distinctBy { it.code }
        if (cityMatches.size >= 2) return cityMatches[0] to cityMatches[1]

        return null
    }

    private fun airportForCode(code: String): AirportInfo? =
        airports.firstOrNull { it.code.equals(code, ignoreCase = true) }

    private fun String.cleanTravelCity(): String =
        replace(Regex("\\b(from|to|departure|arrival|airport)\\b", RegexOption.IGNORE_CASE), "")
            .trim(' ', '-', ':', '(', ')')
            .ifBlank { this.trim() }

    private fun findDateTimeNearAny(text: String, now: LocalDateTime, zoneId: ZoneId, vararg keywords: String): Long? {
        keywords.forEach { keyword ->
            val index = text.indexOf(keyword, ignoreCase = true)
            if (index >= 0) {
                val start = maxOf(0, index - 80)
                val end = minOf(text.length, index + 180)
                val window = text.substring(start, end)
                val afterKeyword = text.substring(index, end)
                val date = findAllDatesInTextOrder(afterKeyword, now.toLocalDate()).firstOrNull()
                    ?: findAllDatesInTextOrder(window, now.toLocalDate()).firstOrNull()
                    ?: findAllDatesInTextOrder(text, now.toLocalDate()).firstOrNull()
                val time = findTime(afterKeyword) ?: findTime(window) ?: findTime(text)
                if (date != null) {
                    return LocalDateTime.of(date, time ?: LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
                }
            }
        }
        return null
    }

    private fun findSecondDateTime(text: String, now: LocalDateTime, zoneId: ZoneId, first: Long?): Long? {
        val dates = findAllDatesInTextOrder(text, now.toLocalDate())
        val times = Regex("\\b([01]?\\d|2[0-3])[:.]([0-5]\\d)\\b").findAll(text)
            .map { LocalTime.of(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
            .toList()
        val date = dates.drop(1).firstOrNull() ?: dates.firstOrNull()
        val time = times.drop(1).firstOrNull() ?: return null
        val second = LocalDateTime.of(date ?: now.toLocalDate(), time).atZone(zoneId).toInstant().toEpochMilli()
        return if (first == null || second > first) second else null
    }

    private data class HotelStayDates(
        val checkIn: LocalDate?,
        val checkOut: LocalDate?,
    )

    private fun findHotelStayDates(text: String, today: LocalDate): HotelStayDates {
        val checkIn = findDateNearAny(text, today, "check-in", "check in", "arrival", "from")
        val checkOut = findDateNearAny(text, today, "check-out", "check out", "checkout", "departure", "to")
        if (checkIn != null || checkOut != null) return HotelStayDates(checkIn, checkOut)

        findHotelDateRange(text, today)?.let { return it }

        val orderedDates = findAllDatesInTextOrder(text, today)
        return HotelStayDates(
            checkIn = orderedDates.firstOrNull(),
            checkOut = orderedDates.drop(1).firstOrNull(),
        )
    }

    private fun findDateNearAny(text: String, today: LocalDate, vararg keywords: String): LocalDate? {
        keywords.forEach { keyword ->
            val index = text.indexOf(keyword, ignoreCase = true)
            if (index >= 0) {
                val window = text.substring(index, minOf(text.length, index + 110))
                findAllDatesInTextOrder(window, today).firstOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun findHotelDateRange(text: String, today: LocalDate): HotelStayDates? {
        val monthRegex = "(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)"

        Regex("\\b(\\d{1,2})\\s*(?:-|to|until|–|—)\\s*(\\d{1,2})\\s+$monthRegex(?:\\s+(\\d{4}))?\\b", RegexOption.IGNORE_CASE)
            .find(text)?.let { match ->
                val month = match.groupValues[3]
                val year = match.groupValues[4].toIntOrNull() ?: today.year
                val checkIn = parseMonthDay(match.groupValues[1], month, year, today)
                val checkOut = parseMonthDay(match.groupValues[2], month, year, today)
                if (checkIn != null && checkOut != null) return HotelStayDates(checkIn, normalizeCheckout(checkIn, checkOut))
            }

        Regex("\\b$monthRegex\\s+(\\d{1,2})\\s*(?:-|to|until|–|—)\\s*(\\d{1,2})(?:,)?\\s*(\\d{4})?\\b", RegexOption.IGNORE_CASE)
            .find(text)?.let { match ->
                val month = match.groupValues[1]
                val year = match.groupValues[4].toIntOrNull() ?: today.year
                val checkIn = parseMonthDay(match.groupValues[2], month, year, today)
                val checkOut = parseMonthDay(match.groupValues[3], month, year, today)
                if (checkIn != null && checkOut != null) return HotelStayDates(checkIn, normalizeCheckout(checkIn, checkOut))
            }

        return null
    }

    private fun parseMonthDay(day: String, month: String, year: Int, today: LocalDate): LocalDate? {
        val pattern = if (month.length > 3) "d MMMM yyyy" else "d MMM yyyy"
        return runCatching {
            normalizeYear(
                LocalDate.parse("$day $month $year", DateTimeFormatter.ofPattern(pattern, Locale.ENGLISH)),
                today,
            )
        }.getOrNull()
    }

    private fun normalizeCheckout(checkIn: LocalDate, checkOut: LocalDate): LocalDate =
        if (checkOut.isBefore(checkIn) || checkOut == checkIn) checkOut.plusYears(1) else checkOut

    private fun findHotelName(text: String): String {
        val line = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { it.containsAny("hotel", "resort", "inn", "suites", "apartment") && it.length in 4..80 }
        if (line != null) {
            return line.replace(Regex("\\b(booking|confirmed|reservation|check-in|check-out)\\b", RegexOption.IGNORE_CASE), "")
                .trim(' ', '-', ':')
                .ifBlank { "Hotel" }
        }
        return Regex("([A-Z][A-Za-z]+(?:\\s+[A-Z][A-Za-z]+){0,4})\\s+(Hotel|Resort|Inn|Suites|Apartment)", RegexOption.IGNORE_CASE)
            .find(text)?.value?.trim() ?: "Hotel"
    }

    private fun findFlightRoute(text: String): Pair<String, String>? {
        val airportPattern = "([A-Za-z][A-Za-z .'-]{1,34})\\s*\\(?([A-Z]{3})\\)?"
        Regex("$airportPattern\\s+(?:to|→|-)\\s+$airportPattern", RegexOption.IGNORE_CASE)
            .find(text)?.let { match ->
                val fromName = match.groupValues[1].cleanPlaceName()
                val fromCode = match.groupValues[2].uppercase(Locale.US)
                val toName = match.groupValues[3].cleanPlaceName()
                val toCode = match.groupValues[4].uppercase(Locale.US)
                return displayPlace(fromName, fromCode) to displayPlace(toName, toCode)
            }

        Regex("\\b([A-Z]{3})\\s+(?:to|→|-)\\s+([A-Z]{3})\\b", RegexOption.IGNORE_CASE)
            .find(text)?.let { match ->
                return match.groupValues[1].uppercase(Locale.US) to match.groupValues[2].uppercase(Locale.US)
            }

        return null
    }

    private fun displayPlace(name: String, code: String): String =
        if (name.length <= 2 || name.equals(code, ignoreCase = true)) code else name

    private fun String.cleanPlaceName(): String =
        replace(Regex("\\b(from|to|departure|arrival)\\b", RegexOption.IGNORE_CASE), "")
            .trim(' ', '-', ':', '(', ')')
            .ifBlank { this.trim() }

    private fun findAllDates(text: String, today: LocalDate): List<LocalDate> {
        return findAllDatesInTextOrder(text, today).distinct().sorted()
    }

    private fun findAllDatesInTextOrder(text: String, today: LocalDate): List<LocalDate> {
        val monthRegex = "(January|February|March|April|May|June|July|August|September|October|November|December|Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)"
        val matches = mutableListOf<Pair<Int, LocalDate>>()

        // dd MMMM yyyy
        Regex("\\b(\\d{1,2})\\s+$monthRegex\\s+(\\d{4})\\b", RegexOption.IGNORE_CASE)
            .findAll(text).forEach { match ->
                val day = match.groupValues[1]
                val month = match.groupValues[2]
                val year = match.groupValues[3]
                val f = if (month.length > 3) "d MMMM yyyy" else "d MMM yyyy"
                runCatching { LocalDate.parse("$day $month $year", DateTimeFormatter.ofPattern(f, Locale.ENGLISH)) }
                    .getOrNull()?.let { matches.add(match.range.first to it) }
            }

        // MMMM dd yyyy
        Regex("\\b$monthRegex\\s+(\\d{1,2})(?:,)?\\s+(\\d{4})\\b", RegexOption.IGNORE_CASE)
            .findAll(text).forEach { match ->
                val month = match.groupValues[1]
                val day = match.groupValues[2]
                val year = match.groupValues[3]
                val f = if (month.length > 3) "MMMM d yyyy" else "MMM d yyyy"
                runCatching { LocalDate.parse("$month $day $year", DateTimeFormatter.ofPattern(f, Locale.ENGLISH)) }
                    .getOrNull()?.let { matches.add(match.range.first to it) }
            }

        // dd MMMM (no year following)
        Regex("\\b(\\d{1,2})\\s+$monthRegex(?!\\s+\\d{4})\\b", RegexOption.IGNORE_CASE)
            .findAll(text).forEach { match ->
                val day = match.groupValues[1]
                val month = match.groupValues[2]
                val f = if (month.length > 3) "d MMMM" else "d MMM"
                runCatching { MonthDay.parse("$day $month", DateTimeFormatter.ofPattern(f, Locale.ENGLISH)) }
                    .getOrNull()?.let { matches.add(match.range.first to normalizeYear(it.atYear(today.year), today)) }
            }

        // MMMM dd (no year following)
        Regex("\\b$monthRegex\\s+(\\d{1,2})(?!,?\\s+\\d{4})\\b", RegexOption.IGNORE_CASE)
            .findAll(text).forEach { match ->
                val month = match.groupValues[1]
                val day = match.groupValues[2]
                val f = if (month.length > 3) "MMMM d" else "MMM d"
                runCatching { MonthDay.parse("$month $day", DateTimeFormatter.ofPattern(f, Locale.ENGLISH)) }
                    .getOrNull()?.let { matches.add(match.range.first to normalizeYear(it.atYear(today.year), today)) }
            }

        // dd/MM/yyyy
        Regex("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})\\b").findAll(text).forEach { match ->
            val parts = match.value.split('/', '-')
            runCatching { LocalDate.of(parts[2].toInt(), parts[1].toInt(), parts[0].toInt()) }
                .getOrNull()?.let { matches.add(match.range.first to it) }
        }

        // dd/MM (no year following)
        Regex("\\b(\\d{1,2})[/-](\\d{1,2})(?![/-]\\d{2,4})\\b").findAll(text).forEach { match ->
            val parts = match.value.split('/', '-')
            runCatching { LocalDate.of(today.year, parts[1].toInt(), parts[0].toInt()) }
                .getOrNull()?.let { matches.add(match.range.first to normalizeYear(it, today)) }
        }

        if (matches.isEmpty()) {
            if (text.contains("tomorrow", true)) matches.add(0 to today.plusDays(1))
            if (text.contains("next week", true)) matches.add(0 to today.plusWeeks(1))
        }

        return matches.sortedBy { it.first }.map { it.second }.distinct()
    }

    private fun normalizeYear(date: LocalDate, today: LocalDate): LocalDate =
        if (date.isBefore(today.minusDays(2))) date.plusYears(1) else date

    private fun findDate(text: String, today: LocalDate): LocalDate? = findAllDates(text, today).firstOrNull()

    private fun confidence(hasDate: Boolean, hasDetail: Boolean, strongKeyword: Boolean): String = when {
        hasDate && hasDetail && strongKeyword -> "High confidence"
        hasDate || strongKeyword -> "Review needed"
        else -> "Low confidence"
    }

    private fun formatEpochDate(epochMillis: Long, zoneId: ZoneId): String =
        LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMillis), zoneId).toLocalDate().format(shortDateFormatter)

    private fun String.rejectMarketingTitleOrRebuild(travel: TravelInfo): String =
        if (containsMarketingTitleText()) travel.buildTitle() else this

    private fun String.containsMarketingTitleText(): Boolean =
        marketingTitleRegex.containsMatchIn(this)

    private fun DetectedAction.withSafeGeneratedTitle(zoneId: ZoneId): DetectedAction {
        if (!title.containsMarketingTitleText()) return this
        val rebuilt = when (type.lowercase()) {
            "travel" -> {
                val route = listOfNotNull(originCity, destinationCity).takeIf { it.size == 2 }?.joinToString(" → ")
                when {
                    airlineName != null && route != null -> "$airlineName • $route"
                    flightNumber != null && route != null -> "$flightNumber • $route"
                    route != null -> route
                    flightNumber != null -> "Flight $flightNumber"
                    airlineName != null -> "$airlineName flight"
                    else -> "Flight reminder"
                }
            }
            "bill" -> buildString {
                append("Bill")
                if (currency != null && amount != null) append(" $currency ${String.format(Locale.US, "%.2f", amount)}")
                dateTime?.let { append(" due ${formatEpochDate(it, zoneId)}") }
            }
            "hotel" -> "Hotel reservation"
            "appointment" -> "Appointment"
            "delivery" -> "Delivery"
            "documents" -> "Document reminder"
            else -> "Reminder"
        }
        return copy(title = rebuilt)
    }

    private fun List<DetectedAction>.suppressWeakNonTravelActionsForClearFlight(text: String): List<DetectedAction> {
        if (none { it.type == "Travel" } || !text.hasClearFlightConfirmation()) return this
        return filter { it.type == "Travel" }
    }

    private fun String.hasClearFlightConfirmation(): Boolean {
        val travelText = withoutMarketingLines()
        val hasAirline = travelText.findAirline() != null || travelText.containsAny("airways", "airlines", "airline", "flight")
        val hasFlightNumber = findFlightNumber() != null
        val hasTicketReference = Regex(
            "\\b(booking reference|booking confirmed|confirmation|flight number|ticket number|e-?ticket|boarding pass|PNR|reservation code)\\b",
            RegexOption.IGNORE_CASE,
        ).containsMatchIn(travelText)
        val hasRouteOrAirport = travelText.findStructuredRoute() != null ||
            travelText.containsAny("airport", "departure", "arrival", "gate", "terminal", "boarding")
        return hasFlightNumber && (hasAirline || hasTicketReference || hasRouteOrAirport)
    }

    private fun String.hasFlightContext(): Boolean =
        withoutMarketingLines().let {
            it.findAirline() != null ||
                it.findStructuredRoute() != null ||
                it.containsAny("flight", "departure", "boarding", "booking", "airport", "airways", "airlines", "airline", "gate", "terminal", "pnr", "e-ticket", "ticket number", "flight number")
        }

    private fun String.findFlightNumber(): String? {
        val knownAirlineCodes = setOf(
            "QR", "EK", "EY", "SV", "GF", "WY", "KU", "BA", "AF", "KL", "LH", "LX", "OS", "TK", "AA", "DL", "UA", "AC",
            "IB", "AZ", "SQ", "CX", "QF", "AI", "6E", "FZ", "XY", "PC", "RJ", "MS", "ET", "SA", "MH", "TG", "JL", "NH",
        )
        return Regex("\\b([A-Z0-9]{2})\\s?([0-9]{2,4}[A-Z]?)\\b", RegexOption.IGNORE_CASE)
            .findAll(withoutMarketingLines())
            .firstOrNull { match ->
                match.groupValues[1].uppercase(Locale.US) in knownAirlineCodes
            }
            ?.let { "${it.groupValues[1]}${it.groupValues[2]}".uppercase(Locale.US) }
    }

    private fun String.containsTime(): Boolean = Regex("\\b([01]?\\d|2[0-3])[:.]([0-5]\\d)\\b").containsMatchIn(this)

    private fun String.containsAny(vararg needles: String): Boolean =
        needles.any { contains(it, ignoreCase = true) }

    private companion object {
        val shortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
        val marketingNoiseRegex = Regex(
            "\\b(join|subscribe|download|claim|offer|promotion|insurance|rewards?|reward programme|privilege club|newsletter|book a hotel|rent a car|upgrade your seat|buy insurance|special offer|avios)\\b",
            RegexOption.IGNORE_CASE,
        )
        val marketingTitleRegex = Regex(
            "\\b(join|subscribe|download|claim|offer|promotion|insurance|rewards?|newsletter|avios|privilege club|legal|terms|footer)\\b",
            RegexOption.IGNORE_CASE,
        )
        val airlines = listOf(
            AirlineInfo("Qatar Airways", setOf("QR"), setOf("qatar airways", "qatarairways.com")),
            AirlineInfo("Air France", setOf("AF"), setOf("air france", "airfrance.com")),
            AirlineInfo("Lufthansa", setOf("LH"), setOf("lufthansa", "lufthansa.com")),
            AirlineInfo("Ryanair", setOf("FR"), setOf("ryanair", "ryanair.com")),
            AirlineInfo("easyJet", setOf("U2", "EJU"), setOf("easyjet", "easyjet.com")),
            AirlineInfo("British Airways", setOf("BA"), setOf("british airways", "ba.com")),
            AirlineInfo("Emirates", setOf("EK"), setOf("emirates", "emirates.com")),
            AirlineInfo("Turkish Airlines", setOf("TK"), setOf("turkish airlines", "turkishairlines.com")),
            AirlineInfo("Etihad", setOf("EY"), setOf("etihad", "etihad.com")),
            AirlineInfo("KLM", setOf("KL"), setOf("klm", "klm.com")),
            AirlineInfo("ITA Airways", setOf("AZ"), setOf("ita airways", "itaspa.com")),
            AirlineInfo("Delta", setOf("DL"), setOf("delta", "delta.com")),
            AirlineInfo("United", setOf("UA"), setOf("united airlines", "united.com")),
            AirlineInfo("American Airlines", setOf("AA"), setOf("american airlines", "aa.com")),
            AirlineInfo("Singapore Airlines", setOf("SQ"), setOf("singapore airlines", "singaporeair.com")),
        )
        val airports = listOf(
            AirportInfo("DOH", "Doha", setOf("doha", "hamad")),
            AirportInfo("CDG", "Paris", setOf("paris", "charles de gaulle")),
            AirportInfo("ORY", "Paris", setOf("paris", "orly")),
            AirportInfo("LHR", "London", setOf("london", "heathrow")),
            AirportInfo("LGW", "London", setOf("london", "gatwick")),
            AirportInfo("JFK", "New York", setOf("new york", "john f kennedy")),
            AirportInfo("EWR", "New York", setOf("newark", "new york")),
            AirportInfo("FCO", "Rome", setOf("rome", "fiumicino")),
            AirportInfo("MXP", "Milan", setOf("milan", "malpensa")),
            AirportInfo("LIN", "Milan", setOf("milan", "linate")),
            AirportInfo("DXB", "Dubai", setOf("dubai")),
            AirportInfo("AUH", "Abu Dhabi", setOf("abu dhabi")),
            AirportInfo("IST", "Istanbul", setOf("istanbul")),
            AirportInfo("AMS", "Amsterdam", setOf("amsterdam", "schiphol")),
            AirportInfo("FRA", "Frankfurt", setOf("frankfurt")),
            AirportInfo("MUC", "Munich", setOf("munich")),
            AirportInfo("MAD", "Madrid", setOf("madrid")),
            AirportInfo("BCN", "Barcelona", setOf("barcelona")),
            AirportInfo("SIN", "Singapore", setOf("singapore", "changi")),
            AirportInfo("ATL", "Atlanta", setOf("atlanta")),
            AirportInfo("LAX", "Los Angeles", setOf("los angeles")),
            AirportInfo("SFO", "San Francisco", setOf("san francisco")),
            AirportInfo("ORD", "Chicago", setOf("chicago", "o'hare")),
            AirportInfo("DFW", "Dallas", setOf("dallas", "fort worth")),
            AirportInfo("MIA", "Miami", setOf("miami")),
        )
    }
}
