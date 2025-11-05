package com.project.businesscardscannerapp

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.navigation.NavController
import com.project.businesscardscannerapp.AppUI.BusinessCardItem
import com.project.businesscardscannerapp.AppUI.ShowAd
import com.project.businesscardscannerapp.AppUI.findActivity
import com.project.businesscardscannerapp.RoomDB.Entity.Folder
import com.project.businesscardscannerapp.ViewModel.BusinessCardViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(navController: NavController, viewModel: BusinessCardViewModel) {
    val folders by viewModel.allFolders.collectAsState(initial = emptyList())
    val selectedFolders by viewModel.selectedFolders.collectAsState()
    val favoritesFolderId by viewModel.favoritesFolderIdState.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") } // Add state for folder name

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = { Text("Folders") },
                actions = {
                    if (selectedFolders.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateFolderDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add folder") },
                text = { Text("New Folder") }
            )
        }
    ) { padding ->
        // Create Folder Dialog
        if (showCreateFolderDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                title = { Text("Create New Folder") },
                text = {
                    TextField(
                        value = newFolderName,
                        onValueChange = { newFolderName = it },
                        label = { Text("Folder name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newFolderName.isNotBlank()) {
                                viewModel.createFolder(newFolderName)
                                newFolderName = ""
                                showCreateFolderDialog = false
                            }
                        }
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    Button(
                        onClick = {
                            newFolderName = ""
                            showCreateFolderDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Delete Folders Dialog
        if (showDeleteDialog) {
            DeleteFoldersDialog(
                count = selectedFolders.size,
                onConfirm = {
                    viewModel.deleteSelectedFolders()
                    showDeleteDialog = false
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(color = MaterialTheme.colorScheme.background)
                .padding(padding),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(folders) { folder ->
                val isFavorites = folder.id == favoritesFolderId
                val isSelected = selectedFolders.contains(folder.id)

                FolderItem(
                    folder = folder,
                    isFavorites = isFavorites,
                    isSelected = isSelected,
                    onClick = {
                        if (selectedFolders.isEmpty()) {
                            navController.navigate("folder/${folder.id}")
                        } else {
                            viewModel.toggleFolderSelection(folder)
                        }
                    },
                    onLongClick = {
                        if (!isFavorites) {
                            viewModel.toggleFolderSelection(folder)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItem(
    folder: Folder,
    isFavorites: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
//                isSelected -> MaterialTheme.colorScheme.secondaryContainer
////                isFavorites -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
//                else -> MaterialTheme.colorScheme.surface

                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 3.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isFavorites) Icons.Default.Star else Icons.Default.Folder,
                contentDescription = "Folder",
                tint =  if (isFavorites) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            if (isSelected && !isFavorites) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DeleteFoldersDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Folders") },
        text = { Text("Are you sure you want to delete $count selected folders?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailsScreen(
    folderId: Int,
    navController: NavController,
    viewModel: BusinessCardViewModel
) {
    val cards by viewModel.getCardsInFolder(folderId).collectAsState(initial = emptyList())
    val folder by viewModel.getFolderById(folderId).collectAsState(initial = null)
    var showAddCardsDialog by remember { mutableStateOf(false) }
    var selectedCards by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val cardImages by viewModel.cardImages.collectAsState()

    val context = LocalContext.current
    val activity = context.findActivity()
    fun Context.findActivity(): Activity? {
        var context = this
        while (context is android.content.ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                title = { Text(folder?.name ?: "Folder") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
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
                    onClick = { showAddCardsDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add cards") },
                    text = { Text("Add Cards") }
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
        if (showAddCardsDialog) {
            AddCardsToFolderDialog(
                folderId = folderId,
                viewModel = viewModel,
                onDismiss = { showAddCardsDialog = false }
            )
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Remove Cards") },
                text = { Text("Are you sure you want to remove ${selectedCards.size} cards from this folder?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.removeCardsFromFolder(selectedCards.toList(), folderId)
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

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No cards in this folder")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background)
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Text(
                        text = "${cards.size} cards in this folder",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(cards) { card ->
                    val isSelected = selectedCards.contains(card.id)
                    BusinessCardItem(
                        card = card,
                        isSelected = isSelected,
                        onCardClick = {
                            if (selectedCards.isNotEmpty()) {
                                selectedCards = if (isSelected) {
                                    selectedCards - card.id
                                } else {
                                    selectedCards + card.id
                                }
                            } else {
                                activity?.let {
                                    ShowAd(it)
                                } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
                                navController.navigate("details/${card.id}")
                            }
                        },
                        onLongClick = {
                            selectedCards = if (isSelected) {
                                selectedCards - card.id
                            } else {
                                selectedCards + card.id
                            }
                        },
                    )
                }
            }
        }
    }
}


@Composable
fun FolderSelectionDialog(
    folders: List<Folder>,
    selectedCardIds: List<Int>,
    onDismiss: () -> Unit,
    onFolderSelected: (Folder) -> Unit,
    onCreateNewFolder: (String) -> Unit
) {
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Folder") },
        text = {
            Column {
                Text("Select a folder for ${selectedCardIds.size} selected cards",
                    style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(folders) { folder ->
                        ListItem(
                            headlineContent = { Text(folder.name) },
                            modifier = Modifier.clickable { onFolderSelected(folder) }
                        )
                        Divider()
                    }
                }

                Button(
                    onClick = { showNewFolderDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Create New Folder")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Folder Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            onCreateNewFolder(newFolderName)
                            showNewFolderDialog = false
                            newFolderName = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                Button(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}







@Composable
private fun NewFolderDialog(
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Folder") },
        text = {
            OutlinedTextField(
                value = folderName,
                onValueChange = { folderName = it },
                label = { Text("Folder Name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onCreate(folderName)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}