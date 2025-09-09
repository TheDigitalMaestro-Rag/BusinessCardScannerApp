package com.raghu.businesscardscanner2.AppUI

import android.app.Activity
import android.content.ActivityNotFoundException
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.core.content.FileProvider
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.raghu.businesscardscanner2.AnimatedBottomBar
import com.raghu.businesscardscanner2.FolderDetailsScreen
import com.raghu.businesscardscanner2.FolderSelectionDialog
import com.raghu.businesscardscanner2.FoldersScreen
import com.raghu.businesscardscanner2.NavItem
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import com.raghu.businesscardscanner2.ViewModel.BusinessCardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults.textFieldColors
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.project.businesscardscannerapp.R
import com.raghu.businesscardscanner2.MLkitTextRec.TextRecognizer
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCardApp(
    navController: NavController,
    viewModel: BusinessCardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Folders", Icons.Default.FolderOpen),
    )

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Track selected cards at the app level
    var selectedCardIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    // Define routes where drawer should be available
    val drawerRoutes = setOf("Home", "Folders")
    val showDrawer = currentRoute in drawerRoutes

    // Content that will be shown inside or outside the drawer
    val scaffoldContent = @Composable {
        val bottomBarRoutes = navItems.map { it.label }
        val selectedIndex = navItems.indexOfFirst { it.label == currentRoute }.takeIf { it != -1 } ?: 0

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (bottomBarRoutes.contains(currentRoute)) {
                    CenterAlignedTopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        navigationIcon = {
                            if (showDrawer && selectedCardIds.isEmpty()) { // Only show menu icon when drawer is available and not in selection mode
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Drawer")
                                }
                            } else if (selectedCardIds.isNotEmpty()) {
                                IconButton(onClick = { selectedCardIds = emptyList() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel selection")
                                }
                            }
                        },
                        actions = {
                            if (selectedCardIds.isNotEmpty()) {
                                DeleteSelectedCardsButton(
                                    selectedCardIds = selectedCardIds,
                                    onDeleteConfirmed = {
                                        scope.launch {
                                            selectedCardIds.forEach { cardId ->
                                                viewModel.deleteCardById(cardId)
                                            }
                                            selectedCardIds = emptyList()
                                        }
                                    }
                                )
                            }
                        }

                    )
                }
            },
            bottomBar = {
                if (bottomBarRoutes.contains(currentRoute)) {
                AnimatedBottomBar(
                    items = navItems,
                    selectedIndex = selectedIndex,
                    onItemSelected = { index ->
                        navController.navigate(navItems[index].label)
                    }
                )
            }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "Home",
                    modifier = Modifier.weight(1f)
                ) {
                    composable("Home") {
                        HomeScreen(
                            navController,
                            viewModel,
                            selectedCardIds = selectedCardIds,
                            onSelectedCardIdsChanged = { ids -> selectedCardIds = ids }
                        )
                    }
                    composable("Folders") { FoldersScreen(navController, viewModel) }
                    composable("folder/{folderId}") {
                        it.arguments?.getString("folderId")?.toIntOrNull()?.let { id ->
                            FolderDetailsScreen(id, navController, viewModel)
                        }
                    }
                    composable("scan") { ScanScreen(navController, viewModel) }
                    composable("details/{cardId}") {
                        it.arguments?.getString("cardId")?.toIntOrNull()?.let { id ->
                            DetailsScreen(id, navController, viewModel)
                        }
                    }
                }
            }
        }
    }

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerHeader()
                    DrawerBody(
                        navItems = navItems,
                        currentRoute = currentRoute,
                        onItemClick = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                            scope.launch { drawerState.close() }
                        }
                    )
                }
            }
        ) {
            scaffoldContent()
        }
    } else {
        scaffoldContent()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: BusinessCardViewModel,
    selectedCardIds: List<Int>,
    onSelectedCardIdsChanged: (List<Int>) -> Unit
) {
    val allCards by viewModel.allCards.collectAsState(initial = emptyList())
    val folders by viewModel.allFolders.collectAsState(initial = emptyList())


    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current


    // Filter cards based on search
    val cards = if (searchQuery.isNotBlank()) {
        allCards.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.company.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }
    } else {
        allCards
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    RoundedSearchBar(
                        query = searchQuery,
                        onQueryChanged = { searchQuery = it },
                        onSearchClicked = { /* optional search trigger */ }
                    )
                }
            )
        },
        floatingActionButton = {
            if (selectedCardIds.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    ExtendedFloatingActionButton(
                        onClick = { showExportMenu = true },
                        icon = { Icon(Icons.Default.Share, contentDescription = "Export") },
                        text = { Text("Export") }
                    )
                    ExtendedFloatingActionButton(
                        onClick = { showFolderDialog = true },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Add to folder") },
                        text = { Text("Add to Folder") }
                    )
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("scan") },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Scan") },
                    text = { Text("Scan Card") }
                )
            }
        }
    ) { padding ->
        if (showFolderDialog) {
            FolderSelectionDialog(
                folders = folders,
                selectedCardIds = selectedCardIds,
                onDismiss = { showFolderDialog = false },
                onFolderSelected = { folder ->
                    viewModel.addCardsToFolder(selectedCardIds, folder.id)
                    onSelectedCardIdsChanged(emptyList())
                    showFolderDialog = false
                },
                onCreateNewFolder = { name ->
                    viewModel.createFolder(name)
                }
            )
        }

        if (showExportMenu) {
            AlertDialog(
                onDismissRequest = { showExportMenu = false },
                title = { Text("Export Options") },
                text = { Text("Select export format for selected cards") },
                confirmButton = {
                    Button(onClick = {
                        selectedCardIds.forEach { cardId ->
                            viewModel.getCardById(cardId)?.let { card ->
                                exportToCSV(context, card)
                            }
                        }
                        showExportMenu = false
                        onSelectedCardIdsChanged(emptyList())
                    }) {
                        Text("Export as CSV")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        selectedCardIds.forEach { cardId ->
                            viewModel.getCardById(cardId)?.let { card ->
                                exportToPDF(context, card)
                            }
                        }
                        showExportMenu = false
                        onSelectedCardIdsChanged(emptyList())
                    }) {
                        Text("Export as PDF")
                    }
                }
            )
        }

        if (cards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("No business cards found. Tap + to scan one.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(cards) { card ->
                    val isSelected = selectedCardIds.contains(card.id)
                    BusinessCardItem(
                        card = card,
                        isSelected = isSelected,
                        isFavorite = card.isFavorite,
                        onCardClick = {
                            if (selectedCardIds.isNotEmpty()) {
                                onSelectedCardIdsChanged(
                                    if (isSelected) {
                                        selectedCardIds - card.id
                                    } else {
                                        selectedCardIds + card.id
                                    }
                                )
                            } else {
                                navController.navigate("details/${card.id}")
                            }
                        },
                        onLongClick = {
                            onSelectedCardIdsChanged(
                                if (isSelected) {
                                    selectedCardIds - card.id
                                } else {
                                    selectedCardIds + card.id
                                }
                            )
                        },
                        onFavoriteClick = {
                            viewModel.update(card.copy(isFavorite = !card.isFavorite))
                        }
                    )
                }
            }
        }
    }
}




