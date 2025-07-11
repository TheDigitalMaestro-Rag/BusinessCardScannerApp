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
import com.google.firebase.firestore.auth.User


//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            BusinessCardScannerTheme {
//                val viewModel: BusinessCardViewModel = viewModel()
//                BusinessCardApp(viewModel)
//            }
//        }
//    }
//}

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.raghu.businesscardscanner2.AdManager
import com.raghu.businesscardscanner2.AppUI.BusinessCardApp
import com.raghu.businesscardscanner2.BusinessCardScannerApp
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpReminderScreen
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpViewModel
import com.raghu.businesscardscanner2.Login.RegistrationScreen
import com.raghu.businesscardscanner2.ViewModel.BusinessCardViewModel
import com.raghu.businesscardscanner2.ui.theme.BusinessCardScannerAppTheme
import com.raghu.businesscardscanner2.ui.theme.BusinessCardScannerTheme
import com.raghu.businesscardscanner2.ui.theme.SupabaseCourseTheme

class MainActivity : ComponentActivity() {
    lateinit var adManager: AdManager
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this)
        adManager = AdManager(this)

        val reminderIdFromIntent = intent?.getIntExtra("reminderId", -1) ?: -1
        val destinationFromIntent = intent?.getStringExtra("destination")

        setContent {
            SupabaseCourseTheme {
                AppNavigation(
                    reminderIdFromIntent = reminderIdFromIntent,
                    destinationFromIntent = destinationFromIntent
                )
            }
        }
    }
}

@Composable
fun AppNavigation(reminderIdFromIntent: Int, destinationFromIntent: String?) {
    val navController = rememberNavController()
    val auth = FirebaseAuth.getInstance()

    val startDestination = if (auth.currentUser != null) "main" else "registration"

    LaunchedEffect(Unit) {
        if (destinationFromIntent == "reminders" && reminderIdFromIntent != -1) {
            navController.navigate("reminders/$reminderIdFromIntent")
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

        composable("reminders/{reminderId}") { backStackEntry ->
            val reminderId = backStackEntry.arguments?.getString("reminderId")?.toIntOrNull()
            val context = LocalContext.current.applicationContext as BusinessCardScannerApp
            val followUpViewModel: FollowUpViewModel = viewModel(
                factory = context.followUpViewModelFactory
            )
            reminderId?.let { id ->
                FollowUpReminderScreen(viewModel = followUpViewModel, reminderId = id)
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