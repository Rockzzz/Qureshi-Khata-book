package com.example.tabelahisabapp.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.db.entity.Customer
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
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

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
        }
    }
    
    /**
     * Load customer/supplier for editing
     */
    fun loadCustomerForEdit(customerId: Int, onLoaded: (Customer) -> Unit) {
        viewModelScope.launch {
            val customer = repository.getCustomerById(customerId).first()
            customer?.let { onLoaded(it) }
        }
    }
    
    /**
     * Save supplier (type = SELLER)
     * 
     * Suppliers are contacts you buy from.
     * Opening balance = what you owe them (stored as negative in transactions)
     */
    fun saveSupplier(
        id: Int? = null,
        name: String,
        phone: String?,
        email: String?,
        businessName: String?,
        category: String?,
        openingBalance: Double,
        notes: String?
    ) {
        viewModelScope.launch {
            val supplier = Customer(
                id = id ?: 0,
                name = name,
                phone = phone,
                email = email,
                businessName = businessName,
                category = category,
                openingBalance = -openingBalance, // Negative = we owe them
                notes = notes,
                type = "SELLER",
                createdAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateCustomer(supplier)
        }
    }
    
    /**
     * Save customer (type = CUSTOMER)
     */
    fun saveCustomer(
        id: Int? = null,
        name: String,
        phone: String?,
        email: String?,
        businessName: String?,
        category: String?,
        openingBalance: Double,
        notes: String?,
        type: String = "CUSTOMER"
    ) {
        viewModelScope.launch {
            val customer = Customer(
                id = id ?: 0,
                name = name,
                phone = phone,
                email = email,
                businessName = businessName,
                category = category,
                openingBalance = openingBalance, // Positive = they owe us
                notes = notes,
                type = type,
                createdAt = System.currentTimeMillis()
            )
            repository.insertOrUpdateCustomer(customer)
        }
    }
    
    /**
     * Refresh function for pull-to-refresh
     * Room StateFlows auto-update, so this just ensures any pending updates are visible
     */
    fun refresh() {
        viewModelScope.launch {
            // Room StateFlows auto-update when data changes
            // This just forces a recomposition
        }
    }
}
