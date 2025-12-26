package com.example.tabelahisabapp.ui.supplier

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Supplier Transaction Types:
 * 
 * PURCHASE (Khareedari):
 * - Creates liability (increases what you owe)
 * - NO cash movement (credit purchase)
 * - Records in CustomerTransaction as DEBIT (for supplier, DEBIT = you owe more)
 * 
 * PAYMENT (Diya):
 * - Reduces liability (decreases what you owe)
 * - CASH/BANK movement OUT
 * - Records in CustomerTransaction as CREDIT
 * - Syncs to DailyLedger as CASH_OUT/BANK_OUT
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSupplierTransactionScreen(
    viewModel: SupplierTransactionViewModel = hiltViewModel(),
    supplierId: Int,
    supplierName: String,
    editTransactionId: Int? = null,
    onTransactionSaved: () -> Unit,
    onCancel: () -> Unit
) {
    // Transaction type: PURCHASE or PAYMENT
    var transactionType by remember { mutableStateOf("PURCHASE") }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("CASH") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Get theme colors
    val extendedColors = AppTheme.colors
    val isDark = AppTheme.isDark
    
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    // Colors for transaction types
    val purchaseColor = Color(0xFFF59E0B) // Amber - Purchase/Liability
    val paymentColor = Color(0xFF22C55E) // Green - Payment

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(
                            colors = if (transactionType == "PURCHASE") 
                                listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                            else 
                                listOf(Color(0xFF22C55E), Color(0xFF16A34A))
                        )
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onCancel) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color.White
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (transactionType == "PURCHASE") "Add Purchase" else "Add Payment",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = supplierName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        },
        containerColor = if (isDark) DarkBackground else BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Transaction Type Toggle
            Text(
                "Transaction Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = extendedColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Purchase Button
                TransactionTypeButton(
                    label = "Purchase",
                    subtitle = "Khareedari",
                    icon = Icons.Default.ShoppingCart,
                    isSelected = transactionType == "PURCHASE",
                    color = purchaseColor,
                    onClick = { transactionType = "PURCHASE" },
                    modifier = Modifier.weight(1f)
                )
                
                // Payment Button
                TransactionTypeButton(
                    label = "Payment",
                    subtitle = "Diya",
                    icon = Icons.Default.Payment,
                    isSelected = transactionType == "PAYMENT",
                    color = paymentColor,
                    onClick = { transactionType = "PAYMENT" },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Explanation card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (transactionType == "PURCHASE") 
                        purchaseColor.copy(alpha = 0.1f) 
                    else 
                        paymentColor.copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (transactionType == "PURCHASE") Icons.Default.Info else Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (transactionType == "PURCHASE") purchaseColor else paymentColor
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (transactionType == "PURCHASE") 
                            "Creates liability - No cash movement. Amount will be added to what you owe."
                        else 
                            "Reduces liability - Cash/Bank outflow. Amount will be deducted from what you owe.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (transactionType == "PURCHASE") purchaseColor else paymentColor
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Amount Input
            Text(
                "Amount",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = extendedColors.textPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                placeholder = { Text("Enter amount") },
                leadingIcon = { 
                    Text(
                        "₹",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.textPrimary
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = extendedColors.cardBackground,
                    focusedContainerColor = extendedColors.cardBackground,
                    unfocusedBorderColor = BorderGray,
                    focusedBorderColor = if (transactionType == "PURCHASE") purchaseColor else paymentColor
                ),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            
            // Payment Method (only for PAYMENT type)
            if (transactionType == "PAYMENT") {
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    "Payment Method",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = extendedColors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PaymentMethodCard(
                        label = "Cash",
                        icon = Icons.Default.Money,
                        isSelected = selectedPaymentMethod == "CASH",
                        onClick = { selectedPaymentMethod = "CASH" },
                        modifier = Modifier.weight(1f)
                    )
                    PaymentMethodCard(
                        label = "Bank",
                        icon = Icons.Default.AccountBalance,
                        isSelected = selectedPaymentMethod == "BANK",
                        onClick = { selectedPaymentMethod = "BANK" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Date Selector
            OutlinedCard(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = extendedColors.textSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            dateFormat.format(Date(selectedDate)),
                            style = MaterialTheme.typography.bodyLarge,
                            color = extendedColors.textPrimary
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = extendedColors.textSecondary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Note
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Add note (optional)") },
                leadingIcon = { 
                    Icon(
                        Icons.Default.Notes,
                        contentDescription = null,
                        tint = extendedColors.textSecondary
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = extendedColors.cardBackground,
                    focusedContainerColor = extendedColors.cardBackground,
                    unfocusedBorderColor = BorderGray,
                    focusedBorderColor = if (transactionType == "PURCHASE") purchaseColor else paymentColor
                )
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save Button
            Button(
                onClick = {
                    if (amount.isNotBlank()) {
                        isLoading = true
                        viewModel.saveSupplierTransaction(
                            supplierId = supplierId,
                            supplierName = supplierName,
                            transactionType = transactionType,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            date = selectedDate,
                            paymentMethod = if (transactionType == "PAYMENT") selectedPaymentMethod else null,
                            note = note.ifBlank { null }
                        )
                        onTransactionSaved()
                    }
                },
                enabled = amount.isNotBlank() && !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (transactionType == "PURCHASE") purchaseColor else paymentColor,
                    disabledContainerColor = BorderGray
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (transactionType == "PURCHASE") "Record Purchase" else "Record Payment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
    
    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Transaction type selection button
 */
@Composable
private fun TransactionTypeButton(
    label: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = AppTheme.colors
    
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.15f) else extendedColors.cardBackground
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isSelected) 2.dp else 1.dp,
            brush = Brush.linearGradient(
                if (isSelected) listOf(color, color) else listOf(BorderGray, BorderGray)
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) color else extendedColors.textSecondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) color else extendedColors.textPrimary
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSelected) color else extendedColors.textSecondary
            )
        }
    }
}

/**
 * Payment method card
 */
@Composable
private fun PaymentMethodCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extendedColors = AppTheme.colors
    val selectedColor = Color(0xFF22C55E)
    
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) selectedColor.copy(alpha = 0.1f) else extendedColors.cardBackground
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            width = if (isSelected) 2.dp else 1.dp,
            brush = Brush.linearGradient(
                if (isSelected) listOf(selectedColor, selectedColor) else listOf(BorderGray, BorderGray)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) selectedColor else extendedColors.textSecondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) selectedColor else extendedColors.textSecondary
            )
        }
    }
}
