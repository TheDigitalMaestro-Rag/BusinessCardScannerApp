package com.raghu.businesscardscanner2.AppUI

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.businesscardscannerapp.R
import com.raghu.businesscardscanner2.NavItem


@Composable
fun DrawerHeader() {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Image(
            painter = painterResource(id = R.drawable.profile), // Replace with your drawable
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        )
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "UserName",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Email Id",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun DrawerBody(
    navItems: List<NavItem>,
    currentRoute: String?,
    onItemClick: (String) -> Unit
) {
    Column {
        navItems.forEach { item ->
            NavigationDrawerItem(
                label = { Text(item.label) },
                icon = { Icon(item.icon, contentDescription = null) },
                selected = item.label == currentRoute,
                onClick = { onItemClick(item.label) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}

data class UserInfo(
    val name: String?,
    val email: String?
)



