package com.example.tabelahisabapp.ui.customer

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import com.example.tabelahisabapp.data.preferences.TransactionPreferences
import com.example.tabelahisabapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    viewModel: AddTransactionViewModel = hiltViewModel(),
    savedStateHandle: SavedStateHandle? = null,
    onTransactionAdded: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val transactionPreferences = remember { TransactionPreferences(context) }
    val focusRequester = remember { FocusRequester() }
    
    val transactionId = savedStateHandle?.get<Int>("transactionId")
    val isEditMode = transactionId != null
    
    // Transaction type options
    val transactionTypes = listOf("Received", "Expense", "Payment", "Purchase")
    var selectedTypeIndex by remember { mutableStateOf(1) } // Default: Expense
    
    // Payment method with preference
    val savedPaymentMethod by transactionPreferences.lastPaymentMethod.collectAsState(initial = "CASH")
    var paymentMethod by remember { mutableStateOf("CASH") }
    
    // Other states
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(System.currentTimeMillis()) }
    var description by remember { mutableStateOf("") }
    var originalCreatedAt by remember { mutableStateOf<Long?>(null) }
    
    val calendar = Calendar.getInstance()
    
    // Initialize payment method from saved preference
    LaunchedEffect(savedPaymentMethod) {
        if (!isEditMode) {
            paymentMethod = savedPaymentMethod
        }
    }

    // Load transaction data when editing
    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            scope.launch {
                viewModel.getTransaction(transactionId)?.let { transaction ->
                    // Map transaction type to index
                    selectedTypeIndex = when (transaction.type) {
                        "DEBIT" -> 0  // Received
                        "EXPENSE" -> 1
                        "CREDIT" -> 2  // Payment
                        "PURCHASE" -> 3
                        else -> 1
                    }
                    amount = transaction.amount.toString()
                    date = transaction.date
                    description = transaction.note ?: ""
                    paymentMethod = transaction.paymentMethod
                    originalCreatedAt = transaction.createdAt
                }
            }
        }
    }
    
    // Auto-focus amount field
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        focusRequester.requestFocus()
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.set(year, month, dayOfMonth)
            date = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    // Get transaction type string for saving
    fun getTransactionTypeString(): String {
        return when (selectedTypeIndex) {
            0 -> "DEBIT"    // Received
            1 -> "EXPENSE"
            2 -> "CREDIT"   // Payment
            3 -> "PURCHASE"
            else -> "EXPENSE"
        }
    }
    
    // Get helper text based on selection
    fun getHelperText(): String {
        return when (selectedTypeIndex) {
            0 -> "Money received from customer"
            1 -> "Daily expenses like food, gas, travel"
            2 -> "Money paid to customer or seller"
            3 -> "Asset or livestock purchase"
            else -> ""
        }
    }
    
    // Get color for selected type
    fun getTypeColor(): Color {
        return when (selectedTypeIndex) {
            0 -> SuccessGreen       // Received
            1 -> WarningOrange      // Expense
            2 -> DangerRed          // Payment
            3 -> PurchasePrimary    // Purchase
            else -> Purple600
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isEditMode) "Edit Transaction" else "Add Transaction",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "This entry will be added to today's records",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground
                )
            )
        },
        bottomBar = {
            // Sticky Save Button
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = CardBackground
            ) {
                Button(
                    onClick = {
                        // Save payment method preference
                        scope.launch {
                            transactionPreferences.setPaymentMethod(paymentMethod)
                        }
                        viewModel.saveTransaction(
                            type = getTransactionTypeString(),
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            date = date,
                            note = description.ifBlank { null },
                            paymentMethod = paymentMethod,
                            voiceNotePath = null,
                            originalCreatedAt = originalCreatedAt
                        )
                        onTransactionAdded()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    enabled = amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple600,
                        disabledContainerColor = BorderGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Save Transaction",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // 1. Transaction Type Segmented Control
            Text(
                text = "Transaction Type",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            // Custom Segmented Control Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                transactionTypes.forEachIndexed { index, label ->
                    val isSelected = selectedTypeIndex == index
                    val shape = when (index) {
                        0 -> RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                        transactionTypes.lastIndex -> RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                        else -> RoundedCornerShape(0.dp)
                    }
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { selectedTypeIndex = index },
                        shape = shape,
                        color = if (isSelected) getTypeColor() else BackgroundGray,
                        tonalElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextPrimary
                            )
                        }
                    }
                }
            }
            
            // Helper text
            Text(
                text = getHelperText(),
                style = MaterialTheme.typography.bodySmall,
                color = getTypeColor(),
                modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
            )
            
            // 2. Amount Input (Primary Focus)
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = amount,
                onValueChange = { newValue ->
                    // Only allow numbers and decimal point
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                        amount = newValue
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                ),
                prefix = {
                    Text(
                        text = "₹ ",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                },
                placeholder = {
                    Text(
                        text = "0",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BorderGray
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple600,
                    unfocusedBorderColor = BorderGray
                )
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 3. Cash/Bank Toggle (Compact Pill Style)
            Text(
                text = "Source",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cash Pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { paymentMethod = "CASH" },
                    shape = RoundedCornerShape(24.dp),
                    color = if (paymentMethod == "CASH") Purple600 else BackgroundGray,
                    tonalElevation = if (paymentMethod == "CASH") 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💵",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cash",
                            fontWeight = FontWeight.SemiBold,
                            color = if (paymentMethod == "CASH") Color.White else TextPrimary
                        )
                    }
                }
                
                // Bank Pill
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { paymentMethod = "BANK" },
                    shape = RoundedCornerShape(24.dp),
                    color = if (paymentMethod == "BANK") Purple600 else BackgroundGray,
                    tonalElevation = if (paymentMethod == "BANK") 2.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🏦",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Bank",
                            fontWeight = FontWeight.SemiBold,
                            color = if (paymentMethod == "BANK") Color.White else TextPrimary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 4. Date Picker (Compact Row)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { datePickerDialog.show() },
                shape = RoundedCornerShape(12.dp),
                color = BackgroundGray
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Date",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Date:",
                            color = TextSecondary
                        )
                    }
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(date)),
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 5. Description (Optional)
            Text(
                text = "Description (optional)",
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Milk payment, Diesel, Home expense...",
                        color = BorderGray
                    )
                },
                maxLines = 3,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple600,
                    unfocusedBorderColor = BorderGray
                )
            )
            
            // Bottom spacing for scroll
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
