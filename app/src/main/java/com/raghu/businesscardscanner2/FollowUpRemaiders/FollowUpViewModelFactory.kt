package com.raghu.businesscardscanner2.FollowUpRemaiders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// FollowUpViewModelFactory.kt

class FollowUpViewModelFactory(
    private val repository: FollowUpRepository,
    private val notificationHelper: NotificationHelper
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FollowUpViewModel::class.java)) {
            return FollowUpViewModel(repository, notificationHelper) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}