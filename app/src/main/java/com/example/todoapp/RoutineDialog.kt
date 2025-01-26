package com.example.todoapp

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineDialog(
    onDismiss: () -> Unit,
    onRoutineCreate: (RoutineItem) -> Unit,
    existingRoutines: List<RoutineItem>
) {
    val context = LocalContext.current
    val notificationHelper = remember { NotificationHelper(context) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var points by remember { mutableStateOf(5) }
    var startTime by remember { mutableStateOf("Set start time") }
    var endTime by remember { mutableStateOf("Set end time") }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Life Management Routine") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Routine Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )

                Text("Points Value:", modifier = Modifier.padding(top = 8.dp))
                Slider(
                    value = points.toFloat(),
                    onValueChange = { points = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 9
                )

                Text("Routine Time:", modifier = Modifier.padding(top = 8.dp))
                OutlinedButton(
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(startTime)
                }

                OutlinedButton(
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(endTime)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && startTime != "Set start time" && endTime != "Set end time") {
                        val routine = RoutineItem(
                            id = existingRoutines.size,
                            title = title,
                            description = description,
                            points = points,
                            isDailyRoutine = true,
                            routineStartHour = startTime.split(":")[0].toInt(),
                            routineStartMinute = startTime.split(":")[1].toInt(),
                            routineEndHour = endTime.split(":")[0].toInt(),
                            routineEndMinute = endTime.split(":")[1].toInt()
                        )
                        onRoutineCreate(routine)
                        notificationHelper.scheduleRoutineNotification(routine)
                        onDismiss()
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showStartTimePicker) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                startTime = String.format("%02d:%02d", hour, minute)
                showStartTimePicker = false
            },
            0,
            0,
            true
        ).show()
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            context,
            { _, hour, minute ->
                endTime = String.format("%02d:%02d", hour, minute)
                showEndTimePicker = false
            },
            0,
            0,
            true
        ).show()
    }
}