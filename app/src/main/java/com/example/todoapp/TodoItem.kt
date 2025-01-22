package com.example.todoapp

data class TodoItem(
    val id: Int,
    val title: String,
    val isCompleted: Boolean = false,
    val notificationTime: Long? = null,
    val category: LifeCategory = LifeCategory.PERSONAL,
    val priority: Priority = Priority.MEDIUM,
    val dueDate: Long? = null,
    val points: Int = 1
)

enum class LifeCategory {
    PERSONAL, WORK, HEALTH, LEARNING, SOCIAL, FINANCE
}

enum class Priority {
    HIGH, MEDIUM, LOW
}