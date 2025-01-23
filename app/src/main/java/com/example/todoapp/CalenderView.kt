package com.example.todoapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarView(
    todoTasks: List<TodoItem>,
    completedTasks: List<TodoItem>
) {
    var showCalendar by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance()) }
    val allTasks = todoTasks + completedTasks

    Column {
        Button(
            onClick = { showCalendar = !showCalendar },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.DateRange, contentDescription = "Calendar")
            Spacer(Modifier.width(8.dp))
            Text(if (showCalendar) "Hide Calendar" else "Show Calendar")
        }

        if (showCalendar) {
            Column {
                // Month navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        selectedMonth.add(Calendar.MONTH, -1)
                        selectedMonth = selectedMonth.clone() as Calendar
                    }) {
                        Icon(Icons.Default.KeyboardArrowLeft, "Previous month")
                    }

                    Text(SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedMonth.time))

                    IconButton(onClick = {
                        selectedMonth.add(Calendar.MONTH, 1)
                        selectedMonth = selectedMonth.clone() as Calendar
                    }) {
                        Icon(Icons.Default.KeyboardArrowRight, "Next month")
                    }
                }

                // Calendar grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    modifier = Modifier.padding(8.dp)
                ) {
                    // Weekday headers
                    items(7) { dayOfWeek ->
                        Text(
                            text = SimpleDateFormat("EEE", Locale.getDefault())
                                .format(Calendar.getInstance().apply {
                                    set(Calendar.DAY_OF_WEEK, dayOfWeek + 1)
                                }.time),
                            modifier = Modifier.padding(4.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    // Calendar days
                    val firstDayOfMonth = selectedMonth.clone() as Calendar
                    firstDayOfMonth.set(Calendar.DAY_OF_MONTH, 1)
                    val daysInMonth = selectedMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
                    val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1

                    // Empty cells before first day
                    items(firstDayOfWeek) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    }

                    // Days of the month
                    items(daysInMonth) { day ->
                        val date = Calendar.getInstance().apply {
                            timeInMillis = selectedMonth.timeInMillis
                            set(Calendar.DAY_OF_MONTH, day + 1)
                            set(Calendar.HOUR_OF_DAY, 0)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val tasksForDay = allTasks.filter { task ->
                            task.notificationTime?.let { time ->
                                val taskDate = Calendar.getInstance().apply {
                                    timeInMillis = time
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                                taskDate.timeInMillis == date.timeInMillis
                            } ?: false
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    if (tasksForDay.isNotEmpty())
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface,
                                    shape = MaterialTheme.shapes.small
                                )
                        ) {
                            Text(
                                text = (day + 1).toString(),
                                modifier = Modifier.padding(4.dp)
                            )
                            if (tasksForDay.isNotEmpty()) {
                                Badge(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(tasksForDay.size.toString())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
