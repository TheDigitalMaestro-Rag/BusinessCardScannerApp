package com.project.businesscardscannerapp

import androidx.activity.compose.setContent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.project.businesscardscannerapp.AppUI.BusinessCardApp
import com.project.businesscardscannerapp.AppUI.DetailsScreen
import com.project.businesscardscannerapp.Insights.InsightsScreen
import com.project.businesscardscannerapp.Registration.AuthScreen
import com.project.businesscardscannerapp.ViewModel.BusinessCardViewModel
import com.project.businesscardscannerapp.ViewModel.NotificationConstants.REMINDER_CARD_ID
import com.project.businesscardscannerapp.ui.theme.BusinessCardScannerAppTheme
import com.project.businesscardscannerapp.ui.theme.ThemePreference

class MainActivity : ComponentActivity() {
    lateinit var adManager: AdManager
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle navigation from notification
        handleNotificationNavigation(intent)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        MobileAds.initialize(this)
        adManager = AdManager(this)

        val reminderCardId = intent.getIntExtra(REMINDER_CARD_ID, -1)
        val navigateToInsights = intent.getBooleanExtra("navigateToInsights", false)

        setContent {
            AppNavigation(
                reminderCardId = reminderCardId,
                navigateToInsights = navigateToInsights,
                auth = auth,
                database = database
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Handle navigation when app is already running
        handleNotificationNavigation(intent)
    }

    private fun handleNotificationNavigation(intent: Intent?) {
        intent?.let {
            if (it.getBooleanExtra("navigateToDetails", false)) {
                val cardId = it.getIntExtra(REMINDER_CARD_ID, -1)
                if (cardId != -1) {
                    // We'll handle this in the AppNavigation composable
                    // The intent extras will be processed there
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppNavigation(
    reminderCardId: Int,
    navigateToInsights: Boolean,
    auth: FirebaseAuth,
    database: FirebaseDatabase
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    val themePreference = remember { ThemePreference(context) }
    val (useDarkTheme, setUseDarkThemeState) = remember {
        mutableStateOf(themePreference.loadTheme())
    }
    val setUseDarkTheme: (Boolean) -> Unit = { isDark ->
        setUseDarkThemeState(isDark)
        themePreference.saveTheme(isDark)
    }

    val currentUser by remember(auth) { mutableStateOf(auth.currentUser) }
    val startDestination = if (currentUser != null) "main" else "auth"

    // Handle navigation from notifications
    LaunchedEffect(reminderCardId, navigateToInsights) {
        if (reminderCardId != -1) {
            navController.navigate("details/$reminderCardId") {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
            }
        } else if (navigateToInsights) {
            navController.navigate("Insights") {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // Also handle when the app is already running and new intent comes
    LaunchedEffect(Unit) {
        // This will handle cases when the app is already in foreground
        // and a new notification intent arrives
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("auth") {
            AuthScreen(
                auth = auth,
                database = database,
                onAuthComplete = {
                    navController.navigate("main") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainAppScreen(navController, useDarkTheme, setUseDarkTheme)
        }

        composable("details/{cardId}") { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")?.toIntOrNull()
            cardId?.let {
                val viewModel: BusinessCardViewModel = viewModel()
                DetailsScreen(it, navController, viewModel)
            }
        }

        composable("Insights") {
            InsightsScreen()
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainAppScreen(
    navController: NavController,
    useDarkTheme: Boolean,
    setUseDarkTheme: (Boolean) -> Unit
) {
    BusinessCardScannerAppTheme(darkTheme = useDarkTheme) {
        val viewModel: BusinessCardViewModel = viewModel()
        BusinessCardApp(
            navController = navController,
            viewModel = viewModel,
            useDarkTheme = useDarkTheme,
            setUseDarkTheme = setUseDarkTheme
        )
    }
}
