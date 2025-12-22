package com.example.tabelahisabapp.ui.customer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.db.entity.CustomerTransaction
import com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: MainRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerId: Int = checkNotNull(savedStateHandle["customerId"])
    private val transactionId: Int? = savedStateHandle["transactionId"]

    suspend fun getTransaction(transactionId: Int): CustomerTransaction? {
        return repository.getTransactionById(transactionId).first()
    }

    fun saveTransaction(
        type: String, 
        amount: Double, 
        date: Long, 
        note: String?, 
        paymentMethod: String = "CASH",
        voiceNotePath: String?, 
        originalCreatedAt: Long? = null
    ) {
        if (amount <= 0) {
            return
        }

        viewModelScope.launch {
            val customer = repository.getCustomerById(customerId).first() ?: return@launch
            
            // Normalize date to midnight
            val normalizedDate = normalizeDateToMidnight(date)
            
            when (type) {
                "PURCHASE" -> {
                    // PURCHASE: Khareedari - "Sirf Record"
                    // 1. Create seller's customer transaction as DEBIT (seller will receive money from us)
                    val customerTransaction = CustomerTransaction(
                        customerId = customerId,
                        type = "DEBIT",  // DEBIT = seller will receive money = "Paisa Mila"
                        amount = amount,
                        date = normalizedDate,
                        note = "🛒 Khareedari: ${note ?: customer.name}",
                        paymentMethod = paymentMethod,
                        voiceNotePath = voiceNotePath,
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertOrUpdateTransaction(customerTransaction)
                    
                    // 2. Create Daily Ledger entry with special "PURCHASE" mode
                    // This mode does NOT affect cash/bank calculations!
                    val ledgerTransaction = DailyLedgerTransaction(
                        date = normalizedDate,
                        mode = "PURCHASE",  // Special mode - doesn't affect cash/bank
                        amount = amount,
                        party = customer.name,
                        note = "🛒 Khareedari: ${note ?: customer.name}",
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertOrUpdateLedgerTransaction(ledgerTransaction)
                }
                
                "EXPENSE" -> {
                    // EXPENSE: Daily expense - save both customer transaction AND daily ledger entry
                    // Customer transaction as CREDIT (we gave money/paid expense for this party)
                    val customerTransaction = CustomerTransaction(
                        customerId = customerId,
                        type = "CREDIT",  // CREDIT = we paid = money went out
                        amount = amount,
                        date = normalizedDate,
                        note = "💰 Kharcha: ${note ?: "Expense"}",
                        paymentMethod = paymentMethod,
                        voiceNotePath = voiceNotePath,
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertOrUpdateTransaction(customerTransaction)
                    
                    // Daily ledger entry (not linked but will show in both places)
                    val ledgerTransaction = DailyLedgerTransaction(
                        date = normalizedDate,
                        mode = if (paymentMethod == "CASH") "CASH_OUT" else "BANK_OUT",
                        amount = amount,
                        party = customer.name,
                        note = "💰 Kharcha: ${note ?: "Expense"}",
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertOrUpdateLedgerTransaction(ledgerTransaction)
                }
                
                else -> {
                    // CREDIT (Udhaar Diya) or DEBIT (Paisa Mila) - normal flow
                    repository.saveCustomerTransactionWithLedgerSync(
                        customerId = customerId,
                        customerName = customer.name,
                        type = type,
                        amount = amount,
                        date = normalizedDate,
                        note = note,
                        paymentMethod = paymentMethod,
                        voiceNotePath = voiceNotePath
                    )
                }
            }
        }
    }
    
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
