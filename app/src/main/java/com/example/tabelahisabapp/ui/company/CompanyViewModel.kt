package com.example.tabelahisabapp.ui.company

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.db.entity.Company
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CompanyViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {
    
    val companies = repository.getAllCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    suspend fun getCompany(companyId: Int): com.example.tabelahisabapp.data.db.entity.Company? {
        return repository.getCompanyById(companyId).first()
    }
    
    fun saveCompany(companyId: Int?, name: String, code: String?, address: String?, phone: String?, email: String?, gstNumber: String?, originalCreatedAt: Long? = null): Result<Unit> {
        if (name.isBlank()) {
            return Result.failure(Exception("Company name cannot be empty"))
        }
        
        viewModelScope.launch {
            val createdAt = if (companyId != null && originalCreatedAt != null) {
                originalCreatedAt
            } else {
                System.currentTimeMillis()
            }
            
            val company = Company(
                id = companyId ?: 0,
                name = name.trim(),
                code = code?.trim(),
                address = address?.trim(),
                phone = phone?.trim(),
                email = email?.trim(),
                gstNumber = gstNumber?.trim(),
                createdAt = createdAt
            )
            repository.insertOrUpdateCompany(company)
        }
        return Result.success(Unit)
    }
    
    fun deleteCompany(company: Company) {
        viewModelScope.launch {
            repository.deleteCompany(company)
        }
    }
}

