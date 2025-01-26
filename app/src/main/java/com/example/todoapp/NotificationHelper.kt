package com.example.todoapp

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import com.example.todoapp.NotificationHelper.Companion.NOTIFICATION_ACTION

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "todo_channel"
        const val CHANNEL_NAME = "Todo Notifications"
        const val NOTIFICATION_ACTION = "TODO_NOTIFICATION_ACTION"
        const val ROUTINE_NOTIFICATION_ACTION = "ROUTINE_NOTIFICATION_ACTION"
    }
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for todo tasks"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleNotification(task: TodoItem, notificationTime: Long) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = NOTIFICATION_ACTION
            putExtra("taskId", task.id)
            putExtra("taskTitle", task.title)
            putExtra("isDailyReminder", task.isDailyReminder)
            putExtra("dailyHour", task.dailyReminderHour)
            putExtra("dailyMinute", task.dailyReminderMinute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (task.isDailyReminder && task.dailyReminderHour != null && task.dailyReminderMinute != null) {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, task.dailyReminderHour)
                set(Calendar.MINUTE, task.dailyReminderMinute)
                set(Calendar.SECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            alarmManager.setAlarmClock(  // Changed to setAlarmClock for more reliable daily reminders
                AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
                pendingIntent
            )
        } else {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(notificationTime, pendingIntent),
                pendingIntent
            )
        }
    }
    fun showNotification(taskId: Int, taskTitle: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            taskId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Todo Task Reminder")
            .setContentText(taskTitle)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(context)) {
                notify(taskId, builder.build())
            }
        }
    }


    fun showNotification(id: Int, title: String, isTask: Boolean) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isTask) "Todo Task Reminder" else "Routine Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            with(NotificationManagerCompat.from(context)) {
                notify(id, builder.build())
            }
        }
    }
    fun scheduleRoutineNotification(routine: RoutineItem) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            action = ROUTINE_NOTIFICATION_ACTION
            putExtra("routineId", routine.id)
            putExtra("routineTitle", routine.title)
            putExtra("isDailyRoutine", routine.isDailyRoutine)
            putExtra("routineStartHour", routine.routineStartHour)
            putExtra("routineStartMinute", routine.routineStartMinute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            routine.id + 10000, // Offset to avoid conflicts with todo notifications
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, routine.routineStartHour ?: 0)
            set(Calendar.MINUTE, routine.routineStartMinute ?: 0)
            set(Calendar.SECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(calendar.timeInMillis, pendingIntent),
            pendingIntent
        )
    }
}
