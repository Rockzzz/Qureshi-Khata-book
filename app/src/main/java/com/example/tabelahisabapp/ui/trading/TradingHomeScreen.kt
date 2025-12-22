package com.example.tabelahisabapp.ui.trading

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.TradeTransaction
import com.example.tabelahisabapp.data.db.entity.Farm
import com.example.tabelahisabapp.ui.components.*
import com.example.tabelahisabapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradingHomeScreen(
    viewModel: TradingViewModel = hiltViewModel(),
    onAddTransaction: () -> Unit,
    onEditTransaction: (Int) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onFarmClick: (Int) -> Unit = {}
) {
    val allTrades by viewModel.allTrades.collectAsState()
    val thisMonthProfit by viewModel.thisMonthProfit.collectAsState()
    val overallProfit by viewModel.overallProfit.collectAsState()
    
    var selectedItem by remember { mutableStateOf<String?>(null) }
    var tradeToDelete by remember { mutableStateOf<TradeTransaction?>(null) }
    var showAddFarmDialog by remember { mutableStateOf(false) }
    
    // Farm edit/delete state
    var farmToDelete by remember { mutableStateOf<Farm?>(null) }
    var showEditFarmDialog by remember { mutableStateOf(false) }
    var editingFarm by remember { mutableStateOf<Farm?>(null) }
    var editFarmName by remember { mutableStateOf("") }
    var editFarmShortCode by remember { mutableStateOf("") }

    val filteredTrades = remember(allTrades, selectedItem) {
        if (selectedItem != null) {
            allTrades.filter { it.itemName.equals(selectedItem, ignoreCase = true) }
        } else {
            allTrades
        }
    }

    val uniqueItems = remember(allTrades) {
        allTrades.map { it.itemName }.distinct().sorted()
    }
    
    // Calculate total profit from all trades (using actual profit field)
    val totalProfitFromTrades = remember(allTrades) {
        allTrades.sumOf { it.profit ?: 0.0 }
    }
    
    // Calculate this month's profit  
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val thisMonthStart = calendar.timeInMillis
    
    val thisMonthProfitFromTrades = remember(allTrades) {
        allTrades.filter { it.date >= thisMonthStart }.sumOf { it.profit ?: 0.0 }
    }
    
    // Calculate last month's profit for percentage change
    val lastMonthCalendar = Calendar.getInstance()
    lastMonthCalendar.add(Calendar.MONTH, -1)
    lastMonthCalendar.set(Calendar.DAY_OF_MONTH, 1)
    lastMonthCalendar.set(Calendar.HOUR_OF_DAY, 0)
    lastMonthCalendar.set(Calendar.MINUTE, 0)
    lastMonthCalendar.set(Calendar.SECOND, 0)
    lastMonthCalendar.set(Calendar.MILLISECOND, 0)
    val lastMonthStart = lastMonthCalendar.timeInMillis
    
    val lastMonthProfitFromTrades = remember(allTrades) {
        allTrades.filter { it.date >= lastMonthStart && it.date < thisMonthStart }.sumOf { it.profit ?: 0.0 }
    }
    
    // Calculate percentage change
    val percentageChange = remember(thisMonthProfitFromTrades, lastMonthProfitFromTrades) {
        if (lastMonthProfitFromTrades != 0.0) {
            ((thisMonthProfitFromTrades - lastMonthProfitFromTrades) / kotlin.math.abs(lastMonthProfitFromTrades) * 100).toInt()
        } else if (thisMonthProfitFromTrades > 0) {
            100 // If last month was 0 and this month is positive, show 100%
        } else {
            0
        }
    }

    Scaffold(
        containerColor = BackgroundGray,
        floatingActionButton = {
            GradientFAB(
                onClick = { showAddFarmDialog = true },
                icon = Icons.Default.Add,
                contentDescription = "Add Farm"
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Gradient Header with Settings
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trading",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = CardBackground
                    )
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = CardBackground
                        )
                    }
                }
            }

            // Farm data - collect early so LazyColumn can use it
            val farms by viewModel.allFarms.collectAsState()
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.screenPadding),
                contentPadding = PaddingValues(top = Spacing.md, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Profit Summary Cards (Stacked - This Month Primary)
                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                    ) {
                        // This Month Card - PRIMARY (larger, prominent)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Spacing.radiusLarge),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.lg)
                            ) {
                                Text(
                                    text = "This Month Profit",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "₹${String.format("%,.0f", thisMonthProfitFromTrades)}",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (thisMonthProfitFromTrades >= 0) SuccessGreen else DangerRed
                                    )
                                    // Percentage change badge
                                    if (percentageChange != 0) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (percentageChange > 0) SuccessGreen.copy(alpha = 0.1f) else DangerRed.copy(alpha = 0.1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (percentageChange > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint = if (percentageChange > 0) SuccessGreen else DangerRed
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${if (percentageChange > 0) "+" else ""}${percentageChange}% vs last month",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (percentageChange > 0) SuccessGreen else DangerRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Overall Card - SECONDARY (smaller, muted)
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(Spacing.radiusMedium),
                            color = BackgroundGray,
                            tonalElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Overall Profit",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "₹${String.format("%,.0f", totalProfitFromTrades)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (totalProfitFromTrades >= 0) SuccessGreen else DangerRed
                                )
                            }
                        }
                    }
                }
                
                // Farm Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏭 Farms",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        TextButton(onClick = { showAddFarmDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Farm")
                        }
                    }
                }
                
                // Empty state or Farm cards
                if (farms.isEmpty()) {
                    item {
                        WhiteCard(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Agriculture,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "No farms yet. Add your first farm!",
                                    color = TextSecondary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                } else {
                    // Farm cards - each as individual scrollable item
                    items(farms, key = { it.id }) { farm ->
                        val farmTrades = allTrades.filter { it.farmId == farm.id }
                        val farmTradeCount = farmTrades.size
                        val farmProfit = farmTrades.sumOf { it.profit ?: 0.0 }
                        val isProfit = farmProfit >= 0
                        var showFarmMenu by remember { mutableStateOf(false) }
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFarmClick(farm.id) },
                            shape = RoundedCornerShape(Spacing.radiusMedium),
                            colors = CardDefaults.cardColors(containerColor = CardBackground),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(Spacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Farm icon/badge with color
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = if (isProfit) SuccessGreen.copy(alpha = 0.1f) else DangerRed.copy(alpha = 0.1f)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = farm.shortCode,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isProfit) SuccessGreen else DangerRed
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(Spacing.sm))
                                
                                // Farm name and entry count
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = farm.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${farmTradeCount} ${if (farmTradeCount == 1) "entry" else "entries"} • ${if (isProfit) "Profit" else "Loss"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextSecondary
                                    )
                                }
                                
                                // Profit/Loss amount
                                Text(
                                    text = "₹${String.format("%,.0f", kotlin.math.abs(farmProfit))}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isProfit) SuccessGreen else DangerRed
                                )
                                
                                // Arrow icon
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = "Open farm",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                
                                // 3-dot menu
                                Box {
                                    IconButton(
                                        onClick = { showFarmMenu = true },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.MoreVert,
                                            contentDescription = "More options",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showFarmMenu,
                                        onDismissRequest = { showFarmMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Edit") },
                                            onClick = {
                                                showFarmMenu = false
                                                editingFarm = farm
                                                editFarmName = farm.name
                                                editFarmShortCode = farm.shortCode
                                                showEditFarmDialog = true
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Edit,
                                                    contentDescription = null,
                                                    tint = Purple600
                                                )
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("Delete", color = DangerRed) },
                                            onClick = {
                                                showFarmMenu = false
                                                farmToDelete = farm
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = DangerRed
                                                )
                                            }
                                        )
                                    }
                                }
                            }
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
            title = { Text("Delete Transaction") },
            text = { Text("Are you sure you want to delete this ${trade.type} transaction?") },
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
    
    // Add Farm Dialog
    if (showAddFarmDialog) {
        var farmName by remember { mutableStateOf("") }
        var farmCode by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddFarmDialog = false },
            title = { Text("🏭 Add Farm", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = farmName,
                        onValueChange = { farmName = it },
                        label = { Text("Farm Name") },
                        placeholder = { Text("e.g., Hindustani Farm") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = farmCode,
                        onValueChange = { farmCode = it.take(4).uppercase() },
                        label = { Text("Short Code (2-4 letters)") },
                        placeholder = { Text("e.g., HF") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Text(
                        text = "Entry numbers will be: ${farmCode.ifBlank { "XX" }}-001, ${farmCode.ifBlank { "XX" }}-002...",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (farmName.isNotBlank() && farmCode.isNotBlank()) {
                            viewModel.addFarm(farmName.trim(), farmCode.trim())
                            showAddFarmDialog = false
                        }
                    },
                    enabled = farmName.isNotBlank() && farmCode.length >= 2,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Add Farm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFarmDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // Delete Farm Confirmation Dialog
    farmToDelete?.let { farm ->
        AlertDialog(
            onDismissRequest = { farmToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = DangerRed) },
            title = { Text("Delete Farm?", fontWeight = FontWeight.Bold) },
            text = { 
                Text("Are you sure you want to delete \"${farm.name}\"? All trades in this farm will become unlinked.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFarm(farm)
                        farmToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { farmToDelete = null }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // Edit Farm Dialog
    if (showEditFarmDialog && editingFarm != null) {
        AlertDialog(
            onDismissRequest = { 
                showEditFarmDialog = false
                editingFarm = null
            },
            title = { Text("✏️ Edit Farm", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = editFarmName,
                        onValueChange = { editFarmName = it },
                        label = { Text("Farm Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = editFarmShortCode,
                        onValueChange = { editFarmShortCode = it.take(4).uppercase() },
                        label = { Text("Short Code (2-4 letters)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingFarm?.let { farm ->
                            val updatedFarm = farm.copy(
                                name = editFarmName.trim(),
                                shortCode = editFarmShortCode.trim()
                            )
                            viewModel.updateFarm(updatedFarm)
                        }
                        showEditFarmDialog = false
                        editingFarm = null
                    },
                    enabled = editFarmName.isNotBlank() && editFarmShortCode.length >= 2,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEditFarmDialog = false
                    editingFarm = null
                }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun ModernTradeCard(
    trade: TradeTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()).format(Date(trade.date))
    val profit = trade.profit ?: (trade.totalAmount - trade.buyRate)
    
    // Number formatting
    val indianFormat = java.text.NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    val numberFormat = java.text.NumberFormat.getNumberInstance(Locale("en", "IN"))
    
    val formattedProfit = if (profit < 0) "-₹${numberFormat.format(kotlin.math.abs(profit))}" else "₹${numberFormat.format(profit)}"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Spacing.radiusLarge),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Main Content Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon / Avatar
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Purple50
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = trade.itemName.take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Purple600
                        )
                    }
                }

                Spacer(modifier = Modifier.width(Spacing.md))

                // Title & Date with Location
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = trade.itemName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        trade.deonar?.let { location ->
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• $location",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                // Profit Badge
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (profit >= 0) "Profit" else "Loss",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Text(
                        text = formattedProfit,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (profit >= 0) SuccessGreen else DangerRed
                    )
                }
            }

            // Details Section (Gray bg)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BackgroundGray.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.md, vertical = Spacing.sm),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Calculation logic text
                    Text(
                        text = "${trade.quantity} x ₹${numberFormat.format(trade.buyRate)} = ₹${numberFormat.format(trade.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Actions
                    Row {
                        TextButton(
                            onClick = onEdit,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp).width(60.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = onDelete,
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(32.dp).width(70.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = DangerRed)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
