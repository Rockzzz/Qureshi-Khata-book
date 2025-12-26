package com.example.tabelahisabapp.ui.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.ui.theme.*
import com.example.tabelahisabapp.ui.expense.expenseCategories
import kotlinx.coroutines.launch

/**
 * Voice Confirmation Screen - Redesigned for clear single-question confirmation
 * Focus: "Is this entry correct?"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceConfirmationScreen(
    viewModel: VoiceFlowViewModel = hiltViewModel(),
    onConfirmSuccess: () -> Unit,
    onEdit: () -> Unit,
    onCancel: () -> Unit
) {
    val parsedTransaction by viewModel.parsedTransaction.collectAsState()
    val voiceFlowState by viewModel.voiceFlowState.collectAsState()
    val scope = rememberCoroutineScope()
    
    val transaction = parsedTransaction
    
    // Show loading if transaction not available
    if (transaction == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(BackgroundGray),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    // Editable states
    var customerName by remember { mutableStateOf(transaction.customerName) }
    var amount by remember { mutableStateOf(transaction.amount.toString()) }
    var transactionType by remember { mutableStateOf(transaction.transactionType) }
    var paymentMethod by remember { mutableStateOf(transaction.paymentMethod) }
    var isExpense by remember { mutableStateOf(transaction.isExpense) }
    var description by remember { mutableStateOf(transaction.originalText) }
    
    // Detect if this party is a supplier (for purchase logic)
    val isSupplier by viewModel.isSupplier(customerName).collectAsState(initial = false)
    
    // Sync description when name or amount changes
    LaunchedEffect(customerName, amount, transactionType, isExpense) {
        // Update description to reflect edited values
        val amountVal = amount.toDoubleOrNull() ?: 0.0
        description = when {
            isExpense && transactionType == "EXPENSE" -> "$customerName - ₹$amount"
            isExpense && transactionType == "PURCHASE" -> "Purchase from $customerName - ₹$amount"
            transactionType == "DEBIT" -> "Received ₹$amount from $customerName"
            else -> "Paid ₹$amount to $customerName"
        }
    }
    
    // Edit section expanded state - expanded by default for easy access
    var isEditExpanded by remember { mutableStateOf(true) }
    
    // Customer suggestions from ViewModel
    val customerNames by viewModel.customerNames.collectAsState()
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    
    // Get display values
    val amountValue = amount.toDoubleOrNull() ?: 0.0
    
    // Get type display info
    val typeInfo = when {
        isExpense && transactionType == "EXPENSE" -> Triple("Expense", WarningOrange, "Daily expense")
        isExpense && transactionType == "PURCHASE" -> Triple("Purchase", PurchasePrimary, "Asset purchase")
        transactionType == "DEBIT" -> Triple("Received", SuccessGreen, "Money received")
        else -> Triple("Payment", DangerRed, "Money paid")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Confirm Entry",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Check once before saving",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.reset()
                        onCancel()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CardBackground
                )
            )
        },
        bottomBar = {
            // Sticky action buttons
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = CardBackground
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Primary Save Button
                    Button(
                        onClick = {
                            scope.launch {
                                val finalType = when {
                                    transactionType == "PURCHASE" -> "PURCHASE"
                                    isExpense -> "EXPENSE"
                                    else -> transactionType
                                }
                                val updatedTransaction = transaction.copy(
                                    customerName = customerName,
                                    amount = amount.toDoubleOrNull() ?: 0.0,
                                    transactionType = finalType,
                                    paymentMethod = paymentMethod,
                                    isExpense = isExpense
                                )
                                val success = viewModel.saveTransaction(updatedTransaction)
                                if (success) {
                                    onConfirmSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save Entry",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Discard text button
                    TextButton(
                        onClick = {
                            viewModel.reset()
                            onCancel()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Discard",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        containerColor = BackgroundGray
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: What You Said (Read-only)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = InfoBlue.copy(alpha = 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = InfoBlue,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "You said",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "\"${transaction.originalText}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                    }
                }
            }
            
            // Section 2: Summary Card (Main Focus)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardBackground,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Amount - Large
                    Text(
                        text = "₹${if (amountValue == amountValue.toLong().toDouble()) amountValue.toLong().toString() else amount}",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Type - Color coded
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = typeInfo.second.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = typeInfo.first,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            color = typeInfo.second,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Party Name
                    Text(
                        text = customerName,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Cash/Bank Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = BackgroundGray
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (paymentMethod == "CASH") "💵" else "🏦",
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (paymentMethod == "CASH") "Cash" else "Bank",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
            
            // Section 3: Quick Edit (Collapsed by default)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isEditExpanded = !isEditExpanded },
                shape = RoundedCornerShape(12.dp),
                color = CardBackground
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Need to change something?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Text(
                            text = "Edit details",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Purple600,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        if (isEditExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isEditExpanded) "Collapse" else "Expand",
                        tint = Purple600
                    )
                }
            }
            
            // Expanded Edit Fields
            AnimatedVisibility(
                visible = isEditExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = CardBackground
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Party/Category Label - changes based on transaction type
                        val isExpenseCategory = isExpense && transactionType == "EXPENSE"
                        
                        Text(
                            text = if (isExpenseCategory) "Expense Category" else "Party",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        ExposedDropdownMenuBox(
                            expanded = customerDropdownExpanded,
                            onExpandedChange = { customerDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { customerName = it },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                placeholder = { Text(if (isExpenseCategory) "Select category" else "Enter name") },
                                leadingIcon = {
                                    Icon(
                                        if (isExpenseCategory) Icons.Default.Category else Icons.Default.Person, 
                                        null, 
                                        tint = if (isExpenseCategory) WarningOrange else Purple600
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { customerDropdownExpanded = !customerDropdownExpanded }) {
                                        Icon(
                                            if (customerDropdownExpanded) Icons.Default.KeyboardArrowUp 
                                            else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            
                            ExposedDropdownMenu(
                                expanded = customerDropdownExpanded,
                                onDismissRequest = { customerDropdownExpanded = false }
                            ) {
                                if (isExpenseCategory) {
                                    // Show expense categories from AddExpenseScreen
                                    expenseCategories.forEach { category ->
                                        DropdownMenuItem(
                                            text = { Text(category.name) },
                                            onClick = {
                                                customerName = category.name
                                                customerDropdownExpanded = false
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    category.icon, 
                                                    null, 
                                                    tint = category.color
                                                )
                                            }
                                        )
                                    }
                                } else {
                                    // Show customer names for non-expense transactions
                                    val filteredNames = if (customerName.isBlank() || customerName == transaction.customerName) {
                                        customerNames.take(10)
                                    } else {
                                        customerNames.filter { 
                                            it.contains(customerName, ignoreCase = true) 
                                        }.take(10)
                                    }
                                    
                                    if (filteredNames.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("+ Create \"$customerName\"", color = SuccessGreen) },
                                            onClick = { customerDropdownExpanded = false }
                                        )
                                    } else {
                                        filteredNames.forEach { name ->
                                            DropdownMenuItem(
                                                text = { Text(name) },
                                                onClick = {
                                                    customerName = name
                                                    customerDropdownExpanded = false
                                                },
                                                leadingIcon = {
                                                    Icon(Icons.Default.Person, null, tint = Purple600)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Amount
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Enter amount") },
                            prefix = { Text("₹ ", fontWeight = FontWeight.Bold, color = TextSecondary) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        
                        // Type Selector - Only show for Customer/Supplier transactions, NOT for expenses
                        // Detect if this is a customer/supplier transaction
                        val isCustomerSupplier = customerName.isNotBlank() && !isExpense && transactionType != "EXPENSE"
                        
                        // Only show transaction type for customer/supplier transactions
                        if (isCustomerSupplier) {
                            Text(
                                text = "Transaction Type",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextSecondary
                            )
                            
                            // Show simplified You Got / You Gave buttons for customer/supplier
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // You Got (Money Received - DEBIT, or PURCHASE for suppliers)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clickable {
                                            if (isSupplier) {
                                                // Supplier: You Got = Purchase (goods received)
                                                transactionType = "PURCHASE"
                                                isExpense = true
                                            } else {
                                                // Customer: You Got = Money received
                                                transactionType = "DEBIT"
                                                isExpense = false
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (transactionType == "DEBIT" && !isExpense) SuccessGreen else BackgroundGray
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⬇️", fontSize = 18.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "You Got",
                                            fontWeight = FontWeight.Bold,
                                            color = if (transactionType == "DEBIT" && !isExpense) Color.White else TextPrimary,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                                
                                // You Gave (Money Paid - CREDIT)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clickable {
                                            transactionType = "CREDIT"
                                            isExpense = false
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (transactionType == "CREDIT" && !isExpense) DangerRed else BackgroundGray
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("⬆️", fontSize = 18.sp)
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "You Gave",
                                            fontWeight = FontWeight.Bold,
                                            color = if (transactionType == "CREDIT" && !isExpense) Color.White else TextPrimary,
                                            fontSize = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                        // For expenses: No transaction type selector needed - expense is fixed
                        
                        // Cash/Bank Toggle
                        Text(
                            text = "Payment Mode",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable { paymentMethod = "CASH" },
                                shape = RoundedCornerShape(24.dp),
                                color = if (paymentMethod == "CASH") Purple600 else BackgroundGray
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("💵", fontSize = 16.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Cash",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (paymentMethod == "CASH") Color.White else TextPrimary
                                    )
                                }
                            }
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clickable { paymentMethod = "BANK" },
                                shape = RoundedCornerShape(24.dp),
                                color = if (paymentMethod == "BANK") Purple600 else BackgroundGray
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("🏦", fontSize = 16.sp)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Bank",
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (paymentMethod == "BANK") Color.White else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TypeChip(
    label: String,
    selected: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(44.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (selected) color else BackgroundGray
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color.White else TextPrimary
            )
        }
    }
}
