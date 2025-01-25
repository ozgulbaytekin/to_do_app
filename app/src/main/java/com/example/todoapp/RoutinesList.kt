package com.example.todoapp

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.*


@Composable
fun RoutinesList(
    routines: List<RoutineItem>,
    onDeleteRoutine: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Text(
            text = "Your Routines",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (routines.isEmpty()) {
            Text("No routines added yet")
        } else {
            LazyColumn {
                items(routines) { routine ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = routine.title,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = routine.description,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (routine.isDailyRoutine) {
                                        Text(
                                            text = "Daily Routine",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (routine.isDailyRoutine && routine.routineStartHour != null) {
                                        Text(
                                            text = "Daily from ${String.format("%02d:%02d", routine.routineStartHour, routine.routineStartMinute)} " +
                                                    "to ${String.format("%02d:%02d", routine.routineEndHour, routine.routineEndMinute)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = "Points: ${routine.points}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                IconButton(onClick = { onDeleteRoutine(routine.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete routine"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}