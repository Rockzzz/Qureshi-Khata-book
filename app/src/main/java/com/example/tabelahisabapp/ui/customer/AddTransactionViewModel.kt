package com.example.tabelahisabapp.ui.customer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.repository.MainRepository
import com.example.tabelahisabapp.data.db.entity.CustomerTransaction
import com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction
import com.example.tabelahisabapp.data.db.entity.LedgerMode
import com.example.tabelahisabapp.data.db.entity.SourceType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionUiModel(
    val id: Int,
    val type: String,
    val amount: Double,
    val date: Long,
    val note: String?,
    val paymentMethod: String,
    val createdAt: Long
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val repository: MainRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val transactionContext: String = savedStateHandle["transactionContext"] ?: "CUSTOMER"
    val initialType: String = savedStateHandle["initialType"] ?: "DEBIT"
    val transactionId: Int? = savedStateHandle.get<Int>("transactionId")
    
    // Try multiple ways to get the customer/supplier ID (could be Int or String in SavedStateHandle)
    private val customerId: Int? = savedStateHandle.get<Int>("customerId") 
        ?: savedStateHandle.get<String>("customerId")?.toIntOrNull()
    
    private val supplierId: Int? = savedStateHandle.get<Int>("supplierId")
        ?: savedStateHandle.get<String>("supplierId")?.toIntOrNull()

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()
    
    init {
        // Debug: Log the IDs to verify they're being retrieved
        android.util.Log.d("AddTransactionViewModel", "Context: $transactionContext, CustomerId: $customerId, SupplierId: $supplierId")
    }

    suspend fun getTransactionUiModel(transId: Int): TransactionUiModel? {
        val tx = repository.getTransactionById(transId).first()
        return tx?.let {
            val uiType = if (transactionContext == "SUPPLIER") {
                when(it.type) {
                    "PAYMENT" -> "CREDIT"
                    "PURCHASE" -> "DEBIT"
                    else -> it.type
                }
            } else {
                it.type
            }
            TransactionUiModel(it.id, uiType, it.amount, it.date, it.note, it.paymentMethod ?: "CASH", it.createdAt)
        }
    }

    fun saveTransaction(
        type: String, // UI Type: "CREDIT" (Red) or "DEBIT" (Green)
        amount: Double,
        date: Long,
        note: String?,
        paymentMethod: String,
        voiceNotePath: String? = null,
        transactionId: Int? = null
    ) {
        viewModelScope.launch {
            try {
                val targetId = if (transactionContext == "SUPPLIER") supplierId else customerId
                
                if (targetId == null) {
                    android.util.Log.e("AddTransactionViewModel", "ERROR: Target ID is null! Context: $transactionContext")
                    _saveSuccess.value = false
                    return@launch
                }
                
                val entity = repository.getCustomerById(targetId).first()
                
                if (entity == null) {
                    android.util.Log.e("AddTransactionViewModel", "ERROR: Entity not found for ID: $targetId")
                    _saveSuccess.value = false
                    return@launch
                }

            if (transactionId != null) {
                // Update Logic - Sync changes to linked Daily Ledger Transaction
                val existing = repository.getTransactionById(transactionId).first() ?: return@launch
                
                // Determine persistent type
                val persistentType = if (transactionContext == "SUPPLIER") {
                    when(type) {
                        "CREDIT" -> "PAYMENT"
                        "DEBIT" -> "PURCHASE"
                        else -> type
                    }
                } else type

                // Check if date changed - need to handle ledger entry move
                val dateChanged = existing.date != date
                val oldDate = existing.date

                // Use sync method to update both CustomerTransaction and linked DailyLedgerTransaction
                repository.updateCustomerTransactionWithSync(
                    transactionId = transactionId,
                    newAmount = amount,
                    newType = persistentType,
                    newPaymentMethod = paymentMethod,
                    newNote = note,
                    date = date
                )
                
                // If date changed, we need to:
                // 1. Delete the ledger entry from old date
                // 2. Create new ledger entry on new date
                // 3. Recalculate balances for both dates
                if (dateChanged) {
                    repository.moveLedgerEntryToNewDate(
                        transactionId = transactionId,
                        oldDate = oldDate,
                        newDate = date,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        type = persistentType,
                        party = entity.name,
                        note = note,
                        isSupplier = transactionContext == "SUPPLIER"
                    )
                    // Recalculate balances for old date (will propagate forward)
                    repository.propagateBalancesForward(oldDate)
                }
                // Always recalculate balances for the transaction date
                repository.propagateBalancesForward(date)
                
            } else {
                // Create New Logic
                if (transactionContext == "SUPPLIER") {
                   if (type == "DEBIT") { 
                       // Supplier PURCHASE (Goods In) -> Use custom logic for correct Ledger Mode
                       savePurchase(targetId, entity.name, amount, date, note, paymentMethod, voiceNotePath)
                   } else {
                       // Supplier PAYMENT (Money Out) -> Use Standard Logic with isSupplier = true
                       repository.saveCustomerTransactionWithLedgerSync(
                           customerId = targetId,
                           customerName = entity.name,
                           type = "CREDIT", // Maps to PAYMENT behavior in Ledger (CASH_OUT)
                           amount = amount,
                           date = date,
                           note = note,
                           paymentMethod = paymentMethod,
                           voiceNotePath = voiceNotePath,
                           isSupplier = true  // Mark as supplier so it appears under "Payment Diya" not "Rozana Kharcha"
                       )
                   }
                } else {
                    // CUSTOMER
                    if (type == "PURCHASE") { // Legacy support if needed, but UI only shows CREDIT/DEBIT
                        savePurchase(targetId, entity.name, amount, date, note, paymentMethod, voiceNotePath)
                    } else {
                        repository.saveCustomerTransactionWithLedgerSync(
                            customerId = targetId,
                            customerName = entity.name,
                            type = type,
                            amount = amount,
                            date = date,
                            note = note,
                            paymentMethod = paymentMethod,
                            voiceNotePath = voiceNotePath
                        )
                    }
                }
            }
            _saveSuccess.value = true
            } catch (e: Exception) {
                android.util.Log.e("AddTransactionViewModel", "Error saving transaction", e)
                _saveSuccess.value = false
            }
        }
    }
    
    private suspend fun savePurchase(
        customerId: Int,
        partyName: String,
        amount: Double,
        date: Long,
        note: String?,
        paymentMethod: String,
        voiceNotePath: String?
    ) {
        // CRITICAL: Normalize date to midnight for proper daily ledger grouping
        val normalizedDate = normalizeDateToMidnight(date)
        
         val tx = CustomerTransaction(
            customerId = customerId,
            type = "PURCHASE", // Or "DEBIT" if we want unified types? Let's use "PURCHASE" for clarity in DB
            amount = amount,
            date = normalizedDate,  // Use normalized date!
            note = note,
            paymentMethod = paymentMethod,
            voiceNotePath = voiceNotePath,
            createdAt = System.currentTimeMillis()
        )
        val txId = repository.insertOrUpdateTransactionAndGetId(tx).toInt()
        
        // For supplier purchases: use PURCHASE mode (no cash impact, record only)
        // For customer purchases (if any): use CASH_OUT/BANK_OUT
        val isSupplierPurchase = transactionContext == "SUPPLIER"
        
        val ledgerMode = if (isSupplierPurchase) {
            "PURCHASE" // Record only, no cash impact
        } else {
            if (paymentMethod == "CASH") LedgerMode.CASH_OUT else LedgerMode.BANK_OUT
        }
        
        val sourceType = if (isSupplierPurchase) SourceType.SUPPLIER else SourceType.CUSTOMER
        
        val ledgerEntry = DailyLedgerTransaction(
            date = normalizedDate,  // Use normalized date!
            mode = ledgerMode,
            amount = amount,
            party = partyName,
            note = note,
            createdAt = System.currentTimeMillis(),
            customerTransactionId = txId,
            sourceType = sourceType,
            sourceId = if (isSupplierPurchase) customerId else txId
        )
        repository.insertOrUpdateLedgerTransaction(ledgerEntry)
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
