package com.project.businesscardscannerapp.ViewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.project.businesscardscannerapp.AI.Insights
import com.project.businesscardscannerapp.AI.InsightsGenerator
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.RoomDB.ProvideDB.BusinessCardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class InsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BusinessCardRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BusinessCardRepository(database.businessCardDao(), database.insightsDao())
        loadInsights()
    }

    private val _insights = MutableStateFlow<Insights?>(null)
    val insights: StateFlow<Insights?> = _insights.asStateFlow()

    fun loadInsights() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()

            // Last 30 days
            val cal30d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }
            val start30d = cal30d.timeInMillis
            val scannedThisMonth = repository.countCards(start30d, now)

            // Previous 30 days
            val calPrev30dEnd = cal30d.timeInMillis
            val calPrev30dStart = Calendar.getInstance().apply {
                timeInMillis = calPrev30dEnd
                add(Calendar.DAY_OF_YEAR, -30)
            }.timeInMillis
            val scannedLastMonth = repository.countCards(calPrev30dStart, calPrev30dEnd)

            val scannedChangePct = if (scannedLastMonth > 0) {
                ((scannedThisMonth - scannedLastMonth).toDouble() / scannedLastMonth * 100).toInt()
            } else if (scannedThisMonth > 0) {
                100 // If no scans last month but some this month, it's a 100% increase
            } else {
                0
            }

            val leadBucketCounts = repository.getLeadBucketCounts().associate { it.leadBucket to it.cnt }
            val topIndustries = repository.getTopIndustries().map { it.name }
            val bestHour = repository.getBestHour()?.hour?.toIntOrNull()

            // Count overdue follow-ups instead of getting the Flow
            val overdueFollowUps = repository.getOverdueFollowUps(now).first().size

            _insights.value = Insights(
                scannedThisMonth = scannedThisMonth,
                scannedChangePct = scannedChangePct,
                topIndustries = topIndustries,
                bestHour = bestHour,
                overdue = overdueFollowUps, // Now passing Int instead of Flow
                bucketShare = leadBucketCounts
            )
        }
    }

    fun getInsightsText(): List<String> {
        return _insights.value?.let { InsightsGenerator.insightsToText(it) } ?: emptyList()
    }
}

class InsightsViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(InsightsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return InsightsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}