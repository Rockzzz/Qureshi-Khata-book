package com.example.tabelahisabapp.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerListViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    val customers = repository.getAllCustomersWithBalance()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun deleteCustomer(customer: com.example.tabelahisabapp.data.db.entity.Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }
}
