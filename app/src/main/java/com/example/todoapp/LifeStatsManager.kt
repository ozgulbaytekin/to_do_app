package com.example.todoapp

object LifeStatsManager {
    fun updateLifeStats(currentStats: LifeStats): LifeStats {
        val newExperience = currentStats.experience
        val experienceForNextLevel = currentStats.level * 100

        return if (newExperience >= experienceForNextLevel) {
            currentStats.copy(
                level = currentStats.level + 1,
                experience = newExperience - experienceForNextLevel
            )
        } else {
            currentStats
        }
    }

    fun calculatePoints(task: TodoItem): Int {
        return when (task.priority) {
            Priority.HIGH -> 5
            Priority.MEDIUM -> 3
            Priority.LOW -> 1
        } * (if (task.dueDate != null && System.currentTimeMillis() <= task.dueDate) 2 else 1)
    }
}