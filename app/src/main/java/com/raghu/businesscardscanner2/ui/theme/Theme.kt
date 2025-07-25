//package com.raghu.businesscardscanner2.ui.theme
//
//import android.os.Build
//import androidx.compose.foundation.isSystemInDarkTheme
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.darkColorScheme
//import androidx.compose.material3.dynamicDarkColorScheme
//import androidx.compose.material3.dynamicLightColorScheme
//import androidx.compose.material3.lightColorScheme
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//
//import androidx.compose.foundation.isSystemInDarkTheme
//
////private val DarkColorScheme = darkColorScheme(
////    primary = Color(0xFFBB86FC),
////    secondary = Color(0xFF03DAC6),
////    tertiary = Color(0xFF3700B3),
////    background = Color(0xFF121212),
////    surface = Color(0xFF1E1E1E),
////    onPrimary = Color.Black,
////    onSecondary = Color.Black,
////    onBackground = Color.White,
////    onSurface = Color.White,
////)
////
////private val LightColorScheme = lightColorScheme(
////    primary = Color(0xFF6200EE),
////    secondary = Color(0xFF03DAC6),
////    tertiary = Color(0xFF3700B3),
////    background = Color.White,
////    surface = Color(0xFFFFFFFF),
////    onPrimary = Color.White,
////    onSecondary = Color.Black,
////    onBackground = Color.Black,
////    onSurface = Color.Black,
////)
//
//private val DarkColorScheme = darkColorScheme(
//    primary = Purple80,
//    secondary = PurpleGrey80,
//    tertiary = Pink80
//)
//private val LightColorScheme = lightColorScheme(
//    primary = Purple40,
//    secondary = PurpleGrey40,
//    tertiary = Pink40
//    /* Other default colors to override
//    background = Color(0xFFFFFBFE),
//    surface = Color(0xFFFFFBFE),
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),
//    */
//)
//
//val LightCardColor = Color(0xFFF6F6F6)
//val DarkCardColor = Color(0xFF2C2C2C)
//
//
//
//@Composable
//fun BusinessCardScannerTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    content: @Composable () -> Unit
//) {
//    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
//
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = Typography,
//        content = content
//    )
//}
//
//
//
//
//@Composable
//fun BusinessCardScannerTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    // Dynamic color is available on Android 12+
//    dynamicColor: Boolean = true,
//    content: @Composable () -> Unit
//) {
//    val colorScheme = when {
//      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//        val context = LocalContext.current
//        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//      }
//      darkTheme -> DarkColorScheme
//      else -> LightColorScheme
//    }
//
//    MaterialTheme(
//      colorScheme = colorScheme,
//      typography = Typography,
//      content = content
//    )
//}
//
//
//val purple = Color(0xFFAF61E2)
//val darkPurple = Color(0xFF2D115A)
//val black = Color(0xFF010101)
//val darkGray = Color(0xFF1A1A1A)
//
//@Composable
//fun SupabaseCourseTheme(
//    content: @Composable () -> Unit
//) {
//
//    MaterialTheme(
//        colorScheme = DarkColorScheme,
//        typography = Typography,
//        content = content
//    )
//}
//
//
//@Composable
//fun BusinessCardScannerAppTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    // Dynamic color is available on Android 12+
//    dynamicColor: Boolean = true,
//    content: @Composable () -> Unit
//) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
//
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = Typography,
//        content = content
//    )
//}



package com.raghu.businesscardscanner2.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

val LightCardColor = Color(0xFFF6F6F6)
val DarkCardColor = Color(0xFF2C2C2C)

@Composable
fun BusinessCardScannerAppTheme(
    darkTheme: Boolean = false, // Default to light theme
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}


@Composable
fun SupabaseCourseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
