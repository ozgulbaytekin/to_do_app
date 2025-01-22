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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = todoText,
                onValueChange = { todoText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter a task") }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (todoText.isNotBlank()) {
                        todoTasks = todoTasks + TodoItem(
                            id = (todoTasks + completedTasks).size,
                            title = todoText,
                            isCompleted = false
                        )
                        todoText = ""
                    }
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text("Add")
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

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        // Update task with notification time
                        todoTasks = todoTasks.map {
                            if (it.id == task.id) {
                                it.copy(notificationTime = calendar.timeInMillis)
                            } else {
                                it
                            }
                        }

                        // Schedule notification
                        notificationHelper.scheduleNotification(task, calendar.timeInMillis)
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

        showDatePicker = false
        selectedTaskForNotification = null
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
            Text(
                text = todo.title,
                modifier = Modifier.weight(1f),
                style = if (todo.isCompleted) {
                    MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                } else {
                    MaterialTheme.typography.bodyLarge
                }
            )

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