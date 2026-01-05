package com.example.tabelahisabapp.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.db.dao.CustomerDao
import com.example.tabelahisabapp.data.db.dao.CustomerTransactionDao
import com.example.tabelahisabapp.data.db.dao.DailyBalanceDao
import com.example.tabelahisabapp.data.db.dao.DailyExpenseDao
import com.example.tabelahisabapp.data.db.entity.CustomerTransaction
import com.example.tabelahisabapp.data.db.entity.DailyBalance
import com.example.tabelahisabapp.data.db.entity.DailyExpense
import com.example.tabelahisabapp.data.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * ViewModel for Voice Recording Flow
 * Coordinates speech recognition and AI parsing
 * Also updates daily balance when transactions are saved
 */
@HiltViewModel
class VoiceFlowViewModel @Inject constructor(
    private val speechRepository: SpeechRepository,
    private val geminiRepository: GeminiRepository,
    private val customerDao: CustomerDao,
    private val transactionDao: CustomerTransactionDao,
    private val dailyBalanceDao: DailyBalanceDao,
    private val dailyExpenseDao: DailyExpenseDao,
    private val repository: MainRepository  // NEW: For auto-sync
) : ViewModel() {

    // Speech recognition state
    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    private val _voiceAmplitude = MutableStateFlow(0f)
    val voiceAmplitude: StateFlow<Float> = _voiceAmplitude.asStateFlow()

    // Parsing state
    private val _parsedTransaction = MutableStateFlow<ParsedTransaction?>(null)
    val parsedTransaction: StateFlow<ParsedTransaction?> = _parsedTransaction.asStateFlow()

    private val _voiceFlowState = MutableStateFlow<VoiceFlowState>(VoiceFlowState.Idle)
    val voiceFlowState: StateFlow<VoiceFlowState> = _voiceFlowState.asStateFlow()

    // Customer data for dropdown and context
    private val _customerNames = MutableStateFlow<List<String>>(emptyList())
    val customerNames: StateFlow<List<String>> = _customerNames.asStateFlow()

    init {
        // Load customer names for AI context
        viewModelScope.launch {
            customerDao.getAllCustomersWithBalance().collect { customers ->
                _customerNames.value = customers.map { it.customer.name }
            }
        }
    }
    
    /**
     * Check if a customer name belongs to a supplier
     * Used to determine if "You Got" should create PURCHASE entry
     */
    fun isSupplier(customerName: String): Flow<Boolean> {
        return customerDao.getAllCustomersWithBalance().map { customers ->
            customers.find { 
                it.customer.name.equals(customerName, ignoreCase = true)
            }?.customer?.let { customer ->
                customer.type == "SELLER" || customer.type == "BOTH"
            } ?: false
        }
    }

    /**
     * Start voice recording
     */
    fun startRecording() {
        if (!speechRepository.isAvailable()) {
            _voiceFlowState.value = VoiceFlowState.Error("Speech recognition not available")
            return
        }

        _isRecording.value = true
        _voiceFlowState.value = VoiceFlowState.Recording
        _transcribedText.value = ""
        _recordingDuration.value = 0L

        viewModelScope.launch {
            speechRepository.startListening(language = "en-IN").collect { result ->
                when (result) {
                    is SpeechResult.Ready -> {
                        _voiceFlowState.value = VoiceFlowState.Recording
                    }
                    is SpeechResult.Started -> {
                        // Start timer
                        startTimer()
                    }
                    is SpeechResult.VolumeChanged -> {
                        _voiceAmplitude.value = result.amplitude
                    }
                    is SpeechResult.Partial -> {
                        _transcribedText.value = result.text
                    }
                    is SpeechResult.Success -> {
                        _transcribedText.value = result.text
                        _isRecording.value = false
                        // Automatically parse after successful transcription
                        parseTranscription(result.text)
                    }
                    is SpeechResult.Ended -> {
                        _isRecording.value = false
                    }
                    is SpeechResult.Error -> {
                        _isRecording.value = false
                        _voiceFlowState.value = VoiceFlowState.Error(result.message)
                    }
                }
            }
        }
    }

    /**
     * Stop recording manually
     */
    fun stopRecording() {
        speechRepository.stopListening()
        _isRecording.value = false
        
        if (_transcribedText.value.isNotEmpty()) {
            parseTranscription(_transcribedText.value)
        }
    }

    /**
     * Cancel recording
     */
    fun cancelRecording() {
        speechRepository.stopListening()
        _isRecording.value = false
        _voiceFlowState.value = VoiceFlowState.Idle
        _transcribedText.value = ""
    }

    /**
     * Parse transcription with Gemini AI
     */
    private fun parseTranscription(text: String) {
        _voiceFlowState.value = VoiceFlowState.Parsing

        viewModelScope.launch {
            when (val result = geminiRepository.parseTransaction(text, _customerNames.value)) {
                is ParseResult.Success -> {
                    _parsedTransaction.value = result.transaction
                    _voiceFlowState.value = VoiceFlowState.Confirmation(result.transaction)
                }
                is ParseResult.Clarification -> {
                    _voiceFlowState.value = VoiceFlowState.NeedsClarification(
                        error = result.error,
                        suggestions = result.suggestions,
                        transcribedText = result.transcribedText
                    )
                }
                is ParseResult.Error -> {
                    _voiceFlowState.value = VoiceFlowState.Error(result.message)
                }
            }
        }
    }

    /**
     * Resolve customer name from suggestions
     */
    fun selectCustomerSuggestion(customerName: String) {
        val current = _parsedTransaction.value ?: return
        _parsedTransaction.value = current.copy(customerName = customerName)
        _voiceFlowState.value = VoiceFlowState.Confirmation(current.copy(customerName = customerName))
    }

    /**
     * Save confirmed transaction
     * Handles both customer transactions and expenses
     * Also updates today's daily balance
     */
    suspend fun saveTransaction(transaction: ParsedTransaction): Boolean {
        return try {
            val today = getTodayStartMillis()
            
            when {
                // PURCHASE type - create daily ledger entry with purchase note
                transaction.transactionType == "PURCHASE" -> {
                    savePurchaseTransaction(today, transaction)
                }
                // Regular expense - update daily balance
                transaction.isExpense -> {
                    updateDailyBalanceForExpense(today, transaction)
                }
                // Customer transaction (CREDIT/DEBIT)
                else -> {
                    saveCustomerTransaction(transaction)
                    updateDailyBalanceForCustomer(today, transaction)
                }
            }

            _voiceFlowState.value = VoiceFlowState.Success
            true
        } catch (e: Exception) {
            _voiceFlowState.value = VoiceFlowState.Error("Failed to save: ${e.message}")
            false
        }
    }
    
    /**
     * Save a purchase transaction directly to Daily Ledger
     * Also creates customer transaction if seller name is provided
     */
    private suspend fun savePurchaseTransaction(today: Long, transaction: ParsedTransaction) {
        // If seller name specified, create customer transaction with ledger sync
        if (transaction.customerName.isNotBlank()) {
            // Find or create seller as customer
            var customerResult = customerDao.getAllCustomersWithBalance().first()
                .find { it.customer.name.equals(transaction.customerName, ignoreCase = true) }
            
            if (customerResult == null) {
                val newCustomer = com.example.tabelahisabapp.data.db.entity.Customer(
                    name = transaction.customerName,
                    phone = null,
                    type = "SELLER",
                    createdAt = System.currentTimeMillis()
                )
                customerDao.insertCustomer(newCustomer)
                customerResult = customerDao.getAllCustomersWithBalance().first()
                    .find { it.customer.name.equals(transaction.customerName, ignoreCase = true) }
            }
            
            if (customerResult != null) {
                // For PURCHASE: Create seller ledger entry + Daily Ledger entry (for Rozana Hisab)
                // Seller ledger shows "Paisa Mila" (they will receive payment)
                // Daily Ledger shows in "Khareedari" section with PURCHASE mode
                
                // 1. Create seller's customer transaction
                val customerTransaction = com.example.tabelahisabapp.data.db.entity.CustomerTransaction(
                    customerId = customerResult.customer.id,
                    type = "DEBIT",  // DEBIT = seller will receive money from us
                    amount = transaction.amount,
                    date = today,
                    note = "Khareedari: ${transaction.originalText}",
                    paymentMethod = transaction.paymentMethod,
                    voiceNotePath = null,
                    createdAt = System.currentTimeMillis()
                )
                transactionDao.insertOrUpdateTransaction(customerTransaction)
                
                // 2. Create Daily Ledger entry for Rozana Hisab "Khareedari" section
                val ledgerTransaction = com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction(
                    date = today,
                    mode = "PURCHASE",  // Special mode for Khareedari section
                    amount = transaction.amount,
                    party = transaction.customerName,
                    note = "Khareedari: ${transaction.originalText}",
                    createdAt = System.currentTimeMillis()
                )
                repository.insertOrUpdateLedgerTransaction(ledgerTransaction)
                return
            }
        }
        
        // Fallback: unnamed purchase to daily ledger only
        val mode = if (transaction.paymentMethod == "CASH") "CASH_OUT" else "BANK_OUT"
        val ledgerTransaction = com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction(
            date = today,
            mode = mode,
            amount = transaction.amount,
            party = null,
            note = "Purchase: ${transaction.customerName}",
            createdAt = System.currentTimeMillis()
        )
        repository.insertOrUpdateLedgerTransaction(ledgerTransaction)
    }
    
    /**
     * Save a customer/seller transaction
     * Now uses auto-sync to also create daily ledger entry
     */
    private suspend fun saveCustomerTransaction(transaction: ParsedTransaction) {
        // Find customer by name
        var customerResult = customerDao.getAllCustomersWithBalance().first()
            .find { it.customer.name.equals(transaction.customerName, ignoreCase = true) }
        
        // Create customer if not found
        if (customerResult == null) {
            val newCustomer = com.example.tabelahisabapp.data.db.entity.Customer(
                name = transaction.customerName,
                phone = null,
                type = "CUSTOMER",
                createdAt = System.currentTimeMillis()
            )
            customerDao.insertCustomer(newCustomer)
            
            // Fetch the newly created customer
            customerResult = customerDao.getAllCustomersWithBalance().first()
                .find { it.customer.name.equals(transaction.customerName, ignoreCase = true) }
        }
        
        if (customerResult == null) {
            throw Exception("Failed to create customer: ${transaction.customerName}")
        }

        // NEW: Use auto-sync method to save both customer transaction and daily ledger entry
        repository.saveCustomerTransactionWithLedgerSync(
            customerId = customerResult.customer.id,
            customerName = transaction.customerName,
            type = transaction.transactionType,
            amount = transaction.amount,
            date = System.currentTimeMillis(),
            note = transaction.originalText,
            paymentMethod = transaction.paymentMethod,
            voiceNotePath = null
        )
    }
    
    /**
     * Update daily balance when an expense is added via voice
     * Also saves to daily_expenses for detailed record tracking
     * Now also creates Daily Ledger entry for visibility in four-section view
     * 
     * IMPORTANT: Expenses do NOT create customer entries!
     * Expenses only create:
     * 1. DailyExpense record (for expense tracking)
     * 2. DailyLedgerTransaction (for Rozana Hisab view with sourceType = EXPENSE)
     */
    private suspend fun updateDailyBalanceForExpense(today: Long, transaction: ParsedTransaction) {
        val existingBalance = dailyBalanceDao.getDailyBalanceByDate(today).first()
        val amount = transaction.amount
        
        // 1. Save detailed expense record for the expense list
        // customerName here is actually the expense category/description (e.g., "Milk", "Petrol")
        val expenseId = dailyExpenseDao.insertExpenseAndGetId(
            DailyExpense(
                date = today,
                category = transaction.customerName, // This is the expense category (Milk, Petrol, etc)
                amount = amount,
                paymentMethod = transaction.paymentMethod,
                createdAt = System.currentTimeMillis()
            )
        )
        
        // 2. Create Daily Ledger entry for four-section view (Daily Kharcha section)
        // Link it to the expense via sourceType and sourceId for sync
        val mode = if (transaction.paymentMethod == "CASH") "CASH_OUT" else "BANK_OUT"
        val ledgerTransaction = com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction(
            date = today,
            mode = mode,
            amount = amount,
            party = transaction.customerName, // Show expense category as party
            note = null,
            createdAt = System.currentTimeMillis(),
            sourceType = com.example.tabelahisabapp.data.db.entity.SourceType.EXPENSE,
            sourceId = expenseId.toInt()
        )
        repository.insertOrUpdateLedgerTransaction(ledgerTransaction)

        // 3. Update summary note and totals
        val entryNote = "${transaction.customerName}: ₹${String.format("%.0f", amount)}"
        
        if (existingBalance != null) {
            // Update existing balance - reduce closing cash for expense
            val newClosingCash = if (transaction.paymentMethod == "CASH") {
                existingBalance.closingCash - amount
            } else {
                existingBalance.closingCash
            }
            val newClosingBank = if (transaction.paymentMethod == "BANK") {
                existingBalance.closingBank - amount
            } else {
                existingBalance.closingBank
            }
            
            // Append to notes
            val updatedNote = if (existingBalance.note.isNullOrBlank()) {
                "📝 Voice Entries:\n- $entryNote"
            } else {
                "${existingBalance.note}\n- $entryNote"
            }
            
            dailyBalanceDao.insertOrUpdateDailyBalance(
                existingBalance.copy(
                    closingCash = newClosingCash,
                    closingBank = newClosingBank,
                    note = updatedNote
                )
            )
        } else {
            // Create new daily balance for today
            val newBalance = DailyBalance(
                date = today,
                openingCash = 0.0,
                openingBank = 0.0,
                closingCash = if (transaction.paymentMethod == "CASH") -amount else 0.0,
                closingBank = if (transaction.paymentMethod == "BANK") -amount else 0.0,
                note = "📝 Voice Entries:\n- $entryNote",
                createdAt = System.currentTimeMillis()
            )
            dailyBalanceDao.insertOrUpdateDailyBalance(newBalance)
        }
    }
    
    /**
     * Update daily balance when a customer transaction is added via voice
     */
    private suspend fun updateDailyBalanceForCustomer(today: Long, transaction: ParsedTransaction) {
        val existingBalance = dailyBalanceDao.getDailyBalanceByDate(today).first()
        val amount = transaction.amount
        
        val entryType = if (transaction.transactionType == "CREDIT") "➡️ Given" else "⬅️ Received"
        val entryNote = "$entryType ${transaction.customerName}: ₹${String.format("%.0f", amount)}"
        
        if (existingBalance != null) {
            // CREDIT = gave money = reduce balance
            // DEBIT = received money = increase balance
            val amountChange = if (transaction.transactionType == "CREDIT") -amount else amount
            
            // Only update the payment method that was used
            val newClosingCash = if (transaction.paymentMethod == "CASH") {
                existingBalance.closingCash + amountChange
            } else {
                existingBalance.closingCash
            }
            val newClosingBank = if (transaction.paymentMethod == "BANK") {
                existingBalance.closingBank + amountChange
            } else {
                existingBalance.closingBank
            }
            
            // Append to notes
            val updatedNote = if (existingBalance.note.isNullOrBlank()) {
                "📝 Voice Entries:\n- $entryNote"
            } else {
                "${existingBalance.note}\n- $entryNote"
            }
            
            dailyBalanceDao.insertOrUpdateDailyBalance(
                existingBalance.copy(
                    closingCash = newClosingCash,
                    closingBank = newClosingBank,
                    note = updatedNote
                )
            )
        } else {
            // Create new daily balance for today
            val cashValue = if (transaction.paymentMethod == "CASH") {
                if (transaction.transactionType == "CREDIT") -amount else amount
            } else 0.0
            val bankValue = if (transaction.paymentMethod == "BANK") {
                if (transaction.transactionType == "CREDIT") -amount else amount
            } else 0.0
            
            val newBalance = DailyBalance(
                date = today,
                openingCash = 0.0,
                openingBank = 0.0,
                closingCash = cashValue,
                closingBank = bankValue,
                note = "📝 Voice Entries:\n- $entryNote",
                createdAt = System.currentTimeMillis()
            )
            dailyBalanceDao.insertOrUpdateDailyBalance(newBalance)
        }
    }
    
    /**
     * Get start of today in milliseconds
     */
    private fun getTodayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Reset to idle state
     */
    fun reset() {
        _voiceFlowState.value = VoiceFlowState.Idle
        _transcribedText.value = ""
        _parsedTransaction.value = null
        _recordingDuration.value = 0L
    }

    private fun startTimer() {
        viewModelScope.launch {
            while (_isRecording.value) {
                kotlinx.coroutines.delay(1000)
                _recordingDuration.value += 1000
            }
        }
    }
}

/**
 * Voice flow states
 */
sealed class VoiceFlowState {
    object Idle : VoiceFlowState()
    object Recording : VoiceFlowState()
    object Parsing : VoiceFlowState()
    data class Confirmation(val transaction: ParsedTransaction) : VoiceFlowState()
    data class NeedsClarification(
        val error: String,
        val suggestions: List<String>,
        val transcribedText: String
    ) : VoiceFlowState()
    object Success : VoiceFlowState()
    data class Error(val message: String) : VoiceFlowState()
}
