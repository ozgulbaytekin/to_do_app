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
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.foundation.clickable

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
@Composable
fun ReminderDialog(
    task: TodoItem,
    todoTasks: List<TodoItem>,
    notificationHelper: NotificationHelper,
    onDismiss: () -> Unit,
    onUpdateTasks: (List<TodoItem>) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()
    var isDailyReminder by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
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

                                val updatedTasks = todoTasks.map {
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
                                    updatedTasks.find { it.id == task.id }!!,
                                    calendar.timeInMillis
                                )

                                onUpdateTasks(updatedTasks)
                                onDismiss()
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

                                        val updatedTasks = todoTasks.map {
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

                                        onUpdateTasks(updatedTasks)
                                        onDismiss()
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoScreen() {
    val context = LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }

    var selectedRoutineForNotification by remember { mutableStateOf<RoutineItem?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTaskForNotification by remember { mutableStateOf<TodoItem?>(null) }
    var todoText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(TaskCategory.PERSONAL) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var todoTasks by remember { mutableStateOf(listOf<TodoItem>()) }
    var completedTasks by remember { mutableStateOf(listOf<TodoItem>()) }
    var showRoutineDialog by remember { mutableStateOf(false) }
    var showRoutinesList by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var routines by remember { mutableStateOf(listOf<RoutineItem>()) }

    if (showRoutineDialog) {
        RoutineDialog(
            onDismiss = { showRoutineDialog = false },
            onRoutineCreate = { newRoutine -> routines = routines + newRoutine },
            existingRoutines = routines
        )
    }

    fun moveTaskToCompleted(taskId: Int) {
        todoTasks.find { it.id == taskId }?.let { task ->
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = NotificationHelper.NOTIFICATION_ACTION
                putExtra("taskId", taskId)
                putExtra("taskTitle", task.title)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, taskId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            notificationHelper.alarmManager.cancel(pendingIntent)

            todoTasks = todoTasks.filter { it.id != taskId }
            completedTasks = completedTasks + task.copy(
                isCompleted = true, notificationTime = null, isDailyReminder = false,
                dailyReminderHour = null, dailyReminderMinute = null
            )
        }
    }

    fun moveTaskToTodo(taskId: Int) {
        completedTasks.find { it.id == taskId }?.let { task ->
            completedTasks = completedTasks.filter { it.id != taskId }
            todoTasks = todoTasks + task.copy(isCompleted = false)
        }
    }

    @Composable
    fun RoutineReminderDialog(
        routine: RoutineItem,
        onDismiss: () -> Unit,
        onUpdateRoutines: (List<RoutineItem>) -> Unit,
        notificationHelper: NotificationHelper
    ) {
        val context = LocalContext.current
        val calendar = Calendar.getInstance()
        var selectedHour by remember { mutableStateOf(routine.routineStartHour ?: 0) }
        var selectedMinute by remember { mutableStateOf(routine.routineStartMinute ?: 0) }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Set Routine Reminder") },
            text = {
                Column {
                    Text("Routine Reminder Time")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${String.format("%02d:%02d", selectedHour, selectedMinute)}")
                        Button(onClick = {
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    selectedHour = hour
                                    selectedMinute = minute
                                },
                                selectedHour,
                                selectedMinute,
                                true
                            ).show()
                        }) {
                            Text("Set Time")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val updatedRoutine = routine.copy(
                        routineStartHour = selectedHour,
                        routineStartMinute = selectedMinute
                    )
                    notificationHelper.scheduleRoutineNotification(updatedRoutine)
                    onUpdateRoutines(routines.map { if (it.id == routine.id) updatedRoutine else it })
                    onDismiss()
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            showRoutineDialog = { showRoutineDialog = true },
            toggleRoutinesList = { showRoutinesList = !showRoutinesList },
            toggleCalendar = { showCalendar = !showCalendar },
            isRoutinesVisible = showRoutinesList,
            isCalendarVisible = showCalendar
        )

        Column(modifier = Modifier.weight(1f).padding(16.dp)) {
            if (showRoutinesList) {
                RoutinesList(routines, onDeleteRoutine = { id ->
                    routines = routines.filter { it.id != id }
                })
            }

            if (showCalendar) {
                CalendarView(todoTasks, completedTasks)
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = todoText,
                    onValueChange = { todoText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter a task") }
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        OutlinedButton(onClick = { showCategoryMenu = true }) {
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

                    Button(onClick = {
                        if (todoText.isNotBlank()) {
                            todoTasks = todoTasks + TodoItem(
                                id = (todoTasks + completedTasks).size,
                                title = todoText,
                                isCompleted = false,
                                category = selectedCategory
                            )
                            todoText = ""
                        }
                    }) {
                        Text("Add")
                    }
                }
            }

            Text(
                "To Do (${todoTasks.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
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

            Text(
                "Completed (${completedTasks.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(completedTasks) { todoItem ->
                    TodoItemRow(
                        todo = todoItem,
                        onToggleCompletion = { moveTaskToTodo(todoItem.id) },
                        onSetNotification = {},
                        onDelete = { completedTasks = completedTasks.filter { it.id != todoItem.id } }
                    )
                }
            }
        }
    }

    if (showDatePicker && selectedTaskForNotification != null) {
        ReminderDialog(
            task = selectedTaskForNotification!!,
            todoTasks = todoTasks,
            notificationHelper = notificationHelper,
            onDismiss = {
                showDatePicker = false
                selectedTaskForNotification = null
            },
            onUpdateTasks = { updatedTasks -> todoTasks = updatedTasks }
        )
    } else if (showRoutineDialog && selectedRoutineForNotification != null) {
        RoutineReminderDialog(
            routine = selectedRoutineForNotification!!,
            onDismiss = {
                showRoutineDialog = false
                selectedRoutineForNotification = null
            },
            onUpdateRoutines = { updatedRoutines -> routines = updatedRoutines },
            notificationHelper = notificationHelper
        )
    }
}
@Composable
fun Sidebar(
    showRoutineDialog: () -> Unit,
    toggleRoutinesList: () -> Unit,
    toggleCalendar: () -> Unit,
    isRoutinesVisible: Boolean,
    isCalendarVisible: Boolean
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .width(if (isExpanded) 240.dp else 60.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { isExpanded = !isExpanded }
    ) {
        if (isExpanded) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = showRoutineDialog, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.List, "Add Routine")
                    Spacer(Modifier.width(8.dp))
                    Text("Add Routine")
                }

                Button(onClick = toggleRoutinesList, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.ViewList, "View Routines")
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRoutinesVisible) "Hide Routines" else "View Routines")
                }

                Button(onClick = toggleCalendar, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DateRange, "Calendar")
                    Spacer(Modifier.width(8.dp))
                    Text(if (isCalendarVisible) "Hide Calendar" else "Show Calendar")
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = showRoutineDialog) {
                    Icon(Icons.Default.List, "Add Routine")
                }
                IconButton(onClick = toggleRoutinesList) {
                    Icon(Icons.Default.ViewList, "View Routines")
                }
                IconButton(onClick = toggleCalendar) {
                    Icon(Icons.Default.DateRange, "Calendar")
                }
            }
        }
    }
}

