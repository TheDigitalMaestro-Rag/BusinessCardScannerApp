package com.project.businesscardscannerapp


import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.core.graphics.Insets
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Shape


data class NavItem(
    val label: String,
    val icon: ImageVector
)

@Composable
fun AnimatedBottomBar(
    items: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 0.dp), // optional: keep if you want spacing from bottom
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier
                .fillMaxWidth() // full width without padding
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp), // spacing between icons
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    BottomNav(
                        item = item,
                        isSelected = index == selectedIndex,
                        onClick = { onItemSelected(index) }
                    )
                }
            }
        }
    }
}


@Composable
fun BottomNav(item: NavItem, isSelected: Boolean, onClick: () -> Unit) {
    val transition = updateTransition(targetState = isSelected, label = "IconFlip")

    val rotateAnimation by transition.animateFloat(
        label = "RotationY",
        transitionSpec = { tween(durationMillis = 400, easing = FastOutSlowInEasing) }
    ) { selected -> if (selected) 0f else 180f }

    val contentColor by transition.animateColor(label = "Color") {
        if (it) Color.Black else Color.Gray
    }

    val backgroundColor by transition.animateColor(label = "Background") {
        if (it) Color(0xFFE0E0E0) else Color.Transparent
    }

    Box(
        modifier = Modifier
            .height(56.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable {
                onClick()
            }
            .background(backgroundColor)
            .padding(horizontal = if (isSelected) 16.dp else 0.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = contentColor,
                modifier = Modifier.graphicsLayer {
                    rotationY = rotateAnimation
                    cameraDistance = 12f * density
                }
            )

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn(tween(300)) + expandHorizontally(tween(300)),
                exit = fadeOut(tween(200)) + shrinkHorizontally(tween(200))
            ) {
                Text(
                    text = item.label.replaceFirstChar { it.uppercaseChar() },
                    color = contentColor,
                    fontSize = 14.sp
                )
            }
        }
    }
}



