package com.example.tabelahisabapp.data.repository

import com.example.tabelahisabapp.data.db.dao.*
import com.example.tabelahisabapp.data.db.entity.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(
    private val customerDao: CustomerDao,
    private val customerTransactionDao: CustomerTransactionDao,
    private val dailyBalanceDao: DailyBalanceDao,
    private val tradeTransactionDao: TradeTransactionDao,
    private val companyDao: CompanyDao,
    private val dailyExpenseDao: DailyExpenseDao,
    private val dailyLedgerTransactionDao: DailyLedgerTransactionDao,
    private val expenseCategoryDao: ExpenseCategoryDao
) {

    // Expense Categories
    fun getAllExpenseCategories() = expenseCategoryDao.getAllCategories()
    suspend fun insertExpenseCategory(category: ExpenseCategory) = expenseCategoryDao.insertCategory(category)
    suspend fun deleteExpenseCategory(category: ExpenseCategory) = expenseCategoryDao.deleteCategory(category)
    suspend fun getExpenseCategoryCount() = expenseCategoryDao.getCount()
    suspend fun insertAllExpenseCategories(categories: List<ExpenseCategory>) = expenseCategoryDao.insertAll(categories)

    // Expenses
    fun getAllExpenses() = dailyExpenseDao.getAllExpenses()

    // Customer
    fun getAllCustomersWithBalance() = customerDao.getAllCustomersWithBalance()
    fun getCustomerById(customerId: Int) = customerDao.getCustomerById(customerId)
    suspend fun getCustomerByName(name: String) = customerDao.getCustomerByName(name)
    suspend fun insertOrUpdateCustomer(customer: Customer) {
        if (customer.id > 0) {
            // UPDATE existing customer (preserves foreign key relationships)
            customerDao.updateCustomer(customer)
        } else {
            // INSERT new customer
            customerDao.insertCustomer(customer)
        }
    }
    suspend fun deleteCustomer(customer: Customer) = customerDao.deleteCustomer(customer)

    // Customer Transaction
    fun getTransactionById(transactionId: Int) = customerTransactionDao.getTransactionById(transactionId)
    fun getTransactionsForCustomer(customerId: Int) = customerTransactionDao.getTransactionsForCustomer(customerId)
    fun getBalanceSummaryForCustomer(customerId: Int) = customerTransactionDao.getBalanceSummaryForCustomer(customerId)
    fun getTransactionsByDateWithCustomer(date: Long) = customerTransactionDao.getTransactionsByDateWithCustomer(date)
    suspend fun insertOrUpdateTransaction(transaction: CustomerTransaction) = customerTransactionDao.insertOrUpdateTransaction(transaction)
    suspend fun insertOrUpdateTransactionAndGetId(transaction: CustomerTransaction) = customerTransactionDao.insertAndGetId(transaction)
    suspend fun deleteTransaction(transaction: CustomerTransaction) = customerTransactionDao.deleteTransaction(transaction)

    // Daily Balance
    fun getDailyBalanceByDate(date: Long) = dailyBalanceDao.getDailyBalanceByDate(date)
    fun getAllDailyBalances() = dailyBalanceDao.getAllDailyBalances()
    suspend fun insertOrUpdateDailyBalance(dailyBalance: DailyBalance) = dailyBalanceDao.insertOrUpdateDailyBalance(dailyBalance)
    suspend fun deleteDailyBalance(dailyBalance: DailyBalance) = dailyBalanceDao.deleteDailyBalance(dailyBalance)

    // Daily Expense
    fun getExpensesByDate(date: Long) = dailyExpenseDao.getExpensesByDate(date)
    suspend fun insertExpense(expense: DailyExpense) = dailyExpenseDao.insertExpense(expense)
    suspend fun insertExpenseAndGetId(expense: DailyExpense) = dailyExpenseDao.insertExpenseAndGetId(expense)
    suspend fun deleteExpense(expense: DailyExpense) = dailyExpenseDao.deleteExpense(expense)
    suspend fun deleteExpensesForDate(date: Long) = dailyExpenseDao.deleteExpensesForDate(date)

    // Trade Transaction
    fun getAllTrades() = tradeTransactionDao.getAllTrades()
    fun getTradesByType(type: String) = tradeTransactionDao.getTradesByType(type)
    fun getTradeTransactionById(transactionId: Int) = tradeTransactionDao.getTradeTransactionById(transactionId)
    fun getMonthlyProfitSummary(start: Long, end: Long) = tradeTransactionDao.getMonthlyProfitSummary(start, end)
    fun getOverallProfitSummary() = tradeTransactionDao.getOverallProfitSummary()
    suspend fun insertOrUpdateTrade(trade: TradeTransaction) = tradeTransactionDao.insertOrUpdateTrade(trade)
    suspend fun deleteTrade(trade: TradeTransaction) = tradeTransactionDao.deleteTrade(trade)
    
    // Get previous day's closing balance
    fun getPreviousDayBalance(currentDate: Long) = dailyBalanceDao.getPreviousDayBalance(currentDate)
    
    // Company
    fun getAllCompanies() = companyDao.getAllCompanies()
    fun getCompanyById(companyId: Int) = companyDao.getCompanyById(companyId)
    suspend fun insertOrUpdateCompany(company: Company) = companyDao.insertOrUpdateCompany(company)
    suspend fun deleteCompany(company: Company) = companyDao.deleteCompany(company)
    
    // Daily Ledger Transaction
    fun getLedgerTransactionsByDate(date: Long) = dailyLedgerTransactionDao.getTransactionsByDate(date)
    suspend fun getLedgerTransactionById(transactionId: Int) = dailyLedgerTransactionDao.getTransactionById(transactionId)
    suspend fun insertOrUpdateLedgerTransaction(transaction: DailyLedgerTransaction) = dailyLedgerTransactionDao.insertOrUpdateTransaction(transaction)
    suspend fun deleteLedgerTransaction(transaction: DailyLedgerTransaction) = dailyLedgerTransactionDao.deleteTransaction(transaction)
    suspend fun deleteLedgerTransactionsForDate(date: Long) = dailyLedgerTransactionDao.deleteTransactionsForDate(date)
    suspend fun getLedgerTotalsForDate(date: Long) = dailyLedgerTransactionDao.getTotalsForDate(date)
    fun getAllLedgerDates() = dailyLedgerTransactionDao.getAllDistinctDates()
    
    /**
     * Save customer transaction and automatically create corresponding daily ledger entry
     * Handles: CREDIT (payment given) and DEBIT (money received)
     * Note: PURCHASE and EXPENSE types are handled directly in ViewModel
     */
    suspend fun saveCustomerTransactionWithLedgerSync(
        customerId: Int,
        customerName: String,
        type: String,  // "CREDIT" or "DEBIT"
        amount: Double,
        date: Long,
        note: String?,
        paymentMethod: String,  // "CASH" or "BANK"
        voiceNotePath: String? = null,
        isSupplier: Boolean = false  // New parameter to identify supplier transactions
    ): Int {
        // Normalize date to midnight for proper daily ledger grouping
        val normalizedDate = normalizeDateToMidnight(date)
        
        // 1. Save customer transaction and get its ID
        val customerTransaction = CustomerTransaction(
            customerId = customerId,
            type = type,
            amount = amount,
            date = normalizedDate,
            note = note,
            paymentMethod = paymentMethod,
            voiceNotePath = voiceNotePath,
            createdAt = System.currentTimeMillis()
        )
        val customerTxId = customerTransactionDao.insertAndGetId(customerTransaction).toInt()
        
        // 2. Determine ledger mode
        val ledgerMode = when {
            type == "DEBIT" && paymentMethod == "CASH" -> "CASH_IN"
            type == "DEBIT" && paymentMethod == "BANK" -> "BANK_IN"
            type == "CREDIT" && paymentMethod == "CASH" -> "CASH_OUT"
            type == "CREDIT" && paymentMethod == "BANK" -> "BANK_OUT"
            else -> "CASH_OUT"
        }
        
        // 3. Determine source type based on whether this is a supplier or customer
        val sourceType = if (isSupplier) SourceType.SUPPLIER else SourceType.CUSTOMER
        
        // 4. Create daily ledger entry LINKED to customer transaction with correct sourceType
        val dailyLedgerTransaction = DailyLedgerTransaction(
            date = normalizedDate,
            mode = ledgerMode,
            amount = amount,
            party = customerName,
            note = note,
            createdAt = System.currentTimeMillis(),
            customerTransactionId = customerTxId,
            sourceType = sourceType,
            sourceId = customerId
        )
        insertOrUpdateLedgerTransaction(dailyLedgerTransaction)
        
        return customerTxId
    }
    
    /**
     * Update a Daily Ledger transaction and sync changes to linked Customer Transaction
     */
    suspend fun updateLedgerWithCustomerSync(
        ledgerTransaction: DailyLedgerTransaction,
        newPartyName: String?,
        newAmount: Double,
        newPaymentMethod: String
    ) {
        // 1. Update the Daily Ledger transaction
        val updatedLedger = ledgerTransaction.copy(
            party = newPartyName,
            amount = newAmount,
            mode = if (ledgerTransaction.mode.contains("IN")) {
                if (newPaymentMethod == "CASH") "CASH_IN" else "BANK_IN"
            } else {
                if (newPaymentMethod == "CASH") "CASH_OUT" else "BANK_OUT"
            }
        )
        insertOrUpdateLedgerTransaction(updatedLedger)
        
        // 2. If linked to a customer transaction, update that too
        val customerTxId = ledgerTransaction.customerTransactionId
        if (customerTxId != null) {
            val existingCustomerTx = customerTransactionDao.getTransactionByIdSync(customerTxId)
            if (existingCustomerTx != null) {
                // Find customer by new name (or keep existing if name didn't change)
                val newCustomerId = if (newPartyName != null && newPartyName != existingCustomerTx.customerId.toString()) {
                    customerDao.getCustomerByName(newPartyName)?.id ?: existingCustomerTx.customerId
                } else {
                    existingCustomerTx.customerId
                }
                
                // Update note to reflect new party name if changed
                val updatedNote = if (newPartyName != null && existingCustomerTx.note != null) {
                    // Replace old party name with new one in the note
                    val oldPartyName = ledgerTransaction.party ?: ""
                    existingCustomerTx.note.replace(oldPartyName, newPartyName, ignoreCase = true)
                } else {
                    existingCustomerTx.note
                }
                
                val updatedCustomerTx = existingCustomerTx.copy(
                    customerId = newCustomerId,
                    amount = newAmount,
                    paymentMethod = newPaymentMethod,
                    note = updatedNote
                )
                insertOrUpdateTransaction(updatedCustomerTx)
            }
        }
    }
    
    /**
     * Delete a Daily Ledger transaction and also delete linked Customer Transaction
     * This ensures two-way sync when deleting from Rozana Hisab
     */
    suspend fun deleteLedgerTransactionWithSync(transaction: DailyLedgerTransaction) {
        // 1. Delete linked customer transaction if exists
        val customerTxId = transaction.customerTransactionId
        if (customerTxId != null) {
            val customerTx = customerTransactionDao.getTransactionByIdSync(customerTxId)
            if (customerTx != null) {
                deleteTransaction(customerTx)
            }
        }
        
        // 2. Delete the ledger transaction
        deleteLedgerTransaction(transaction)
    }
    
    /**
     * Update customer name in all linked entries:
     * - Daily Ledger: party field
     * - Customer Transactions: replace old name in notes
     */
    suspend fun updateCustomerNameEverywhere(customerId: Int, oldName: String, newName: String) {
        // 1. Update party name in all Daily Ledger transactions
        val allLedgerEntries = dailyLedgerTransactionDao.getAllTransactions()
        allLedgerEntries.forEach { ledgerTx ->
            if (ledgerTx.party?.equals(oldName, ignoreCase = true) == true) {
                val updatedTx = ledgerTx.copy(party = newName)
                dailyLedgerTransactionDao.insertOrUpdateTransaction(updatedTx)
            }
            // Also update notes that contain the old name
            if (ledgerTx.note?.contains(oldName, ignoreCase = true) == true) {
                val updatedNote = ledgerTx.note.replace(oldName, newName, ignoreCase = true)
                val updatedTx = ledgerTx.copy(
                    party = if (ledgerTx.party?.equals(oldName, ignoreCase = true) == true) newName else ledgerTx.party,
                    note = updatedNote
                )
                dailyLedgerTransactionDao.insertOrUpdateTransaction(updatedTx)
            }
        }
        
        // 2. Update notes in Customer Transactions that mention the old name
        val customerTransactions = customerTransactionDao.getTransactionsForCustomerDirect(customerId)
        customerTransactions.forEach { tx ->
            if (tx.note?.contains(oldName, ignoreCase = true) == true) {
                val updatedNote = tx.note.replace(oldName, newName, ignoreCase = true)
                val updatedTx = tx.copy(note = updatedNote)
                customerTransactionDao.insertOrUpdateTransaction(updatedTx)
            }
        }
    }
    
    /**
     * Propagate balance changes forward from a specific date
     * 
     * This is called after any backdated entry to ensure all subsequent
     * days have their opening balances updated correctly.
     * 
     * Flow:
     * 1. Get all DailyBalance entries from the given date onwards
     * 2. For the FIRST date: keep its opening balance, recalculate closing from transactions
     * 3. For subsequent dates: set opening = previous day's closing, recalculate closing
     * 4. Update all affected DailyBalance entries
     */
    suspend fun propagateBalancesForward(fromDate: Long) {
        val normalizedFromDate = normalizeDateToMidnight(fromDate)
        
        // Get all balance records from the affected date onwards
        val affectedBalances = dailyBalanceDao.getBalancesFromDate(normalizedFromDate)
        if (affectedBalances.isEmpty()) return
        
        var isFirstDate = true
        var runningCash = 0.0
        var runningBank = 0.0
        
        // Process each day in sequence
        for (balance in affectedBalances) {
            // Get all ledger transactions for this day
            val transactions = dailyLedgerTransactionDao.getTransactionsForDateSync(balance.date)
            
            // Calculate this day's movements
            var cashIn = 0.0
            var cashOut = 0.0
            var bankIn = 0.0
            var bankOut = 0.0
            
            for (tx in transactions) {
                when (tx.mode) {
                    "CASH_IN" -> cashIn += tx.amount
                    "CASH_OUT" -> cashOut += tx.amount
                    "BANK_IN" -> bankIn += tx.amount
                    "BANK_OUT" -> bankOut += tx.amount
                }
            }
            
            // For the FIRST date: PRESERVE its opening balance (user's manual entry)
            // For subsequent dates: use previous day's closing as opening
            val openingCash: Double
            val openingBank: Double
            
            if (isFirstDate) {
                // Keep the user's manually set opening balance
                openingCash = balance.openingCash
                openingBank = balance.openingBank
                isFirstDate = false
            } else {
                // Use previous day's closing as this day's opening
                openingCash = runningCash
                openingBank = runningBank
            }
            
            // Calculate new closing balances
            val closingCash = openingCash + cashIn - cashOut
            val closingBank = openingBank + bankIn - bankOut
            
            // Update if changed
            if (balance.openingCash != openingCash || 
                balance.openingBank != openingBank ||
                balance.closingCash != closingCash ||
                balance.closingBank != closingBank) {
                    
                val updatedBalance = balance.copy(
                    openingCash = openingCash,
                    openingBank = openingBank,
                    closingCash = closingCash,
                    closingBank = closingBank
                )
                dailyBalanceDao.insertOrUpdateDailyBalance(updatedBalance)
            }
            
            // Set closing for next iteration (will become next day's opening)
            runningCash = closingCash
            runningBank = closingBank
        }
    }
    
    /**
     * Get balance info for a date (sync version for propagation)
     */
    suspend fun getBalanceByDateSync(date: Long) = dailyBalanceDao.getBalanceByDateSync(date)
    
    /**
     * Get transactions for a date (sync version for propagation)
     */
    suspend fun getLedgerTransactionsForDateSync(date: Long) = 
        dailyLedgerTransactionDao.getTransactionsForDateSync(date)
    
    /**
     * Normalize a timestamp to midnight of that day
     */
    private fun normalizeDateToMidnight(timestamp: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    // ========== TRANSACTION EDIT/DELETE WITH SYNC ==========
    
    /**
     * Update customer transaction and sync to daily ledger
     */
    suspend fun updateCustomerTransactionWithSync(
        transactionId: Int,
        newAmount: Double,
        newType: String,
        newPaymentMethod: String,
        newNote: String?,
        date: Long
    ) {
        // 1. Get existing transaction
        val existingTransaction = customerTransactionDao.getTransactionByIdSync(transactionId)
        if (existingTransaction != null) {
            // 2. Update customer transaction
            val updated = existingTransaction.copy(
                amount = newAmount,
                type = newType,
                paymentMethod = newPaymentMethod,
                note = newNote
            )
            customerTransactionDao.update(updated)
            
            // 3. Find and update linked daily ledger entry
            val linkedEntry = dailyLedgerTransactionDao.getBySourceId("customer", transactionId)
            if (linkedEntry != null) {
                val mode = when {
                    newPaymentMethod == "CASH" && newType == "DEBIT" -> "CASH_IN"
                    newPaymentMethod == "CASH" && newType == "CREDIT" -> "CASH_OUT"
                    newPaymentMethod == "BANK" && newType == "DEBIT" -> "BANK_IN"
                    else -> "BANK_OUT"
                }
                
                dailyLedgerTransactionDao.updateTransaction(
                    linkedEntry.copy(
                        amount = newAmount,
                        mode = mode,
                        note = newNote
                    )
                )
            }
        }
    }
    
    /**
     * Delete customer transaction and sync to daily ledger
     */
    suspend fun deleteCustomerTransactionWithSync(transactionId: Int, date: Long) {
        // 1. Find and delete linked daily ledger entry first
        val linkedEntry = dailyLedgerTransactionDao.getBySourceId("customer", transactionId)
        if (linkedEntry != null) {
            dailyLedgerTransactionDao.deleteTransaction(linkedEntry)
        }
        
        // 2. Delete customer transaction
        val transaction = customerTransactionDao.getTransactionByIdSync(transactionId)
        if (transaction != null) {
            customerTransactionDao.deleteTransaction(transaction)
        }
    }
    
    /**
     * Update expense and sync to daily ledger
     */
    suspend fun updateExpenseWithSync(
        expenseId: Int,
        newAmount: Double,
        newCategory: String,
        newNote: String?,
        newPaymentMethod: String,
        date: Long
    ) {
        // 1. Get existing expense
        val existingExpense = dailyExpenseDao.getExpenseById(expenseId)
        if (existingExpense != null) {
            // 2. Update expense
            val updated = existingExpense.copy(
                amount = newAmount,
                category = newCategory,
                note = newNote,
                paymentMethod = newPaymentMethod
            )
            dailyExpenseDao.update(updated)
            
            // 3. Find and update linked daily ledger entry
            val linkedEntry = dailyLedgerTransactionDao.getBySourceId("expense", expenseId)
            if (linkedEntry != null) {
                val mode = if (newPaymentMethod == "CASH") "CASH_OUT" else "BANK_OUT"
                
                dailyLedgerTransactionDao.updateTransaction(
                    linkedEntry.copy(
                        amount = newAmount,
                        party = newCategory,
                        note = newNote,
                        mode = mode
                    )
                )
            }
        }
    }
    
    /**
     * Delete expense and sync to daily ledger
     */
    suspend fun deleteExpenseWithSync(expenseId: Int, date: Long) {
        // 1. Find and delete linked daily ledger entry first
        val linkedEntry = dailyLedgerTransactionDao.getBySourceId("expense", expenseId)
        if (linkedEntry != null) {
            dailyLedgerTransactionDao.deleteTransaction(linkedEntry)
        }
        
        // 2. Delete expense
        dailyExpenseDao.deleteExpenseById(expenseId)
    }
    
    // ========== BULK IMPORT METHODS ==========
    
    /**
     * Insert multiple customers in bulk
     * Returns the count of successfully inserted customers
     */
    suspend fun insertCustomersBulk(customers: List<Customer>): Int {
        var count = 0
        for (customer in customers) {
            try {
                // Check if customer with same name exists
                val existing = customerDao.getCustomerByName(customer.name)
                if (existing == null) {
                    customerDao.insertCustomer(customer)
                    count++
                }
            } catch (e: Exception) {
                // Skip failed inserts
            }
        }
        return count
    }
    
    /**
     * Insert multiple daily ledger transactions in bulk
     */
    suspend fun insertDailyLedgerTransactionsBulk(transactions: List<DailyLedgerTransaction>): Int {
        var count = 0
        for (transaction in transactions) {
            try {
                dailyLedgerTransactionDao.insertOrUpdateTransaction(transaction)
                count++
            } catch (e: Exception) {
                // Skip failed inserts
            }
        }
        return count
    }
    
    /**
     * Insert multiple expenses in bulk
     */
    suspend fun insertExpensesBulk(expenses: List<DailyExpense>): Int {
        var count = 0
        for (expense in expenses) {
            try {
                dailyExpenseDao.insertExpense(expense)
                count++
            } catch (e: Exception) {
                // Skip failed inserts
            }
        }
        return count
    }
    
    /**
     * Insert multiple customer transactions in bulk
     */
    suspend fun insertCustomerTransactionsBulk(transactions: List<CustomerTransaction>): Int {
        var count = 0
        for (transaction in transactions) {
            try {
                customerTransactionDao.insertOrUpdateTransaction(transaction)
                count++
            } catch (e: Exception) {
                // Skip failed inserts
            }
        }
        return count
    }
    
    /**
     * Insert multiple trade transactions in bulk
     */
    suspend fun insertTradeTransactionsBulk(trades: List<TradeTransaction>): Int {
        var count = 0
        for (trade in trades) {
            try {
                tradeTransactionDao.insertOrUpdateTrade(trade)
                count++
            } catch (e: Exception) {
                // Skip failed inserts
            }
        }
        return count
    }
    
    /**
     * Recalculate all daily balances from a start date with initial opening balance
     * This is the core function for bulk import balance calculation
     * 
     * Flow:
     * 1. Create/update balance for startDate with initial opening values
     * 2. Get all unique dates with ledger transactions from startDate onwards
     * 3. For each date, calculate closing = opening + in - out
     * 4. Next date's opening = previous date's closing
     */
    suspend fun recalculateAllDailyBalances(
        startDate: Long,
        initialOpeningCash: Double,
        initialOpeningBank: Double
    ) {
        val normalizedStartDate = normalizeDateToMidnight(startDate)
        
        // Get all unique dates that have ledger transactions from startDate onwards
        val allDates = dailyLedgerTransactionDao.getDistinctDatesFrom(normalizedStartDate)
        
        if (allDates.isEmpty()) {
            // No transactions, just create the initial balance record
            val initialBalance = DailyBalance(
                date = normalizedStartDate,
                openingCash = initialOpeningCash,
                openingBank = initialOpeningBank,
                closingCash = initialOpeningCash,
                closingBank = initialOpeningBank,
                note = null,
                createdAt = System.currentTimeMillis()
            )
            dailyBalanceDao.insertOrUpdateDailyBalance(initialBalance)
            return
        }
        
        var runningCash = initialOpeningCash
        var runningBank = initialOpeningBank
        
        for (date in allDates) {
            // Get all transactions for this date
            val transactions = dailyLedgerTransactionDao.getTransactionsForDateSync(date)
            
            // Calculate movements
            var cashIn = 0.0
            var cashOut = 0.0
            var bankIn = 0.0
            var bankOut = 0.0
            
            for (tx in transactions) {
                when (tx.mode) {
                    "CASH_IN" -> cashIn += tx.amount
                    "CASH_OUT" -> cashOut += tx.amount
                    "BANK_IN" -> bankIn += tx.amount
                    "BANK_OUT" -> bankOut += tx.amount
                }
            }
            
            // Calculate closing
            val closingCash = runningCash + cashIn - cashOut
            val closingBank = runningBank + bankIn - bankOut
            
            // Create/update daily balance
            val balance = DailyBalance(
                date = date,
                openingCash = runningCash,
                openingBank = runningBank,
                closingCash = closingCash,
                closingBank = closingBank,
                note = null,
                createdAt = System.currentTimeMillis()
            )
            dailyBalanceDao.insertOrUpdateDailyBalance(balance)
            
            // Carry forward for next day
            runningCash = closingCash
            runningBank = closingBank
        }
    }
    
    /**
     * Get or create customer by name
     * Returns existing customer ID or creates new customer and returns new ID
     */
    suspend fun getOrCreateCustomerByName(name: String, type: String = "CUSTOMER"): Int {
        val existing = customerDao.getCustomerByName(name)
        return if (existing != null) {
            existing.id
        } else {
            val newCustomer = Customer(
                name = name,
                type = type,
                createdAt = System.currentTimeMillis()
            )
            customerDao.insertCustomer(newCustomer).toInt()
        }
    }
    
    /**
     * Get all customers as a list (for matching during import)
     */
    suspend fun getAllCustomersSync(): List<Customer> = customerDao.getAllCustomersSync()
}
