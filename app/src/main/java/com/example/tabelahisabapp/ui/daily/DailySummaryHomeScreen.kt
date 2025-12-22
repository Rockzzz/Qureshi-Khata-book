package com.example.tabelahisabapp.ui.daily

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.DailyBalance
import com.example.tabelahisabapp.ui.components.*
import com.example.tabelahisabapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

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

@OptIn(ExperimentalMaterial3Api::class)
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

    var entryToDelete by remember { mutableStateOf<DailyBalance?>(null) }

    // Combine and sort transactions for "Today's Ledger"
    val todayLedger = remember(todayExpenses, todayTransactions) {
        val list = mutableListOf<LedgerItem>()
        
        todayExpenses.forEach { exp ->
            list.add(LedgerItem(
                id = "exp_${exp.id}",
                title = exp.category,
                subtitle = "Expense (${exp.paymentMethod})",
                amount = exp.amount,
                type = LedgerType.EXPENSE,
                timestamp = exp.createdAt
            ))
        }
        
        todayTransactions.forEach { trans ->
            // CREDIT in CustomerTransaction means YOU gave money to them (Payment OUT)
            // DEBIT in CustomerTransaction means THEY gave money to you (Money IN)
            val isMoneyIn = trans.transaction.type == "DEBIT"
            val paymentMode = if (trans.transaction.paymentMethod == "CASH") "Cash" else "Bank"
            val isPurchase = trans.transaction.note?.contains("Khareedari", ignoreCase = true) == true
            
            val subtitle = when {
                isPurchase -> "Khareedari ($paymentMode)"
                isMoneyIn -> "Paisa Aaya ($paymentMode)"
                else -> "Payment ($paymentMode)"
            }
            
            list.add(LedgerItem(
                id = "trans_${trans.transaction.id}",
                title = trans.customer.name,
                subtitle = subtitle,
                amount = trans.transaction.amount,
                type = if (isMoneyIn) LedgerType.MONEY_IN else LedgerType.MONEY_OUT,
                timestamp = trans.transaction.createdAt
            ))
        }
        
        list.sortedByDescending { it.timestamp }
    }

    Scaffold(
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Gradient Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientPurpleStart, GradientPurpleEnd)
                        )
                    )
                    .padding(horizontal = Spacing.screenPadding, vertical = Spacing.lg)
            ) {
                Text(
                    text = "Daily Cash & Bank",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = CardBackground
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screenPadding)
                    .padding(top = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.cardSpacing)
            ) {
                // Today's Summary Card
                WhiteCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SectionHeader(text = "Today's Summary")
                        
                        // Edit Button (Small)
                        todayBalance?.let {
                            FilledTonalButton(
                                onClick = { onEditEntry(it.date) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Edit", fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(Spacing.md))
                    
                    todayBalance?.let { balance ->
                        // Calculate totals from transactions - EXCLUDE purchases (just records, no cash movement)
                        val totalReceived = todayTransactions
                            .filter { it.transaction.type == "DEBIT" && 
                                     it.transaction.note?.contains("Khareedari", ignoreCase = true) != true }
                            .sumOf { it.transaction.amount }
                        val totalSpent = todayTransactions
                            .filter { it.transaction.type == "CREDIT" }  // CREDIT = money given
                            .sumOf { it.transaction.amount } + 
                            todayExpenses.sumOf { it.amount }
                        
                        // Main Stats Row - Show Received and Spent separately
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Paisa Aaya (Money Received)
                            Column(modifier = Modifier.weight(1f)) {
                                LabelText(text = "Aaj Paisa Aaya")
                                Text(
                                    text = "₹${String.format("%,.0f", totalReceived)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                            
                            // Kharcha (Money Spent)
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                LabelText(text = "Aaj Kharcha")
                                Text(
                                    text = "₹${String.format("%,.0f", totalSpent)}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        
                        // Cash/Bank Breakdown
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Cash Baki: ₹${String.format("%,.0f", balance.closingCash)}", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("Bank Baki: ₹${String.format("%,.0f", balance.closingBank)}", fontSize = 12.sp, color = TextSecondary)
                        }
                        
                        Divider(
                            modifier = Modifier.padding(vertical = Spacing.md),
                            color = BorderGray
                        )
                        
                        // Today's Activity Feed
                        if (todayLedger.isNotEmpty()) {
                            Text(
                                "Today's Activity",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            
                            // Scrollable list within the card (limited height)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                todayLedger.take(5).forEach { item ->
                                    LedgerItemRow(item = item)
                                }
                                if (todayLedger.size > 5) {
                                    Text(
                                        "+ ${todayLedger.size - 5} more entries...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Purple600,
                                        modifier = Modifier.padding(top = 4.dp).clickable { onEditEntry(balance.date) }
                                    )
                                }
                            }
                        } else {
                            Text(
                                "No transactions yet today.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                    } ?: run {
                        Text(
                            text = "No entry started for today",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(Spacing.md))
                        Button(
                            onClick = onAddToday,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Purple600
                            ),
                            shape = AppShapes.button
                        ) {
                            Text("Start Today's Hisab", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                
                // Recent Entries List
                SectionHeader(text = "Recent Days")
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.cardSpacing),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(recentBalances.take(10)) { balance ->
                        ModernEntryCard(
                            balance = balance,
                            onEdit = { onEditEntry(balance.date) },
                            onDelete = { entryToDelete = balance }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    entryToDelete?.let { entry ->
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(entry.date))
        AlertDialog(
            onDismissRequest = { entryToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
            title = { Text("Delete Entry") },
            text = { Text("Are you sure you want to delete the entry for $dateStr?") },
            confirmButton = {
                Button(onClick = { viewModel.deleteDailyBalance(entry); entryToDelete = null }, colors = ButtonDefaults.buttonColors(containerColor = DangerRed)) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { entryToDelete = null }) { Text("Cancel") } }
        )
    }
}

@Composable
fun LedgerItemRow(item: LedgerItem) {
    val amountColor = when (item.type) {
        LedgerType.MONEY_IN -> SuccessGreen
        LedgerType.MONEY_OUT -> DangerRed
        LedgerType.EXPENSE -> WarningOrange
    }
    
    val icon = when (item.type) {
        LedgerType.MONEY_IN -> Icons.Default.ArrowDownward // Money coming in
        LedgerType.MONEY_OUT -> Icons.Default.ArrowUpward // Money going out
        LedgerType.EXPENSE -> Icons.Default.ShoppingCart // Shopping/Expense
    }
    
    val iconBg = amountColor.copy(alpha = 0.1f)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = iconBg,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = amountColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
        Text(
            text = "₹${String.format("%,.0f", item.amount)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

@Composable
fun ModernEntryCard(
    balance: DailyBalance,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val cashChange = balance.closingCash - balance.openingCash
    val bankChange = balance.closingBank - balance.openingBank
    val totalChange = cashChange + bankChange
    val dateStr = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(balance.date))
    
    WhiteCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onEdit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (totalChange >= 0) "Saved: ₹${String.format("%,.0f", totalChange)}" else "Spent: ₹${String.format("%,.0f", -totalChange)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (totalChange >= 0) SuccessGreen else DangerRed
                )
            }
            
            // Delete icon only visible here for quick access
             IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = TextSecondary.copy(alpha = 0.5f))
            }
        }
    }
}
