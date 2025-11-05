package com.project.businesscardscannerapp.AppUI

import android.content.res.Resources.Theme
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
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.isSystemInDarkTheme
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.project.businesscardscannerapp.NavItem
import com.project.businesscardscannerapp.R

@Composable
fun DrawerHeader(useDarkTheme:Boolean, setUseDarkTheme: (Boolean) -> Unit) { // Add parameter

    var username by remember { mutableStateOf("Guest") }
    var email by remember { mutableStateOf("No Email") }
    var isLoading by remember { mutableStateOf(true) }

    // Use the current user's UID as key for LaunchedEffect to restart when user changes
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotEmpty()) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users").child(currentUserId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    username = snapshot.child("name").getValue(String::class.java) ?: "Guest"
                    email = snapshot.child("email").getValue(String::class.java) ?: snapshot.child("emailId").getValue(String::class.java) ?: "No Email"
                    isLoading = false
                }

                override fun onCancelled(error: DatabaseError) {
                    println("Database error: ${error.message}")
                    isLoading = false
                }
            })
        } else {
            isLoading = false
        }
    }

    val isDarkTheme = isSystemInDarkTheme() // Get current system theme status

    // State to hold the toggle switch's checked status
    var checked by remember { mutableStateOf(useDarkTheme) }

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
            text =  username,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = email,
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(modifier = Modifier.height(12.dp)) // Add some space

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Dark Mode", style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    setUseDarkTheme(it) // Update the theme state
                }
            )
        }
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

