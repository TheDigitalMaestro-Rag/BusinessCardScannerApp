package com.raghu.businesscardscanner2.FollowUpRemaiders.SnoozeData

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpRemainderUtility.ReminderConstants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnoozeDialog(
    onDismiss: () -> Unit,
    onSnoozeSelected: (minutes: Long) -> Unit
) {
    val defaultOptions = listOf(
        SnoozeOption("10 minutes", 10),
        SnoozeOption("30 minutes", 30),
        SnoozeOption("1 hour", 60),
        SnoozeOption("2 hours", 120),
        SnoozeOption("Custom", 0, true)
    )

    var showCustomDialog by remember { mutableStateOf(false) }
    var customMinutes by remember { mutableStateOf("") }

    if (showCustomDialog) {
        AlertDialog(
            onDismissRequest = { showCustomDialog = false },
            title = { Text("Custom Snooze Time") },
            text = {
                Column {
                    Text("Enter snooze time in minutes:")
                    OutlinedTextField(
                        value = customMinutes,
                        onValueChange = { customMinutes = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
//            confirmButton = {
//                Button(
//                    onClick = {
//                        val minutes = customMinutes.toLongOrNull() ?: 10
//                        onSnoozeSelected(minutes)
//                        showCustomDialog = false
//                        onDismiss()
//                    },
//                    enabled = customMinutes.isNotBlank()
//                ) {
//                    Text("Set")
//                }
//            },
            confirmButton = {
                Button(
                    onClick = {
                        val minutes = customMinutes.toLongOrNull() ?: ReminderConstants.DEFAULT_SNOOZE_MINUTES
                        val validatedMinutes = minutes.coerceIn(
                            ReminderConstants.MIN_SNOOZE_MINUTES,
                            ReminderConstants.MAX_SNOOZE_MINUTES
                        )
                        onSnoozeSelected(validatedMinutes)
                        showCustomDialog = false
                        onDismiss()
                    },
                    enabled = customMinutes.isNotBlank() &&
                            customMinutes.toLongOrNull()?.let {
                                it >= ReminderConstants.MIN_SNOOZE_MINUTES
                            } ?: false
                ) {
                    Text("Set")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Snooze for") },
            text = { Text("Select snooze duration") },
            buttons = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    defaultOptions.forEach { option ->
                        Button(
                            onClick = {
                                if (option.isCustom) {
                                    showCustomDialog = true
                                } else {
                                    onSnoozeSelected(option.minutes)
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(option.label)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        )
    }
}