@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoundedSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
    onSearchClicked: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp)
            .background(
                color = Color(0xFFF0F0F0),
                shape = RoundedCornerShape(28.dp)
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChanged,
                placeholder = { Text("Search here", color = Color.Gray) },
                singleLine = true,
                colors = textFieldColors(
                    containerColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    cursorColor = Color.DarkGray,
                    focusedTextColor = Color.Black
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )

            IconButton(
                onClick = onSearchClicked,
                modifier = Modifier
                    .size(40.dp)
                    .padding(end = 8.dp)
                    .background(Color(0xFF5B5B79), shape = CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BusinessCardItem(
    card: BusinessCard,
    capturedImage: Bitmap? = null,
    isSelected: Boolean = false,
    isFavorite: Boolean = false,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    showFavoriteIcon: Boolean = true
) {
    val context = LocalContext.current
    val cardImage = remember(card.imagePath) {
        capturedImage ?: card.imagePath?.let { path ->
            loadImageFromStorage(context, path)
        }
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onCardClick,
                onLongClick = onLongClick
            )
            .background(
                if (isSelected)  Color.White
                else MaterialTheme.colorScheme.surface
            ),

        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection indicator
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (cardImage != null) {
                    Image(
                        bitmap = cardImage.asImageBitmap(),
                        contentDescription = "Business card",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "Business card placeholder",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(32.dp)
                            .align(Alignment.Center)
                    )
                }
            }

            Spacer(modifier = Modifier.padding(9.dp))

            // Main content
            Column(modifier = Modifier.weight(1f)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                card.position?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                card.company?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Go",
                tint = Color.Black
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(navController: NavController, viewModel: BusinessCardViewModel) {
    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
    var extractedCard by remember { mutableStateOf<BusinessCard?>(null) }
    val textRecognizer = remember { TextRecognizer() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as Activity

    // ML Kit Scanner Options
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }

    // Scanner Client
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }

    // Launcher for Document Scan
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        try {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pages = scanResult?.pages

            if (!pages.isNullOrEmpty()) {
                val imageUri = pages[0].imageUri

                coroutineScope.launch {
                    try {
                        val bitmap = withContext(Dispatchers.IO) {
                            context.contentResolver.openInputStream(imageUri)?.use { input ->
                                BitmapFactory.decodeStream(input)
                            }
                        }

                        bitmap?.let {
                            capturedImage = it
                            val inputImage = InputImage.fromBitmap(it, 0)
                            val recognizedText = textRecognizer.processImage(inputImage)
                            val imagePath = saveImageToInternalStorage(context, it)
                            extractedCard = textRecognizer.extractBusinessCardInfo(recognizedText, imagePath)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Failed to process scanned image", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "No scan result found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to handle scan result", Toast.LENGTH_SHORT).show()
        }

    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan Business Card") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .padding(padding)
            .fillMaxSize()) {

            if (capturedImage == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Tap below to scan or pick from gallery")
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        scanner.getStartScanIntent(activity)
                            .addOnSuccessListener { intentSender ->
                                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                            }
                            .addOnFailureListener { e ->
                                e.printStackTrace()
                            }
                    }) {
                        Icon(Icons.Default.Camera, contentDescription = "Scan")
                        Text(" Scan Card")
                    }
                }
            } else {
                extractedCard?.let { card ->
                    BusinessCardForm(
                        card = card,
                        onSave = {
                            coroutineScope.launch {
                                viewModel.insert(it)
                                navController.popBackStack()
                            }
                        },
                        onCancel = {
                            capturedImage = null
                            extractedCard = null
                        },
                        capturedImage = capturedImage
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCardForm(
    card: BusinessCard,
    onSave: (BusinessCard) -> Unit,
    onCancel: () -> Unit,
    capturedImage: Bitmap?,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(card.name) }
    var company by remember { mutableStateOf(card.company ?: "") }
    var position by remember { mutableStateOf(card.position ?: "") }
    var phone by remember { mutableStateOf(card.phone ?: "") }
    var email by remember { mutableStateOf(card.email ?: "") }
    var address by remember { mutableStateOf(card.address ?: "") }
    var website by remember { mutableStateOf(card.website ?: "") }
    var notes by remember { mutableStateOf(card.notes ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Captured image preview
        capturedImage?.let { bitmap ->
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp)
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Captured business card",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Section Title
        Text("Business Card Details", style = MaterialTheme.typography.titleMedium)

        // Input Fields
        AnimatedOutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = "Name",
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedOutlinedTextField(
            value = position,
            onValueChange = { position = it },
            label ="Position",
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedOutlinedTextField(
            value = company,
            onValueChange = { company = it },
            label = "Company",
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedOutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = "Phone",
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedOutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedOutlinedTextField(
            value = website,
            onValueChange = { website = it },
            label = "Website",
            modifier = Modifier.fillMaxWidth()
        )

        AnimatedOutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = "Address",
            modifier = Modifier.fillMaxWidth(),
        )

        AnimatedOutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = "Notes",
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Save/Cancel Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(onClick = {
                onSave(
                    card.copy(
                        name = name,
                        company = company,
                        position = position,
                        phone = phone,
                        email = email,
                        address = address,
                        website = website,
                        notes = notes
                    )
                )
            }) {
                Text("Save")
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(cardId: Int, navController: NavController, viewModel: BusinessCardViewModel) {
    var card by remember { mutableStateOf<BusinessCard?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) } // New state for edit mode
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(cardId) {
        card = viewModel.getCardById(cardId)
    }

    val currentCard by viewModel.getCardByIdFlow(cardId).collectAsState(initial = null)

    LaunchedEffect(currentCard) {
        if (currentCard != null) {
            card = currentCard
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Card" else "Card Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditing) {
                            isEditing = false
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                },
                actions = {
                    if (card != null) {
                        if (!isEditing) {
                            // Show normal actions when not editing
                            IconButton(onClick = {
                                isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = {
                                card?.let { shareBusinessCard(context, it) }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                showExportMenu = true }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                            IconButton(onClick = {
                                showDeleteDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        } else {
                            // Show save action when editing
                            IconButton(onClick = {
                                card?.let { updatedCard ->
                                    coroutineScope.launch {
                                        viewModel.update(updatedCard)
                                        isEditing = false
                                    }
                                }
                            }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Save")
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isEditing && card != null) {
                // Add CRM action buttons at the bottom
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { card?.phone?.let { call(context, it) } }) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { card?.phone?.let { message(context, it) } }) {
                        Icon(Icons.Default.Message, contentDescription = "SMS", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { card?.email?.let { email(context, it) } }) {
                        Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { card?.company?.let { searchFacebook(context, it) } }) {
                        Icon(Icons.Default.Facebook, contentDescription = "Facebook", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { card?.website?.let { openWebsite(context, it) } }) {
                        Icon(Icons.Default.Public, contentDescription = "Website", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    ) { padding ->
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Business Card") },
                text = { Text("Are you sure you want to delete this business card?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                card?.let {
                                    viewModel.delete(it)
                                    navController.popBackStack()
                                }
                            }
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showExportMenu) {
            AlertDialog(
                onDismissRequest = { showExportMenu = false },
                title = { Text("Export Business Card") },
                text = { Text("Choose a format to export the business card.") },
                confirmButton = {
                    TextButton(onClick = {
                        showExportMenu = false
                        card?.let { exportToCSV(context, it) }
                    }) {
                        Text("Export as CSV")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showExportMenu = false
                        card?.let { exportToPDF(context, it) }
                    }) {
                        Text("Export as PDF")
                    }
                }
            )
        }


        when {
            card == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            isEditing -> {
                // Show editable form
                card?.let { currentCard ->
                    var name by remember { mutableStateOf(currentCard.name) }
                    var company by remember { mutableStateOf(currentCard.company ?: "") }
                    var position by remember { mutableStateOf(currentCard.position ?: "") }
                    var phone by remember { mutableStateOf(currentCard.phone ?: "") }
                    var email by remember { mutableStateOf(currentCard.email ?: "") }
                    var address by remember { mutableStateOf(currentCard.address ?: "") }
                    var website by remember { mutableStateOf(currentCard.website ?: "") }
                    var notes by remember { mutableStateOf(currentCard.notes ?: "") }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        currentCard.imagePath?.let { imagePath ->
                            val bitmap = loadImageFromStorage(imagePath)
                            bitmap?.let {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Image(
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = "Business card image",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        AnimatedOutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = "Name",
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedOutlinedTextField(
                            value = position,
                            onValueChange = { position = it },
                            label ="Position",
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedOutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = "Company",
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedOutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "Phone",
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedOutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email",
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedOutlinedTextField(
                            value = website,
                            onValueChange = { website = it },
                            label = "Website",
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedOutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = "Address",
                            modifier = Modifier.fillMaxWidth(),
                        )

                        AnimatedOutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = "Notes",
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Update the card object whenever any field changes
                        LaunchedEffect(name, company, position, phone, email, address, website, notes) {
                            card = currentCard.copy(
                                name = name,
                                company = company,
                                position = position,
                                phone = phone,
                                email = email,
                                address = address,
                                website = website,
                                notes = notes
                            )
                        }
                    }
                }
            }
            else -> {
                val scrollState = rememberScrollState()
                // Show read-only details (existing implementation)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding).verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    card?.imagePath?.let { imagePath ->
                        val bitmap = loadImageFromStorage(imagePath)
                        bitmap?.let {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Image(
                                    bitmap = it.asImageBitmap(),
                                    contentDescription = "Business card image",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Text(
                        text = card?.name ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = card?.position ?: "",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = card?.company ?: "",
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider()

                    Text(
                        text = "Contact Information",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    card?.phone?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }

                    card?.email?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }

                    card?.website?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }

                    card?.address?.let {
                        HorizontalDivider()
                        Text(
                            text = "Address",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }

                    card?.notes?.let {
                        if (it.isNotEmpty()) {
                            HorizontalDivider()
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(text = it, style = MaterialTheme.typography.bodyLarge)
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


// Helper function to load image from storage
fun loadImageFromStorage(imagePath: String): Bitmap? {
    return try {
        val file = File(imagePath)
        BitmapFactory.decodeStream(FileInputStream(file))
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun loadImageFromStorage(context: Context, imagePath: String): Bitmap? {
    return try {
        val file = File(imagePath)
        if (file.exists()) {
            BitmapFactory.decodeStream(FileInputStream(file))
        } else {
            null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Share business card function
fun shareBusinessCard(context: Context, card: BusinessCard) {
    val shareText = buildString {
        append("Business Card Details\n\n")
        append("Name: ${card.name}\n")
        card.position?.let { append("Position: $it\n") }
        card.company?.let { append("Company: $it\n") }
        card.phone?.let { append("Phone: $it\n") }
        card.email?.let { append("Email: $it\n") }
        card.website?.let { append("Website: $it\n") }
        card.address?.let { append("Address: $it\n") }
        card.notes?.let { append("Notes: $it\n") }
    }

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"

        // Share image if available
        card.imagePath?.let { imagePath ->
            val imageFile = File(imagePath)
            if (imageFile.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

//    context.startActivity(Intent.createChooser(shareIntent, "Share Business Card"))
    try {
        context.startActivity(Intent.createChooser(shareIntent, "Share Business Card"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to share", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }

}


// Export to CSV function
fun exportToCSV(context: Context, card: BusinessCard) {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "BusinessCard_${card.name}_$timeStamp.csv"

    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        FileWriter(file).use { writer ->
            writer.append("Field,Value\n")
            writer.append("Name,${card.name}\n")
            card.position?.let { writer.append("Position,$it\n") }
            card.company?.let { writer.append("Company,$it\n") }
            card.phone?.let { writer.append("Phone,$it\n") }
            card.email?.let { writer.append("Email,$it\n") }
            card.website?.let { writer.append("Website,$it\n") }
            card.address?.let { writer.append("Address,$it\n") }
            card.notes?.let { writer.append("Notes,$it\n") }
        }

        Toast.makeText(context, "Exported to Downloads/$fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Export to PDF function
fun exportToPDF(context: Context, card: BusinessCard) {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val fileName = "BusinessCard_${card.name}_$timeStamp.pdf"

    try {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint()
        paint.textSize = 12f

        var y = 50f
        canvas.drawText("Business Card Details", 50f, y, paint)
        y += 30f
        canvas.drawText("Name: ${card.name}", 50f, y, paint)
        y += 20f

        card.position?.let {
            canvas.drawText("Position: $it", 50f, y, paint)
            y += 20f
        }

        card.company?.let {
            canvas.drawText("Company: $it", 50f, y, paint)
            y += 20f
        }

        card.phone?.let {
            canvas.drawText("Phone: $it", 50f, y, paint)
            y += 20f
        }

        card.email?.let {
            canvas.drawText("Email: $it", 50f, y, paint)
            y += 20f
        }

        card.website?.let {
            canvas.drawText("Website: $it", 50f, y, paint)
            y += 20f
        }

        card.address?.let {
            canvas.drawText("Address: $it", 50f, y, paint)
            y += 20f
        }

        card.notes?.let {
            canvas.drawText("Notes: $it", 50f, y, paint)
        }

        document.finishPage(page)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { outputStream ->
            document.writeTo(outputStream)
        }

        document.close()

        Toast.makeText(context, "Exported to Downloads/$fileName", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Add this function to save images
fun saveImageToInternalStorage(context: Context, bitmap: Bitmap): String {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File.createTempFile(
        "BC_${timeStamp}_",
        ".jpg",
        storageDir
    )

    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }

    return file.absolutePath
}


fun call(context: Context, phone: String) {
    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Calling not supported on this device", Toast.LENGTH_SHORT).show()
    }
}

fun message(context: Context, phone: String) {
    if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone"))
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "Messaging not supported on this device", Toast.LENGTH_SHORT).show()
    }
}

fun email(context: android.content.Context, email: String) {
    if (email.isNotBlank()) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$email")
        }
        context.startActivity(intent)
    }
}

fun searchFacebook(context: android.content.Context, company: String) {
    if (company.isNotBlank()) {
        val url = "https://www.facebook.com/search/top?q=${Uri.encode(company)}"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}

fun openWebsite(context: android.content.Context, website: String) {
    if (website.isNotBlank()) {
        val fixedUrl = if (!website.startsWith("http")) "https://$website" else website
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fixedUrl))
        context.startActivity(intent)
    }
}



//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun ScanScreen(
//    navController: NavController,
//    viewModel: BusinessCardViewModel,
//    onScanComplete: () -> Unit = {}
//) {
//    // State management
//    var capturedImage by remember { mutableStateOf<Bitmap?>(null) }
//    var extractedCard by remember { mutableStateOf<BusinessCard?>(null) }
//    var isLoading by remember { mutableStateOf(false) }
//    var errorMessage by remember { mutableStateOf<String?>(null) }
//
//    // Dependencies
//    val textRecognizer = remember { TextRecognizer() }
//    val coroutineScope = rememberCoroutineScope()
//    val context = LocalContext.current
//    val activity = context as ComponentActivity
//    val adManager = remember { (context.applicationContext as BusinessCardScannerApp).adManager }
//
//    // Scanner configuration
//    val scannerOptions = remember {
//        GmsDocumentScannerOptions.Builder()
//            .setGalleryImportAllowed(true)
//            .setPageLimit(1)
//            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
//            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
//            .build()
//    }
//
//    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }
//
//    // Handle scan result
//    val scannerLauncher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.StartIntentSenderForResult()
//    ) { result ->
//        if (result.resultCode == Activity.RESULT_OK) {
//            isLoading = true
//            errorMessage = null
//
//            coroutineScope.launch {
//                try {
//                    val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
//                    val pages = scanResult?.pages
//
//                    if (!pages.isNullOrEmpty()) {
//                        val imageUri = pages[0].imageUri
//
//                        withContext(Dispatchers.IO) {
//                            val bitmap = context.contentResolver
//                                .openInputStream(imageUri)
//                                ?.use { BitmapFactory.decodeStream(it) }
//
//                            bitmap?.let { bmp ->
//                                capturedImage = bmp
//                                val inputImage = InputImage.fromBitmap(bmp, 0)
//                                val recognizedText = textRecognizer.processImage(inputImage)
//                                val imagePath = saveImageToInternalStorage(context, bmp)
//                                extractedCard = textRecognizer.extractBusinessCardInfo(recognizedText, imagePath)
//                            }
//                        }
//                    }
//                } catch (e: Exception) {
//                    errorMessage = "Failed to process image: ${e.localizedMessage}"
//                    Log.e("ScanScreen", "Scan processing error", e)
//                } finally {
//                    isLoading = false
//                }
//            }
//        }
//    }
//
//    // Handle back navigation with potential ad show
//    BackHandler {
//        if (capturedImage != null) {
//            capturedImage = null
//            extractedCard = null
//        } else {
//            navController.popBackStack()
//        }
//    }
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Scan Business Card") },
//                navigationIcon = {
//                    IconButton(onClick = {
//                        if (capturedImage != null) {
//                            capturedImage = null
//                            extractedCard = null
//                        } else {
//                            navController.popBackStack()
//                        }
//                    }) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Box(
//            modifier = Modifier
//                .padding(padding)
//                .fillMaxSize()
//        ) {
//            when {
//                isLoading -> {
//                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
//                }
//
//                errorMessage != null -> {
//                    ErrorMessage(
//                        message = errorMessage!!,
//                        onRetry = {
//                            errorMessage = null
//                            scanner.getStartScanIntent(activity)
//                                .addOnSuccessListener { intentSender ->
//                                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
//                                }
//                        },
//                        modifier = Modifier.align(Alignment.Center)
//                    )
//                }
//
//                capturedImage == null -> {
//                    ScanPromptView(
//                        onScanClicked = {
//                            scanner.getStartScanIntent(activity)
//                                .addOnSuccessListener { intentSender ->
//                                    scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
//                                }
//                                .addOnFailureListener { e ->
//                                    errorMessage = "Failed to start scanner: ${e.localizedMessage}"
//                                }
//                        },
//                        modifier = Modifier.align(Alignment.Center)
//                    )
//                }
//
//                else -> {
//                    extractedCard?.let { card ->
//                        BusinessCardForm(
//                            card = card,
//                            onSave = { savedCard ->
//                                coroutineScope.launch {
//                                    viewModel.insert(savedCard)
//                                    onScanComplete() // Notify parent about completed scan
//                                    navController.popBackStack()
//                                }
//                            },
//                            onCancel = {
//                                capturedImage = null
//                                extractedCard = null
//                            },
//                            capturedImage = capturedImage,
//                            modifier = Modifier.fillMaxSize()
//                        )
//                    }
//                }
//            }
//        }
//    }
//}
//
//@Composable
//private fun ScanPromptView(
//    onScanClicked: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier,
//        verticalArrangement = Arrangement.Center,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Icon(
//            imageVector = Icons.Default.CameraAlt,
//            contentDescription = "Scan",
//            modifier = Modifier.size(64.dp),
//            tint = MaterialTheme.colorScheme.primary
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//        Text(
//            text = "Scan a Business Card",
//            style = MaterialTheme.typography.headlineSmall
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//        Text(
//            text = "Position the card within the frame for best results",
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
//        )
//        Spacer(modifier = Modifier.height(24.dp))
//        Button(
//            onClick = onScanClicked,
//            modifier = Modifier.width(200.dp)
//        ) {
//            Icon(Icons.Default.Camera, contentDescription = null)
//            Spacer(modifier = Modifier.width(8.dp))
//            Text("Start Scan")
//        }
//    }
//}
//
//@Composable
//private fun ErrorMessage(
//    message: String,
//    onRetry: () -> Unit,
//    modifier: Modifier = Modifier
//) {
//    Column(
//        modifier = modifier,
//        horizontalAlignment = Alignment.CenterHorizontally
//    ) {
//        Icon(
//            imageVector = Icons.Default.Error,
//            contentDescription = "Error",
//            tint = MaterialTheme.colorScheme.error,
//            modifier = Modifier.size(48.dp)
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//        Text(
//            text = message,
//            style = MaterialTheme.typography.bodyMedium,
//            color = MaterialTheme.colorScheme.error,
//            textAlign = TextAlign.Center
//        )
//        Spacer(modifier = Modifier.height(16.dp))
//        Button(onClick = onRetry) {
//            Text("Try Again")
//        }
//    }
//}