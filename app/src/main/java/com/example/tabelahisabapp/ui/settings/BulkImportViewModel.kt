package com.example.tabelahisabapp.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.db.entity.*
import com.example.tabelahisabapp.data.repository.MainRepository
import com.example.tabelahisabapp.utils.CSVImportHelper
import com.example.tabelahisabapp.utils.CSVImportHelper.getColumn
import com.example.tabelahisabapp.utils.ImportResult
import com.example.tabelahisabapp.utils.ImportError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class BulkImportState(
    val isLoading: Boolean = false,
    val loadingMessage: String = "",
    val importResults: List<ImportResult> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    
    // Initial balance settings
    val startDate: Long = System.currentTimeMillis(),
    val initialOpeningCash: Double = 0.0,
    val initialOpeningBank: Double = 0.0,
    val isBalanceSet: Boolean = false
)

@HiltViewModel
class BulkImportViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(BulkImportState())
    val state: StateFlow<BulkImportState> = _state.asStateFlow()
    
    // Cache of existing customers for matching during import
    private var customerCache: Map<String, Int> = emptyMap()
    
    /**
     * Set the initial opening balance for bulk import
     */
    fun setInitialBalance(startDate: Long, openingCash: Double, openingBank: Double) {
        _state.value = _state.value.copy(
            startDate = startDate,
            initialOpeningCash = openingCash,
            initialOpeningBank = openingBank,
            isBalanceSet = true
        )
    }
    
    /**
     * Import customers/suppliers from CSV
     * Expected columns: Name, Phone, Type, OpeningBalance, BusinessName, Category
     */
    fun importCustomers(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadingMessage = "Importing Customers...")
            
            val result = CSVImportHelper.readCSV(context, uri)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to read CSV: ${result.exceptionOrNull()?.message}"
                )
                return@launch
            }
            
            val rows = result.getOrNull() ?: emptyList()
            
            // Validate required columns
            val validation = CSVImportHelper.validateColumns(rows, listOf("name"))
            if (!validation.isValid) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = validation.message
                )
                return@launch
            }
            
            // Parse and insert customers
            val customers = mutableListOf<Customer>()
            val errors = mutableListOf<ImportError>()
            
            for (row in rows) {
                val lineNum = row["_line"]?.toIntOrNull() ?: 0
                val name = row.getColumn("name")
                
                if (name.isBlank()) {
                    errors.add(ImportError(lineNum, "name", "", "Name is required"))
                    continue
                }
                
                val typeStr = row.getColumn("type").uppercase().ifBlank { "CUSTOMER" }
                val type = when {
                    typeStr.contains("SELLER") || typeStr.contains("SUPPLIER") -> "SELLER"
                    typeStr.contains("BOTH") -> "BOTH"
                    else -> "CUSTOMER"
                }
                
                val openingBalance = row.getColumn("openingbalance").toDoubleOrNull() ?: 0.0
                
                customers.add(Customer(
                    name = name,
                    phone = row.getColumn("phone").ifBlank { null },
                    type = type,
                    businessName = row.getColumn("businessname").ifBlank { null },
                    category = row.getColumn("category").ifBlank { null },
                    openingBalance = openingBalance,
                    createdAt = System.currentTimeMillis()
                ))
            }
            
            val insertedCount = repository.insertCustomersBulk(customers)
            refreshCustomerCache()
            
            val importResult = ImportResult(
                success = true,
                importedCount = insertedCount,
                skippedCount = customers.size - insertedCount,
                errorCount = errors.size,
                errors = errors,
                message = "Imported $insertedCount customers (${customers.size - insertedCount} duplicates skipped)"
            )
            
            _state.value = _state.value.copy(
                isLoading = false,
                importResults = _state.value.importResults + importResult,
                successMessage = importResult.message
            )
        }
    }
    
    /**
     * Import daily ledger transactions from CSV
     * Expected columns: Date, Mode, Amount, Party, Note
     */
    fun importDailyLedger(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadingMessage = "Importing Daily Ledger...")
            
            val result = CSVImportHelper.readCSV(context, uri)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to read CSV: ${result.exceptionOrNull()?.message}"
                )
                return@launch
            }
            
            val rows = result.getOrNull() ?: emptyList()
            
            // Validate required columns
            val validation = CSVImportHelper.validateColumns(rows, listOf("date", "mode", "amount"))
            if (!validation.isValid) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = validation.message
                )
                return@launch
            }
            
            // Parse transactions
            val transactions = mutableListOf<DailyLedgerTransaction>()
            val errors = mutableListOf<ImportError>()
            
            for (row in rows) {
                val lineNum = row["_line"]?.toIntOrNull() ?: 0
                
                // Parse date
                val dateStr = row.getColumn("date")
                val date = CSVImportHelper.parseDate(dateStr)
                if (date == null) {
                    errors.add(ImportError(lineNum, "date", dateStr, "Invalid date format"))
                    continue
                }
                
                // Parse mode
                val modeStr = row.getColumn("mode").uppercase()
                val mode = when {
                    modeStr.contains("CASH") && modeStr.contains("IN") -> LedgerMode.CASH_IN
                    modeStr.contains("CASH") && modeStr.contains("OUT") -> LedgerMode.CASH_OUT
                    modeStr.contains("BANK") && modeStr.contains("IN") -> LedgerMode.BANK_IN
                    modeStr.contains("BANK") && modeStr.contains("OUT") -> LedgerMode.BANK_OUT
                    modeStr == "CASH_IN" || modeStr == "CASHIN" -> LedgerMode.CASH_IN
                    modeStr == "CASH_OUT" || modeStr == "CASHOUT" -> LedgerMode.CASH_OUT
                    modeStr == "BANK_IN" || modeStr == "BANKIN" -> LedgerMode.BANK_IN
                    modeStr == "BANK_OUT" || modeStr == "BANKOUT" -> LedgerMode.BANK_OUT
                    else -> {
                        errors.add(ImportError(lineNum, "mode", modeStr, "Invalid mode. Use: CASH_IN, CASH_OUT, BANK_IN, BANK_OUT"))
                        continue
                    }
                }
                
                // Parse amount
                val amount = row.getColumn("amount").replace(",", "").toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    errors.add(ImportError(lineNum, "amount", row.getColumn("amount"), "Invalid amount"))
                    continue
                }
                
                transactions.add(DailyLedgerTransaction(
                    date = date,
                    mode = mode,
                    amount = amount,
                    party = row.getColumn("party").ifBlank { null },
                    note = row.getColumn("note").ifBlank { null },
                    createdAt = System.currentTimeMillis()
                ))
            }
            
            val insertedCount = repository.insertDailyLedgerTransactionsBulk(transactions)
            
            // Recalculate balances if initial balance is set
            if (_state.value.isBalanceSet) {
                _state.value = _state.value.copy(loadingMessage = "Calculating balances...")
                repository.recalculateAllDailyBalances(
                    _state.value.startDate,
                    _state.value.initialOpeningCash,
                    _state.value.initialOpeningBank
                )
            }
            
            val importResult = ImportResult(
                success = true,
                importedCount = insertedCount,
                skippedCount = 0,
                errorCount = errors.size,
                errors = errors,
                message = "Imported $insertedCount daily ledger entries" + 
                    if (errors.isNotEmpty()) " (${errors.size} errors)" else ""
            )
            
            _state.value = _state.value.copy(
                isLoading = false,
                importResults = _state.value.importResults + importResult,
                successMessage = importResult.message
            )
        }
    }
    
    /**
     * Import expenses from CSV
     * Expected columns: Date, Category, Amount, PaymentMethod, Note
     */
    fun importExpenses(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadingMessage = "Importing Expenses...")
            
            val result = CSVImportHelper.readCSV(context, uri)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to read CSV: ${result.exceptionOrNull()?.message}"
                )
                return@launch
            }
            
            val rows = result.getOrNull() ?: emptyList()
            
            val validation = CSVImportHelper.validateColumns(rows, listOf("date", "category", "amount"))
            if (!validation.isValid) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = validation.message
                )
                return@launch
            }
            
            val expenses = mutableListOf<DailyExpense>()
            val errors = mutableListOf<ImportError>()
            
            for (row in rows) {
                val lineNum = row["_line"]?.toIntOrNull() ?: 0
                
                val dateStr = row.getColumn("date")
                val date = CSVImportHelper.parseDate(dateStr)
                if (date == null) {
                    errors.add(ImportError(lineNum, "date", dateStr, "Invalid date format"))
                    continue
                }
                
                val category = row.getColumn("category")
                if (category.isBlank()) {
                    errors.add(ImportError(lineNum, "category", "", "Category is required"))
                    continue
                }
                
                val amount = row.getColumn("amount").replace(",", "").toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    errors.add(ImportError(lineNum, "amount", row.getColumn("amount"), "Invalid amount"))
                    continue
                }
                
                val paymentMethod = row.getColumn("paymentmethod").uppercase().let {
                    if (it.contains("BANK")) "BANK" else "CASH"
                }
                
                expenses.add(DailyExpense(
                    date = date,
                    category = category,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    note = row.getColumn("note").ifBlank { null },
                    createdAt = System.currentTimeMillis()
                ))
            }
            
            val insertedCount = repository.insertExpensesBulk(expenses)
            
            val importResult = ImportResult(
                success = true,
                importedCount = insertedCount,
                skippedCount = 0,
                errorCount = errors.size,
                errors = errors,
                message = "Imported $insertedCount expenses" + 
                    if (errors.isNotEmpty()) " (${errors.size} errors)" else ""
            )
            
            _state.value = _state.value.copy(
                isLoading = false,
                importResults = _state.value.importResults + importResult,
                successMessage = importResult.message
            )
        }
    }
    
    /**
     * Import customer transactions (udhaar) from CSV
     * Expected columns: Date, CustomerName, Type, Amount, PaymentMethod, Note
     */
    fun importCustomerTransactions(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadingMessage = "Importing Customer Transactions...")
            
            refreshCustomerCache()
            
            val result = CSVImportHelper.readCSV(context, uri)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to read CSV: ${result.exceptionOrNull()?.message}"
                )
                return@launch
            }
            
            val rows = result.getOrNull() ?: emptyList()
            
            val validation = CSVImportHelper.validateColumns(rows, listOf("date", "customername", "type", "amount"))
            if (!validation.isValid) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = validation.message
                )
                return@launch
            }
            
            val transactions = mutableListOf<CustomerTransaction>()
            val errors = mutableListOf<ImportError>()
            
            for (row in rows) {
                val lineNum = row["_line"]?.toIntOrNull() ?: 0
                
                val dateStr = row.getColumn("date")
                val date = CSVImportHelper.parseDate(dateStr)
                if (date == null) {
                    errors.add(ImportError(lineNum, "date", dateStr, "Invalid date format"))
                    continue
                }
                
                val customerName = row.getColumn("customername")
                if (customerName.isBlank()) {
                    errors.add(ImportError(lineNum, "customername", "", "Customer name is required"))
                    continue
                }
                
                // Get or create customer
                val customerId = repository.getOrCreateCustomerByName(customerName)
                refreshCustomerCache() // Refresh cache after creating
                
                val typeStr = row.getColumn("type").uppercase()
                val type = when {
                    typeStr.contains("CREDIT") || typeStr.contains("GIVEN") || typeStr.contains("DIYA") -> "CREDIT"
                    typeStr.contains("DEBIT") || typeStr.contains("RECEIVED") || typeStr.contains("MILA") -> "DEBIT"
                    else -> {
                        errors.add(ImportError(lineNum, "type", typeStr, "Invalid type. Use: CREDIT or DEBIT"))
                        continue
                    }
                }
                
                val amount = row.getColumn("amount").replace(",", "").toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    errors.add(ImportError(lineNum, "amount", row.getColumn("amount"), "Invalid amount"))
                    continue
                }
                
                val paymentMethod = row.getColumn("paymentmethod").uppercase().let {
                    if (it.contains("BANK")) "BANK" else "CASH"
                }
                
                transactions.add(CustomerTransaction(
                    customerId = customerId,
                    type = type,
                    amount = amount,
                    date = date,
                    note = row.getColumn("note").ifBlank { null },
                    voiceNotePath = null,
                    paymentMethod = paymentMethod,
                    createdAt = System.currentTimeMillis()
                ))
            }
            
            val insertedCount = repository.insertCustomerTransactionsBulk(transactions)
            
            val importResult = ImportResult(
                success = true,
                importedCount = insertedCount,
                skippedCount = 0,
                errorCount = errors.size,
                errors = errors,
                message = "Imported $insertedCount customer transactions" + 
                    if (errors.isNotEmpty()) " (${errors.size} errors)" else ""
            )
            
            _state.value = _state.value.copy(
                isLoading = false,
                importResults = _state.value.importResults + importResult,
                successMessage = importResult.message
            )
        }
    }
    
    /**
     * Import trade transactions from CSV
     * Expected columns: Date, Type, ItemName, Quantity, BuyRate, TotalAmount, Location, Note
     */
    fun importTradeTransactions(context: Context, uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadingMessage = "Importing Trade Transactions...")
            
            val result = CSVImportHelper.readCSV(context, uri)
            if (result.isFailure) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to read CSV: ${result.exceptionOrNull()?.message}"
                )
                return@launch
            }
            
            val rows = result.getOrNull() ?: emptyList()
            
            val validation = CSVImportHelper.validateColumns(rows, listOf("date", "type", "itemname", "quantity", "totalamount"))
            if (!validation.isValid) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = validation.message
                )
                return@launch
            }
            
            val trades = mutableListOf<TradeTransaction>()
            val errors = mutableListOf<ImportError>()
            
            for (row in rows) {
                val lineNum = row["_line"]?.toIntOrNull() ?: 0
                
                val dateStr = row.getColumn("date")
                val date = CSVImportHelper.parseDate(dateStr)
                if (date == null) {
                    errors.add(ImportError(lineNum, "date", dateStr, "Invalid date format"))
                    continue
                }
                
                val typeStr = row.getColumn("type").uppercase()
                val type = when {
                    typeStr.contains("BUY") || typeStr.contains("PURCHASE") -> "BUY"
                    typeStr.contains("SELL") || typeStr.contains("SALE") -> "SELL"
                    else -> {
                        errors.add(ImportError(lineNum, "type", typeStr, "Invalid type. Use: BUY or SELL"))
                        continue
                    }
                }
                
                val itemName = row.getColumn("itemname")
                if (itemName.isBlank()) {
                    errors.add(ImportError(lineNum, "itemname", "", "Item name is required"))
                    continue
                }
                
                val quantity = row.getColumn("quantity").toIntOrNull() ?: 1
                val buyRate = row.getColumn("buyrate").replace(",", "").toDoubleOrNull() ?: 0.0
                val totalAmount = row.getColumn("totalamount").replace(",", "").toDoubleOrNull()
                if (totalAmount == null || totalAmount <= 0) {
                    errors.add(ImportError(lineNum, "totalamount", row.getColumn("totalamount"), "Invalid total amount"))
                    continue
                }
                
                trades.add(TradeTransaction(
                    date = date,
                    type = type,
                    itemName = itemName,
                    quantity = quantity,
                    buyRate = buyRate,
                    totalAmount = totalAmount,
                    pricePerUnit = if (quantity > 0) totalAmount / quantity else totalAmount,
                    deonar = row.getColumn("location").ifBlank { row.getColumn("deonar") }.ifBlank { null },
                    note = row.getColumn("note").ifBlank { null },
                    createdAt = System.currentTimeMillis()
                ))
            }
            
            val insertedCount = repository.insertTradeTransactionsBulk(trades)
            
            val importResult = ImportResult(
                success = true,
                importedCount = insertedCount,
                skippedCount = 0,
                errorCount = errors.size,
                errors = errors,
                message = "Imported $insertedCount trade transactions" + 
                    if (errors.isNotEmpty()) " (${errors.size} errors)" else ""
            )
            
            _state.value = _state.value.copy(
                isLoading = false,
                importResults = _state.value.importResults + importResult,
                successMessage = importResult.message
            )
        }
    }
    
    /**
     * Recalculate all daily balances after import
     */
    fun recalculateBalances() {
        if (!_state.value.isBalanceSet) return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, loadingMessage = "Recalculating balances...")
            
            repository.recalculateAllDailyBalances(
                _state.value.startDate,
                _state.value.initialOpeningCash,
                _state.value.initialOpeningBank
            )
            
            _state.value = _state.value.copy(
                isLoading = false,
                successMessage = "Balances recalculated successfully"
            )
        }
    }
    
    private suspend fun refreshCustomerCache() {
        val customers = repository.getAllCustomersSync()
        customerCache = customers.associate { it.name.lowercase() to it.id }
    }
    
    fun clearMessages() {
        _state.value = _state.value.copy(errorMessage = null, successMessage = null)
    }
}
