// FileName: MultipleFiles/MainActivity.kt
package com.raghu.businesscardscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.raghu.businesscardscanner2.AdManager
import com.raghu.businesscardscanner2.AppUI.BusinessCardApp
import com.raghu.businesscardscanner2.AppUI.DetailsScreen
import com.raghu.businesscardscanner2.BusinessCardScannerApp
import com.raghu.businesscardscanner2.Login.RegistrationScreen
import com.raghu.businesscardscanner2.ViewModel.BusinessCardViewModel
import com.raghu.businesscardscanner2.ui.theme.BusinessCardScannerAppTheme
import com.raghu.businesscardscanner2.ui.theme.BusinessCardScannerTheme
import com.raghu.businesscardscanner2.ui.theme.SupabaseCourseTheme
import com.raghu.businesscardscanner2.REMINDER_CARD_ID // Import the constant

class MainActivity : ComponentActivity() {
    lateinit var adManager: AdManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // In your Application class or main activity
        MobileAds.initialize(this)
        adManager = AdManager(this)

        // Handle intent from notification
        val reminderCardId = intent.getIntExtra(REMINDER_CARD_ID, -1)

        setContent {
            SupabaseCourseTheme {
                AppNavigation(reminderCardId = reminderCardId)
            }
        }
    }
}

@Composable
fun AppNavigation(reminderCardId: Int) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    val startDestination = if (auth.currentUser != null) "main" else "registration"

    LaunchedEffect(reminderCardId) {
        if (reminderCardId != -1) {
            // Navigate to the details screen of the card that triggered the reminder
            navController.navigate("details/$reminderCardId") {
                // Clear back stack to prevent navigating back to an empty state
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true // Prevent multiple copies of the same destination
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("registration") {
            RegistrationScreen(
                onRegistrationComplete = {
                    navController.navigate("main") {
                        popUpTo("registration") { inclusive = true }
                    }
                }
            )
        }

        composable("main") {
            MainAppScreen(navController)
        }

        // Add the details route here if it's not already defined in BusinessCardApp's NavHost
        // (It is, but this ensures it's accessible from the root NavHost if needed)
        composable("details/{cardId}") { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")?.toIntOrNull()
            if (cardId != null) {
                val viewModel: BusinessCardViewModel = viewModel()
                DetailsScreen(cardId = cardId, navController = navController, viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(navController: NavController) {
    BusinessCardScannerAppTheme {
        val viewModel: BusinessCardViewModel = viewModel()
        BusinessCardApp(navController = navController, viewModel = viewModel)
    }
}
