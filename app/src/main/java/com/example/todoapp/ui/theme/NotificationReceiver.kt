package com.example.todoapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationHelper.NOTIFICATION_ACTION) {
            val taskId = intent.getIntExtra("taskId", -1)
            val taskTitle = intent.getStringExtra("taskTitle") ?: return

            val notificationHelper = NotificationHelper(context)
            notificationHelper.showNotification(taskId, taskTitle)
        }
    }
}