package com.example.tabelahisabapp.ui.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.db.entity.CustomerTransaction
import com.example.tabelahisabapp.data.db.entity.DailyBalance
import com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DailySummaryViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {
    
    init {
        // Auto-initialize today's balance from yesterday's closing
        viewModelScope.launch {
            ensureTodayBalanceExists()
        }
    }
    
    /**
     * Ensures today's balance record exists.
     * Sets opening balance from yesterday's closing balance.
     * This is the core carry-forward logic.
     */
    private suspend fun ensureTodayBalanceExists() {
        val today = getTodayDateNormalized()
        val existingBalance = repository.getBalanceByDateSync(today)
        
        if (existingBalance == null) {
            // Get yesterday's closing balance
            val yesterday = getYesterdayDateNormalized()
            val yesterdayBalance = repository.getBalanceByDateSync(yesterday)
            
            val openingCash = yesterdayBalance?.closingCash ?: 0.0
            val openingBank = yesterdayBalance?.closingBank ?: 0.0
            
            // Get today's transactions to calculate closing
            val transactions = repository.getLedgerTransactionsForDateSync(today)
            val (closingCash, closingBank) = calculateBalancesFromTransactions(
                openingCash, openingBank, transactions
            )
            
            // Create today's balance record
            val todayBalance = DailyBalance(
                date = today,
                openingCash = openingCash,
                openingBank = openingBank,
                closingCash = closingCash,
                closingBank = closingBank,
                note = null,
                createdAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateDailyBalance(todayBalance)
        }
    }

    fun getTodayDateNormalized(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    private fun getYesterdayDateNormalized(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun normalizeDateToMidnight(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val todayBalance = repository.getDailyBalanceByDate(getTodayDateNormalized())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentBalances = repository.getAllDailyBalances()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // For autocomplete suggestions in Daily Entry
    val allCustomers = repository.getAllCustomersWithBalance()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Today's detailed activity for the Home Screen
    val todayExpenses = repository.getExpensesByDate(getTodayDateNormalized())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTransactions = repository.getTransactionsByDateWithCustomer(getTodayDateNormalized())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // Live ledger transactions for today (for real-time balance calculation on Home screen)
    val todayLedgerTransactions = repository.getLedgerTransactionsByDate(getTodayDateNormalized())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // All distinct dates that have ledger transactions (for showing day cards even without DailyBalance)
    val allLedgerDates = repository.getAllLedgerDates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getBalanceByDate(date: Long) = repository.getDailyBalanceByDate(normalizeDateToMidnight(date))
    
    fun getExpensesByDate(date: Long) = repository.getExpensesByDate(normalizeDateToMidnight(date))
    
    fun getTransactionsByDateWithCustomer(date: Long) = repository.getTransactionsByDateWithCustomer(normalizeDateToMidnight(date))
    
    fun getPreviousDayBalance(currentDate: Long) = repository.getPreviousDayBalance(normalizeDateToMidnight(currentDate))
    
    // New Daily Ledger methods
    fun getLedgerTransactionsByDate(date: Long) = repository.getLedgerTransactionsByDate(normalizeDateToMidnight(date))
    
    // Get DERIVED ledger entries (single source of truth from CustomerTransaction + DailyExpense)
    suspend fun getDerivedLedgerForDate(date: Long) = repository.getDerivedDailyLedgerForDate(normalizeDateToMidnight(date))
    
    suspend fun checkLedgerExistsForDate(date: Long): Boolean {
        val normalizedDate = normalizeDateToMidnight(date)
        val balance = repository.getDailyBalanceByDate(normalizedDate).first()
        return balance != null
    }
    
    fun calculateBalancesFromTransactions(
        openingCash: Double,
        openingBank: Double,
        transactions: List<DailyLedgerTransaction>
    ): Pair<Double, Double> {
        val totals = transactions.fold(
            TransactionTotals(0.0, 0.0, 0.0, 0.0)
        ) { acc, transaction ->
            when (transaction.mode) {
                "CASH_IN" -> acc.copy(totalCashIn = acc.totalCashIn + transaction.amount)
                "CASH_OUT" -> acc.copy(totalCashOut = acc.totalCashOut + transaction.amount)
                "BANK_IN" -> acc.copy(totalBankIn = acc.totalBankIn + transaction.amount)
                "BANK_OUT" -> acc.copy(totalBankOut = acc.totalBankOut + transaction.amount)
                else -> acc
            }
        }
        
        val closingCash = openingCash + totals.totalCashIn - totals.totalCashOut
        val closingBank = openingBank + totals.totalBankIn - totals.totalBankOut
        
        return Pair(closingCash, closingBank)
    }
    
    data class TransactionTotals(
        val totalCashIn: Double = 0.0,
        val totalCashOut: Double = 0.0,
        val totalBankIn: Double = 0.0,
        val totalBankOut: Double = 0.0
    )
    
    suspend fun saveDailyLedger(
        date: Long,
        openingCash: Double,
        openingBank: Double,
        transactions: List<DailyLedgerTransaction>,
        note: String? = null,
        originalCreatedAt: Long? = null
    ) {
        val normalizedDate = normalizeDateToMidnight(date)
        val createdAt = originalCreatedAt ?: System.currentTimeMillis()
        
        // Calculate closing balances from transactions
        val (closingCash, closingBank) = calculateBalancesFromTransactions(
            openingCash,
            openingBank,
            transactions
        )
        
        // Save daily balance
        val dailyBalance = DailyBalance(
            date = normalizedDate,
            openingCash = openingCash,
            openingBank = openingBank,
            closingCash = closingCash,
            closingBank = closingBank,
            note = note,
            createdAt = createdAt
        )
        repository.insertOrUpdateDailyBalance(dailyBalance)
        
        // Clear existing transactions for this date
        repository.deleteLedgerTransactionsForDate(normalizedDate)
        
        // Save all transactions
        transactions.forEach { transaction ->
            repository.insertOrUpdateLedgerTransaction(
                transaction.copy(date = normalizedDate)
            )
        }
        
        // CRITICAL: Propagate closing balances to update next day's opening balance
        // This ensures the carry-forward logic works correctly
        repository.propagateBalancesForward(normalizedDate)
    }
    
    suspend fun saveOrUpdateLedgerTransaction(transaction: DailyLedgerTransaction) {
        repository.insertOrUpdateLedgerTransaction(transaction)
    }
    
    /**
     * Update ledger transaction WITH two-way sync to Customer Ledger
     * Use this when editing party/customer name to keep both ledgers in sync
     */
    suspend fun updateLedgerTransactionWithSync(
        transaction: DailyLedgerTransaction,
        newPartyName: String?,
        newAmount: Double,
        newPaymentMethod: String
    ) {
        repository.updateLedgerWithCustomerSync(
            ledgerTransaction = transaction,
            newPartyName = newPartyName,
            newAmount = newAmount,
            newPaymentMethod = newPaymentMethod
        )
    }
    
    /**
     * Delete ledger transaction WITH two-way sync to Customer Ledger
     * This also deletes the linked customer transaction
     */
    suspend fun deleteLedgerTransaction(transaction: DailyLedgerTransaction) {
        repository.deleteLedgerTransactionWithSync(transaction)
    }

    // Simplified version for AddEditDailyEntryScreen (legacy support)
    suspend fun saveDailyBalance(
        date: Long,
        openingCash: Double,
        openingBank: Double,
        closingCash: Double,
        closingBank: Double,
        note: String?,
        originalCreatedAt: Long? = null
    ) {
        val normalizedDate = normalizeDateToMidnight(date)
        val createdAt = originalCreatedAt ?: System.currentTimeMillis()
        
        // Save daily balance totals only (no transactions)
        val dailyBalance = DailyBalance(
            date = normalizedDate,
            openingCash = openingCash,
            openingBank = openingBank,
            closingCash = closingCash,
            closingBank = closingBank,
            note = note,
            createdAt = createdAt
        )
        repository.insertOrUpdateDailyBalance(dailyBalance)
        
        // Propagate closing balances to next day's opening
        repository.propagateBalancesForward(normalizedDate)
    }

    fun deleteDailyBalance(dailyBalance: DailyBalance) {
        viewModelScope.launch {
            repository.deleteDailyBalance(dailyBalance)
        }
    }


    
    fun createCustomer(name: String, type: String = "CUSTOMER") {
        viewModelScope.launch {
            val customer = com.example.tabelahisabapp.data.db.entity.Customer(
                name = name.trim(),
                phone = null,
                type = type,
                createdAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateCustomer(customer)
        }
    }
    
    /**
     * Update opening balance for a specific date
     * This will:
     * 1. Create or update DailyBalance record for that date
     * 2. Recalculate closing balance based on transactions
     * 3. Propagate changes to all subsequent dates (carry-forward)
     */
    fun updateOpeningBalance(
        date: Long,
        openingCash: Double,
        openingBank: Double
    ) {
        viewModelScope.launch {
            val normalizedDate = normalizeDateToMidnight(date)
            
            // Get transactions for this date to calculate closing
            val transactions = repository.getLedgerTransactionsForDateSync(normalizedDate)
            val (closingCash, closingBank) = calculateBalancesFromTransactions(
                openingCash, openingBank, transactions
            )
            
            // Get existing balance or create new one
            val existingBalance = repository.getBalanceByDateSync(normalizedDate)
            val dailyBalance = DailyBalance(
                id = existingBalance?.id ?: 0,
                date = normalizedDate,
                openingCash = openingCash,
                openingBank = openingBank,
                closingCash = closingCash,
                closingBank = closingBank,
                note = existingBalance?.note,
                createdAt = existingBalance?.createdAt ?: System.currentTimeMillis()
            )
            
            repository.insertOrUpdateDailyBalance(dailyBalance)
            
            // Propagate balances to all days after this date
            repository.propagateBalancesForward(normalizedDate)
        }
    }
    
    /**
     * Recalculate closing balance for a date based on transactions
     * and propagate to subsequent dates
     */
    fun recalculateBalances(date: Long) {
        viewModelScope.launch {
            repository.propagateBalancesForward(normalizeDateToMidnight(date))
        }
    }
    
    /**
     * Refresh function for pull-to-refresh
     * Re-ensures today's balance exists and is current
     */
    fun refresh() {
        viewModelScope.launch {
            ensureTodayBalanceExists()
        }
    }
}
