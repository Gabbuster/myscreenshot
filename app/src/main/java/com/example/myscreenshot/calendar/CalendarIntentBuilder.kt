package com.example.myscreenshot.calendar

import android.content.Intent
import android.provider.CalendarContract
import com.example.myscreenshot.data.Reminder
import java.util.concurrent.TimeUnit

object CalendarIntentBuilder {
    fun build(reminder: Reminder): Intent {
        val start = reminder.dateTime ?: System.currentTimeMillis()
        val end = reminder.endDateTime ?: (start + TimeUnit.HOURS.toMillis(1))
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, reminder.title)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            reminder.location?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "${reminder.notes}\n\nCreated from Screenshot Reminder. Source processed locally.",
            )
        }
    }
}

