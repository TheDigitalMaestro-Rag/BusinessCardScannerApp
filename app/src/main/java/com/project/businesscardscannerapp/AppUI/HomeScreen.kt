package com.project.businesscardscannerapp.AppUI

import android.annotation.SuppressLint
import android.app.Activity
import android.app.TimePickerDialog
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
import androidx.compose.material3.IconButton
import androidx.core.content.FileProvider
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import android.content.Context

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
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
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
import com.project.businesscardscannerapp.AnimatedBottomBar
import com.project.businesscardscannerapp.FolderDetailsScreen
import com.project.businesscardscannerapp.FolderSelectionDialog
import com.project.businesscardscannerapp.FoldersScreen
import com.project.businesscardscannerapp.NavItem
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.project.businesscardscannerapp.ViewModel.BusinessCardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.net.Uri
import android.util.Log
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.businesscardscannerapp.AdHelper
import com.project.businesscardscannerapp.AdHelper.showAd
import com.project.businesscardscannerapp.BannerAdView
import com.project.businesscardscannerapp.BusinessCardScannerApp
import com.project.businesscardscannerapp.MLkitTextRec.TextRecognizer
import java.util.Calendar
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.os.Build
import android.app.AlarmManager
import android.content.pm.PackageManager
import androidx.activity.result.PickVisualMediaRequest
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.sp
import com.project.businesscardscannerapp.R
import com.project.businesscardscannerapp.AI.FollowUpSuggester
import com.project.businesscardscannerapp.AI.Priority
import com.project.businesscardscannerapp.Insights.InsightsScreen
import com.project.businesscardscannerapp.LeadScoring.LeadScreen
import com.project.businesscardscannerapp.ReminderBroadcastReceiver
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.project.businesscardscannerapp.AI.SmartFollowUpPrediction
import com.project.businesscardscannerapp.HubSpotIntegration.HubSpotIntegrationViewModel
import com.project.businesscardscannerapp.HubSpotIntegration.HubSpotIntegrationViewModelFactory
import com.project.businesscardscannerapp.HubSpotIntegration.UI.HubSpotActionsDialog
import com.project.businesscardscannerapp.HubSpotIntegration.UI.HubSpotSettingsScreen
import com.project.businesscardscannerapp.NotificationConstants.REMINDER_CARD_ID
import com.project.businesscardscannerapp.NotificationConstants.REMINDER_MESSAGE
import com.project.businesscardscannerapp.NotificationConstants.REMINDER_NOTIFICATION_ID
import com.project.businesscardscannerapp.NotificationConstants.REMINDER_REQUEST_CODE_BASE
import com.project.businesscardscannerapp.Registration.BackupScreen

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BusinessCardApp(
    navController: NavController,
    viewModel: BusinessCardViewModel,
    modifier: Modifier = Modifier,
    useDarkTheme: Boolean ,
    setUseDarkTheme: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val application = context.applicationContext as BusinessCardScannerApp

    val activity = context.findActivity()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navItems = listOf(
        NavItem("Home", Icons.Default.Home),
        NavItem("Folders", Icons.Default.FolderOpen),
        NavItem("LeadScore", Icons.Default.Dashboard),
        NavItem("SearchBar", Icons.Default.Search),
        NavItem("Insights", Icons.Default.Lightbulb),
        NavItem("M-CRM", Icons.Default.OnDeviceTraining),
        NavItem("HubSpot", Icons.Default.CropPortrait),
        NavItem("Backup", Icons.Default.Cloud)
    )

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Track selected cards at the app level
    var selectedCardIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    // Define routes where drawer should be available
    val drawerRoutes = setOf("Home", "Folders", "LeadScore", "SearchBar", "Insights") // Add Insights
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
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        ),
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
                        },
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
                    composable("LeadScore") {
                        LeadScreen(context = LocalContext.current)
                    }
                    composable("SearchBar") {
                        SearchBarScreen(navController = navController)
                    }
                    composable("M-CRM"){
                        PipelineScreen(navController = navController)
                    }
                    composable("Insights") {
                        InsightsScreen() // Call your new InsightsScreen
                    }
                    // In your navigation setup, add:
                    composable("Backup") {
                        BackupScreen()
                    }
                    composable("HubSpot"){
                        HubSpotSettingsScreen()
                    }
                    // In your NavHost setup, add:
                    composable("nfc") {
                        val viewModel: BusinessCardViewModel = viewModel()
                        NFCScreen(
                            navController = navController,
                            viewModel = viewModel,
                            nfcIntent = null
                        )
                    }
                    // Qr Scanner
                    // In your NavHost setup in HomeScreen.kt
                    composable("qrScan") {
                        val viewModel: BusinessCardViewModel = viewModel()
                        var isProcessing by remember { mutableStateOf(false) }
                        var errorMessage by remember { mutableStateOf<String?>(null) }

                        if (isProcessing) {
                            // Show loading screen while processing
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Processing QR code...")
                                }
                            }
                        } else if (errorMessage != null) {
                            // Show error screen
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(onClick = {
                                        errorMessage = null
                                        navController.popBackStack()
                                    }) {
                                        Text("Go Back")
                                    }
                                }
                            }
                        } else {
                            QrScanScreen(
                                onQrScanned = { qrData ->
                                    isProcessing = true
                                    errorMessage = null

                                    viewModel.processScannedQr(qrData) { card ->
                                        isProcessing = false
                                        if (card != null) {
                                            // Success - navigate back
                                            navController.popBackStack()
                                            // Show success message
                                            Toast.makeText(context, "Card imported successfully: ${card.name}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            // Error - show error message
                                            errorMessage = "Failed to process QR code. Please try again."
                                        }
                                    }
                                },
                                onBack = {
                                    if (!isProcessing) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }
                    }
                }
                BannerAdView()
            }
        }
    }

    if (showDrawer) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    DrawerHeader(useDarkTheme = useDarkTheme,setUseDarkTheme = setUseDarkTheme)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarScreen(
    navController: NavController = rememberNavController(),
    viewModel: BusinessCardViewModel = viewModel()
) {
    val sortedCards by viewModel.sortedCards.collectAsState(initial = emptyList())
    val folders by viewModel.allFolders.collectAsState(initial = emptyList())
    var selectedCardIds by remember { mutableStateOf<List<Int>>(emptyList()) }

    CardListWithSearchScreen(
        navController = navController,
        cards = sortedCards,
        selectedCardIds = selectedCardIds,
    )
}


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

