package com.example.tabelahisabapp.ui.daily

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction
import com.example.tabelahisabapp.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Transaction categories for the four-section ledger
 */
enum class TransactionCategory {
    MONEY_RECEIVED,    // Paisa Aaya (Credit) - Green
    DAILY_EXPENSE,     // Daily Kharcha (Debit) - Orange
    PURCHASE,          // Purchase/Buffalo (Debit) - Purple
    PAYMENT_GIVEN      // Payment (Debit) - Red
}

/**
 * Auto-categorize a transaction based on mode, amount, party, note, and sourceType
 * Rules:
 * - sourceType = "expense": Always DAILY_EXPENSE (regardless of amount)
 * - sourceType = "supplier" + PURCHASE mode: PURCHASE category
 * - sourceType = "supplier" + CASH/BANK_OUT: PAYMENT_GIVEN (supplier payment)
 * - PURCHASE mode: Always goes to Purchase category
 * - MONEY_RECEIVED: Any CASH_IN or BANK_IN transaction
 * - DAILY_EXPENSE: Small amounts (< ₹5,000) regardless of party
 * - PURCHASE: Large amounts (≥ ₹5,000) without party OR with purchase keywords
 * - PAYMENT_GIVEN: Large amounts (≥ ₹5,000) with a party/customer name
 */
