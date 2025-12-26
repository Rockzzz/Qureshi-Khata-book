package com.example.tabelahisabapp.ui.trading

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.example.tabelahisabapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Premium colors for trade screen
private val GradientStart = Color(0xFF667EEA)
private val GradientEnd = Color(0xFF764BA2)
private val CardBg = Color.White
private val SectionBg = Color(0xFFF8FAFC)
private val ProfitGreen = Color(0xFF10B981)
private val LossRed = Color(0xFFEF4444)
private val InfoBlue = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTradeTransactionScreen(
    viewModel: TradingViewModel = hiltViewModel(),
    savedStateHandle: SavedStateHandle? = null,
    farmId: Int? = null,
    onTransactionSaved: () -> Unit,
    onCancel: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val transactionId = savedStateHandle?.get<Int>("transactionId")
    val isEditMode = transactionId != null
    
    val allFarms by viewModel.allFarms.collectAsState()
    val selectedFarm = farmId?.let { id -> allFarms.find { it.id == id } }
    
    // Form state
    var location by remember { mutableStateOf("") }
    var itemName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var buyingAmount by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var rate by remember { mutableStateOf("") }
    var extraBonus by remember { mutableStateOf("") }
    var weightLessFeePerBuffalo by remember { mutableStateOf("") }
    var feePerBuffalo by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var originalCreatedAt by remember { mutableStateOf<Long?>(null) }
    var editFarmId by remember { mutableStateOf<Int?>(null) } // Store farmId from original trade
    
    val allTrades by viewModel.allTrades.collectAsState()
    val locationOptions = listOf("Deonar", "Allana", "Al Qureshi")
    val animalOptions = listOf("Buffalo", "Goat", "Other")
    val isAllanaSelected = location in listOf("Allana", "Al Qureshi")

    // Load transaction data when editing
    LaunchedEffect(transactionId, allTrades) {
        if (transactionId != null && allTrades.isNotEmpty()) {
            allTrades.find { it.id == transactionId }?.let { trade ->
                location = trade.deonar ?: ""
                itemName = trade.itemName
                quantity = trade.quantity.toString()
                buyingAmount = trade.buyRate.toString()
                weight = trade.weight?.toString() ?: ""
                rate = trade.rate?.toString() ?: ""
                extraBonus = trade.extraBonus?.toString() ?: ""
                // Load Allana-specific fields
                weightLessFeePerBuffalo = if (trade.weight != null && trade.netWeight != null) {
                    (trade.weight - trade.netWeight).toString()
                } else ""
                feePerBuffalo = trade.fee?.toString() ?: ""
                date = trade.date
                note = trade.note ?: ""
                originalCreatedAt = trade.createdAt
                editFarmId = trade.farmId // Preserve farmId for update
            }
        }
    }

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = date

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            date = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Calculations
    val buyingAmountValue = buyingAmount.toDoubleOrNull() ?: 0.0
    val weightValue = weight.toDoubleOrNull() ?: 0.0
    val rateValue = rate.toDoubleOrNull() ?: 0.0
    val extraBonusValue = extraBonus.toDoubleOrNull() ?: 0.0
    val quantityValue = quantity.toIntOrNull() ?: 0
    val weightLessFeeValue = weightLessFeePerBuffalo.toDoubleOrNull() ?: 0.0
    val feeValue = feePerBuffalo.toDoubleOrNull() ?: 0.0
    
    val netWeightAfterLess = if (isAllanaSelected && weightValue > 0) {
        weightValue - weightLessFeeValue
    } else {
        weightValue
    }
    
    val grossAmount = if (isAllanaSelected) {
        (netWeightAfterLess * rateValue) + extraBonusValue
    } else {
        (weightValue * rateValue) + extraBonusValue
    }
    
    val tdsPerBuffalo = if (isAllanaSelected && grossAmount > 0) {
        grossAmount * 0.001
    } else {
        0.0
    }
    
    val totalAmount = if (isAllanaSelected) {
        grossAmount - feeValue - tdsPerBuffalo
    } else {
        grossAmount
    }
    
    val profit = if (totalAmount > 0 && buyingAmountValue > 0) {
        totalAmount - buyingAmountValue
    } else {
        null
    }

    Scaffold(
        containerColor = SectionBg,
        topBar = {
            // Premium gradient header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        )
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditMode) "Edit Trade" else "Add Trade",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        selectedFarm?.let {
                            Text(
                                text = "Farm: ${it.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color.White
                        )
                    }
                }
            }
        },
        bottomBar = {
            // Sticky save button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = CardBg
            ) {
                Button(
                    onClick = {
                        scope.launch {
                            viewModel.saveTrade(
                                type = "TRADE",
                                deonar = location.ifBlank { null },
                                itemName = itemName.trim(),
                                quantity = quantityValue,
                                buyRate = buyingAmountValue,
                                weight = weightValue.takeIf { it > 0 },
                                rate = rateValue.takeIf { it > 0 },
                                extraBonus = extraBonusValue.takeIf { it > 0 },
                                netWeight = if (isAllanaSelected) netWeightAfterLess.takeIf { it > 0 } else null,
                                fee = if (isAllanaSelected) feeValue.takeIf { it > 0 } else null,
                                tds = if (isAllanaSelected) tdsPerBuffalo.takeIf { it > 0 } else null,
                                totalAmount = totalAmount,
                                profit = profit,
                                pricePerUnit = buyingAmountValue,
                                date = date,
                                note = note.ifBlank { null },
                                transactionId = transactionId,
                                originalCreatedAt = originalCreatedAt,
                                farmId = if (isEditMode) editFarmId else farmId,
                                entryNumber = null
                            )
                            onTransactionSaved()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = itemName.isNotBlank() && quantityValue > 0 && buyingAmountValue > 0,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GradientStart,
                        disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEditMode) "Update Trade" else "Save Trade",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Basic Info Card
            PremiumCard(title = "Basic Info", icon = Icons.Default.Info) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Date Picker
                    PremiumField(
                        label = "Date",
                        value = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date)),
                        onClick = { datePickerDialog.show() },
                        leadingIcon = Icons.Default.CalendarToday,
                        trailingIcon = Icons.Default.ChevronRight
                    )
                    
                    // Location Dropdown
                    var locationExpanded by remember { mutableStateOf(false) }
                    PremiumDropdown(
                        label = "Location",
                        value = location.ifBlank { "Select Location" },
                        expanded = locationExpanded,
                        onExpandedChange = { locationExpanded = it },
                        options = locationOptions,
                        onOptionSelected = { 
                            location = it
                            locationExpanded = false
                        },
                        leadingIcon = Icons.Default.LocationOn
                    )
                    
                    // Animal Type Dropdown
                    var itemNameExpanded by remember { mutableStateOf(false) }
                    PremiumDropdown(
                        label = "Animal",
                        value = itemName.ifBlank { "Select Animal" },
                        expanded = itemNameExpanded,
                        onExpandedChange = { itemNameExpanded = it },
                        options = animalOptions,
                        onOptionSelected = { 
                            itemName = it
                            itemNameExpanded = false
                        },
                        leadingIcon = Icons.Default.Pets
                    )
                    
                    // Quantity
                    PremiumTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter { c -> c.isDigit() } },
                        label = "Count",
                        placeholder = "1",
                        leadingIcon = Icons.Default.Numbers,
                        keyboardType = KeyboardType.Number
                    )
                }
            }
            
            // Section 2: Purchase Details Card
            PremiumCard(title = "Purchase Details", icon = Icons.Default.ShoppingCart) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PremiumTextField(
                        value = buyingAmount,
                        onValueChange = { buyingAmount = it.filter { c -> c.isDigit() || c == '.' } },
                        label = "Buying Amount",
                        placeholder = "Full amount paid",
                        prefix = "₹",
                        leadingIcon = Icons.Default.Payment,
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }
            
            // Section 3: Selling Details Card
            PremiumCard(title = "Selling Details", icon = Icons.Default.Sell) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PremiumTextField(
                            value = weight,
                            onValueChange = { weight = it.filter { c -> c.isDigit() || c == '.' } },
                            label = "Weight",
                            placeholder = "0",
                            suffix = "kg",
                            leadingIcon = Icons.Default.Scale,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                        PremiumTextField(
                            value = rate,
                            onValueChange = { rate = it.filter { c -> c.isDigit() || c == '.' } },
                            label = "Rate/kg",
                            placeholder = "0",
                            prefix = "₹",
                            leadingIcon = Icons.Default.CurrencyRupee,
                            keyboardType = KeyboardType.Decimal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    PremiumTextField(
                        value = extraBonus,
                        onValueChange = { extraBonus = it.filter { c -> c.isDigit() || c == '.' } },
                        label = "Extra Bonus",
                        placeholder = "0",
                        prefix = "₹",
                        leadingIcon = Icons.Default.CardGiftcard,
                        keyboardType = KeyboardType.Decimal
                    )
                }
            }
            
            // Section 4: Allana Specific (Conditional)
            AnimatedVisibility(
                visible = isAllanaSelected,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                PremiumCard(
                    title = "Allana/Al Qureshi Deductions", 
                    icon = Icons.Default.Remove,
                    accentColor = LossRed
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PremiumTextField(
                                value = weightLessFeePerBuffalo,
                                onValueChange = { weightLessFeePerBuffalo = it.filter { c -> c.isDigit() || c == '.' } },
                                label = "Weight Less",
                                placeholder = "0",
                                suffix = "kg",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                            PremiumTextField(
                                value = feePerBuffalo,
                                onValueChange = { feePerBuffalo = it.filter { c -> c.isDigit() || c == '.' } },
                                label = "Fee",
                                placeholder = "0",
                                prefix = "₹",
                                keyboardType = KeyboardType.Decimal,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        // Auto-calculated fields (read-only display)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SectionBg, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Net Weight", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("${String.format("%.1f", netWeightAfterLess)} kg", fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TDS (0.10%)", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text("₹${String.format("%.0f", tdsPerBuffalo)}", fontWeight = FontWeight.Bold, color = LossRed)
                            }
                        }
                    }
                }
            }
            
            // Section 5: Calculation Summary
            if (totalAmount > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Total Amount
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total Amount", fontWeight = FontWeight.Medium, color = TextSecondary)
                            Text(
                                "₹${String.format("%,.0f", totalAmount)}",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = GradientStart
                            )
                        }
                        
                        if (isAllanaSelected) {
                            Text(
                                "= (${String.format("%.1f", netWeightAfterLess)} kg × ₹${String.format("%.0f", rateValue)}) + ₹${String.format("%.0f", extraBonusValue)} - Fee - TDS",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                        
                        HorizontalDivider(color = BorderGray)
                        
                        // Profit/Loss
                        profit?.let { p ->
                            val isProfit = p >= 0
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isProfit) ProfitGreen.copy(alpha = 0.1f) else LossRed.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (isProfit) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = if (isProfit) ProfitGreen else LossRed
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        if (isProfit) "Profit" else "Loss",
                                        fontWeight = FontWeight.Bold,
                                        color = if (isProfit) ProfitGreen else LossRed
                                    )
                                }
                                Text(
                                    "₹${String.format("%,.0f", kotlin.math.abs(p))}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isProfit) ProfitGreen else LossRed
                                )
                            }
                        }
                    }
                }
            }
            
            // Section 6: Note
            PremiumCard(title = "Note", icon = Icons.Default.Notes) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Add optional note...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = BorderGray,
                        focusedBorderColor = GradientStart
                    ),
                    maxLines = 3
                )
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom bar
        }
    }
}

