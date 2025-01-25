package com.example.todoapp

import android.Manifest
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.todoapp.ui.theme.TodoAppTheme
import java.util.*
import android.content.Intent
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ViewList

@OptIn(ExperimentalMaterial3Api::class)
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TodoApp()
        }
    }
}

@Composable
fun TodoApp() {
    TodoAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            TodoScreen()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen() {
    val context = LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTaskForNotification by remember { mutableStateOf<TodoItem?>(null) }
    var todoText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.PERSONAL) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var todoTasks by remember { mutableStateOf(listOf<TodoItem>()) }
    var completedTasks by remember { mutableStateOf(listOf<TodoItem>()) }
    var showRoutineDialog by remember { mutableStateOf(false) }
    var routines by remember { mutableStateOf(listOf<RoutineItem>()) }
    var showRoutinesList by remember { mutableStateOf(false) }

// Add this where showRoutineDialog is handled
    if (showRoutineDialog) {
        RoutineDialog(
            onDismiss = {
                showRoutineDialog = false
                showRoutinesList = true
            },
            onRoutineCreate = { newRoutine ->
                routines = routines + newRoutine
                val notificationHelper = NotificationHelper(context)
                notificationHelper.scheduleRoutineNotification(newRoutine)
            },
            existingRoutines = routines
        )
    }

    fun moveTaskToCompleted(taskId: Int) {
        val taskToMove = todoTasks.find { it.id == taskId }
        taskToMove?.let {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationHelper.NOTIFICATION_ACTION
                putExtra("taskId", taskId)
                putExtra("taskTitle", it.title)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationHelper.alarmManager.cancel(pendingIntent)

            todoTasks = todoTasks.filter { it.id != taskId }
            completedTasks = completedTasks + it.copy(
                isCompleted = true,
                notificationTime = null,
                isDailyReminder = false,
                dailyReminderHour = null,
                dailyReminderMinute = null
            )
        }
    }

    fun moveTaskToTodo(taskId: Int) {
        val taskToMove = completedTasks.find { it.id == taskId }
        taskToMove?.let {
            completedTasks = completedTasks.filter { it.id != taskId }
            todoTasks = todoTasks + it.copy(isCompleted = false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        // Keep your existing Routines button:
        Button(
            onClick = { showRoutineDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.List, contentDescription = "Routines")
            Spacer(Modifier.width(8.dp))
            Text("Add Routine")
        }

// Add new button for viewing routines:
        Button(
            onClick = { showRoutinesList = !showRoutinesList },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Icon(Icons.Default.ViewList, contentDescription = "View Routines")
            Spacer(Modifier.width(8.dp))
            Text("View Routines")
        }

// Add conditional rendering for routines list:
        if (showRoutinesList) {
            RoutinesList(
                routines = routines,
                onDeleteRoutine = { id ->
                    routines = routines.filter { it.id != id }
                }
            )
        }


        CalendarView(todoTasks, completedTasks)

        // Input section
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = todoText,
                onValueChange = { todoText = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter a task") }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    OutlinedButton(
                        onClick = { showCategoryMenu = true }
                    ) {
                        Text(selectedCategory.name)
                    }

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        TaskCategory.values().forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (todoText.isNotBlank()) {
                            todoTasks = todoTasks + TodoItem(
                                id = (todoTasks + completedTasks).size,
                                title = todoText,
                                isCompleted = false,
                                category = selectedCategory
                            )
                            todoText = ""
                        }
                    }
                ) {
                    Text("Add")
                }
            }
        }

        // To-do tasks section
        Text(
            text = "To Do (${todoTasks.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(todoTasks) { todoItem ->
                TodoItemRow(
                    todo = todoItem,
                    onToggleCompletion = { moveTaskToCompleted(todoItem.id) },
                    onSetNotification = {
                        selectedTaskForNotification = todoItem
                        showDatePicker = true
                    }
                )
            }
        }

        // Completed tasks section
        Text(
            text = "Completed (${completedTasks.size})",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )
        LazyColumn(
            modifier = Modifier.weight(1f)
        ) {
            items(completedTasks) { todoItem ->
                TodoItemRow(
                    todo = todoItem,
                    onToggleCompletion = { moveTaskToTodo(todoItem.id) },
                    onSetNotification = { },
                    onDelete = {
                        completedTasks = completedTasks.filter { it.id != todoItem.id }
                    }
                )
            }
        }
    }

    // Date picker dialog
    if (showDatePicker && selectedTaskForNotification != null) {
        val task = selectedTaskForNotification!!
        val calendar = Calendar.getInstance()
        var isDailyReminder by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showDatePicker = false
                selectedTaskForNotification = null
            },
            title = { Text("Set Reminder") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Checkbox(
                            checked = isDailyReminder,
                            onCheckedChange = { isDailyReminder = it }
                        )
                        Text(
                            text = "Remind every day",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    if (isDailyReminder) {
                        Text(
                            text = "Choose daily reminder time:",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        TextButton(onClick = {
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    calendar.apply {
                                        set(Calendar.HOUR_OF_DAY, hourOfDay)
                                        set(Calendar.MINUTE, minute)
                                    }

                                    todoTasks = todoTasks.map {
                                        if (it.id == task.id) {
                                            it.copy(
                                                notificationTime = calendar.timeInMillis,
                                                dailyReminderHour = hourOfDay,
                                                dailyReminderMinute = minute,
                                                isDailyReminder = true
                                            )
                                        } else {
                                            it
                                        }
                                    }

                                    notificationHelper.scheduleNotification(
                                        todoTasks.find { it.id == task.id }!!,
                                        calendar.timeInMillis
                                    )

                                    showDatePicker = false
                                    selectedTaskForNotification = null
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        }) {
                            Text("Select Time")
                        }
                    } else {
                        TextButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    calendar.set(year, month, dayOfMonth)
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                                            calendar.set(Calendar.MINUTE, minute)

                                            todoTasks = todoTasks.map {
                                                if (it.id == task.id) {
                                                    it.copy(
                                                        notificationTime = calendar.timeInMillis,
                                                        isDailyReminder = false
                                                    )
                                                } else {
                                                    it
                                                }
                                            }

                                            notificationHelper.scheduleNotification(
                                                task.copy(isDailyReminder = false),
                                                calendar.timeInMillis
                                            )
                                            showDatePicker = false
                                            selectedTaskForNotification = null
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }) {
                            Text("Set One-time Reminder")
                        }
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    selectedTaskForNotification = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TodoItemRow(
    todo: TodoItem,
    onToggleCompletion: () -> Unit,
    onSetNotification: () -> Unit,
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = todo.title,
                    style = if (todo.isCompleted) {
                        MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = TextDecoration.LineThrough,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    } else {
                        MaterialTheme.typography.bodyLarge
                    }
                )
                Text(
                    text = todo.category.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (todo.isDailyReminder && todo.dailyReminderHour != null) {
                    Text(
                        text = "Daily at ${String.format("%02d:%02d", todo.dailyReminderHour, todo.dailyReminderMinute)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!todo.isCompleted) {
                IconButton(onClick = onSetNotification) {
                    Icon(
                        imageVector = if (todo.notificationTime != null)
                            Icons.Filled.Notifications
                        else
                            Icons.Outlined.Notifications,
                        contentDescription = "Set notification"
                    )
                }
            } else {
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete task"
                    )
                }
            }

            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggleCompletion() }
            )
        }
    }
}