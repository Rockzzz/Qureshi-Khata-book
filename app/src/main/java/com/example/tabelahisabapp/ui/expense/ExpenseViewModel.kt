package com.example.tabelahisabapp.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tabelahisabapp.data.db.entity.DailyExpense
import com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction
import com.example.tabelahisabapp.data.db.entity.LedgerMode
import com.example.tabelahisabapp.data.db.entity.SourceType
import com.example.tabelahisabapp.data.repository.MainRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/**
 * Expense category summary for UI
 */
data class ExpenseCategorySummary(
    val category: String,
    val totalAmount: Double,
    val count: Int
)

/**
 * ViewModel for expense management
 * 
 * Handles:
 * - Saving expenses with ledger sync
 * - Category-wise expense summaries
 * - Expense CRUD operations
 */
@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: MainRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess.asStateFlow()

    // Dynamic Categories
    val categories = repository.getAllExpenseCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenses = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        initializeCategories()
    }

    private fun initializeCategories() {
        viewModelScope.launch {
            if (repository.getExpenseCategoryCount() == 0) {
                val defaults = listOf(
                    com.example.tabelahisabapp.data.db.entity.ExpenseCategory(name = "Fuel", iconName = "LocalGasStation", colorHex = "#EF4444", isDefault = true),
                    com.example.tabelahisabapp.data.db.entity.ExpenseCategory(name = "Labour", iconName = "Group", colorHex = "#3B82F6", isDefault = true),
                    com.example.tabelahisabapp.data.db.entity.ExpenseCategory(name = "Electricity", iconName = "Bolt", colorHex = "#F59E0B", isDefault = true),
                    com.example.tabelahisabapp.data.db.entity.ExpenseCategory(name = "Home Expense", iconName = "Home", colorHex = "#8B5CF6", isDefault = true),
                    com.example.tabelahisabapp.data.db.entity.ExpenseCategory(name = "Transport", iconName = "DirectionsCar", colorHex = "#10B981", isDefault = true),
                    com.example.tabelahisabapp.data.db.entity.ExpenseCategory(name = "Maintenance", iconName = "Build", colorHex = "#6366F1", isDefault = true),
                    com.example.tabelahisabapp.data.db.entity.ExpenseCategory(name = "Feed", iconName = "Restaurant", colorHex = "#22D3EE", isDefault = true),
                    com.example.tabelahisabapp.data.db.entity.ExpenseCategory(name = "Medical", iconName = "MedicalServices", colorHex = "#EC4899", isDefault = true),
                    com.example.tabelahisabapp.data.db.entity.ExpenseCategory(name = "Misc", iconName = "MoreHoriz", colorHex = "#71717A", isDefault = true)
                )
                repository.insertAllExpenseCategories(defaults)
            }
        }
    }

    fun addNewCategory(name: String, colorHex: String, iconName: String = "Notes") {
        viewModelScope.launch {
            val category = com.example.tabelahisabapp.data.db.entity.ExpenseCategory(
                name = name,
                iconName = iconName,
                colorHex = colorHex,
                isDefault = false
            )
            repository.insertExpenseCategory(category)
        }
    }

    /**
     * Save expense with automatic ledger sync
     * 
     * This creates:
     * 1. DailyExpense entry (for expense tracking)
     * 2. DailyLedgerTransaction entry (for cash book sync)
     */
    fun saveExpense(
        category: String,
        amount: Double,
        date: Long,
        paymentMethod: String,
        note: String?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Normalize date to midnight
                val normalizedDate = normalizeDateToMidnight(date)
                
                // Create expense entry
                val expense = DailyExpense(
                    date = normalizedDate,
                    category = category,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    note = note,
                    createdAt = System.currentTimeMillis()
                )
                
                // Insert expense and get ID
                val expenseId = repository.insertExpenseAndGetId(expense)
                
                // Create corresponding ledger entry for cash book sync
                val ledgerMode = if (paymentMethod == "CASH") LedgerMode.CASH_OUT else LedgerMode.BANK_OUT
                
                val ledgerEntry = DailyLedgerTransaction(
                    date = normalizedDate,
                    mode = ledgerMode,
                    amount = amount,
                    party = category, // Use category as party name for display (Transport, Labour, etc.)
                    note = note, // Keep note separate
                    createdAt = System.currentTimeMillis(),
                    sourceType = SourceType.EXPENSE,
                    sourceId = expenseId.toInt()
                )
                
                repository.insertOrUpdateLedgerTransaction(ledgerEntry)
                
                _saveSuccess.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update existing expense
     */
    fun updateExpense(
        expenseId: Int,
        category: String,
        amount: Double,
        date: Long,
        paymentMethod: String,
        note: String?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val normalizedDate = normalizeDateToMidnight(date)
                
                val expense = DailyExpense(
                    id = expenseId,
                    date = normalizedDate,
                    category = category,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    note = note,
                    createdAt = System.currentTimeMillis()
                )
                
                repository.insertExpense(expense)
                
                // Update corresponding ledger entry
                // TODO: Find and update linked ledger entry by sourceType and sourceId
                
                _saveSuccess.value = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Delete expense and its linked ledger entry
     */
    fun deleteExpense(expense: DailyExpense) {
        viewModelScope.launch {
            try {
                // Use sync method to auto-delete from both expense and daily ledger
                repository.deleteExpenseWithSync(expense.id, expense.date)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Normalize date to midnight for consistent grouping
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
