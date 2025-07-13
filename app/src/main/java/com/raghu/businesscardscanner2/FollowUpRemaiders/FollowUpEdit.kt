package com.raghu.businesscardscanner2.FollowUpRemaiders

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderDialog(
    reminder: FollowUpReminderEntity,
    onDismiss: () -> Unit,
    onSave: (FollowUpReminderEntity) -> Unit
) {
    var editedReminder by remember { mutableStateOf(reminder.copy()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = editedReminder.dueDate
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Reminder") },
        text = {
            Column {
                OutlinedTextField(
                    value = editedReminder.contactName,
                    onValueChange = { editedReminder = editedReminder.copy(contactName = it) },
                    label = { Text("Contact Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = editedReminder.message,
                    onValueChange = { editedReminder = editedReminder.copy(message = it) },
                    label = { Text("Reminder Message") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Change Due Date: ${formatDate(editedReminder.dueDate)}")
                }

                if (showDatePicker) {
                    val calendar = Calendar.getInstance().apply {
                        timeInMillis = editedReminder.dueDate
                    }

                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            Button(onClick = {
                                datePickerState.selectedDateMillis?.let { dateMillis ->
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val newCalendar = Calendar.getInstance().apply {
                                                timeInMillis = dateMillis
                                                set(Calendar.HOUR_OF_DAY, hour)
                                                set(Calendar.MINUTE, minute)
                                            }
                                            editedReminder = editedReminder.copy(
                                                dueDate = newCalendar.timeInMillis
                                            )
                                            showDatePicker = false
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                }
                            }) {
                                Text("Select Time")
                            }
                        },
                        dismissButton = {
                            Button(onClick = { showDatePicker = false }) {
                                Text("Cancel")
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(editedReminder)
                    onDismiss()
                }
            ) {
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


private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
        .format(Date(timestamp))
}