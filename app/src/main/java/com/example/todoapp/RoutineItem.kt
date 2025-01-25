package com.example.todoapp

data class RoutineItem(
    val id: Int,
    val title: String,
    val description: String,
    val points: Int,
    val isDailyRoutine: Boolean = true,
    val routineStartHour: Int? = null,
    val routineStartMinute: Int? = null,
    val routineEndHour: Int? = null,
    val routineEndMinute: Int? = null
)