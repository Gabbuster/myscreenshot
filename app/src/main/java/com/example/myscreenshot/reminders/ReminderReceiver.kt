package com.example.myscreenshot.reminders

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.myscreenshot.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID) ?: return
        val action = intent.getStringExtra(EXTRA_ACTION) ?: "open"
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.get(context).reminderDao()
            val reminder = dao.observeById(reminderId).firstOrNull()
            when (action) {
                "done" -> dao.updateStatus(reminderId, "completed", System.currentTimeMillis())
                "snooze" -> reminder?.let { ReminderScheduler(context).scheduleOneHourSnooze(it) }
                else -> reminder?.let { NotificationHelper(context).showReminder(it, intent.getStringExtra(EXTRA_LABEL) ?: "Reminder") }
            }
            pending.finish()
        }
    }

    companion object {
        private const val EXTRA_REMINDER_ID = "reminderId"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_ACTION = "action"

        fun intent(context: Context, reminderId: String, label: String): Intent =
            Intent(context, ReminderReceiver::class.java).apply {
                putExtra(EXTRA_REMINDER_ID, reminderId)
                putExtra(EXTRA_LABEL, label)
            }

        fun actionIntent(context: Context, reminderId: String, action: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                (reminderId + action).hashCode(),
                Intent(context, ReminderReceiver::class.java).apply {
                    putExtra(EXTRA_REMINDER_ID, reminderId)
                    putExtra(EXTRA_ACTION, action)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
    }
}

