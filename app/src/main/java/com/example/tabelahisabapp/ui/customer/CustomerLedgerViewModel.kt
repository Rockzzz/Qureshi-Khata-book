package com.example.tabelahisabapp.ui.customer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerLedgerViewModel @Inject constructor(
    private val repository: MainRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val customerId: Int = checkNotNull(savedStateHandle["customerId"])

    val customer = repository.getCustomerById(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val transactions = repository.getTransactionsForCustomer(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val balanceSummary = repository.getBalanceSummaryForCustomer(customerId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun deleteTransaction(transaction: com.example.tabelahisabapp.data.db.entity.CustomerTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
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
