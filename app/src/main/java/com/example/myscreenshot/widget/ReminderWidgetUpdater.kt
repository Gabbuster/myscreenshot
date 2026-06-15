package com.example.myscreenshot.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.myscreenshot.MainActivity
import com.example.myscreenshot.R
import com.example.myscreenshot.data.AppDatabase
import com.example.myscreenshot.data.Reminder
import java.text.DateFormat
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object ReminderWidgetUpdater {
    fun refresh(context: Context) {
        val appContext = context.applicationContext
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val ids = appWidgetManager.getAppWidgetIds(ComponentName(appContext, ReminderWidgetProvider::class.java))
        if (ids.isEmpty()) return
        update(appContext, appWidgetManager, ids)
    }

    fun update(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val reminders = AppDatabase.get(appContext)
                .reminderDao()
                .nextReminders(now = System.currentTimeMillis(), limit = 5)
            withContext(Dispatchers.Main) {
                appWidgetIds.forEach { id ->
                    appWidgetManager.updateAppWidget(id, buildViews(appContext, reminders))
                }
            }
        }
    }

    private fun buildViews(context: Context, reminders: List<Reminder>): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.screen4u_reminder_widget)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)
        views.setViewVisibility(R.id.widget_empty, if (reminders.isEmpty()) View.VISIBLE else View.GONE)

        val textIds = listOf(
            R.id.widget_text_1,
            R.id.widget_text_2,
            R.id.widget_text_3,
            R.id.widget_text_4,
            R.id.widget_text_5,
        )

        textIds.forEachIndexed { index, rowId ->
            val reminder = reminders.getOrNull(index)
            views.setViewVisibility(rowId, if (reminder == null) View.GONE else View.VISIBLE)
            if (reminder != null) {
                views.setTextViewText(rowId, reminder.widgetLine())
            }
        }
        return views
    }

    private fun Reminder.widgetLine(): String {
        val date = dateTime?.let {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
        }
        return listOfNotNull(date, title.take(42)).joinToString("  ")
    }
}
