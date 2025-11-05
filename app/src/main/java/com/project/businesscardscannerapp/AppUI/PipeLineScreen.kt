package com.project.businesscardscannerapp.AppUI

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.project.businesscardscannerapp.RoomDB.Entity.PipelineStage
import com.project.businesscardscannerapp.ViewModel.BusinessCardViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PipelineScreen(
    viewModel: BusinessCardViewModel = viewModel(),
    navController: NavController
) {
    val newLeads by viewModel.newLeads.collectAsState(initial = emptyList())
    val contactedLeads by viewModel.contactedLeads.collectAsState(initial = emptyList())
    val meetingLeads by viewModel.meetingLeads.collectAsState(initial = emptyList())
    val negotiationLeads by viewModel.negotiationLeads.collectAsState(initial = emptyList())
    val closedWonLeads by viewModel.closedWonLeads.collectAsState(initial = emptyList())
    val closedLostLeads by viewModel.closedLostLeads.collectAsState(initial = emptyList())

    val pipelineRowState = rememberLazyListState()
    var autoScrollEnabled by remember { mutableStateOf(true) }

    // Pause auto-scroll while user is interacting
    LaunchedEffect(pipelineRowState.isScrollInProgress) {
        autoScrollEnabled = !pipelineRowState.isScrollInProgress
    }

    // Auto-scroll effect
    LaunchedEffect(autoScrollEnabled) {
        if (autoScrollEnabled && !pipelineRowState.isScrollInProgress) {
            while (autoScrollEnabled) {
                delay(5000)
                if (autoScrollEnabled && !pipelineRowState.isScrollInProgress) {
                    val currentItem = pipelineRowState.firstVisibleItemIndex
                    val nextItem = (currentItem + 1) % 6
                    pipelineRowState.animateScrollToItem(nextItem)
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "Sales Pipeline",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Drag cards between stages to update progress",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            PipelineStatsRow(
                newCount = newLeads.size,
                contactedCount = contactedLeads.size,
                meetingCount = meetingLeads.size,
                negotiationCount = negotiationLeads.size,
                wonCount = closedWonLeads.size,
                lostCount = closedLostLeads.size
            )
        }

        // Pipeline board
        LazyRow(
            state = pipelineRowState,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item { PipelineColumn(PipelineStage.NEW, newLeads, viewModel, navController) }
            item { PipelineColumn(PipelineStage.CONTACTED, contactedLeads, viewModel, navController) }
            item { PipelineColumn(PipelineStage.MEETING, meetingLeads, viewModel, navController) }
            item { PipelineColumn(PipelineStage.NEGOTIATION, negotiationLeads, viewModel, navController) }
            item { PipelineColumn(PipelineStage.CLOSED_WON, closedWonLeads, viewModel, navController) }
            item { PipelineColumn(PipelineStage.CLOSED_LOST, closedLostLeads, viewModel, navController) }
        }

        // FAB
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            FloatingActionButton(
                onClick = { navController.navigate("Insights") },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Insights",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}


@Composable
fun PipelineStatsRow(
    newCount: Int,
    contactedCount: Int,
    meetingCount: Int,
    negotiationCount: Int,
    wonCount: Int,
    lostCount: Int
) {
    val total = newCount + contactedCount + meetingCount + negotiationCount + wonCount + lostCount

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(total, "Total", Color(0xFF4299E1))
        HorizontalDivider(
            modifier = Modifier
                .height(24.dp)
                .width(1.dp),
            color = Color(0xFFE2E8F0)
        )
        StatItem(newCount, "New", getStageColor(PipelineStage.NEW))
        StatItem(contactedCount, "Contacted", getStageColor(PipelineStage.CONTACTED))
        StatItem(meetingCount, "Meeting", getStageColor(PipelineStage.MEETING))
        StatItem(wonCount, "Won", getStageColor(PipelineStage.CLOSED_WON))
    }
}

@Composable
fun StatItem(count: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF718096)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PipelineColumn(
    stage: PipelineStage,
    contacts: List<BusinessCard>,
    viewModel: BusinessCardViewModel,
    navController: NavController
) {
    val columnWidth = 300.dp
    val lazyColumnState = rememberLazyListState()
    val emptyStateHeight by animateDpAsState(
        targetValue = if (contacts.isEmpty()) 120.dp else 0.dp,
        label = "emptyStateHeight"
    )

    Column(
        modifier = Modifier
            .width(columnWidth)
            .fillMaxHeight()
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            getStageColor(stage),
                            getStageColor(stage).copy(alpha = 0.9f)
                        )
                    ),
                    RoundedCornerShape(12.dp)
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stage.getDisplayName(),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${contacts.size} contacts",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = contacts.size.toString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Contacts list
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (contacts.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Empty",
                        tint = Color(0xFFCBD5E0),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No contacts",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF718096)
                    )
                }
            } else {
                LazyColumn(
                    state = lazyColumnState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White, RoundedCornerShape(12.dp)),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(12.dp)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        PipelineContactCard(
                            contact = contact,
                            modifier = Modifier
                                .animateItemPlacement()
                                .draggableToStages(contact, viewModel),
                            onCardClick = { navController.navigate("details/${contact.id}") }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PipelineContactCard(
    contact: BusinessCard,
    modifier: Modifier = Modifier,
    onCardClick: () -> Unit
) {
    val isUrgent = contact.reminderTime?.let {
        it > System.currentTimeMillis() && it - System.currentTimeMillis() < 24 * 60 * 60 * 1000
    } == true

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick() }
            .shadow(2.dp, RoundedCornerShape(12.dp), clip = false),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .background(Color.White)
                .padding(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            getStageColor(contact.pipelineStage).copy(alpha = 0.1f),
                            CircleShape
                        )
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1).uppercase(),
                        color = getStageColor(contact.pipelineStage),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color(0xFF2D3748)
                    )
                    if (contact.company.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Work,
                                contentDescription = "Company",
                                tint = Color(0xFF718096),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = contact.company,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF718096),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Lead score
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Lead Score",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF718096)
                    )
                    Text(
                        text = "${contact.leadScore}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = getScoreColor(contact.leadScore)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { contact.leadScore / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = getScoreColor(contact.leadScore),
                    trackColor = Color(0xFFE2E8F0)
                )
            }

            // Reminder
            contact.reminderTime?.let { followUpDate ->
                if (followUpDate > System.currentTimeMillis()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                if (isUrgent) Color(0xFFFED7D7) else Color(0xFFE6FFFA),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Follow-up",
                            tint = if (isUrgent) Color(0xFFC53030) else Color(0xFF38A169),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isUrgent) "URGENT: ${formatFollowUpDate(followUpDate)}"
                            else "Follow-up: ${formatFollowUpDate(followUpDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUrgent) Color(0xFFC53030) else Color(0xFF38A169),
                            fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Tags
            if (contact.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    contact.tags.take(3).forEach { tag ->
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier
                                .background(
                                    getStageColor(contact.pipelineStage),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// Drag to move between pipeline stages
fun Modifier.draggableToStages(
    contact: BusinessCard,
    viewModel: BusinessCardViewModel
): Modifier = this.pointerInput(Unit) {
    detectDragGestures(
        onDragStart = { },
        onDragEnd = { }
    ) { change, dragAmount ->
        change.consumeAllChanges()
        val (x, _) = dragAmount
        if (x > 150) {
            contact.pipelineStage.getNextStage()?.let { nextStage ->
                viewModel.updatePipelineStage(contact.id, nextStage)
            }
        } else if (x < -150) {
            contact.pipelineStage.getPreviousStage()?.let { prevStage ->
                viewModel.updatePipelineStage(contact.id, prevStage)
            }
        }
    }
}

// Helpers
fun getStageColor(stage: PipelineStage): Color = when (stage) {
    PipelineStage.NEW -> Color(0xFF4299E1)
    PipelineStage.CONTACTED -> Color(0xFF48BB78)
    PipelineStage.MEETING -> Color(0xFFED8936)
    PipelineStage.NEGOTIATION -> Color(0xFF9F7AEA)
    PipelineStage.CLOSED_WON -> Color(0xFF38B2AC)
    PipelineStage.CLOSED_LOST -> Color(0xFFF56565)
}

fun getScoreColor(score: Int): Color = when {
    score >= 80 -> Color(0xFF48BB78)
    score >= 50 -> Color(0xFFED8936)
    else -> Color(0xFFF56565)
}

fun formatFollowUpDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
