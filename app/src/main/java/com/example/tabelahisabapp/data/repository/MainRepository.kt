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
    private val dailyLedgerTransactionDao: DailyLedgerTransactionDao
) {

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
    suspend fun deleteTransaction(transaction: CustomerTransaction) = customerTransactionDao.deleteTransaction(transaction)

    // Daily Balance
    fun getDailyBalanceByDate(date: Long) = dailyBalanceDao.getDailyBalanceByDate(date)
    fun getAllDailyBalances() = dailyBalanceDao.getAllDailyBalances()
    suspend fun insertOrUpdateDailyBalance(dailyBalance: DailyBalance) = dailyBalanceDao.insertOrUpdateDailyBalance(dailyBalance)
    suspend fun deleteDailyBalance(dailyBalance: DailyBalance) = dailyBalanceDao.deleteDailyBalance(dailyBalance)

    // Daily Expense
    fun getExpensesByDate(date: Long) = dailyExpenseDao.getExpensesByDate(date)
    suspend fun insertExpense(expense: DailyExpense) = dailyExpenseDao.insertExpense(expense)
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
        voiceNotePath: String? = null
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
        
        // 3. Create daily ledger entry LINKED to customer transaction
        val dailyLedgerTransaction = DailyLedgerTransaction(
            date = normalizedDate,
            mode = ledgerMode,
            amount = amount,
            party = customerName,
            note = note,
            createdAt = System.currentTimeMillis(),
            customerTransactionId = customerTxId
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
}
