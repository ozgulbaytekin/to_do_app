package com.example.todoapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.NOTIFICATION_ACTION) {
            val taskId = intent.getIntExtra("taskId", -1)
            val taskTitle = intent.getStringExtra("taskTitle") ?: return
            val isDailyReminder = intent.getBooleanExtra("isDailyReminder", false)

            val notificationHelper = NotificationHelper(context)
            notificationHelper.showNotification(taskId, "$taskTitle${if (isDailyReminder) " (Daily)" else ""}")

            // Reschedule next daily reminder if needed
            if (isDailyReminder) {
                val dailyHour = intent.getIntExtra("dailyHour", 0)
                val dailyMinute = intent.getIntExtra("dailyMinute", 0)
                val task = TodoItem(
                    id = taskId,
                    title = taskTitle,
                    isDailyReminder = true,
                    dailyReminderHour = dailyHour,
                    dailyReminderMinute = dailyMinute
                )
                notificationHelper.scheduleNotification(task, System.currentTimeMillis())
            }
        }
    }
}