package com.example.myscreenshot.reminders

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myscreenshot.data.AppDatabase
import kotlinx.coroutines.flow.firstOrNull

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val reminderId = inputData.getString("reminderId") ?: return Result.failure()
        val label = inputData.getString("label") ?: "Reminder"
        val reminder = AppDatabase.get(applicationContext).reminderDao().observeById(reminderId).firstOrNull()
            ?: return Result.failure()
        NotificationHelper(applicationContext).showReminder(reminder, label)
        return Result.success()
    }
}

