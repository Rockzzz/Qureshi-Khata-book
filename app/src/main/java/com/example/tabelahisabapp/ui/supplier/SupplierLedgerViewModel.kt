package com.example.tabelahisabapp.ui.supplier

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.repository.MainRepository
import com.example.tabelahisabapp.data.db.entity.CustomerTransaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SupplierLedgerViewModel @Inject constructor(
    private val repository: MainRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val supplierId: Int = checkNotNull(savedStateHandle["supplierId"])

    // Suppliers are stored as Customers in this DB schema
    val supplier = repository.getCustomerById(supplierId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions = repository.getTransactionsForCustomer(supplierId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteTransaction(transaction: CustomerTransaction) {
        viewModelScope.launch {
            // Use sync method to delete both supplier transaction AND linked daily ledger entry
            repository.deleteSupplierTransactionWithSync(transaction.id, transaction.date)
            
            // Recalculate balances from the transaction date forward
            repository.propagateBalancesForward(transaction.date)
            
            // Also delete voice note file if exists
            transaction.voiceNotePath?.let { path ->
                try {
                    java.io.File(path).delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
