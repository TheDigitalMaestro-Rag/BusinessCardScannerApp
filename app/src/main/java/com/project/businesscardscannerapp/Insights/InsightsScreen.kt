package com.project.businesscardscannerapp.Insights

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.businesscardscannerapp.RoomDB.Entity.PipelineStage
import com.project.businesscardscannerapp.ViewModel.BusinessCardViewModel
import com.project.businesscardscannerapp.ViewModel.InsightsViewModel
import com.project.businesscardscannerapp.ViewModel.InsightsViewModelFactory
import com.project.businesscardscannerapp.ViewModel.PipelineAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun InsightsScreen() {
    val context = LocalContext.current
    val viewModel: InsightsViewModel = viewModel(factory = InsightsViewModelFactory(context.applicationContext as Application))
    val insights by viewModel.insights.collectAsState()
    val insightsText = viewModel.getInsightsText()
    val businessCardViewModel: BusinessCardViewModel = viewModel()

    // Add this state for pipeline analytics
    val pipelineAnalytics = remember { mutableStateOf<PipelineAnalytics?>(null) }

    // Load pipeline analytics
    LaunchedEffect(Unit) {
        val analytics = withContext(Dispatchers.IO) {
            businessCardViewModel.calculateAnalytics()
        }
        pipelineAnalytics.value = analytics
    }

    // Use LazyColumn as the main container for smooth scrolling
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        item {
            Text(
                text = "Your Business Card Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Pipeline Analytics Section
        item {
            Text(
                text = "Pipeline Analytics",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Pipeline Distribution - Only show if we have data
        pipelineAnalytics.value?.let { analytics ->
            item {
                Text(
                    text = "Pipeline Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            item {
                PipelineDistributionChart(analytics.stageDistribution)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Pipeline Metrics Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    MetricCard(
                        title = "Total Contacts",
                        value = analytics.totalContacts.toString(),
                        valueColor = MaterialTheme.colorScheme.primary
                    )
                    MetricCard(
                        title = "Conversion Rate",
                        value = "${String.format("%.1f", analytics.conversionRate)}%",
                        valueColor = if (analytics.conversionRate > 10) Color.Green else MaterialTheme.colorScheme.onSurface
                    )
                    MetricCard(
                        title = "Overdue Follow-ups",
                        value = analytics.overdueFollowUps.toString(),
                        valueColor = if (analytics.overdueFollowUps > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        if (insights == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading insights...")
                    }
                }
            }
        } else {
            // AI Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "AI Summary",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        insightsText.forEach { line ->
                            Text(
                                text = "• $line",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Key Metrics
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    insights?.let {
                        MetricCard(
                            title = "Cards Scanned (30d)",
                            value = it.scannedThisMonth.toString(),
                            change = "${if (it.scannedChangePct >= 0) "+" else ""}${it.scannedChangePct}%"
                        )
                        MetricCard(
                            title = "Overdue Follow-ups",
                            value = it.overdue.toString(),
                            valueColor = if (it.overdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Top Industries
            insights?.topIndustries?.takeIf { it.isNotEmpty() }?.let { industries ->
                item {
                    Text(
                        text = "Top Industries",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(industries) { industry ->
                    Text(
                        text = "• $industry",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            // Lead Bucket Distribution
            insights?.bucketShare?.takeIf { it.isNotEmpty() }?.let { bucketShare ->
                item {
                    Text(
                        text = "Lead Distribution",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(bucketShare.entries.toList()) { (bucket, count) ->
                    Text(
                        text = "• $bucket: $count cards",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // Add some extra padding at the bottom for better scrolling
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// Keep the existing MetricCard and PipelineDistributionChart composables unchanged
@Composable
fun MetricCard(title: String, value: String, change: String? = null, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Card(
        modifier = Modifier
            .padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            change?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (it.startsWith("+")) Color.Green.copy(alpha = 0.7f) else Color.Red.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun PipelineDistributionChart(distribution: Map<PipelineStage, Int>) {
    val total = distribution.values.sum()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            PipelineStage.values().forEach { stage ->
                val count = distribution[stage] ?: 0
                val percentage = if (total > 0) (count.toFloat() / total.toFloat() * 100) else 0f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stage.getDisplayName(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )

                    LinearProgressIndicator(
                        progress = percentage / 100f,
                        modifier = Modifier
                            .weight(2f)
                            .height(8.dp)
                            .padding(horizontal = 8.dp),
                        color = getStageColor(stage)
                    )

                    Text(
                        text = "$count (${String.format("%.1f", percentage)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// Add this helper function to get stage colors
fun getStageColor(stage: PipelineStage): Color {
    return when (stage) {
        PipelineStage.NEW -> Color(0xFF2196F3)
        PipelineStage.CONTACTED -> Color(0xFF4CAF50)
        PipelineStage.MEETING -> Color(0xFFFF9800)
        PipelineStage.NEGOTIATION -> Color(0xFF9C27B0)
        PipelineStage.CLOSED_WON -> Color(0xFF009688)
        PipelineStage.CLOSED_LOST -> Color(0xFFF44336)
    }
}