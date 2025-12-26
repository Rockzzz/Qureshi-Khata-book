package com.example.tabelahisabapp.ui.supplier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.db.entity.CustomerTransaction
import com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction
import com.example.tabelahisabapp.data.db.entity.LedgerMode
import com.example.tabelahisabapp.data.db.entity.SourceType
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * ViewModel for Supplier Transaction management
 * 
 * Handles two types of supplier transactions:
 * 
 * 1. PURCHASE (Khareedari):
 *    - Creates a DEBIT in CustomerTransaction (increases what you owe)
 *    - NO entry in DailyLedger (no cash movement)
 *    - Balance becomes more negative
 * 
 * 2. PAYMENT (Diya):
 *    - Creates a CREDIT in CustomerTransaction (reduces what you owe)
 *    - Creates CASH_OUT/BANK_OUT in DailyLedger (cash leaves)
 *    - Balance becomes less negative (or positive if overpaid)
 */
@HiltViewModel
class SupplierTransactionViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    /**
     * Save supplier transaction with appropriate ledger sync
     * 
     * @param transactionType "PURCHASE" or "PAYMENT"
     * @param paymentMethod Only used for PAYMENT type (CASH or BANK)
     */
    fun saveSupplierTransaction(
        supplierId: Int,
        supplierName: String,
        transactionType: String,
        amount: Double,
        date: Long,
        paymentMethod: String?,
        note: String?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val normalizedDate = normalizeDateToMidnight(date)
                
                when (transactionType) {
                    "PURCHASE" -> savePurchaseTransaction(
                        supplierId = supplierId,
                        supplierName = supplierName,
                        amount = amount,
                        date = normalizedDate,
                        note = note
                    )
                    "PAYMENT" -> savePaymentTransaction(
                        supplierId = supplierId,
                        supplierName = supplierName,
                        amount = amount,
                        date = normalizedDate,
                        paymentMethod = paymentMethod ?: "CASH",
                        note = note
                    )
                }
                
                _saveSuccess.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * PURCHASE Transaction:
     * - Creates liability (you owe more)
     * - NO cash movement BUT syncs to daily ledger as PURCHASE (record only)
     * - Records as DEBIT in CustomerTransaction
     */
    private suspend fun savePurchaseTransaction(
        supplierId: Int,
        supplierName: String,
        amount: Double,
        date: Long,
        note: String?
    ) {
        android.util.Log.d("SupplierTxVM", "=== SAVING PURCHASE TRANSACTION ===")
        android.util.Log.d("SupplierTxVM", "Supplier: $supplierName, Amount: $amount, Date: $date")
        
        // Create CustomerTransaction as DEBIT (you owe more to supplier)
        val transaction = CustomerTransaction(
            customerId = supplierId,
            type = "DEBIT", // DEBIT for supplier = you owe them more
            amount = amount,
            date = date,
            note = note ?: "Purchase from $supplierName",
            voiceNotePath = null,
            paymentMethod = "CREDIT", // Credit purchase, no cash
            createdAt = System.currentTimeMillis()
        )
        
        // Insert transaction and get ID
        val transactionId = repository.insertOrUpdateTransactionAndGetId(transaction)
        android.util.Log.d("SupplierTxVM", "CustomerTransaction saved with ID: $transactionId")
        
        // Create DailyLedgerTransaction with PURCHASE mode (for record only, no cash impact)
        val ledgerEntry = DailyLedgerTransaction(
            date = date,
            mode = "PURCHASE", // Special mode for purchases - record only, no cash flow
            amount = amount,
            party = supplierName,
            note = note ?: "Khareedari - $supplierName",
            createdAt = System.currentTimeMillis(),
            customerTransactionId = transactionId.toInt(),
            sourceType = SourceType.SUPPLIER,
            sourceId = supplierId
        )
        
        android.util.Log.d("SupplierTxVM", "Creating DailyLedger entry: mode=${ledgerEntry.mode}, sourceType=${ledgerEntry.sourceType}")
        repository.insertOrUpdateLedgerTransaction(ledgerEntry)
        android.util.Log.d("SupplierTxVM", "=== LEDGER ENTRY CREATED SUCCESSFULLY ===")
    }
    
    /**
     * PAYMENT Transaction:
     * - Reduces liability (you owe less)
     * - CASH/BANK outflow
     * - Records as CREDIT in CustomerTransaction
     * - Syncs to DailyLedger
     */
    private suspend fun savePaymentTransaction(
        supplierId: Int,
        supplierName: String,
        amount: Double,
        date: Long,
        paymentMethod: String,
        note: String?
    ) {
        // Create CustomerTransaction as CREDIT (reducing what you owe)
        val transaction = CustomerTransaction(
            customerId = supplierId,
            type = "CREDIT", // CREDIT for supplier = you paid them, owe less
            amount = amount,
            date = date,
            note = note ?: "Payment to $supplierName",
            voiceNotePath = null,
            paymentMethod = paymentMethod,
            createdAt = System.currentTimeMillis()
        )
        
        // Insert transaction and get ID
        val transactionId = repository.insertOrUpdateTransactionAndGetId(transaction)
        
        // Create DailyLedgerTransaction for cash book sync
        val ledgerMode = if (paymentMethod == "CASH") LedgerMode.CASH_OUT else LedgerMode.BANK_OUT
        
        val ledgerEntry = DailyLedgerTransaction(
            date = date,
            mode = ledgerMode,
            amount = amount,
            party = supplierName,
            note = note ?: "Payment to $supplierName",
            createdAt = System.currentTimeMillis(),
            customerTransactionId = transactionId.toInt(),
            sourceType = SourceType.SUPPLIER,
            sourceId = supplierId
        )
        
        repository.insertOrUpdateLedgerTransaction(ledgerEntry)
    }
    
    /**
     * Normalize date to midnight
     */
    private fun normalizeDateToMidnight(date: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = date
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
    
    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}
