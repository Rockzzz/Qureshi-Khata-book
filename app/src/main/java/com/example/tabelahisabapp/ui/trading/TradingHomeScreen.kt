package com.example.tabelahisabapp.ui.trading

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.TradeTransaction
import com.example.tabelahisabapp.data.db.entity.Farm
import com.example.tabelahisabapp.ui.theme.*
import java.util.*
import kotlin.math.abs

// ═══════════════════════════════════════════════════════════════════════════
// ANTIGRAVITY UI - TRADING SCREEN (Gemini-Inspired Design)
// ═══════════════════════════════════════════════════════════════════════════

// Color System - Using accent colors (theme-independent)
private val GradientPurple1 = Color(0xFF6366F1)
private val GradientPurple2 = Color(0xFF8B5CF6)
private val ProfitGreen = Color(0xFF10B981)
private val LossRed = Color(0xFFEF4444)
// Note: Card and text colors now use MaterialTheme.colorScheme for dark mode support

// Farm icon colors
private val FarmColors = listOf(
    Color(0xFF10B981), // Green
    Color(0xFF6366F1), // Indigo
    Color(0xFFF59E0B), // Amber
    Color(0xFF06B6D4), // Cyan
    Color(0xFFEC4899), // Pink
    Color(0xFF8B5CF6), // Purple
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TradingHomeScreen(
    viewModel: TradingViewModel = hiltViewModel(),
    onAddTransaction: () -> Unit,
    onEditTransaction: (Int) -> Unit,
    onNavigateToSettings: () -> Unit = {},
    onFarmClick: (Int) -> Unit = {}
) {
    val allTrades by viewModel.allTrades.collectAsState()
    val farms by viewModel.allFarms.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    
    // Dialog states
    var showAddFarmDialog by remember { mutableStateOf(false) }
    var showEditFarmDialog by remember { mutableStateOf(false) }
    var editingFarm by remember { mutableStateOf<Farm?>(null) }
    var editFarmName by remember { mutableStateOf("") }
    var farmToDelete by remember { mutableStateOf<Farm?>(null) }
    var selectedFarm by remember { mutableStateOf<Farm?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    
    // Calculate profits
    val calendar = Calendar.getInstance()
    calendar.set(Calendar.DAY_OF_MONTH, 1)
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val thisMonthStart = calendar.timeInMillis
    
    val lastMonthCalendar = Calendar.getInstance()
    lastMonthCalendar.add(Calendar.MONTH, -1)
    lastMonthCalendar.set(Calendar.DAY_OF_MONTH, 1)
    lastMonthCalendar.set(Calendar.HOUR_OF_DAY, 0)
    lastMonthCalendar.set(Calendar.MINUTE, 0)
    lastMonthCalendar.set(Calendar.SECOND, 0)
    lastMonthCalendar.set(Calendar.MILLISECOND, 0)
    val lastMonthStart = lastMonthCalendar.timeInMillis
    
    val thisMonthProfit = remember(allTrades) {
        allTrades.filter { it.farmId != null && it.date >= thisMonthStart }.sumOf { it.profit ?: 0.0 }
    }
    
    val lastMonthProfit = remember(allTrades) {
        allTrades.filter { it.farmId != null && it.date >= lastMonthStart && it.date < thisMonthStart }.sumOf { it.profit ?: 0.0 }
    }
    
    val totalProfit = remember(allTrades) {
        allTrades.filter { it.farmId != null }.sumOf { it.profit ?: 0.0 }
    }
    
    val percentageChange = remember(thisMonthProfit, lastMonthProfit) {
        if (lastMonthProfit != 0.0) {
            ((thisMonthProfit - lastMonthProfit) / abs(lastMonthProfit) * 100).toInt()
        } else if (thisMonthProfit > 0) 100 else 0
    }

    var isLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isLoaded = true }

    Box(modifier = Modifier.fillMaxSize()) {
        // Extended Gradient Background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GradientPurple1, GradientPurple2)
                    )
                )
        )
        
        // Content
        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                AnimatedVisibility(
                    visible = isLoaded,
                    enter = fadeIn(animationSpec = tween(300, delayMillis = 400)) + 
                            slideInVertically(initialOffsetY = { 50 })
                ) {
                    FloatingActionButton(
                        onClick = { showAddFarmDialog = true },
                        containerColor = GradientPurple1,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Farm")
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
                // HEADER + HERO CARD
                // ═══════════════════════════════════════════════════════════
                item {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp)
                    ) {
                        // App Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp, bottom = 20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Trading",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(
                                onClick = onNavigateToSettings,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        // Hero Profit Card
                        AnimatedVisibility(
                            visible = isLoaded,
                            enter = fadeIn(animationSpec = tween(400)) +
                                    slideInVertically(initialOffsetY = { 30 })
                        ) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(12.dp, RoundedCornerShape(20.dp)),
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Text(
                                        text = "This Month",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Animated amount
                                    val animatedAmount by animateFloatAsState(
                                        targetValue = thisMonthProfit.toFloat(),
                                        animationSpec = tween(1000, easing = EaseOut),
                                        label = "amount"
                                    )
                                    
                                    Text(
                                        text = "₹${String.format("%,.0f", animatedAmount)}",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (thisMonthProfit >= 0) ProfitGreen else LossRed
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // Growth badge
                                    if (percentageChange != 0) {
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = if (percentageChange > 0) 
                                                ProfitGreen.copy(alpha = 0.1f) 
                                            else 
                                                LossRed.copy(alpha = 0.1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = if (percentageChange > 0) 
                                                        Icons.Default.TrendingUp 
                                                    else 
                                                        Icons.Default.TrendingDown,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (percentageChange > 0) ProfitGreen else LossRed
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${if (percentageChange > 0) "+" else ""}$percentageChange% vs last month",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (percentageChange > 0) ProfitGreen else LossRed
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Overall Profit Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Overall Profit",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "₹${String.format("%,.0f", totalProfit)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                // ═══════════════════════════════════════════════════════════
                // FARMS SECTION (White Container)
                // ═══════════════════════════════════════════════════════════
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Farms",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
                
                // Farm Cards
                if (farms.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 20.dp)
                        ) {
                            EmptyFarmsCard()
                        }
                    }
                } else {
                    itemsIndexed(farms, key = { _, farm -> farm.id }) { index, farm ->
                        val farmTrades = allTrades.filter { it.farmId == farm.id }
                        val farmProfit = farmTrades.sumOf { it.profit ?: 0.0 }
                        val entryCount = farmTrades.size
                        
                        // Generate sparkline data from trades
                        val sparklineData = remember(farmTrades) {
                            farmTrades.takeLast(7).map { (it.profit ?: 0.0).toFloat() }
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            AnimatedVisibility(
                                visible = isLoaded,
                                enter = fadeIn(animationSpec = tween(300, delayMillis = 200 + (index * 50))) +
                                        slideInVertically(initialOffsetY = { 20 })
                            ) {
                                FarmCardWithSparkline(
                                    farm = farm,
                                    profit = farmProfit,
                                    entryCount = entryCount,
                                    sparklineData = sparklineData,
                                    colorIndex = index,
                                    onClick = { onFarmClick(farm.id) },
                                    onLongPress = {
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedFarm = farm
                                        showContextMenu = true
                                    }
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
                            .background(MaterialTheme.colorScheme.background)
                    )
                }
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // CONTEXT MENU
    // ═══════════════════════════════════════════════════════════════════════
    if (showContextMenu && selectedFarm != null) {
        ModalBottomSheet(
            onDismissRequest = { 
                showContextMenu = false
                selectedFarm = null 
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = selectedFarm?.name ?: "",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(20.dp))
                
                ContextMenuItem(
                    icon = Icons.Default.Edit,
                    text = "Edit Farm",
                    color = GradientPurple1
                ) {
                    showContextMenu = false
                    editingFarm = selectedFarm
                    editFarmName = selectedFarm?.name ?: ""
                    showEditFarmDialog = true
                    selectedFarm = null
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ContextMenuItem(
                    icon = Icons.Default.Delete,
                    text = "Delete Farm",
                    color = LossRed
                ) {
                    showContextMenu = false
                    farmToDelete = selectedFarm
                    selectedFarm = null
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════════
    // DIALOGS
    // ═══════════════════════════════════════════════════════════════════════
    
    farmToDelete?.let { farm ->
        AlertDialog(
            onDismissRequest = { farmToDelete = null },
            icon = { Icon(Icons.Default.Delete, null, tint = LossRed) },
            title = { Text("Delete Farm") },
            text = { Text("Delete \"${farm.name}\" and all its trades?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFarm(farm)
                        farmToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = LossRed)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { farmToDelete = null }) { Text("Cancel") }
            }
        )
    }
    
    if (showAddFarmDialog) {
        var farmName by remember { mutableStateOf("") }
        val autoShortCode = remember(farmName) {
            farmName.split(" ").filter { it.isNotBlank() }.take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }.ifBlank { "FM" }
        }
        
        AlertDialog(
            onDismissRequest = { showAddFarmDialog = false },
            title = { Text("Add Farm", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = farmName,
                    onValueChange = { farmName = it },
                    label = { Text("Farm Name") },
                    placeholder = { Text("e.g., Green Valley Farm") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (farmName.isNotBlank()) {
                            viewModel.addFarm(farmName.trim(), autoShortCode)
                            showAddFarmDialog = false
                        }
                    },
                    enabled = farmName.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddFarmDialog = false }) { Text("Cancel") }
            }
        )
    }
    
    if (showEditFarmDialog && editingFarm != null) {
        val autoShortCode = remember(editFarmName) {
            editFarmName.split(" ").filter { it.isNotBlank() }.take(2)
                .joinToString("") { it.first().uppercaseChar().toString() }.ifBlank { "FM" }
        }
        
        AlertDialog(
            onDismissRequest = { showEditFarmDialog = false; editingFarm = null },
            title = { Text("Edit Farm", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editFarmName,
                    onValueChange = { editFarmName = it },
                    label = { Text("Farm Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editFarmName.isNotBlank()) {
                            viewModel.updateFarm(editingFarm!!.copy(name = editFarmName.trim(), shortCode = autoShortCode))
                            showEditFarmDialog = false
                            editingFarm = null
                        }
                    },
                    enabled = editFarmName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditFarmDialog = false; editingFarm = null }) { Text("Cancel") }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// FARM CARD WITH SPARKLINE
// ═══════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FarmCardWithSparkline(
    farm: Farm,
    profit: Double,
    entryCount: Int,
    sparklineData: List<Float>,
    colorIndex: Int,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val isProfit = profit >= 0
    val farmColor = FarmColors[colorIndex % FarmColors.size]
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "scale"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        isPressed = true
                        onLongPress()
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored Avatar with Icon
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = farmColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = getFarmIcon(colorIndex),
                        contentDescription = null,
                        tint = farmColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Name + Entry count
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = farm.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$entryCount ${if (entryCount == 1) "entry" else "entries"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Amount
            Text(
                text = "₹${String.format("%,.0f", abs(profit))}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isProfit) ProfitGreen else LossRed
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(150)
            isPressed = false
        }
    }
}

// Sparkline Chart Component
@Composable
private fun SparklineChart(
    data: List<Float>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val maxValue = data.maxOrNull() ?: 1f
        val minValue = data.minOrNull() ?: 0f
        val range = (maxValue - minValue).coerceAtLeast(1f)
        
        val path = Path()
        data.forEachIndexed { index, value ->
            val x = (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * width
            val y = height - ((value - minValue) / range * height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
        
        // Draw dots
        data.forEachIndexed { index, value ->
            val x = (index.toFloat() / (data.size - 1).coerceAtLeast(1)) * width
            val y = height - ((value - minValue) / range * height)
            drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(x, y))
        }
    }
}

// Get farm icon based on index
private fun getFarmIcon(index: Int): ImageVector {
    return when (index % 6) {
        0 -> Icons.Default.Agriculture
        1 -> Icons.Default.Grass
        2 -> Icons.Default.WbSunny
        3 -> Icons.Default.Nature
        4 -> Icons.Default.Park
        else -> Icons.Default.Eco
    }
}

@Composable
private fun EmptyFarmsCard() {
    Surface(
        modifier = Modifier.fillMaxWidth().shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Agriculture,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No farms yet", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Tap + to add your first farm", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, fontWeight = FontWeight.Medium, color = color)
        }
    }
}