@Composable
fun TodoItemRow(
    todo: TodoItem,
    onToggleCompletion: () -> Unit,
    onSetNotification: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val categoryColor = getCategoryColor(todo.category)

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(12.dp).background(categoryColor, CircleShape).align(Alignment.CenterVertically)
            )
            Spacer(Modifier.width(16.dp))

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
                if (todo.isDailyReminder && todo.dailyReminderHour != null && todo.dailyReminderMinute != null) {
                    Text(
                        text = "Daily at ${formatTime(todo.dailyReminderHour, todo.dailyReminderMinute)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!todo.isCompleted) {
                    IconButton(onClick = onSetNotification) {
                        Icon(
                            imageVector = if (todo.notificationTime != null) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                            contentDescription = "Set notification",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete task",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Checkbox(
                    checked = todo.isCompleted,
                    onCheckedChange = { onToggleCompletion() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}
@Composable
private fun getCategoryColor(category: TaskCategory): Color {
    return when (category) {
        TaskCategory.PERSONAL -> Color(0xFF2196F3)
        TaskCategory.WORK -> Color(0xFFF44336)
        TaskCategory.HEALTH -> Color(0xFF4CAF50)
        TaskCategory.STUDY -> Color(0xFFFF9800)
        TaskCategory.SHOPPING -> Color(0xFF9C27B0)
        else -> MaterialTheme.colorScheme.secondary
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    return String.format("%02d:%02d", hour, minute)
}