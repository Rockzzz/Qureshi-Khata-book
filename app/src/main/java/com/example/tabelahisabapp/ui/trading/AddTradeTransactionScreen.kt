package com.example.tabelahisabapp.ui.trading

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTradeTransactionScreen(
    viewModel: TradingViewModel = hiltViewModel(),
    savedStateHandle: SavedStateHandle? = null,
    farmId: Int? = null, // When adding from inside a farm
    onTransactionSaved: () -> Unit,
    onCancel: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val transactionId = savedStateHandle?.get<Int>("transactionId")
    val isEditMode = transactionId != null
    
    // Get farm info if farmId provided
    val allFarms by viewModel.allFarms.collectAsState()
    val selectedFarm = farmId?.let { id -> allFarms.find { it.id == id } }
    
    // Removed type - all transactions are the same now
    var location by remember { mutableStateOf("") } // Location: "Deonar" or "Allana"
    var itemName by remember { mutableStateOf("") } // Buffalo/Item name
    var quantity by remember { mutableStateOf("") } // Number of buffalo
    var buyingAmount by remember { mutableStateOf("") } // Full buying amount for buffalo
    var weight by remember { mutableStateOf("") } // Weight in kg
    var rate by remember { mutableStateOf("") } // Rate per kg
    var extraBonus by remember { mutableStateOf("") } // Extra bonus amount
    // Allana-specific fields
    var weightLessFeePerBuffalo by remember { mutableStateOf("") } // Weight less fee per buffalo
    var feePerBuffalo by remember { mutableStateOf("") } // Fee per buffalo (new field)
    // netWeightAfterLess and tdsPerBuffalo are auto-calculated
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var note by remember { mutableStateOf("") }
    var originalCreatedAt by remember { mutableStateOf<Long?>(null) }
    
    val allTrades by viewModel.allTrades.collectAsState()
    val uniqueItems = remember(allTrades) {
        allTrades.map { it.itemName }.distinct().sorted()
    }
    
    val locationOptions = listOf("Deonar", "Allana")
    val isAllanaSelected = location == "Allana"

    // Load transaction data when editing
    LaunchedEffect(transactionId, allTrades) {
        if (transactionId != null && allTrades.isNotEmpty()) {
            allTrades.find { it.id == transactionId }?.let { trade ->
                location = trade.deonar ?: ""
                itemName = trade.itemName
                quantity = trade.quantity.toString()
                buyingAmount = trade.buyRate.toString() // Use buyRate as buyingAmount
                weight = trade.weight?.toString() ?: ""
                rate = trade.rate?.toString() ?: ""
                extraBonus = trade.extraBonus?.toString() ?: ""
                // Note: Allana-specific fields would need to be stored in note or new fields
                date = trade.date
                note = trade.note ?: ""
                originalCreatedAt = trade.createdAt
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
    
    // Total = (Weight * Rate) + Extra Bonus
    val totalAmount = (weightValue * rateValue) + extraBonusValue
    
    // Allana-specific auto-calculations
    // Net Weight = Weight - Weight Less Fee (auto-calculated)
    val netWeightAfterLess = if (isAllanaSelected && weightValue > 0) {
        weightValue - weightLessFeeValue
    } else {
        0.0
    }
    
    // TDS = 0.10% of Total Amount (auto-calculated)
    val tdsPerBuffalo = if (isAllanaSelected && totalAmount > 0) {
        totalAmount * 0.001 // 0.10% = 0.001
    } else {
        0.0
    }
    
    // Profit = Total - Buying Amount (automatically calculated)
    val profit = if (totalAmount > 0 && buyingAmountValue > 0) {
        totalAmount - buyingAmountValue
    } else {
        null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (isEditMode) "Edit Trade" else "Add Trade")
                        selectedFarm?.let {
                            Text(
                                text = "Farm: ${it.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Date
            OutlinedTextField(
                value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(date)),
                onValueChange = { },
                label = { Text("Date") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { datePickerDialog.show() }) {
                        Icon(Icons.Default.CalendarToday, contentDescription = "Select Date")
                    }
                }
            )

            // Location (Deonar or Allana)
            var locationExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = locationExpanded,
                onExpandedChange = { locationExpanded = it }
            ) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { },
                    label = { Text("Location") },
                    placeholder = { Text("Select Location") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .clickable { locationExpanded = true },
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = locationExpanded,
                    onDismissRequest = { locationExpanded = false }
                ) {
                    locationOptions.forEach { loc ->
                        DropdownMenuItem(
                            text = { Text(loc) },
                            onClick = {
                                location = loc
                                locationExpanded = false
                            }
                        )
                    }
                }
            }

            // Item Name (Buffalo/Goat) - Fixed Dropdown
            val animalOptions = listOf("Buffalo", "Goat", "Other")
            var itemNameExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = itemNameExpanded,
                onExpandedChange = { itemNameExpanded = it }
            ) {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { },
                    label = { Text("Item Name") },
                    placeholder = { Text("Select Animal") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                        .clickable { itemNameExpanded = true },
                    readOnly = true,
                    singleLine = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = itemNameExpanded)
                    }
                )
                ExposedDropdownMenu(
                    expanded = itemNameExpanded,
                    onDismissRequest = { itemNameExpanded = false }
                ) {
                    animalOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                itemName = option
                                itemNameExpanded = false
                            }
                        )
                    }
                }
            }

            // Quantity (Buffalo count)
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Count (Quantity)") },
                placeholder = { Text("1") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            // Buying Amount (full amount paid for buffalo)
            OutlinedTextField(
                value = buyingAmount,
                onValueChange = { buyingAmount = it },
                label = { Text("Buying Amount") },
                placeholder = { Text("Full amount paid") },
                prefix = { Text("₹") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Weight
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                suffix = { Text("kg") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Rate (per kg)
            OutlinedTextField(
                value = rate,
                onValueChange = { rate = it },
                label = { Text("Rate (per kg)") },
                prefix = { Text("₹") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Extra Bonus
            OutlinedTextField(
                value = extraBonus,
                onValueChange = { extraBonus = it },
                label = { Text("Extra Bonus") },
                prefix = { Text("₹") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Allana-specific fields
            if (isAllanaSelected) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Allana Specific Fields", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                
                // Weight Less Fee Per Buffalo
                OutlinedTextField(
                    value = weightLessFeePerBuffalo,
                    onValueChange = { weightLessFeePerBuffalo = it },
                    label = { Text("Weight Less Fee (per buffalo)") },
                    suffix = { Text("kg") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                
                // Fee Per Buffalo (new field)
                OutlinedTextField(
                    value = feePerBuffalo,
                    onValueChange = { feePerBuffalo = it },
                    label = { Text("Fee (per buffalo)") },
                    prefix = { Text("₹") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                
                // Net Weight After Less (Auto-calculated)
                OutlinedTextField(
                    value = String.format("%.2f", netWeightAfterLess),
                    onValueChange = { }, // Read-only
                    label = { Text("Net Weight After Less (Auto)") },
                    suffix = { Text("kg") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
                
                // TDS 0.10% Per Buffalo
                OutlinedTextField(
                    value = String.format("%.2f", tdsPerBuffalo),
                    onValueChange = { }, // Read-only
                    label = { Text("TDS 0.10% (Auto)") },
                    prefix = { Text("₹") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }

            Divider()

            // Calculated Total
            val indianFormat = java.text.NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            indianFormat.format(totalAmount),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        "= (Weight × Rate) + Bonus",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Profit (automatically calculated)
            if (profit != null) {
                val isProfit = profit >= 0
                val formattedProfit = indianFormat.format(profit)
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProfit) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(if (isProfit) "Profit:" else "Loss:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                formattedProfit,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isProfit) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                        Text(
                            "= Total - Buying Amount",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("Note (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Save button
            Button(
                onClick = {
                    scope.launch {
                        viewModel.saveTrade(
                            type = "TRADE", // Default type for all transactions
                            deonar = location.ifBlank { null },
                            itemName = itemName.trim(),
                            quantity = quantityValue,
                            buyRate = buyingAmountValue, // Use buyingAmount as buyRate
                            weight = weightValue.takeIf { it > 0 },
                            rate = rateValue.takeIf { it > 0 },
                            extraBonus = extraBonusValue.takeIf { it > 0 },
                            totalAmount = totalAmount,
                            profit = profit,
                            pricePerUnit = buyingAmountValue, // Keep for backward compatibility
                            date = date,
                            note = note.ifBlank { null },
                            transactionId = transactionId,
                            originalCreatedAt = originalCreatedAt,
                            farmId = farmId,
                            entryNumber = null // Auto-generated if needed
                        )
                        onTransactionSaved()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = itemName.isNotBlank() && quantityValue > 0 && buyingAmountValue > 0
            ) {
                Text("Save Transaction")
            }

            // Cancel button
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cancel")
            }
        }
    }
}