private var adDisplayCounter = 0

fun maybeShowAd(activity: Activity) {
    adDisplayCounter++
    if (adDisplayCounter % 3 == 0) { // show every 3rd time
        showAd(activity)
    }
}


private var adDisplayCount = 0

fun ShowAd(activity: Activity) {
    adDisplayCount++
    if (adDisplayCount % 2 == 0) { // show every 2nd time
        showAd(activity)
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
    // Use sortedCards instead of allCards
    val sortedCards by viewModel.sortedCards.collectAsState(initial = emptyList())
    val folders by viewModel.allFolders.collectAsState(initial = emptyList())
    val currentSort by viewModel.sortOption.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context.findActivity()

    // Filter cards based on search - now using the sorted cards
    val filteredCards = if (searchQuery.isNotBlank()) {
        sortedCards.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.company.contains(searchQuery, ignoreCase = true) ||
                    it.email.contains(searchQuery, ignoreCase = true)
        }
    } else {
        sortedCards
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                title = {
                    SortFilterBar(
                        currentSort = currentSort,
                        onSortSelected = { viewModel.setSortOption(it) }
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
                        onClick = { showExportMenu = true
                            activity?.let {
                                ShowAd(it)
                            } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
                        },
                        icon = { Icon(Icons.Default.Share, contentDescription = "Export") },
                        text = { Text("Export") }
                    )
                    ExtendedFloatingActionButton(
                        onClick = { showFolderDialog = true
                            activity?.let {
                                ShowAd(it)
                            } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
                        },
                        icon = { Icon(Icons.Default.Folder, contentDescription = "Add to folder") },
                        text = { Text("Add to Folder") }
                    )
                }
            } else {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate("scan")
                        activity?.let {
                            maybeShowAd(it)
                        } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
                    },
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
                        activity?.let {
                            maybeShowAd(it)
                        } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
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

        if (filteredCards.isEmpty()) {
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
                    .background(color = MaterialTheme.colorScheme.background)
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(filteredCards) { card ->
                    val isSelected = selectedCardIds.contains(card.id)
                    BusinessCardItem(
                        card = card,
                        isSelected = isSelected,
                        isFavorite = card.isFavorite,
                        onCardClick = {
                            viewModel.updateLastViewedSafe(card.id)
                            if (selectedCardIds.isNotEmpty()) {
                                onSelectedCardIdsChanged(
                                    if (isSelected) {
                                        selectedCardIds - card.id
                                    } else {
                                        selectedCardIds + card.id
                                    }
                                )
                            } else {
                                activity?.let {
                                    ShowAd(it)
                                } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
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


// Add this function in HomeScreen.kt
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun EnhancedSmartFollowUpDialog(
    card: BusinessCard,
    onDismiss: () -> Unit,
    onSetFollowUp: (BusinessCard, SmartFollowUpPrediction) -> Unit
) {
    val viewModel: BusinessCardViewModel = viewModel()
    var smartPrediction by remember { mutableStateOf<SmartFollowUpPrediction?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showCalendarOption by remember { mutableStateOf(true) }
    val context = LocalContext.current

    LaunchedEffect(card) {
        isLoading = true
        smartPrediction = viewModel.suggestAndScheduleSmartFollowUp(card)
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.ic_reminder), // Add this icon
                    contentDescription = "AI",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("🧠 Smart Follow-Up Suggestion")
            }
        },
        text = {
            if (isLoading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Analyzing optimal follow-up time...")
                }
            } else {
                smartPrediction?.let { prediction ->
                    Column {
                        Text("I recommend following up in ${ChronoUnit.DAYS.between(ZonedDateTime.now(), prediction.optimalTime)} days.",
                            style = MaterialTheme.typography.bodyMedium)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Confidence indicator
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Confidence: ", style = MaterialTheme.typography.bodySmall)
                            LinearProgressIndicator(
                                progress = { prediction.confidence },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .padding(horizontal = 8.dp)
                            )
                            Text("${(prediction.confidence * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Priority badge
                        Box(
                            modifier = Modifier
                                .background(
                                    color = when (prediction.priorityLevel) {
                                        Priority.HIGH -> Color(0xFFFF6B6B)
                                        Priority.MEDIUM -> Color(0xFFFFA726)
                                        Priority.LOW -> Color(0xFF66BB6A)
                                    },
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Priority: ${prediction.priorityLevel}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Suggested message
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Suggested Message:", style = MaterialTheme.typography.labelMedium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(prediction.smartMessage, style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Recommended action
                        Text("Recommended: ${prediction.recommendedAction}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Calendar option
                        if (viewModel.hasCalendarPermission()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = showCalendarOption,
                                    onCheckedChange = { showCalendarOption = it }
                                )
                                Text("Add to calendar", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else {
                            Text("Calendar permission required for auto-sync",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error)
                        }
                    }
                } ?: Text("Could not generate smart suggestion. Please try manual setup.")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    smartPrediction?.let { prediction ->
                        onSetFollowUp(card, prediction)

                        // Add to calendar if selected
                        if (showCalendarOption && viewModel.hasCalendarPermission()) {
                            viewModel.addFollowUpToCalendar(
                                card.name,
                                card.company,
                                prediction.optimalTime.toInstant().toEpochMilli(),
                                "Smart follow-up: ${prediction.smartMessage}"
                            )
                        }
                    }
                },
                enabled = smartPrediction != null && !isLoading
            ) {
                Text("Schedule Smart Follow-up")
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
                color = MaterialTheme.colorScheme.surfaceVariant,
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
                colors = TextFieldDefaults.textFieldColors(
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
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
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

// Updated Compose UI for FollowUpReminderDialog and FollowUpReminderItem



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
                if (isSelected) MaterialTheme.colorScheme.background
                else MaterialTheme.colorScheme.surface
            ),

        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.background
            }
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                    // UPDATED: Placeholder canvas with company initial
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(4.dp), // Small padding for better fit in 64.dp box
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = card.company.takeIf { !it.isNullOrBlank() }?.firstOrNull()?.toString()?.uppercase() ?: "C",
                                style = MaterialTheme.typography.headlineSmall, // Smaller for 64.dp size
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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

                Spacer(modifier = Modifier.height(8.dp))
                TagChips(tags = card.tags)
            }

            Icon(
                imageVector = Icons.Default.ArrowForwardIos,
                contentDescription = "Go",
                tint = Color.Black
            )
        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
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
                        val activity = context.findActivity()
                        activity?.let {
                            maybeShowAd(it)
                        } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
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
                    Button(onClick = { navController.navigate("qrScan") }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                        Text("Scan QR Code")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { navController.navigate("nfc") }) {
                        Icon(Icons.Default.Nfc, contentDescription = "NFC")
                        Text("NFC Business Card")
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
                        capturedImage = capturedImage,
                        showImageSection = true
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
    showImageSection: Boolean,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(card.name) }
    var company by remember { mutableStateOf(card.company ?: "") }
    var position by remember { mutableStateOf(card.position ?: "") }
    var phones by remember { mutableStateOf(card.phones.toMutableList()) }
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
        if (showImageSection){
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

        Text("Phone Numbers", style = MaterialTheme.typography.titleSmall)

        phones.forEachIndexed { index, phone ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                AnimatedOutlinedTextField(
                    value = phone,
                    onValueChange = { newValue ->
                        phones = phones.toMutableList().apply {
                            this[index] = newValue
                        }
                    },
                    label = "Phone ${index + 1}",
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = {
                        phones = phones.toMutableList().apply {
                            removeAt(index)
                            if (isEmpty()) add("") // Ensure at least one field remains
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Remove phone",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        OutlinedButton(
            onClick = { phones = phones.toMutableList().apply { add("") } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add phone")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Phone Number")
        }

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
                        phones = phones.filter { it.isNotBlank() },
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


@SuppressLint("StateFlowValueCalledInComposition")
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(cardId: Int, navController: NavController, viewModel: BusinessCardViewModel) {
    var card by remember { mutableStateOf<BusinessCard?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) } // New state for edit mode
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showReminderDialog by remember { mutableStateOf(false) }

    var showShareDialog by remember { mutableStateOf(false) }

    var showShareCardDialog by remember { mutableStateOf(false) }

    val application = context.applicationContext as BusinessCardScannerApp

    LaunchedEffect(cardId) {
        card = viewModel.getCardById(cardId)
    }

    val currentCard by viewModel.getCardByIdFlow(cardId).collectAsState(initial = null)

    var showSmartFollowUpDialog by remember { mutableStateOf(false) } // New state

    var showEnhancedSmartFollowUpDialog by remember { mutableStateOf(false) }


    var showNotionActions by remember { mutableStateOf(false) }

    val hubSpotViewModel: HubSpotIntegrationViewModel = viewModel(
        factory = HubSpotIntegrationViewModelFactory(context)
    )

    val isHubSpotConnected by hubSpotViewModel.isConnected.collectAsState()
    val activity = context.findActivity()

    LaunchedEffect(currentCard) {
        if (currentCard != null) {
            card = currentCard
        }
    }

    LaunchedEffect(Unit) {
        if (!isEditing) {
            viewModel.updateLastViewed(cardId)
        }
    }

    var showImagePicker by remember { mutableStateOf(false) }

    fun updateCardImage(
        context: Context,
        newImage: Bitmap,
        currentCard: BusinessCard?,
        viewModel: BusinessCardViewModel,
        onSuccess: (BusinessCard) -> Unit
    ) {
        if (currentCard == null) return

        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Save the new image
                val imagePath = saveImageToInternalStorageEdit(context, newImage)

                // Delete the old image if it exists
                currentCard.imagePath?.let { oldPath ->
                    val oldFile = File(oldPath)
                    if (oldFile.exists()) {
                        oldFile.delete()
                    }
                }

                // Update the card with the new image path
                val updatedCard = currentCard.copy(imagePath = imagePath)

                // Save to database
                viewModel.update(updatedCard)

                // Call the success callback on main thread
                withContext(Dispatchers.Main) {
                    onSuccess(updatedCard)
                    Toast.makeText(context, "Image updated successfully", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to update image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    // Camera/Image Picker Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            updateCardImage(context, it, card, viewModel) { updatedCard ->
                card = updatedCard
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                bitmap?.let {
                    updateCardImage(context, it, card, viewModel) { updatedCard ->
                        card = updatedCard
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Card" else "Card Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        activity?.let {
                            maybeShowAd(it)
                        } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
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
                            IconButton(onClick = {
                                activity?.let { maybeShowAd(it) }
                                showNotionActions = true
                            }) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Save to Zoho")
                            }
                            // Show normal actions when not editing
                            IconButton(onClick = {
                                activity?.let {
                                    maybeShowAd(it)
                                } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
                                isEditing = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = {
                                activity?.let {
                                    maybeShowAd(it)
                                } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
//                                card?.let { shareBusinessCard(context, it) }
                                showShareDialog=true
                                coroutineScope.launch {
                                    viewModel.recordGoalCompletion(cardId)
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Share")
                            }
                            IconButton(onClick = {
                                activity?.let {
                                    maybeShowAd(it)
                                } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
                                showExportMenu = true
                                coroutineScope.launch {
                                    viewModel.recordGoalCompletion(cardId)
                                }
                            }
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export")
                            }
                            IconButton(onClick = {
                                activity?.let {
                                    maybeShowAd(it)
                                } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
                                showDeleteDialog = true

                                coroutineScope.launch {
                                    viewModel.recordGoalCompletion(cardId)
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                            // Reminder button
                            IconButton(onClick = {
                                activity?.let {
                                    maybeShowAd(it)
                                } ?: Log.e("AdHelper", "Activity is null, cannot show ad.")
                                showReminderDialog = true
                                coroutineScope.launch {
                                    viewModel.recordGoalCompletion(cardId)
                                }
                            }) {
                                if (card?.reminderTime != null && card?.reminderTime!! > System.currentTimeMillis()) {
                                    Icon(Icons.Filled.NotificationsActive, contentDescription = "Reminder Set", tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Icon(Icons.Outlined.Notifications, contentDescription = "Set Reminder")
                                }
                            }

                            // Smart Follow-up Button
                            IconButton(onClick = {
                                activity?.let { maybeShowAd(it) }
                                showEnhancedSmartFollowUpDialog = true
                                coroutineScope.launch {
                                    viewModel.recordGoalCompletion(cardId)
                                }
                            }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Smart Follow-up")
                            }

                            IconButton(onClick = {
                                showImagePicker = true
                                coroutineScope.launch {
                                    viewModel.recordGoalCompletion(cardId)
                                }
                            }) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = "Update Image")
                            }

                        } else {
                            // Show save action when editing
                            IconButton(onClick = {
                                card?.let { updatedCard ->
                                    coroutineScope.launch {
                                        viewModel.update(updatedCard)
                                    }
                                    isEditing = false
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
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(16.dp)
                        )
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { card?.phones?.firstOrNull()?.let{ call(context, it) }
                        coroutineScope.launch {
                            viewModel.recordGoalCompletion(cardId)
                        }
                    }) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { card?.phones?.firstOrNull()?.let{ message(context, it) }
                        coroutineScope.launch {
                            viewModel.recordGoalCompletion(cardId)
                        }
                    }) {
                        Icon(Icons.Default.Message, contentDescription = "SMS", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { card?.email?.let { email(context, it) }
                        coroutineScope.launch {
                            viewModel.recordGoalCompletion(cardId)
                        }
                    }) {
                        Icon(Icons.Default.Email, contentDescription = "Email", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { card?.company?.let { searchFacebook(context, it) }
                        coroutineScope.launch {
                            viewModel.recordGoalCompletion(cardId)
                        }
                    }) {
                        Icon(Icons.Default.Facebook, contentDescription = "Facebook", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { card?.website?.let { openWebsite(context, it) }
                        coroutineScope.launch {
                            viewModel.recordGoalCompletion(cardId)
                        }
                    }) {
                        Icon(Icons.Default.Public, contentDescription = "Website", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = {
                        activity?.let { ShowAd(it) }
                        showShareCardDialog = true
                        coroutineScope.launch {
                            viewModel.recordGoalCompletion(cardId)
                        }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            }
        }
    ) { padding ->
        if (showNotionActions && card != null) {
            HubSpotActionsDialog(
                businessCard = card!!,
                onDismiss = { showNotionActions = false },
                onSaveToCRM = { businessCard ->
                    if (isHubSpotConnected) {
                        hubSpotViewModel.saveToHubSpot(businessCard)
                        showNotionActions = false
                    } else {
                        // Show message that HubSpot is not configured
                        Toast.makeText(context, "Please configure HubSpot first", Toast.LENGTH_LONG).show()
                        showNotionActions = false
                    }
                },
                isHubSpotConnected = isHubSpotConnected
            )
        }

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

        if (showImagePicker) {
            AlertDialog(
                onDismissRequest = { showImagePicker = false },
                title = { Text("Update Card Image") },
                text = { Text("Choose how to update the card image") },
                confirmButton = {
                    Button(onClick = {
                        cameraLauncher.launch(null)
                        showImagePicker = false
                    }) {
                        Text("Take Photo")
                    }
                },
                dismissButton = {
                    Button(onClick = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        showImagePicker = false
                    }) {
                        Text("Choose from Gallery")
                    }
                }
            )
        }

        LaunchedEffect(Unit) {
            if (!isEditing) {
                viewModel.updateLastViewed(cardId)
            }
        }

        if (showExportMenu) {
            AlertDialog(
                onDismissRequest = { showExportMenu = false },
                title = { Text("Export Business Card") },
                text = { Text("Choose a format to export the business card.") },
                confirmButton = {
                    TextButton(onClick = {
                        activity?.let {
                            AdHelper.showAdIfAvailable(it)
                        } ?: Log.e("AdHelper", "Context is not an Activity!")
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

        if (showReminderDialog && card != null) {
            ReminderDialog(
                card = card!!,
                onDismiss = { showReminderDialog = false },
                onSetReminder = { cardId, time, message ->
                    viewModel.setReminder(cardId, time, message)
                    scheduleReminder(context, cardId, time, message, card?.name ?: "Business Card")
                    showReminderDialog = false
                },
                onClearReminder = { cardId ->
                    viewModel.clearReminder(cardId)
                    cancelReminder(context, cardId)
                    showReminderDialog = false
                }
            )
        }

        if (showEnhancedSmartFollowUpDialog && card != null) {
            EnhancedSmartFollowUpDialog(
                card = card!!,
                onDismiss = { showEnhancedSmartFollowUpDialog = false },
                onSetFollowUp = { cardToFollowUp, prediction ->
                    coroutineScope.launch {
                        // The follow-up is already scheduled in the prediction function
                        showEnhancedSmartFollowUpDialog = false
                        Toast.makeText(context, "Smart follow-up scheduled!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        if (showShareDialog == true) {
            ShareBusinessCardDialog(
                card = card!!,
                onDismiss = {
                    showShareDialog = false
                }
            )
        }

        if (showSmartFollowUpDialog && card != null) {
            SmartFollowUpDialog(
                card = card!!,
                onDismiss = { showSmartFollowUpDialog = false },
                onSetFollowUp = { cardToFollowUp, reason ->
                    coroutineScope.launch {
                        viewModel.suggestAndScheduleFollowUp(cardToFollowUp, reason)
                        showSmartFollowUpDialog = false
                    }
                }
            )
        }

        if (showShareCardDialog && card != null) {
            ShareBusinessCardDialog2(
                card = card!!,
                viewModel = viewModel,
                onDismiss = { showShareCardDialog = false }
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
                    var phones by remember { mutableStateOf(currentCard.phones.toMutableList()) }
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
                        // UPDATED: Image preview with placeholder fallback in editing mode
                        currentCard.imagePath?.let { imagePath ->
                            val bitmap = loadImageFromStorage(imagePath)
                            if (bitmap != null) {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                ) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Business card image",
                                        contentScale = ContentScale.FillWidth,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                // Placeholder canvas with company initial
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentCard.company.takeIf { !it.isNullOrBlank() }
                                                ?.firstOrNull()?.toString()?.uppercase() ?: "C",
                                            style = MaterialTheme.typography.headlineLarge,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } ?: run {
                            // No imagePath at all - show placeholder
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currentCard.company.takeIf { !it.isNullOrBlank() }
                                            ?.firstOrNull()?.toString()?.uppercase() ?: "C",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
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
                            label = "Position",
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedOutlinedTextField(
                            value = company,
                            onValueChange = { company = it },
                            label = "Company",
                            modifier = Modifier.fillMaxWidth()
                        )

                        // In the editing section of DetailsScreen:
                        Text("Phone Numbers", style = MaterialTheme.typography.titleSmall)

                        phones.forEachIndexed { index, phone ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                AnimatedOutlinedTextField(
                                    value = phone,
                                    onValueChange = { newValue ->
                                        phones = phones.toMutableList().apply {
                                            this[index] = newValue
                                        }
                                    },
                                    label = "Phone ${index + 1}",
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        phones = phones.toMutableList().apply {
                                            removeAt(index)
                                            if (isEmpty()) add("")
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Remove phone",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { phones = phones.toMutableList().apply { add("") } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add phone")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Phone Number")
                        }


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
                        LaunchedEffect(
                            name,
                            company,
                            position,
                            phones,
                            email,
                            address,
                            website,
                            notes
                        ) {
                            card = currentCard.copy(
                                name = name,
                                company = company,
                                position = position,
                                phones = phones.filter { it.isNotBlank() },
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
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // FIXED: Image preview with placeholder fallback in read-only mode
                    card?.imagePath?.let { imagePath ->
                        val bitmap = loadImageFromStorage(imagePath)
                        if (bitmap != null) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Business card image",
                                    contentScale = ContentScale.FillWidth,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        } else {
                            // Placeholder canvas with company initial
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = card!!.company.takeIf { !it.isNullOrBlank() }
                                            ?.firstOrNull()?.toString()?.uppercase() ?: "C",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } ?: run {
                        // No imagePath at all - show placeholder
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = card!!.company.takeIf { !it.isNullOrBlank() }
                                        ?.firstOrNull()?.toString()?.uppercase() ?: "C",
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    ContactActionRow(card!!.company.toString())

                    Text(
                        text = card!!.name ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = card!!.position ?: "",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = card!!.company ?: "",
                        style = MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider()

                    Text(
                        text = "Contact Information",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    card!!.phones.forEach {
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }

                    card!!.email?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }

                    card!!.website?.let {
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }

                    card!!.address?.let {
                        HorizontalDivider()
                        Text(
                            text = "Address",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(text = it, style = MaterialTheme.typography.bodyLarge)
                    }

                    card!!.notes?.let {
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

                    // Add this in the read-only details section (after notes section)

                    card!!.tags.takeIf { it.isNotEmpty() }?.let { tags ->
                        HorizontalDivider()
                        Text(
                            text = "Suggested Tags",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TagChips(tags = tags, modifier = Modifier.padding(vertical = 8.dp))
                    }

                    Text(
                        text = "Follow-Up Reminders",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    card!!.reminderTime?.let { reminderTime ->
                        if (reminderTime > System.currentTimeMillis()) {
                            val formattedTime =
                                SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(
                                    Date(reminderTime)
                                )
                            Text(
                                text = "Reminder set for: $formattedTime",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            card!!.reminderMessage.takeIf { it!!.isNotBlank() }?.let { message ->
                                Text(
                                    text = "Message: $message",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            TextButton(onClick = { showReminderDialog = true }) {
                                Text("Edit/Clear Reminder")
                            }
                        } else {
                            Text(
                                text = "No active reminder set.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } ?: run {
                        Text(
                            text = "No reminder set.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


@Composable
fun ReminderDialog(
    card: BusinessCard,
    onDismiss: () -> Unit,
    onSetReminder: (cardId: Int, time: Long, message: String) -> Unit,
    onClearReminder: (cardId: Int) -> Unit
) {
    val context = LocalContext.current
    var selectedDateTime by remember { mutableStateOf(card.reminderTime ?: System.currentTimeMillis()) }
    var reminderMessage by remember { mutableStateOf(card.reminderMessage ?: "") }

    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDateTime }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            selectedDateTime = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
            calendar.set(Calendar.MINUTE, minute)
            selectedDateTime = calendar.timeInMillis
        },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        false // 24 hour format
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Reminder for ${card.name}") },
        text = {
            Column {
                Text("Selected Date & Time: ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(selectedDateTime))}")
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(onClick = { datePickerDialog.show() }) {
                        Text("Select Date")
                    }
                    Button(onClick = { timePickerDialog.show() }) {
                        Text("Select Time")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = reminderMessage,
                    onValueChange = { reminderMessage = it },
                    label = { Text("Reminder Message (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedDateTime <= System.currentTimeMillis()) {
                        Toast.makeText(context, "Please select a future date and time.", Toast.LENGTH_SHORT).show()
                    } else {
                        onSetReminder(card.id, selectedDateTime, reminderMessage)
                    }
                }
            ) {
                Text("Set Reminder")
            }
        },
        dismissButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (card.reminderTime != null) {
                    TextButton(onClick = { onClearReminder(card.id) }) {
                        Text("Clear Reminder", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}

fun scheduleReminder(context: Context, cardId: Int, time: Long, message: String, cardName: String) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
        putExtra(REMINDER_CARD_ID, cardId)
        putExtra(REMINDER_MESSAGE, message)
        putExtra(REMINDER_NOTIFICATION_ID, cardId + REMINDER_REQUEST_CODE_BASE) // Unique ID for notification
        putExtra("cardName", cardName) // Pass card name for notification title
    }

    val pendingIntent = PendingIntent.getBroadcast(
        context,
        cardId + REMINDER_REQUEST_CODE_BASE, // Use cardId for unique request code
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    // Check for exact alarm permission on Android 12+
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            Toast.makeText(context, "Reminder set for $cardName", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Please grant 'Alarms & reminders' permission for exact reminders.", Toast.LENGTH_LONG).show()
            // Optionally, direct user to settings
            // val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            // context.startActivity(intent)
        }
    } else {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        Toast.makeText(context, "Reminder set for $cardName", Toast.LENGTH_SHORT).show()
    }
}

fun cancelReminder(context: Context, cardId: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderBroadcastReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        cardId + REMINDER_REQUEST_CODE_BASE,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
    Toast.makeText(context, "Reminder cancelled.", Toast.LENGTH_SHORT).show()
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


fun shareBusinessCard(context: Context, card: BusinessCard) {
    val shareText = buildString {
        append("Business Card Details\n\n")
        append("Name: ${card.name}\n")
        card.position?.let { append("Position: $it\n") }
        card.company?.let { append("Company: $it\n") }
        card.phones?.let { append("Phone: $it\n") }
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

    context.startActivity(Intent.createChooser(shareIntent, "Share Business Card"))
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
            card.phones?.let { writer.append("Phone,$it\n") }
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

        card.phones?.let {
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

// New Composable for Smart Follow-up Dialog
@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartFollowUpDialog(
    card: BusinessCard,
    onDismiss: () -> Unit,
    onSetFollowUp: (BusinessCard, String) -> Unit
) {
    val viewModel: BusinessCardViewModel = viewModel()
    var suggestedTime by remember { mutableStateOf<ZonedDateTime?>(null) }
    var isLoadingSuggestion by remember { mutableStateOf(true) }
    var selectedReason by remember { mutableStateOf("first-followup") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(card) {
        isLoadingSuggestion = true
        errorMessage = null

        try {
            // FIXED: Use the repository through the ViewModel instead of directly
            val banditArms = withContext(Dispatchers.IO) {
                viewModel.getRepository().getAllBanditArms()
            }
            val userTz = ZoneId.systemDefault()
            suggestedTime = FollowUpSuggester.suggestFollowup(card, banditArms, userTz)
        } catch (e: Exception) {
            errorMessage = "Failed to load suggestions: ${e.message}"
            Log.e("SmartFollowUp", "Error getting bandit arms", e)
        } finally {
            isLoadingSuggestion = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Smart Follow-up for ${card.name}") },
        text = {
            Column {
                if (isLoadingSuggestion) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Calculating best follow-up time...")
                    }
                } else if (errorMessage != null) {
                    Text(errorMessage ?: "Unknown error", color = MaterialTheme.colorScheme.error)
                } else {
                    suggestedTime?.let { time ->
                        val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")
                        Text("Suggested Follow-up: ${time.format(formatter)}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Reason:")
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            FilterChip(
                                selected = selectedReason == "first-followup",
                                onClick = { selectedReason = "first-followup" },
                                label = { Text("First Follow-up") }
                            )
                            FilterChip(
                                selected = selectedReason == "no-reply",
                                onClick = { selectedReason = "no-reply" },
                                label = { Text("No Reply") }
                            )
                            FilterChip(
                                selected = selectedReason == "reopen",
                                onClick = { selectedReason = "reopen" },
                                label = { Text("Re-engage") }
                            )
                        }
                    } ?: Text("Could not suggest a follow-up time.")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    suggestedTime?.let {
                        onSetFollowUp(card, selectedReason)
                    }
                },
                enabled = suggestedTime != null && !isLoadingSuggestion && errorMessage == null
            ) {
                Text("Set Smart Follow-up")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


// Add this to your HomeScreen.kt file
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChips(tags: List<String>, modifier: Modifier = Modifier) {
    if (tags.isNotEmpty()) {
        FlowRow(
            horizontalArrangement = Arrangement.Start,
            verticalArrangement = Arrangement.Center,
            modifier = modifier
        ) {
            tags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .background(
                            color = when (tag) {
                                "Priority" -> Color(0xFFFF6B6B)
                                "Investor" -> Color(0xFF4ECDC4)
                                "Recruiter" -> Color(0xFF45B7D1)
                                "Supplier" -> Color(0xFF96CEB4)
                                "Potential Client" -> Color(0xFFFFEAA7)
                                "Tech" -> Color(0xFFDDA0DD)
                                else -> Color(0xFFCCCCCC)
                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tag,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// Update the saveImageToInternalStorage function to handle quality better
fun saveImageToInternalStorageEdit(context: Context, bitmap: Bitmap): String {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File.createTempFile(
        "BC_${timeStamp}_",
        ".jpg",
        storageDir
    )

    // Compress with better quality settings
    file.outputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
    }

    return file.absolutePath
}