fun categorizeTransaction(
    mode: String,
    amount: Double,
    party: String?,
    note: String?,
    sourceType: String? = null
): TransactionCategory {
    // First check sourceType for explicit categorization
    when (sourceType) {
        "expense" -> return TransactionCategory.DAILY_EXPENSE // Expenses always go to Kharcha
        "supplier" -> {
            // Supplier purchase = PURCHASE, supplier payment = PAYMENT_GIVEN
            return if (mode == "PURCHASE") {
                TransactionCategory.PURCHASE
            } else {
                TransactionCategory.PAYMENT_GIVEN
            }
        }
    }
    
    // Check for purchase keywords
    val hasPurchaseKeyword = note?.contains("buffalo", ignoreCase = true) == true ||
                             note?.contains("purchase", ignoreCase = true) == true ||
                             note?.contains("bakri", ignoreCase = true) == true ||
                             note?.contains("khareedari", ignoreCase = true) == true ||
                             note?.contains("goat", ignoreCase = true) == true ||
                             note?.contains("cow", ignoreCase = true) == true
    
    return when {
        // PURCHASE mode (from voice purchase) = Always Purchase category
        mode == "PURCHASE" -> TransactionCategory.PURCHASE
        
        // Money IN = Always Money Received
        mode == "CASH_IN" || mode == "BANK_IN" -> TransactionCategory.MONEY_RECEIVED
        
        // Purchase keywords = Purchase category (regardless of amount)
        hasPurchaseKeyword -> TransactionCategory.PURCHASE
        
        // Small amounts (< ₹5,000) = Daily Expense (even if party exists)
        amount < 5000 -> TransactionCategory.DAILY_EXPENSE
        
        // Large amounts (≥ ₹5,000) with party = Payment Given
        !party.isNullOrBlank() -> TransactionCategory.PAYMENT_GIVEN
        
        // Large amounts without party = Purchase
        else -> TransactionCategory.PURCHASE
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyEntryScreen(
    viewModel: DailySummaryViewModel = hiltViewModel(),
    editDate: Long? = null,
    onEntrySaved: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedDate by remember { mutableStateOf(editDate ?: viewModel.getTodayDateNormalized()) }
    val normalizedDate = viewModel.normalizeDateToMidnight(selectedDate)
    
    var openingCash by remember { mutableStateOf(0.0) }
    var openingBank by remember { mutableStateOf(0.0) }
    var showEditOpeningDialog by remember { mutableStateOf(false) }
    
    val previousBalance by viewModel.getPreviousDayBalance(normalizedDate).collectAsState(initial = null)
    
    LaunchedEffect(previousBalance, normalizedDate) {
        previousBalance?.let {
            openingCash = it.closingCash
            openingBank = it.closingBank
        } ?: run {
            openingCash = 0.0
            openingBank = 0.0
        }
    }
    
    val existingEntry by viewModel.getBalanceByDate(normalizedDate).collectAsState(initial = null)
    val existingTransactions by viewModel.getLedgerTransactionsByDate(normalizedDate).collectAsState(initial = emptyList())
    
    LaunchedEffect(existingEntry) {
        existingEntry?.let {
            openingCash = it.openingCash
            openingBank = it.openingBank
        }
    }
    
    var transactions by remember { mutableStateOf<List<DailyLedgerTransaction>>(emptyList()) }
    
    LaunchedEffect(existingTransactions) {
        transactions = existingTransactions
    }
    
    val categorizedTransactions = remember(transactions) {
        transactions.groupBy { tx -> categorizeTransaction(tx.mode, tx.amount, tx.party, tx.note, tx.sourceType) }
    }
    
    val moneyReceivedTotal = categorizedTransactions[TransactionCategory.MONEY_RECEIVED]?.sumOf { it.amount } ?: 0.0
    val dailyExpenseTotal = categorizedTransactions[TransactionCategory.DAILY_EXPENSE]?.sumOf { it.amount } ?: 0.0
    val purchaseTotal = categorizedTransactions[TransactionCategory.PURCHASE]?.sumOf { it.amount } ?: 0.0
    val paymentTotal = categorizedTransactions[TransactionCategory.PAYMENT_GIVEN]?.sumOf { it.amount } ?: 0.0
    
    var moneyReceivedExpanded by remember { mutableStateOf(true) }
    var dailyExpenseExpanded by remember { mutableStateOf(true) }
    var purchaseExpanded by remember { mutableStateOf(true) }
    var paymentExpanded by remember { mutableStateOf(true) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var addingToCategory by remember { mutableStateOf<TransactionCategory?>(null) }
    var editingTransaction by remember { mutableStateOf<DailyLedgerTransaction?>(null) }
    
    val calendar = Calendar.getInstance().apply { timeInMillis = selectedDate }
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, day ->
            calendar.set(year, month, day, 0, 0, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            selectedDate = calendar.timeInMillis
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )
    
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color(0xFF667EEA), Color(0xFF764BA2))
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Rozana Hisab",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(selectedDate)),
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 15.sp
                        )
                    }
                    
                    // Date picker button
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { datePickerDialog.show() },
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.CalendarToday, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
            
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Opening Balance Card - Premium design
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                            .clickable { showEditOpeningDialog = true },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Purple600.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.AccountBalance, null, tint = Purple600, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Subah Ka Balance", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                                }
                                Icon(Icons.Default.Edit, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Cash", fontSize = 12.sp, color = TextSecondary)
                                    Text(
                                        "₹${String.format("%,.0f", openingCash)}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (openingCash >= 0) SuccessGreen else DangerRed
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                    Text("Bank", fontSize = 12.sp, color = TextSecondary)
                                    Text(
                                        "₹${String.format("%,.0f", openingBank)}",
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (openingBank >= 0) SuccessGreen else DangerRed
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Section 1: Paisa Aaya (Money Received) - GREEN
                item {
                    PremiumLedgerSection(
                        title = "💰 Paisa Aaya",
                        total = moneyReceivedTotal,
                        headerColor = Color(0xFF10B981),
                        accentColor = Color(0xFF059669),
                        isExpanded = moneyReceivedExpanded,
                        onToggle = { moneyReceivedExpanded = !moneyReceivedExpanded },
                        transactions = categorizedTransactions[TransactionCategory.MONEY_RECEIVED] ?: emptyList(),
                        onAddClick = {
                            addingToCategory = TransactionCategory.MONEY_RECEIVED
                            editingTransaction = null
                            showAddDialog = true
                        },
                        onEditClick = { tx ->
                            addingToCategory = TransactionCategory.MONEY_RECEIVED
                            editingTransaction = tx
                            showAddDialog = true
                        },
                        onDeleteClick = { tx ->
                            scope.launch {
                                viewModel.deleteLedgerTransaction(tx)
                                transactions = transactions.filter { it.id != tx.id }
                            }
                        },
                        addButtonText = "+ Entry Daalo"
                    )
                }
                
                // Section 2: Daily Kharcha - ORANGE  
                item {
                    PremiumLedgerSection(
                        title = "🛒 Rozana Kharcha",
                        total = dailyExpenseTotal,
                        headerColor = Color(0xFFF59E0B),
                        accentColor = Color(0xFFD97706),
                        isExpanded = dailyExpenseExpanded,
                        onToggle = { dailyExpenseExpanded = !dailyExpenseExpanded },
                        transactions = categorizedTransactions[TransactionCategory.DAILY_EXPENSE] ?: emptyList(),
                        onAddClick = {
                            addingToCategory = TransactionCategory.DAILY_EXPENSE
                            editingTransaction = null
                            showAddDialog = true
                        },
                        onEditClick = { tx ->
                            addingToCategory = TransactionCategory.DAILY_EXPENSE
                            editingTransaction = tx
                            showAddDialog = true
                        },
                        onDeleteClick = { tx ->
                            scope.launch {
                                viewModel.deleteLedgerTransaction(tx)
                                transactions = transactions.filter { it.id != tx.id }
                            }
                        },
                        addButtonText = "+ Kharcha Daalo"
                    )
                }
                
                // Section 3: Purchase - PURPLE
                item {
                    PremiumLedgerSection(
                        title = "🐃 Khareedari (Sirf Record)",
                        total = purchaseTotal,
                        headerColor = Color(0xFF8B5CF6),
                        accentColor = Color(0xFF7C3AED),
                        isExpanded = purchaseExpanded,
                        onToggle = { purchaseExpanded = !purchaseExpanded },
                        transactions = categorizedTransactions[TransactionCategory.PURCHASE] ?: emptyList(),
                        onAddClick = {
                            addingToCategory = TransactionCategory.PURCHASE
                            editingTransaction = null
                            showAddDialog = true
                        },
                        onEditClick = { tx ->
                            addingToCategory = TransactionCategory.PURCHASE
                            editingTransaction = tx
                            showAddDialog = true
                        },
                        onDeleteClick = { tx ->
                            scope.launch {
                                viewModel.deleteLedgerTransaction(tx)
                                transactions = transactions.filter { it.id != tx.id }
                            }
                        },
                        addButtonText = "+ Khareedari Likho"
                    )
                }
                
                // Section 4: Payment - RED
                item {
                    PremiumLedgerSection(
                        title = "💸 Payment Diya",
                        total = paymentTotal,
                        headerColor = Color(0xFFEF4444),
                        accentColor = Color(0xFFDC2626),
                        isExpanded = paymentExpanded,
                        onToggle = { paymentExpanded = !paymentExpanded },
                        transactions = categorizedTransactions[TransactionCategory.PAYMENT_GIVEN] ?: emptyList(),
                        onAddClick = {
                            addingToCategory = TransactionCategory.PAYMENT_GIVEN
                            editingTransaction = null
                            showAddDialog = true
                        },
                        onEditClick = { tx ->
                            addingToCategory = TransactionCategory.PAYMENT_GIVEN
                            editingTransaction = tx
                            showAddDialog = true
                        },
                        onDeleteClick = { tx ->
                            scope.launch {
                                viewModel.deleteLedgerTransaction(tx)
                                transactions = transactions.filter { it.id != tx.id }
                            }
                        },
                        addButtonText = "+ Payment Daalo"
                    )
                }
                
                // Summary & Closing Balance Section
                item {
                    // Calculate totals from transaction modes
                    val totals = transactions.fold(TransactionTotals()) { acc, tx ->
                        when (tx.mode) {
                            "CASH_IN" -> acc.copy(totalCashIn = acc.totalCashIn + tx.amount)
                            "CASH_OUT" -> acc.copy(totalCashOut = acc.totalCashOut + tx.amount)
                            "BANK_IN" -> acc.copy(totalBankIn = acc.totalBankIn + tx.amount)
                            "BANK_OUT" -> acc.copy(totalBankOut = acc.totalBankOut + tx.amount)
                            else -> acc
                        }
                    }
                    
                    val totalIn = totals.totalCashIn + totals.totalBankIn
                    val totalOut = totals.totalCashOut + totals.totalBankOut
                    val closingCash = openingCash + totals.totalCashIn - totals.totalCashOut
                    val closingBank = openingBank + totals.totalBankIn - totals.totalBankOut
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            // Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF667EEA).copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Analytics, null, tint = Color(0xFF667EEA), modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Aaj Ka Hisab", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Summary Grid
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Money Received
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Paisa Aaya", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        "₹${String.format("%,.0f", moneyReceivedTotal)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981)
                                    )
                                }
                                // Daily Expense
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Rozana Kharcha", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        "₹${String.format("%,.0f", dailyExpenseTotal)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF59E0B)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth()) {
                                // Purchase
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Khareedari (Sirf Record)", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        "₹${String.format("%,.0f", purchaseTotal)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF8B5CF6)
                                    )
                                }
                                // Payment Given
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Payment Diya", fontSize = 11.sp, color = TextSecondary)
                                    Text(
                                        "₹${String.format("%,.0f", paymentTotal)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFEF4444)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color(0xFFE5E7EB))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Closing Balance
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF0F9FF), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Cash Baki", fontSize = 12.sp, color = TextSecondary)
                                    Text(
                                        "₹${String.format("%,.0f", closingCash)}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (closingCash >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Bank Baki", fontSize = 12.sp, color = TextSecondary)
                                    Text(
                                        "₹${String.format("%,.0f", closingBank)}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (closingBank >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "↳ Yeh balance kal subah ka opening balance banega",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
        
        // Floating Save Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFFF5F7FA), Color(0xFFF5F7FA))
                    )
                )
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        viewModel.saveDailyLedger(
                            date = normalizedDate,
                            openingCash = openingCash,
                            openingBank = openingBank,
                            transactions = transactions,
                            note = null,
                            originalCreatedAt = existingEntry?.createdAt
                        )
                        onEntrySaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF667EEA)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Hisab Save Karo", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
    
    // Dialogs
    if (showEditOpeningDialog) {
        EditOpeningBalanceDialog(
            openingCash = openingCash,
            openingBank = openingBank,
            onDismiss = { showEditOpeningDialog = false },
            onSave = { cash, bank ->
                openingCash = cash
                openingBank = bank
                showEditOpeningDialog = false
            }
        )
    }
    
    if (showAddDialog) {
        AddLedgerEntryDialog(
            category = addingToCategory,
            transaction = editingTransaction,
            allContacts = viewModel.allCustomers.collectAsState().value.map { it.customer.name },
            onDismiss = {
                showAddDialog = false
                editingTransaction = null
                addingToCategory = null
            },
            onSave = { newTransaction ->
                scope.launch {
                    if (editingTransaction != null) {
                        // If editing an existing transaction with customer link, use sync
                        if (editingTransaction!!.customerTransactionId != null) {
                            val paymentMethod = if (newTransaction.mode.contains("CASH")) "CASH" else "BANK"
                            viewModel.updateLedgerTransactionWithSync(
                                transaction = editingTransaction!!,
                                newPartyName = newTransaction.party,
                                newAmount = newTransaction.amount,
                                newPaymentMethod = paymentMethod
                            )
                        } else {
                            // No customer link, just update ledger
                            viewModel.saveOrUpdateLedgerTransaction(
                                newTransaction.copy(
                                    id = editingTransaction!!.id,
                                    date = viewModel.normalizeDateToMidnight(selectedDate)
                                )
                            )
                        }
                        // Update local list
                        transactions = transactions.map {
                            if (it.id == editingTransaction!!.id) newTransaction.copy(
                                id = editingTransaction!!.id,
                                date = viewModel.normalizeDateToMidnight(selectedDate),
                                customerTransactionId = editingTransaction!!.customerTransactionId
                            )
                            else it
                        }
                    } else {
                        // Adding new transaction
                        val txToSave = newTransaction.copy(
                            date = viewModel.normalizeDateToMidnight(selectedDate)
                        )
                        transactions = transactions + txToSave
                    }
                    showAddDialog = false
                    editingTransaction = null
                    addingToCategory = null
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PremiumLedgerSection(
    title: String,
    total: Double,
    headerColor: Color,
    accentColor: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    transactions: List<DailyLedgerTransaction>,
    onAddClick: () -> Unit = {},  // Keep for compatibility but won't be used
    onEditClick: (DailyLedgerTransaction) -> Unit = {},
    onDeleteClick: (DailyLedgerTransaction) -> Unit = {},
    addButtonText: String = ""
) {
    // State for bottom sheet
    var selectedTransaction by remember { mutableStateOf<DailyLedgerTransaction?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(headerColor, accentColor)
                        )
                    )
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = Color.White, modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Text("₹${String.format("%,.0f", total)}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            
            // Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (transactions.isEmpty()) {
                        // Empty state
                        Text(
                            text = "No entries",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            textAlign = TextAlign.Center,
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    } else {
                        transactions.forEach { tx ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .combinedClickable(
                                        onClick = { },
                                        onLongClick = { selectedTransaction = tx }
                                    ),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9FAFB)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .height(44.dp)
                                            .background(headerColor, RoundedCornerShape(2.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            tx.party ?: tx.note ?: "Entry",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp,
                                            color = TextPrimary
                                        )
                                        Text(
                                            "${if (tx.mode.contains("CASH")) "Cash" else "Bank"} • ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.createdAt))}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                    }
                                    Text(
                                        "₹${String.format("%,.0f", tx.amount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Bottom Sheet for Edit/Delete options
    if (selectedTransaction != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedTransaction = null },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Transaction info header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(headerColor.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = headerColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            selectedTransaction!!.party ?: selectedTransaction!!.note ?: "Entry",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Text(
                            "₹${String.format("%,.0f", selectedTransaction!!.amount)}",
                            fontSize = 14.sp,
                            color = headerColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Divider(color = Color(0xFFE5E7EB))
                
                // Edit Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedTransaction?.let { tx ->
                                onEditClick(tx)
                                selectedTransaction = null
                            }
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF3B82F6)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "✏️ Edit Entry",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }
                
                // Delete Option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDeleteConfirmDialog = true
                        }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFEF4444)
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(
                        "🗑️ Delete Entry",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFEF4444)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Info text
                Text(
                    "⚠️ Delete karne se Customer/Supplier ledger se bhi entry hat jayegi",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && selectedTransaction != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteConfirmDialog = false
                selectedTransaction = null
            },
            icon = { 
                Icon(
                    Icons.Default.Delete, 
                    contentDescription = null, 
                    tint = Color(0xFFEF4444)
                ) 
            },
            title = { 
                Text("Entry Delete Karein?", fontWeight = FontWeight.Bold) 
            },
            text = { 
                Column {
                    Text(
                        "${selectedTransaction!!.party ?: selectedTransaction!!.note ?: "Entry"} - ₹${String.format("%,.0f", selectedTransaction!!.amount)}",
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Yeh entry Daily Ledger aur Customer/Supplier Ledger dono se delete ho jayegi.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedTransaction?.let { tx ->
                            onDeleteClick(tx)
                        }
                        showDeleteConfirmDialog = false
                        selectedTransaction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Delete Karein")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteConfirmDialog = false
                        selectedTransaction = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EditOpeningBalanceDialog(
    openingCash: Double,
    openingBank: Double,
    onDismiss: () -> Unit,
    onSave: (Double, Double) -> Unit
) {
    var cashText by remember { mutableStateOf(openingCash.toString()) }
    var bankText by remember { mutableStateOf(openingBank.toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subah Ka Balance Badlo", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cashText,
                    onValueChange = { cashText = it },
                    label = { Text("Cash Balance") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = bankText,
                    onValueChange = { bankText = it },
                    label = { Text("Bank Balance") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(cashText.toDoubleOrNull() ?: 0.0, bankText.toDoubleOrNull() ?: 0.0)
                },
                shape = RoundedCornerShape(8.dp)
            ) { Text("Save Karo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Rehne Do") } },
        shape = RoundedCornerShape(20.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLedgerEntryDialog(
    category: TransactionCategory?,
    transaction: DailyLedgerTransaction?,
    allContacts: List<String>,
    onDismiss: () -> Unit,
    onSave: (DailyLedgerTransaction) -> Unit
) {
    val isMoneyReceived = category == TransactionCategory.MONEY_RECEIVED || transaction?.mode?.contains("IN") == true
    val isPaymentOrReceived = category == TransactionCategory.PAYMENT_GIVEN || category == TransactionCategory.MONEY_RECEIVED
    
    var amountText by remember { mutableStateOf(transaction?.amount?.toString() ?: "") }
    var partyText by remember { mutableStateOf(transaction?.party ?: "") }
    var noteText by remember { mutableStateOf(transaction?.note ?: "") }
    var isCash by remember { mutableStateOf(transaction?.mode?.contains("CASH") != false) }
    
    // Dropdown state for customer selection
    var expanded by remember { mutableStateOf(false) }
    val filteredContacts = remember(partyText, allContacts) {
        if (partyText.isBlank()) allContacts
        else allContacts.filter { it.contains(partyText, ignoreCase = true) }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        title = { 
            Text(
                when (category) {
                    TransactionCategory.MONEY_RECEIVED -> "💰 Paisa Aaya Entry"
                    TransactionCategory.DAILY_EXPENSE -> "🛒 Kharcha Entry"
                    TransactionCategory.PURCHASE -> "🐃 Khareedari Likho"
                    TransactionCategory.PAYMENT_GIVEN -> "💸 Payment Entry"
                    else -> "Entry Daalo"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Amount field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Kitna Paisa?") },
                    prefix = { Text("₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Customer/Seller dropdown - now available for ALL categories
                ExposedDropdownMenuBox(
                    expanded = expanded && filteredContacts.isNotEmpty(),
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = partyText,
                        onValueChange = { 
                            partyText = it 
                            expanded = true
                        },
                        label = { 
                            Text(when (category) {
                                TransactionCategory.PAYMENT_GIVEN -> "Party Ka Naam"
                                TransactionCategory.MONEY_RECEIVED -> "Kisse Mila?"
                                TransactionCategory.DAILY_EXPENSE -> "Kya Kharcha?"
                                TransactionCategory.PURCHASE -> "Kisse Khareed?"
                                else -> "Description"
                            })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expanded && filteredContacts.isNotEmpty(),
                        onDismissRequest = { expanded = false }
                    ) {
                        filteredContacts.take(5).forEach { contact ->
                            DropdownMenuItem(
                                text = { 
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                contact.firstOrNull()?.uppercase() ?: "?",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(contact)
                                    }
                                },
                                onClick = {
                                    partyText = contact
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                // Note field (for all categories)
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note (Zaruri Nahi)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Payment method selection
                Text("Cash Ya Bank?", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isCash,
                        onClick = { isCash = true },
                        label = { Text("Cash") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isCash,
                        onClick = { isCash = false },
                        label = { Text("Bank") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    if (amount > 0) {
                        val mode = when {
                            isMoneyReceived && isCash -> "CASH_IN"
                            isMoneyReceived && !isCash -> "BANK_IN"
                            !isMoneyReceived && isCash -> "CASH_OUT"
                            else -> "BANK_OUT"
                        }
                        onSave(
                            DailyLedgerTransaction(
                                id = transaction?.id ?: 0,
                                date = 0,
                                mode = mode,
                                amount = amount,
                                party = partyText.ifBlank { null },
                                note = noteText.ifBlank { null },
                                createdAt = transaction?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                shape = RoundedCornerShape(8.dp)
            ) { Text("Save Karo") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Rehne Do") } },
        shape = RoundedCornerShape(20.dp)
    )
}

data class TransactionTotals(
    val totalCashIn: Double = 0.0,
    val totalCashOut: Double = 0.0,
    val totalBankIn: Double = 0.0,
    val totalBankOut: Double = 0.0
)
