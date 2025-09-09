package com.project.businesscardscannerapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.project.businesscardscannerapp.ui.theme.BusinessCardScannerAppTheme
import com.raghu.businesscardscanner2.AppUI.BusinessCardApp
import com.raghu.businesscardscanner2.ViewModel.BusinessCardViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel:BusinessCardViewModel = viewModel()
            val navController = rememberNavController()
            BusinessCardScannerAppTheme {
                BusinessCardApp(navController, viewModel)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    BusinessCardScannerAppTheme {

    }
}