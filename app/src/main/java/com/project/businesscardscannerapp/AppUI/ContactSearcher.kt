package com.project.businesscardscannerapp.AppUI

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.project.businesscardscannerapp.R

@Composable
fun ContactActionRow(contactName: String) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {


        IconButton(onClick = {
            val query = Uri.encode(contactName)
            val linkedinSearchUrl = "https://www.linkedin.com/search/results/all/?keywords=$query"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(linkedinSearchUrl))
            context.startActivity(intent)
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google_logo), // Replace with your LinkedIn icon
                contentDescription = "LinkedIn Search",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        IconButton(onClick = {
            val query = Uri.encode(contactName)
            val googleSearchUrl = "https://www.google.com/search?q=$query"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(googleSearchUrl))
            context.startActivity(intent)
        }) {
            Icon(
                painter = painterResource(id = R.drawable.ic_google_logo), // Replace with your Google icon
                contentDescription = "Google Search",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}
