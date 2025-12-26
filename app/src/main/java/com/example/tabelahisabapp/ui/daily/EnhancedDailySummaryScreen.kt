package com.example.tabelahisabapp.ui.daily

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.DailyBalance
import com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction
import com.example.tabelahisabapp.ui.daily.components.DailyAddEntryBottomSheet
import com.example.tabelahisabapp.ui.daily.components.DailyEntryType
import com.example.tabelahisabapp.ui.theme.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedDailySummaryScreen(
    viewModel: DailySummaryViewModel = hiltViewModel(),
    onAddCustomerTransaction: (Long, Boolean) -> Unit,
    onAddSupplierTransaction: (Long) -> Unit,
    onAddExpense: (Long) -> Unit,
    onEditEntry: (Long) -> Unit,
    onViewCustomerLedger: (Int) -> Unit,
    onViewSupplierLedger: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    
    // State
    var showAddSheet by remember { mutableStateOf(false) }
    var selectedDateForAdd by remember { mutableStateOf(normalizeToMidnight(System.currentTimeMillis())) }
    var isLoaded by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) { isLoaded = true }
    
    // Load recent balances (list of days with data)
    val recentBalances by viewModel.recentBalances.collectAsState()
    val ledgerDates by viewModel.allLedgerDates.collectAsState()
    
    val today = normalizeToMidnight(System.currentTimeMillis())
    
    // Get dates that have balances/transactions
    val daysWithData = remember(recentBalances, ledgerDates, today) {
        val balanceDates = recentBalances.map { normalizeToMidnight(it.date) }.toSet()
        val transactionDates = ledgerDates.map { normalizeToMidnight(it) }.toSet()
        val allOtherDates = (balanceDates + transactionDates)
            .filter { it != today }
            .sortedDescending()
        listOf(today) + allOtherDates
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Extended Gradient Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GradientPurple1, GradientPurple2)
                    )
                )
        )
        
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isLoaded,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 400)) + 
                            slideInVertically(initialOffsetY = { 50 })
                ) {
                    FloatingActionButton(
                        onClick = { 
                            selectedDateForAdd = normalizeToMidnight(System.currentTimeMillis())
                            showAddSheet = true 
                        },
                        containerColor = GradientPurple1,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Entry")
                    }
                }
            }
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
                        AnimatedVisibility(
                            visible = isLoaded,
                            enter = fadeIn(animationSpec = tween(300)) +
                                    slideInVertically(initialOffsetY = { -20 })
                        ) {
                            Text(
                                text = "Daily Ka Hisab",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 16.dp, bottom = 20.dp)
                            )
                        }
                        
                        // Today Hero Card with Gradient
                        AnimatedVisibility(
                            visible = isLoaded,
                            enter = fadeIn(animationSpec = tween(400, delayMillis = 100)) +
                                    slideInVertically(initialOffsetY = { 30 })
                        ) {
                            TodayHeroCard(
                                date = today,
                                viewModel = viewModel,
                                onClick = { onEditEntry(today) }
                            )
                        }
                    }
                }
                
                // ═══════════════════════════════════════════════════════════
                // WHITE CONTAINER FOR DATE CARDS
                // ═══════════════════════════════════════════════════════════
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Transition to white background
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = BgGray
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // Date Cards (past days)
                val pastDays = daysWithData.drop(1) // Skip today
                if (pastDays.isEmpty()) {
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
                } else {
                    itemsIndexed(pastDays, key = { _, date -> date }) { index, date ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BgGray)
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            AnimatedVisibility(
                                visible = isLoaded,
                                enter = fadeIn(animationSpec = tween(300, delayMillis = 200 + (index * 50))) +
                                        slideInVertically(initialOffsetY = { 20 })
                            ) {
                                DayCard(
                                    date = date,
                                    viewModel = viewModel,
                                    onClick = { onEditEntry(date) }
                                )
                            }
                        }
                    }
                }
                
                // Extra bottom padding
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
    
    // Add Entry Bottom Sheet
    if (showAddSheet) {
        DailyAddEntryBottomSheet(
            selectedDate = selectedDateForAdd,
            onDismiss = { showAddSheet = false },
            onSelectEntryType = { entryType ->
                showAddSheet = false
                when (entryType) {
                    is DailyEntryType.CustomerReceived -> onAddCustomerTransaction(selectedDateForAdd, false)
                    is DailyEntryType.CustomerGiven -> onAddCustomerTransaction(selectedDateForAdd, true)
                    is DailyEntryType.SupplierPurchase -> onAddSupplierTransaction(selectedDateForAdd)
                    is DailyEntryType.SupplierPayment -> onAddSupplierTransaction(selectedDateForAdd)
                    is DailyEntryType.Expense -> onAddExpense(selectedDateForAdd)
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TODAY HERO CARD - Gradient Purple Card
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun TodayHeroCard(
    date: Long,
    viewModel: DailySummaryViewModel,
    onClick: () -> Unit
) {
    val balanceFlow = remember(date) { viewModel.getBalanceByDate(date) }
    val transactionsFlow = remember(date) { viewModel.getLedgerTransactionsByDate(date) }
    
    val balance by balanceFlow.collectAsState(initial = null)
    val transactions by transactionsFlow.collectAsState(initial = emptyList())
    
    // Calculate net and closing
    val netBalance = remember(transactions) {
        var received = 0.0
        var spent = 0.0
        transactions.forEach { tx ->
            if (tx.mode != "PURCHASE") {
                when (tx.mode) {
                    "CASH_IN", "BANK_IN" -> received += tx.amount
                    "CASH_OUT", "BANK_OUT" -> spent += tx.amount
                }
            }
        }
        received - spent
    }
    
    val closingTotal = (balance?.closingCash ?: 0.0) + (balance?.closingBank ?: 0.0)
    val txCount = transactions.size
    
    // Animated amount
    val animatedNet by animateFloatAsState(
        targetValue = netBalance.toFloat(),
        animationSpec = tween(800, easing = EaseOut),
        label = "net_animation"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clickable { onClick() },
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
                            Color.White.copy(alpha = 0.95f)
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Large amount
                Text(
                    text = "₹${String.format("%,.0f", animatedNet)}",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (netBalance >= 0) ProfitGreen else LossRed
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Entry count + closing
                Text(
                    text = if (txCount > 0) 
                        "$txCount entries • Closing: ₹${String.format("%,.0f", closingTotal)}"
                    else 
                        "No entries yet • Tap to add",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark.copy(alpha = 0.7f)
                )
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

// ═══════════════════════════════════════════════════════════════════════════
// DAY CARD - Floating White Card
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun DayCard(
    date: Long,
    viewModel: DailySummaryViewModel,
    onClick: () -> Unit
) {
    val balanceFlow = remember(date) { viewModel.getBalanceByDate(date) }
    val transactionsFlow = remember(date) { viewModel.getLedgerTransactionsByDate(date) }
    
    val balance by balanceFlow.collectAsState(initial = null)
    val transactions by transactionsFlow.collectAsState(initial = emptyList())
    
    // Calculate net
    val netBalance = remember(transactions) {
        var received = 0.0
        var spent = 0.0
        transactions.forEach { tx ->
            if (tx.mode != "PURCHASE") {
                when (tx.mode) {
                    "CASH_IN", "BANK_IN" -> received += tx.amount
                    "CASH_OUT", "BANK_OUT" -> spent += tx.amount
                }
            }
        }
        received - spent
    }
    
    val closingTotal = (balance?.closingCash ?: 0.0) + (balance?.closingBank ?: 0.0)
    val txCount = transactions.size
    
    val dateFormat = remember { SimpleDateFormat("dd MMM, EEE", Locale.getDefault()) }
    val dateStr = dateFormat.format(Date(date))
    val isPositive = netBalance >= 0
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = CardWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - date + details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (txCount > 0) 
                        "$txCount entries • Closing: ₹${String.format("%,.0f", closingTotal)}"
                    else 
                        "No entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
            }
            
            // Right side - net amount
            if (txCount > 0) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${if (isPositive) "+" else ""}₹${String.format("%,.0f", netBalance)}",
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
            }
            
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
                text = "No past entries",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
            Text(
                text = "Your daily records will appear here",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted.copy(alpha = 0.6f)
            )
        }
    }
}

private fun normalizeToMidnight(timestamp: Long): Long {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = timestamp
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}
