package com.raghu.businesscardscanner2.FollowUpRemaiders

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.raghu.businesscardscanner2.AppUI.FollowUpReminderItem
import com.raghu.businesscardscanner2.BusinessCardScannerApp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun FollowUpReminderScreen(viewModel: FollowUpViewModel, reminderId: Int) {
    val context = LocalContext.current

    val reminder by viewModel.getReminderByIdFlow(reminderId).collectAsState(initial = null)

    if (reminder == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Reminder not found")
        }
    } else {
        Column(modifier = Modifier.padding(16.dp)) {
            FollowUpReminderItem(
                reminder = reminder!!,
                onComplete = { viewModel.completeReminder(reminder!!.id) },
                onSnooze = { viewModel.snoozeReminder(reminder!!.id, 10 * 60 * 1000) },
                onAddToCalendar = { viewModel.addToGoogleCalendar(context, reminder!!) }
            )
        }
    }
}


