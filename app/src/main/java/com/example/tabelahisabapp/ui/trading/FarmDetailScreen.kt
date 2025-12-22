package com.example.tabelahisabapp.ui.trading

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.Farm
import com.example.tabelahisabapp.data.db.entity.TradeTransaction
import com.example.tabelahisabapp.ui.components.*
import com.example.tabelahisabapp.ui.theme.*

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
