package com.raghu.businesscardscanner2.FollowUpRemaiders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpRemainderUtility.GoogleCalendarHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// --- ViewModel: FollowUpViewModel.kt ---
class FollowUpViewModel(
    private val repository: FollowUpRepository,
    private val notificationHelper: NotificationHelper
) : ViewModel() {

    val pendingReminders = repository.getPendingReminders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addReminder(reminder: FollowUpReminderEntity) = viewModelScope.launch {
        repository.addReminder(reminder)
        notificationHelper.scheduleFollowUpNotification(
            cardId = reminder.id,
            message = reminder.message,
            triggerTime = reminder.dueDate,
            repeatType = reminder.repeatType,
            contactName = reminder.contactName,
            companyName = reminder.companyName
        )
    }

    fun updateReminder(reminder: FollowUpReminderEntity) = viewModelScope.launch {
        repository.updateReminder(reminder)
        // Reschedule notification if due date changed
        notificationHelper.cancelNotification(reminder.id)
        notificationHelper.scheduleNotification(reminder)
    }

    fun completeReminder(reminderId: Int) = viewModelScope.launch {
        repository.completeReminder(reminderId)
        notificationHelper.cancelNotification(reminderId)
    }

    fun snoozeReminder(reminderId: Int, snoozeMillis: Long) = viewModelScope.launch {
        repository.snoozeReminder(reminderId, snoozeMillis)
    }

    fun addToGoogleCalendar(context: Context, reminder: FollowUpReminderEntity) {
        GoogleCalendarHelper.addEvent(context, reminder)
    }

    fun getReminderByIdFlow(reminderId: Int) = repository.getReminderByIdFlow(reminderId)
}