// Premium Card Component
@Composable
private fun PremiumCard(
    title: String,
    icon: ImageVector,
    accentColor: Color = GradientStart,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accentColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

// Premium TextField Component
@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    prefix: String? = null,
    suffix: String? = null,
    leadingIcon: ImageVector? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextSecondary.copy(alpha = 0.5f)) },
            prefix = prefix?.let { { Text(it, fontWeight = FontWeight.Bold) } },
            suffix = suffix?.let { { Text(it, color = TextSecondary) } },
            leadingIcon = leadingIcon?.let { { Icon(it, null, tint = GradientStart) } },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = BorderGray,
                focusedBorderColor = GradientStart,
                unfocusedContainerColor = SectionBg,
                focusedContainerColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true
        )
    }
}

// Premium Dropdown Component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PremiumDropdown(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    leadingIcon: ImageVector? = null
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = onExpandedChange
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = { },
                readOnly = true,
                leadingIcon = leadingIcon?.let { { Icon(it, null, tint = GradientStart) } },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = BorderGray,
                    focusedBorderColor = GradientStart,
                    unfocusedContainerColor = SectionBg,
                    focusedContainerColor = Color.White
                ),
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onOptionSelected(option) }
                    )
                }
            }
        }
    }
}

// Premium Field Component (for read-only clickable fields)
@Composable
private fun PremiumField(
    label: String,
    value: String,
    onClick: () -> Unit,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray),
            color = SectionBg
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                leadingIcon?.let {
                    Icon(it, null, tint = GradientStart, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    value,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium
                )
                trailingIcon?.let {
                    Icon(it, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
