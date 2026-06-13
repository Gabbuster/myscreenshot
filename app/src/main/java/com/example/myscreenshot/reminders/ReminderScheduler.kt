package com.example.myscreenshot.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myscreenshot.data.Reminder
import com.example.myscreenshot.data.ReminderAlert
import java.util.concurrent.TimeUnit

class ReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(reminder: Reminder, alert: ReminderAlert): Boolean {
        val delay = alert.alertDateTime - System.currentTimeMillis()
        if (delay <= 0) return false
        val intent = ReminderReceiver.intent(context, reminder.id, alert.alertLabel)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alert.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return if (canUseExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alert.alertDateTime, pendingIntent)
            true
        } else {
            val data = Data.Builder()
                .putString("reminderId", reminder.id)
                .putString("label", alert.alertLabel)
                .build()
            val request = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(data)
                .build()
            WorkManager.getInstance(context).enqueue(request)
            false
        }
    }

    fun scheduleOneHourSnooze(reminder: Reminder) {
        val alert = ReminderAlert(
            id = reminder.id + "-snooze-" + System.currentTimeMillis(),
            reminderId = reminder.id,
            alertDateTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
            alertLabel = "Snoozed reminder",
            isScheduled = false,
            isTriggered = false,
        )
        schedule(reminder, alert)
    }

    private fun canUseExactAlarms(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
}

