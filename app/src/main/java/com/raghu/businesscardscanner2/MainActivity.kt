//package com.raghu.businesscardscanner
//
//import android.os.Bundle
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.platform.LocalContext
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.google.android.gms.ads.MobileAds
//import com.google.firebase.auth.FirebaseAuth
//import com.raghu.businesscardscanner2.AdManager
//import com.raghu.businesscardscanner2.AppUI.BusinessCardApp
//import com.raghu.businesscardscanner2.AppUI.DetailsScreen
//import com.raghu.businesscardscanner2.BusinessCardScannerApp
//import com.raghu.businesscardscanner2.Login.RegistrationScreen
//import com.raghu.businesscardscanner2.ViewModel.BusinessCardViewModel
//import com.raghu.businesscardscanner2.ui.theme.BusinessCardScannerAppTheme
//import com.raghu.businesscardscanner2.ui.theme.SupabaseCourseTheme
//import com.raghu.businesscardscanner2.REMINDER_CARD_ID // Import the constant
//import androidx.compose.foundation.isSystemInDarkTheme // Import this for initial theme
//
//class MainActivity : ComponentActivity() {
//    lateinit var adManager: AdManager
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        // In your Application class or main activity
//        MobileAds.initialize(this)
//        adManager = AdManager(this)
//
//        // Handle intent from notification
//        val reminderCardId = intent.getIntExtra(REMINDER_CARD_ID, -1)
//
//        setContent {
//            AppNavigation(reminderCardId = reminderCardId)
//        }
//    }
//}
//
//@Composable
//fun AppNavigation(reminderCardId: Int) {
//    val navController = rememberNavController()
//    val auth = FirebaseAuth.getInstance()
//
//    // Fix: Move isSystemInDarkTheme() outside remember block
//    val systemDarkTheme = isSystemInDarkTheme()
//    val (useDarkTheme, setUseDarkTheme) = remember { mutableStateOf(systemDarkTheme) }
//
//    val startDestination = if (auth.currentUser != null) "main" else "registration"
//
//    LaunchedEffect(reminderCardId) {
//        if (reminderCardId != -1) {
//            navController.navigate("details/$reminderCardId") {
//                popUpTo(navController.graph.startDestinationId) { inclusive = false }
//                launchSingleTop = true
//            }
//        }
//    }
//
//    // Rest of the NavHost setup remains same...
//    NavHost(
//        navController = navController,
//        startDestination = startDestination
//    ) {
//        composable("registration") {
//            RegistrationScreen(
//                onRegistrationComplete = {
//                    navController.navigate("main") {
//                        popUpTo("registration") { inclusive = true }
//                    }
//                }
//            )
//        }
//
//        composable("main") {
//            MainAppScreen(navController, useDarkTheme, setUseDarkTheme)
//        }
//
//        composable("details/{cardId}") { backStackEntry ->
//            val cardId = backStackEntry.arguments?.getString("cardId")?.toIntOrNull()
//            cardId?.let {
//                val viewModel: BusinessCardViewModel = viewModel()
//                DetailsScreen(it, navController, viewModel)
//            }
//        }
//    }
//}
//
//
//@Composable
//fun MainAppScreen(
//    navController: NavController,
//    useDarkTheme: Boolean, // Receive theme state
//    setUseDarkTheme: (Boolean) -> Unit // Receive theme setter
//) {
//    // Pass the useDarkTheme state to your theme composable
//    BusinessCardScannerAppTheme(darkTheme = useDarkTheme) {
//        val viewModel: BusinessCardViewModel = viewModel()
//        // Pass the theme setter down to BusinessCardApp so it can be used in the Drawer
//        BusinessCardApp(navController = navController, viewModel = viewModel, setUseDarkTheme = setUseDarkTheme)
//    }
//}
//
package com.raghu.businesscardscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import com.raghu.businesscardscanner2.REMINDER_CARD_ID
import com.raghu.businesscardscanner2.ui.theme.ThemePreference

class MainActivity : ComponentActivity() {
    lateinit var adManager: AdManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        adManager = AdManager(this)

        val reminderCardId = intent.getIntExtra(REMINDER_CARD_ID, -1)

        setContent {
            AppNavigation(reminderCardId = reminderCardId)
        }
    }
}

@Composable
fun AppNavigation(reminderCardId: Int) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()
    val context = LocalContext.current // Get context for ThemePreference

    // Initialize ThemePreference
    val themePreference = remember { ThemePreference(context) }

    // Load initial theme state from preferences
    val (useDarkTheme, setUseDarkThemeState) = remember {
        mutableStateOf(themePreference.loadTheme())
    }

    // Wrapper function to save preference when theme changes
    val setUseDarkTheme: (Boolean) -> Unit = { isDark ->
        setUseDarkThemeState(isDark)
        themePreference.saveTheme(isDark)
    }

    val startDestination = if (auth.currentUser != null) "main" else "registration"

    LaunchedEffect(reminderCardId) {
        if (reminderCardId != -1) {
            navController.navigate("details/$reminderCardId") {
                popUpTo(navController.graph.startDestinationId) { inclusive = false }
                launchSingleTop = true
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
            MainAppScreen(navController, useDarkTheme, setUseDarkTheme)
        }

        composable("details/{cardId}") { backStackEntry ->
            val cardId = backStackEntry.arguments?.getString("cardId")?.toIntOrNull()
            cardId?.let {
                val viewModel: BusinessCardViewModel = viewModel()
                DetailsScreen(it, navController, viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(
    navController: NavController,
    useDarkTheme: Boolean,
    setUseDarkTheme: (Boolean) -> Unit
) {
    BusinessCardScannerAppTheme(darkTheme = useDarkTheme) {
        val viewModel: BusinessCardViewModel = viewModel()
        BusinessCardApp(navController = navController, viewModel = viewModel, setUseDarkTheme = setUseDarkTheme)
    }
}
