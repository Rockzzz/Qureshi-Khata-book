package com.example.tabelahisabapp.ui.trading

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.Farm
import com.example.tabelahisabapp.data.db.entity.TradeTransaction
import com.example.tabelahisabapp.ui.components.*
import com.example.tabelahisabapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// Antigravity colors
private val AntigravityCard = Color.White
private val AntigravityProfit = Color(0xFF22C55E)
private val AntigravityLoss = Color(0xFFEF4444)
private val AntigravityText = Color(0xFF1E293B)
private val AntigravityMuted = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmDetailScreen(
    farmId: Int,
    viewModel: TradingViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onAddTransaction: (Int) -> Unit, // farmId passed
    onEditTransaction: (Int) -> Unit
) {
    val allTrades by viewModel.allTrades.collectAsState()
    val allFarms by viewModel.allFarms.collectAsState()
    
    val farm = allFarms.find { it.id == farmId }
    val farmTrades = allTrades.filter { it.farmId == farmId }
    
    var tradeToDelete by remember { mutableStateOf<TradeTransaction?>(null) }
    var selectedItem by remember { mutableStateOf<String?>(null) }
    
    val filteredTrades = remember(farmTrades, selectedItem) {
        if (selectedItem != null) {
            farmTrades.filter { it.itemName.equals(selectedItem, ignoreCase = true) }
        } else {
            farmTrades
        }
    }
    
    val uniqueItems = remember(farmTrades) {
        farmTrades.map { it.itemName }.distinct().sorted()
    }

    Scaffold(
        containerColor = BackgroundGray,
        floatingActionButton = {
            GradientFAB(
                onClick = { onAddTransaction(farmId) },
                icon = Icons.Default.Add,
                contentDescription = "Add Trade"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Header with back button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientPurpleStart, GradientPurpleEnd)
                        )
                    )
                    .padding(horizontal = Spacing.sm, vertical = Spacing.md)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = CardBackground
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = farm?.name ?: "Farm",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = CardBackground
                        )
                        Text(
                            text = "${farmTrades.size} entries • ${farm?.shortCode ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = CardBackground.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screenPadding)
                    .padding(top = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Item Filter
                if (uniqueItems.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = AppShapes.button,
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = CardBackground
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(Spacing.xs))
                            Text(
                                selectedItem ?: "Filter by Item",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Items") },
                                onClick = {
                                    selectedItem = null
                                    expanded = false
                                }
                            )
                            uniqueItems.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = {
                                        selectedItem = item
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Trade List
                if (filteredTrades.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(Spacing.md))
                            Text(
                                text = "No trades in this farm yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "Tap + to add your first trade",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Spacing.cardSpacing),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(filteredTrades) { trade ->
                            ModernTradeCard(
                                trade = trade,
                                onEdit = { onEditTransaction(trade.id) },
                                onDelete = { tradeToDelete = trade }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation
    tradeToDelete?.let { trade ->
        AlertDialog(
            onDismissRequest = { tradeToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
            title = { Text("Delete Trade") },
            text = { Text("Delete this ${trade.itemName} trade?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTrade(trade)
                        tradeToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { tradeToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// MODERN TRADE CARD - Antigravity Style with Long-Press Actions
// ═══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ModernTradeCard(
    trade: TradeTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val hapticFeedback = LocalHapticFeedback.current
    var showMenu by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    
    val isProfit = (trade.profit ?: 0.0) >= 0
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "card_scale"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = if (isPressed) 12.dp else 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f)
            )
            .combinedClickable(
                onClick = { onEdit() },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    isPressed = true
                    showMenu = true
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = AntigravityCard
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header Row: Item + Location + Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (isProfit) AntigravityProfit.copy(alpha = 0.1f) else AntigravityLoss.copy(alpha = 0.1f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = trade.itemName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = if (isProfit) AntigravityProfit else AntigravityLoss
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = trade.itemName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = AntigravityText
                        )
                        Row {
                            trade.deonar?.let {
                                Text(
                                    text = it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AntigravityMuted
                                )
                                Text(" • ", color = AntigravityMuted)
                            }
                            Text(
                                text = dateFormat.format(Date(trade.date)),
                                style = MaterialTheme.typography.bodySmall,
                                color = AntigravityMuted
                            )
                        }
                    }
                }
                
                // Profit/Loss with arrow
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isProfit) AntigravityProfit else AntigravityLoss
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "₹${String.format("%,.0f", abs(trade.profit ?: 0.0))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isProfit) AntigravityProfit else AntigravityLoss
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${trade.weight?.let { String.format("%.0f", it) } ?: "-"} kg × ₹${trade.rate?.let { String.format("%.0f", it) } ?: "-"} = ₹${String.format("%,.0f", trade.totalAmount)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AntigravityMuted
                )
                Text(
                    text = "Long press for options",
                    style = MaterialTheme.typography.labelSmall,
                    color = AntigravityMuted.copy(alpha = 0.5f)
                )
            }
        }
    }
    
    // Long-press context menu
    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { 
                showMenu = false
                isPressed = false
            },
            containerColor = AntigravityCard,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = trade.itemName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AntigravityText
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                // Edit
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMenu = false
                            isPressed = false
                            onEdit()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = Purple600.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, null, tint = Purple600)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Edit Trade", fontWeight = FontWeight.Medium, color = Purple600)
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Delete
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showMenu = false
                            isPressed = false
                            onDelete()
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = DangerRed.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, null, tint = DangerRed)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Delete Trade", fontWeight = FontWeight.Medium, color = DangerRed)
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed && !showMenu) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}
