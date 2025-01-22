package com.example.todoapp

data class LifeStats(
    val level: Int = 1,
    val experience: Int = 0,
    val categoryProgress: Map<LifeCategory, Int> = LifeCategory.values().associateWith { 0 }
)