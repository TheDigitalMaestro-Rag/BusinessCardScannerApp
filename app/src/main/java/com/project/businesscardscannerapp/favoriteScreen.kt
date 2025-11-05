package com.project.businesscardscannerapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.businesscardscannerapp.AppUI.BusinessCardItem
import com.project.businesscardscannerapp.ViewModel.BusinessCardViewModel

@Composable
fun FavoriteScreen(navController: NavController, viewModel: BusinessCardViewModel) {
    val favoriteCards by viewModel.favoriteCards.collectAsState(initial = emptyList())
    var selectedCards by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favorites") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedCards.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteConfirmation = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedCards.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { /* No action needed for favorites - no "Add Cards" button */ },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Favorite") },
                    modifier = Modifier.alpha(0f) // Make it invisible but keep layout
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = { showDeleteConfirmation = true },
                    icon = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove selected",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    },
                    text = { Text("Remove Selected") },
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            }
        }
    ) { padding ->
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Remove from Favorites") },
                text = { Text("Are you sure you want to remove ${selectedCards.size} cards from favorites?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.removeCardsFromFavorites(selectedCards.toList())
                            selectedCards = emptySet()
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Remove")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (favoriteCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No favorite cards yet",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    // You could add an icon here if you want
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Text(
                        text = "${favoriteCards.size} cards in favorites",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(favoriteCards) { card ->
                    val isSelected = selectedCards.contains(card.id)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (selectedCards.isNotEmpty()) {
                                    selectedCards = if (isSelected) {
                                        selectedCards - card.id
                                    } else {
                                        selectedCards + card.id
                                    }
                                } else {
                                    navController.navigate("details/${card.id}")
                                }
                            },
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = card.company ?: "No company",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Text(
                                text = card.name ?: "No name",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}