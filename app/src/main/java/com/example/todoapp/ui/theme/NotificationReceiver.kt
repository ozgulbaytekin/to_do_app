package com.example.todoapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

// NotificationReceiver.kt
class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationHelper = NotificationHelper(context)

        when (intent.action) {
            NotificationHelper.ROUTINE_NOTIFICATION_ACTION -> {
                val routineId = intent.getIntExtra("routineId", -1)
                val routineTitle = intent.getStringExtra("routineTitle") ?: return
                val isDailyRoutine = intent.getBooleanExtra("isDailyRoutine", false)

                notificationHelper.showNotification(routineId, routineTitle, false)

                // Reschedule if daily routine
                if (isDailyRoutine) {
                    val startHour = intent.getIntExtra("routineStartHour", 0)
                    val startMinute = intent.getIntExtra("routineStartMinute", 0)
                    val routine = RoutineItem(
                        id = routineId,
                        title = routineTitle,
                        description = "",
                        points = 0,
                        isDailyRoutine = true,
                        routineStartHour = startHour,
                        routineStartMinute = startMinute
                    )
                    notificationHelper.scheduleRoutineNotification(routine)
                }
            }
            NotificationHelper.NOTIFICATION_ACTION -> {
                // Existing todo notification handling
                val taskId = intent.getIntExtra("taskId", -1)
                val taskTitle = intent.getStringExtra("taskTitle") ?: return
                val isDailyReminder = intent.getBooleanExtra("isDailyReminder", false)

                notificationHelper.showNotification(taskId, taskTitle, true)

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
}