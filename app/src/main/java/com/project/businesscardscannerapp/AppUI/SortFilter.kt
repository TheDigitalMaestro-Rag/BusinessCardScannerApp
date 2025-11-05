package com.project.businesscardscannerapp.AppUI

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.text.style.TextOverflow
import com.project.businesscardscannerapp.ViewModel.BusinessCardViewModel


@Composable
fun SortFilterBar(
    currentSort: BusinessCardViewModel.SortOption,
    onSortSelected: (BusinessCardViewModel.SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sort by:",
                style = MaterialTheme.typography.labelLarge
            )

            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.width(200.dp)
            ) {
                Text(
                    text = when (currentSort) {
                        BusinessCardViewModel.SortOption.RECENTLY_ADDED -> "Recently Added"
                        BusinessCardViewModel.SortOption.RECENTLY_VIEWED -> "Recently Viewed"
                        BusinessCardViewModel.SortOption.NAME -> "Name (A-Z)"
                        BusinessCardViewModel.SortOption.COMPANY -> "Company (A-Z)"
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Sort options"
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.width(200.dp)
        ) {
            DropdownMenuItem(
                text = { Text("Recently Added") },
                onClick = {
                    onSortSelected(BusinessCardViewModel.SortOption.RECENTLY_ADDED)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Recently Viewed") },
                onClick = {
                    onSortSelected(BusinessCardViewModel.SortOption.RECENTLY_VIEWED)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Name (A-Z)") },
                onClick = {
                    onSortSelected(BusinessCardViewModel.SortOption.NAME)
                    expanded = false
                }
            )
            DropdownMenuItem(
                text = { Text("Company (A-Z)") },
                onClick = {
                    onSortSelected(BusinessCardViewModel.SortOption.COMPANY)
                    expanded = false
                }
            )
        }
    }
}