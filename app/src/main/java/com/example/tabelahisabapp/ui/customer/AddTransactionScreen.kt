package com.example.tabelahisabapp.ui.customer

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.preferences.TransactionPreferences
import com.example.tabelahisabapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Simplified Add Transaction Screen
 * 
 * Design similar to Image 2 (the working customer ledger add screen):
 * - Clean header showing transaction type and amount
 * - Simple amount input
 * - Payment mode selection
 * - Save button
 * 
 * NOTE: The ViewModel's SavedStateHandle is automatically populated by Navigation
 * with route arguments like supplierId, customerId, initialType, etc.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel = hiltViewModel(),
    onTransactionAdded: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val transactionPreferences = remember { TransactionPreferences(context) }
    val focusRequester = remember { FocusRequester() }

    // These values come from the ViewModel's SavedStateHandle, which is automatically
    // populated by Navigation from route arguments
    val transactionContext = viewModel.transactionContext
    val initialType = viewModel.initialType
    
    // Determine if this is a "gave" (red) or "got" (green) transaction
    // This is now MUTABLE - will be updated from original transaction type when editing
    var isGaveTransaction by remember { mutableStateOf(initialType == "CREDIT") }
    
    // Transaction ID for edit mode
    val transactionId = viewModel.transactionId
    val isEditMode = transactionId != null && transactionId != 0

    // States
    var amount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var isSaving by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Date formatter
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    
    // Theme colors based on transaction type
    val primaryColor = if (isGaveTransaction) DangerRed else SuccessGreen
    val gradientColors = if (isGaveTransaction) {
        listOf(Color(0xFFEF4444), Color(0xFFDC2626))
    } else {
        listOf(Color(0xFF22C55E), Color(0xFF16A34A))
    }
    
    // Payment method preference
    val savedPaymentMethod by transactionPreferences.lastPaymentMethod.collectAsState(initial = "CASH")
    
    // Initialize payment method from saved preference
    LaunchedEffect(savedPaymentMethod) {
        if (!isEditMode) {
            paymentMethod = savedPaymentMethod
        }
    }

    // Load existing data if editing - PRESERVE ORIGINAL TRANSACTION DIRECTION
    LaunchedEffect(transactionId) {
        if (transactionId != null && transactionId != 0) {
            viewModel.getTransactionUiModel(transactionId)?.let { tx ->
                amount = tx.amount.toInt().toString()
                paymentMethod = tx.paymentMethod
                selectedDate = tx.date
                
                // CRITICAL: Preserve original transaction direction
                // DO NOT infer from balance - use the stored type
                isGaveTransaction = (tx.type == "CREDIT")
            }
        }
    }
    
    // Auto-focus amount field
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        focusRequester.requestFocus()
    }

    // Transaction label
    val transactionLabel = if (isGaveTransaction) "You gave" else "You got"
    val contextLabel = when (transactionContext) {
        "SUPPLIER" -> "Supplier"
        else -> "Customer"
    }

    // Observe save success/failure
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            onTransactionAdded()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header with gradient background
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.linearGradient(gradientColors)
                    )
                    .padding(16.dp)
            ) {
                Column {
                    // Back button and title
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onCancel,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$transactionLabel ₹ ${amount.ifEmpty { "0" }}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Amount Input Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Amount TextField with Rupee prefix
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.matches(Regex("^\\d*$"))) {
                                amount = newValue
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Start
                        ),
                        prefix = {
                            Text(
                                text = "₹ ",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryColor
                            )
                        },
                        placeholder = {
                            Text(
                                text = "Enter amount",
                                fontSize = 20.sp,
                                color = Color.Gray
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { keyboardController?.hide() }
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.LightGray,
                            cursorColor = primaryColor
                        )
                    )
                }
            }
            
            // Payment Mode Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Payment Mode",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Cash Option
                        PaymentModeCard(
                            label = "Cash",
                            icon = "💵",
                            isSelected = paymentMethod == "CASH",
                            color = primaryColor,
                            onClick = { paymentMethod = "CASH" },
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Bank Option
                        PaymentModeCard(
                            label = "Bank",
                            icon = "🏦",
                            isSelected = paymentMethod == "BANK",
                            color = primaryColor,
                            onClick = { paymentMethod = "BANK" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            
            // Date Selector Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Transaction Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = primaryColor
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                dateFormat.format(Date(selectedDate)),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                        }
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Save Button
            Button(
                onClick = {
                    if (!isSaving && amount.isNotBlank() && (amount.toIntOrNull() ?: 0) > 0) {
                        isSaving = true
                        // Save payment preference
                        scope.launch {
                            transactionPreferences.setPaymentMethod(paymentMethod)
                        }
                        // Save transaction
                        viewModel.saveTransaction(
                            type = if (isGaveTransaction) "CREDIT" else "DEBIT",
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            date = selectedDate,
                            note = null,
                            paymentMethod = paymentMethod,
                            voiceNotePath = null,
                            transactionId = transactionId
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                enabled = amount.isNotBlank() && (amount.toIntOrNull() ?: 0) > 0 && !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "SAVE",
                        fontSize = 18.sp,
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

@Composable
private fun PaymentModeCard(
    label: String,
    icon: String,
    isSelected: Boolean,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else Color.LightGray
        ),
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.1f) else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = if (isSelected) color else Color.Gray
            )
        }
    }
}
