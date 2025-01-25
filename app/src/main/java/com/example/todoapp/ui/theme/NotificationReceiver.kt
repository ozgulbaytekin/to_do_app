package com.example.todoapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationHelper = NotificationHelper(context)

        if (intent.action == NotificationHelper.NOTIFICATION_ACTION) {
            val taskId = intent.getIntExtra("taskId", -1)
            val taskTitle = intent.getStringExtra("taskTitle") ?: return
            val isDailyReminder = intent.getBooleanExtra("isDailyReminder", false)

            notificationHelper.showNotification(taskId, "$taskTitle${if (isDailyReminder) " (Daily)" else ""}", true)

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
        } else if (intent.action == NotificationHelper.ROUTINE_NOTIFICATION_ACTION) {
            val routineId = intent.getIntExtra("routineId", -1)
            val routineTitle = intent.getStringExtra("routineTitle") ?: return

            notificationHelper.showNotification(routineId, routineTitle, false)
        }
    }
}