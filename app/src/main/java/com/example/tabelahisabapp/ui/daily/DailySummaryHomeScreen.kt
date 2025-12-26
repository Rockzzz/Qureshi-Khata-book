package com.example.tabelahisabapp.ui.daily

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.DailyBalance
import com.example.tabelahisabapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════
// ANTIGRAVITY UI - DAILY KA HISAB SCREEN
// ═══════════════════════════════════════════════════════════════════════════

// Color System
private val GradientPurple1 = Color(0xFF6366F1)
private val GradientPurple2 = Color(0xFF8B5CF6)
private val GradientPurple3 = Color(0xFFA78BFA)
private val ProfitGreen = Color(0xFF10B981)
private val LossRed = Color(0xFFEF4444)
private val CardWhite = Color.White
private val TextDark = Color(0xFF1E293B)
private val TextMuted = Color(0xFF64748B)
private val BgGray = Color(0xFFF8FAFC)

// Unified model for display
data class LedgerItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val amount: Double,
    val type: LedgerType,
    val timestamp: Long
)

enum class LedgerType {
    EXPENSE, MONEY_IN, MONEY_OUT
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailySummaryHomeScreen(
    viewModel: DailySummaryViewModel = hiltViewModel(),
    onAddToday: () -> Unit,
    onEditEntry: (Long) -> Unit
) {
    val todayBalance by viewModel.todayBalance.collectAsState()
    val recentBalances by viewModel.recentBalances.collectAsState()
    val todayExpenses by viewModel.todayExpenses.collectAsState()
    val todayTransactions by viewModel.todayTransactions.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current

    var entryToDelete by remember { mutableStateOf<DailyBalance?>(null) }
    var selectedEntry by remember { mutableStateOf<DailyBalance?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    
    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isLoaded = true }

    // Calculate today's totals
    val todayNet = remember(todayTransactions, todayExpenses) {
        val received = todayTransactions
            .filter { it.transaction.type == "DEBIT" && 
                     it.transaction.note?.contains("Khareedari", ignoreCase = true) != true }
            .sumOf { it.transaction.amount }
        val spent = todayTransactions
            .filter { it.transaction.type == "CREDIT" }
            .sumOf { it.transaction.amount } + todayExpenses.sumOf { it.amount }
        received - spent
    }
    
    val todayEntryCount = remember(todayTransactions, todayExpenses) {
        todayTransactions.size + todayExpenses.size
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GradientPurple1, GradientPurple2)
                    )
                )
        )
        
        Scaffold(
            containerColor = Color.Transparent
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // ═══════════════════════════════════════════════════════════
                // HEADER + TODAY HERO CARD
                // ═══════════════════════════════════════════════════════════
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        // Title
                        Text(
                            text = "Daily Ka Hisab",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
                        )
                        
                        // Today Hero Card with Gradient
                        AnimatedVisibility(
                            visible = isLoaded,
                            enter = fadeIn(animationSpec = tween(400)) +
                                    slideInVertically(initialOffsetY = { 30 })
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(16.dp, RoundedCornerShape(24.dp))
                                    .clickable { 
                                        todayBalance?.let { onEditEntry(it.date) } ?: onAddToday()
                                    },
                                shape = RoundedCornerShape(24.dp),
                                color = Color.Transparent
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            brush = Brush.linearGradient(
                                                colors = listOf(
                                                    GradientPurple2,
                                                    GradientPurple3,
                                                    Color.White.copy(alpha = 0.9f)
                                                )
                                            ),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .padding(24.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "Today",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        // Animated amount
                                        val animatedAmount by animateFloatAsState(
                                            targetValue = (todayBalance?.closingCash?.plus(todayBalance?.closingBank ?: 0.0) ?: 0.0).toFloat(),
                                            animationSpec = tween(1000, easing = EaseOut),
                                            label = "amount"
                                        )
                                        
                                        if (todayBalance != null) {
                                            Text(
                                                text = "₹${String.format("%,.0f", animatedAmount)}",
                                                style = MaterialTheme.typography.displaySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = ProfitGreen
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = "$todayEntryCount entries • Closing: ₹${String.format("%,.0f", (todayBalance?.closingCash ?: 0.0) + (todayBalance?.closingBank ?: 0.0))}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextDark.copy(alpha = 0.7f)
                                            )
                                        } else {
                                            Text(
                                                text = "₹0",
                                                style = MaterialTheme.typography.displaySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = ProfitGreen
                                            )
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = "Tap to start today's hisab",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = TextDark.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                    
                                    // Chevron arrow
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .size(28.dp),
                                        tint = TextDark.copy(alpha = 0.4f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // ═══════════════════════════════════════════════════════════
                // DATE CARDS (White Container)
                // ═══════════════════════════════════════════════════════════
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
                
                // Recent day cards
                if (recentBalances.isNotEmpty()) {
                    itemsIndexed(
                        recentBalances.take(15), 
                        key = { _, balance -> balance.date }
                    ) { index, balance ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgGray)
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            AnimatedVisibility(
                                visible = isLoaded,
                                enter = fadeIn(animationSpec = tween(300, delayMillis = 150 + (index * 50))) +
                                        slideInVertically(initialOffsetY = { 20 })
                            ) {
                                DayCard(
                                    balance = balance,
                                    onClick = { onEditEntry(balance.date) },
                                    onLongPress = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedEntry = balance
                                        showContextMenu = true
                                    }
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgGray)
                                .padding(20.dp)
                        ) {
                            EmptyStateCard()
                        }
                    }
                }
                
                // Extra bottom space
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .background(BgGray)
                    )
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // LONG-PRESS CONTEXT MENU
    // ═══════════════════════════════════════════════════════════════════════
    if (showContextMenu && selectedEntry != null) {
        val dateStr = SimpleDateFormat("dd MMM, EEE", Locale.getDefault()).format(Date(selectedEntry!!.date))
        
        ModalBottomSheet(
            onDismissRequest = { 
                showContextMenu = false
                selectedEntry = null 
            },
            containerColor = CardWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                // Edit
                ContextMenuItem(
                    icon = Icons.Default.Edit,
                    text = "Edit Entry",
                    color = GradientPurple1
                ) {
                    showContextMenu = false
                    selectedEntry?.let { onEditEntry(it.date) }
                    selectedEntry = null
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Delete
                ContextMenuItem(
                    icon = Icons.Default.Delete,
                    text = "Delete Entry",
                    color = LossRed
                ) {
                    showContextMenu = false
                    entryToDelete = selectedEntry
                    selectedEntry = null
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Delete confirmation
    entryToDelete?.let { entry ->
        val dateStr = SimpleDateFormat("dd MMM, EEE", Locale.getDefault()).format(Date(entry.date))
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = LossRed) },
            title = { Text("Delete Entry") },
            text = { Text("Delete entry for $dateStr?") },
            confirmButton = {
                Button(
                    onClick = { 
                        viewModel.deleteDailyBalance(entry)
                        entryToDelete = null 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { entryToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DAY CARD COMPONENT
// ═══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCard(
    balance: DailyBalance,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val cashChange = balance.closingCash - balance.openingCash
    val bankChange = balance.closingBank - balance.openingBank
    val netChange = cashChange + bankChange
    val isPositive = netChange >= 0
    
    val dateFormat = SimpleDateFormat("dd MMM, EEE", Locale.getDefault())
    val dateStr = dateFormat.format(Date(balance.date))
    val closingTotal = balance.closingCash + balance.closingBank
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date + Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Net change amount (green/red)
                Text(
                    text = "${if (isPositive) "+" else ""}₹${String.format("%,.0f", netChange)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) ProfitGreen else LossRed
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Closing: ₹${String.format("%,.0f", closingTotal)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            
            // Right side: Net amount + chevron
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isPositive) "+" else ""}₹${String.format("%,.0f", netChange)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) ProfitGreen else LossRed
                )
                Text(
                    text = "Net",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.EventNote,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = TextMuted.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No entries yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
            Text(
                text = "Start your daily hisab today",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, fontWeight = FontWeight.Medium, color = color)
        }
    }
}
