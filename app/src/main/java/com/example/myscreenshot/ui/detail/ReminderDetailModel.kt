package com.example.myscreenshot.ui.detail

import com.example.myscreenshot.data.Reminder

enum class ReminderCategory {
    Travel,
    Hotel,
    Appointment,
    Bill,
    Document,
    Delivery,
    Unknown,
}

data class DetailAction(
    val label: String,
    val kind: ActionKind,
)

enum class ActionKind {
    Calendar,
    Maps,
    Call,
    Pay,
    Track,
    View,
    Share,
    Reminder,
    CheckIn,
    Airport,
    Edit,
    Delete,
}

fun Reminder.category(): ReminderCategory = when (type.trim().lowercase()) {
    "travel", "flight" -> ReminderCategory.Travel
    "hotel" -> ReminderCategory.Hotel
    "appointment" -> ReminderCategory.Appointment
    "bill", "payment" -> ReminderCategory.Bill
    "document", "documents" -> ReminderCategory.Document
    "delivery", "package" -> ReminderCategory.Delivery
    else -> ReminderCategory.Unknown
}

fun getCategoryLabel(category: ReminderCategory): String = when (category) {
    ReminderCategory.Travel -> "TRAVEL"
    ReminderCategory.Hotel -> "HOTEL"
    ReminderCategory.Appointment -> "APPOINTMENT"
    ReminderCategory.Bill -> "BILL"
    ReminderCategory.Document -> "DOCUMENT"
    ReminderCategory.Delivery -> "DELIVERY"
    ReminderCategory.Unknown -> "REMINDER"
}

fun getPrimaryActions(category: ReminderCategory): List<DetailAction> = when (category) {
    ReminderCategory.Travel -> listOf(
        DetailAction("Add to Calendar", ActionKind.Calendar),
        DetailAction("Check-in", ActionKind.CheckIn),
        DetailAction("Open Airport", ActionKind.Airport),
    )
    ReminderCategory.Hotel -> listOf(
        DetailAction("Add to Calendar", ActionKind.Calendar),
        DetailAction("Open Maps", ActionKind.Maps),
        DetailAction("Call Hotel", ActionKind.Call),
    )
    ReminderCategory.Appointment -> listOf(
        DetailAction("Add to Calendar", ActionKind.Calendar),
        DetailAction("Open Maps", ActionKind.Maps),
        DetailAction("Call", ActionKind.Call),
    )
    ReminderCategory.Bill -> listOf(
        DetailAction("Pay", ActionKind.Pay),
        DetailAction("Add Reminder", ActionKind.Reminder),
        DetailAction("View Bill", ActionKind.View),
    )
    ReminderCategory.Document -> listOf(
        DetailAction("Add Reminder", ActionKind.Reminder),
        DetailAction("Open Document", ActionKind.View),
        DetailAction("Share", ActionKind.Share),
    )
    ReminderCategory.Delivery -> listOf(
        DetailAction("Track", ActionKind.Track),
        DetailAction("Open Address", ActionKind.Maps),
        DetailAction("Add Reminder", ActionKind.Reminder),
    )
    ReminderCategory.Unknown -> listOf(
        DetailAction("Add Reminder", ActionKind.Reminder),
        DetailAction("Edit", ActionKind.Edit),
        DetailAction("Delete", ActionKind.Delete),
    )
}

fun getSmartReminderSuggestions(category: ReminderCategory): List<String> = when (category) {
    ReminderCategory.Travel -> listOf("Check-in opens", "Leave for airport", "Flight departure")
    ReminderCategory.Hotel -> listOf("Prepare documents", "Check-in reminder", "Check-out reminder")
    ReminderCategory.Appointment -> listOf("Reminder 1 day before", "Reminder 2 hours before", "Leave on time")
    ReminderCategory.Bill -> listOf("Due soon", "Due tomorrow", "Due today")
    ReminderCategory.Document -> listOf("Renew 6 months before", "Renew 3 months before", "Expiry warning")
    ReminderCategory.Delivery -> listOf("Delivery expected", "Package arriving today", "Follow up if delayed")
    ReminderCategory.Unknown -> listOf("Review reminder", "Act on time")
}

fun DetailAction.isAvailable(reminder: Reminder): Boolean = when (kind) {
    ActionKind.Calendar -> reminder.dateTime != null
    ActionKind.Maps -> reminder.location?.isNotBlank() == true
    ActionKind.Share -> true
    ActionKind.Delete -> true
    else -> false
}

fun DetailAction.isVisibleAction(reminder: Reminder): Boolean = when (kind) {
    ActionKind.Calendar -> reminder.dateTime != null && reminder.calendarSavedAt == null
    ActionKind.Maps -> reminder.location?.isNotBlank() == true
    ActionKind.Share -> true
    else -> false
}
