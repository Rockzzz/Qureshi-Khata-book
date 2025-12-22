package com.example.tabelahisabapp.ui.customer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.db.entity.Customer
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddCustomerViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    suspend fun getCustomer(customerId: Int): Customer? {
        return repository.getCustomerById(customerId).first()
    }

    suspend fun checkNameExists(name: String, excludeId: Int? = null): Boolean {
        val allCustomers = repository.getAllCustomersWithBalance().first()
        return allCustomers.any { 
            it.customer.name.equals(name.trim(), ignoreCase = true) && 
            (excludeId == null || it.customer.id != excludeId)
        }
    }

    suspend fun saveCustomer(customerId: Int?, name: String, phone: String?, type: String = "CUSTOMER", originalCreatedAt: Long? = null): Result<Unit> {
        if (name.isBlank()) {
            return Result.failure(Exception("Name cannot be empty"))
        }

        // Check for duplicate name (case-insensitive)
        val nameExists = checkNameExists(name, excludeId = customerId)
        if (nameExists) {
            return Result.failure(Exception("A contact with name \"${name.trim()}\" already exists. Please use a different name."))
        }

        // Get old name if updating existing customer
        val oldName = if (customerId != null) {
            repository.getCustomerById(customerId).first()?.name
        } else null

        val createdAt = if (customerId != null && originalCreatedAt != null) {
            originalCreatedAt
        } else {
            System.currentTimeMillis()
        }
        
        val customer = Customer(
            id = customerId ?: 0,
            name = name.trim(),
            phone = phone?.trim(),
            type = type,
            createdAt = createdAt
        )
        repository.insertOrUpdateCustomer(customer)
        
        // If name changed, update all linked Daily Ledger entries and transaction notes
        if (oldName != null && !oldName.equals(name.trim(), ignoreCase = true)) {
            repository.updateCustomerNameEverywhere(customerId!!, oldName, name.trim())
        }
        
        return Result.success(Unit)
    }
}
