package com.project.businesscardscannerapp.LeadScoring

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.project.businesscardscannerapp.ViewModel.LeadScoringViewModel
import com.project.businesscardscannerapp.ViewModel.LeadScoringViewModelFactory

// Create a new file LeadScoringScreen.kt
@Composable
fun LeadScreen(context: Context) {
    val viewModel: LeadScoringViewModel = viewModel(factory = LeadScoringViewModelFactory(context))
    val leads by viewModel.allLeads.collectAsState(initial = emptyList())
    val categories = viewModel.getLeadCategories()
    var selectedCategory by remember { mutableStateOf("All") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Category filter chips
        CategoryFilterChips(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Leads list
        LeadList(
            leads = if (selectedCategory == "All") leads
            else leads.filter { it.leadCategory == selectedCategory }
        )
    }
}

@Composable
private fun CategoryFilterChips(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        // "All" chip
        FilterChip(
            selected = selectedCategory == "All",
            onClick = { onCategorySelected("All") },
            label = { Text("All") }
        )

        // Category chips
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(category) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (category) {
                        "Hot Lead" -> Color.Red.copy(alpha = 0.2f)
                        "Warm Lead" -> Color(0xFFFFA500).copy(alpha = 0.2f) // Orange
                        "Cool Lead" -> Color(0xFFADD8E6).copy(alpha = 0.2f) // Light Blue
                        "Cold Lead" -> Color(0xFFD3D3D3).copy(alpha = 0.2f) // Light Gray
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    }
                )
            )
        }
    }
}

@Composable
private fun LeadList(leads: List<BusinessCard>) {
    LazyColumn {
        items(leads) { lead ->
            LeadCardItem(lead = lead)
        }
    }
}

@Composable
private fun LeadCardItem(lead: BusinessCard) {
    val backgroundColor = when (lead.leadCategory) {
        "Hot Lead" -> Color.Red.copy(alpha = 0.1f)
        "Warm Lead" -> Color(0xFFFFA500).copy(alpha = 0.1f) // Orange
        "Cool Lead" -> Color(0xFFADD8E6).copy(alpha = 0.1f) // Light Blue
        "Cold Lead" -> Color(0xFFD3D3D3).copy(alpha = 0.1f) // Light Gray
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Score indicator
                Box(
                    modifier = Modifier
                        .background(
                            color = backgroundColor.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .size(40.dp)
                        .border(
                            width = 2.dp,
                            color = when (lead.leadCategory) {
                                "Hot Lead" -> Color.Red
                                "Warm Lead" -> Color(0xFFFFA500) // Orange
                                "Cool Lead" -> Color(0xFFADD8E6) // Light Blue
                                "Cold Lead" -> Color(0xFFD3D3D3) // Light Gray
                                else -> Color.Gray
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = lead.leadScore.toString(),
                        fontWeight = FontWeight.Bold,
                        color = when (lead.leadCategory) {
                            "Hot Lead" -> Color.Red
                            "Warm Lead" -> Color(0xFFFFA500) // Orange
                            "Cool Lead" -> Color.Blue
                            "Cold Lead" -> Color.Gray
                            else -> Color.Black
                        }
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = lead.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${lead.position} at ${lead.company}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = lead.leadCategory,
                    color = when (lead.leadCategory) {
                        "Hot Lead" -> Color.Red
                        "Warm Lead" -> Color(0xFFFFA500) // Orange
                        "Cool Lead" -> Color.Blue
                        "Cold Lead" -> Color.Gray
                        else -> Color.Black
                    },
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Industry: ${lead.industry}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
