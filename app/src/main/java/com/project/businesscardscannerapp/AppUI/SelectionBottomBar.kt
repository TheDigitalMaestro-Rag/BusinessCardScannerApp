package com.project.businesscardscannerapp.AppUI

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SelectionBottomBar(
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onFavorite: () -> Unit
) {
//    BottomAppBar(
//        containerColor = MaterialTheme.colorScheme.surfaceVariant,
//        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
//    ) {
        Row(
            modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Delete action
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }

            // Share action
            IconButton(onClick = onShare) {
                Icon(Icons.Filled.Share, contentDescription = "Share")
            }

            // Export action
            IconButton(onClick = onExport) {
                Icon(Icons.Filled.FileDownload, contentDescription = "Export")
            }

            // Favorite action
            IconButton(onClick = onFavorite) {
                Icon(Icons.Filled.Favorite, contentDescription = "Favorite")
            }
        }
//    }
}

@Composable
fun DeleteSelectedCardsButton(
    selectedCardIds: List<Int>,
    onDeleteConfirmed: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    IconButton(
        onClick = { showDialog = true }
    ) {
        Icon(
            Icons.Default.Delete,
            contentDescription = "Delete Selected",
            tint = MaterialTheme.colorScheme.error
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Delete Cards") },
            text = {
                Text(
                    "Are you sure you want to delete ${selectedCardIds.size} selected card${if (selectedCardIds.size > 1) "s" else ""}?"
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteConfirmed()
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}



//bottomBar = {
//    if (selectedCardIds.isNotEmpty()) {
//        SelectionBottomBar(
//            onDelete = {
//                selectedCardIds.forEach { cardId ->
//                    viewModel.getCardById(cardId)?.let { card ->
//                        viewModel.delete(card)
//                    }
//                }
//                selectedCardIds = emptyList()
//            },
//            onShare = {
//                if (selectedCardIds.isNotEmpty()) {
//                    val firstCard = viewModel.getCardById(selectedCardIds.first())
//                    firstCard?.let { shareBusinessCard(context, it) }
//                }
//            },
//            onExport = { showExportMenu = true },
//            onFavorite = {
//                selectedCardIds.forEach { cardId ->
//                    viewModel.getCardById(cardId)?.let { card ->
//                        viewModel.update(card.copy(isFavorite = !card.isFavorite))
//                    }
//                }
//                selectedCardIds = emptyList()
//            }
//        )
//    }
//}