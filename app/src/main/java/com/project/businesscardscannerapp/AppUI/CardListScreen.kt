package com.project.businesscardscannerapp.AppUI

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard

@Composable
fun CardListWithSearchScreen(
    navController: NavController,
    cards: List<BusinessCard>, // Replace with actual Folder type
    selectedCardIds: List<Int>,
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    var searchQuery by remember { mutableStateOf("") }

    val filteredCards = if (searchQuery.isNotBlank()) {
        cards.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.company.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }
    } else {
        cards
    }

    Column(
        modifier = Modifier.background(color = MaterialTheme.colorScheme.background)
    ){
        RoundedSearchBar(
            query = searchQuery,
            onQueryChanged = { searchQuery = it },
            onSearchClicked = { /* Optional */ }
        )

        if (filteredCards.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No cards found.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCards) { card ->
                    val isSelected = selectedCardIds.contains(card.id)
                    BusinessCardItem(
                        card = card,
                        isSelected = isSelected,
                        isFavorite = card.isFavorite,
                        onCardClick = {
                            if (selectedCardIds.isNotEmpty()) {

                            } else {
                                navController.navigate("details/${card.id}")
                            }
                        }
                    )
                }
            }
        }
    }
}
