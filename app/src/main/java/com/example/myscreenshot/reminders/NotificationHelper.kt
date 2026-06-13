package com.example.myscreenshot.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myscreenshot.MainActivity
import com.example.myscreenshot.R
import com.example.myscreenshot.data.Reminder

class NotificationHelper(private val context: Context) {
    fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Reminder alerts",
                NotificationManager.IMPORTANCE_HIGH,
            )
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun showReminder(reminder: Reminder, label: String) {
        ensureChannel()
        val openIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("reminderId", reminder.id)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            reminder.id.hashCode(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val body = buildString {
            append(label)
            reminder.amount?.let {
                append(" - ")
                append(reminder.currency ?: "")
                append(" ")
                append(String.format("%.2f", it))
            }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(reminder.title)
            .setContentText(body)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Mark done", ReminderReceiver.actionIntent(context, reminder.id, "done"))
            .addAction(0, "Snooze 1 hour", ReminderReceiver.actionIntent(context, reminder.id, "snooze"))
            .build()

        NotificationManagerCompat.from(context).notify(reminder.id.hashCode(), notification)
    }

    companion object {
        const val CHANNEL_ID = "reminders"
    }
}

