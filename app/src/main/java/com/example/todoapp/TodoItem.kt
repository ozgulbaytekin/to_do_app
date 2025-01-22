package com.example.todoapp

enum class TaskCategory {
    PERSONAL, WORK, HEALTH, STUDY, SHOPPING, OTHER
}

data class TodoItem(
    val id: Int,
    val title: String,
    val isCompleted: Boolean = false,
    val notificationTime: Long? = null,
    val category: TaskCategory = TaskCategory.OTHER
)