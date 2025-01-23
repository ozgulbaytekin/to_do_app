package com.example.todoapp

import android.Manifest
import android.app.DatePickerDialog
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

    fun moveTaskToCompleted(taskId: Int) {
        val taskToMove = todoTasks.find { it.id == taskId }
        taskToMove?.let {
            todoTasks = todoTasks.filter { it.id != taskId }
            completedTasks = completedTasks + it.copy(isCompleted = true)
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
                    onSetNotification = { }
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
            },
            confirmButton = {
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
                                                isDailyReminder = isDailyReminder
                                            )
                                        } else {
                                            it
                                        }
                                    }

                                    val updatedTask = task.copy(isDailyReminder = isDailyReminder)
                                    notificationHelper.scheduleNotification(
                                        updatedTask,
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
                    Text("Set Time")
                }
            },
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
        onSetNotification: () -> Unit
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
                    if (todo.isDailyReminder && todo.notificationTime != null) {
                        Text(
                            text = "Daily reminder",
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
                }

                Checkbox(
                    checked = todo.isCompleted,
                    onCheckedChange = { onToggleCompletion() }
                )
            }
        }
    }
