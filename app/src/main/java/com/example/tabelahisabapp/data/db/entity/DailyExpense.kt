package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Expense entity - Records standalone expenses (money gone forever)
 * 
 * Key distinction from Supplier transactions:
 * - Expenses: No person ledger, no future settlement, money is GONE
 * - Supplier Payment: Creates/reduces liability in supplier ledger
 * 
 * Examples: Fuel, Labour, Electricity, Home Expense, Transport, etc.
 */
@Entity(
    tableName = "daily_expenses",
    indices = [
        Index(value = ["date"]),
        Index(value = ["category"])
    ]
)
data class DailyExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    // Midnight normalized date
    val date: Long,
    
    // Category: Fuel, Labour, Electricity, Home Expense, Transport, etc.
    val category: String,
    
    // Expense amount
    val amount: Double,
    
    // Payment method: CASH or BANK
    val paymentMethod: String = "CASH",
    
    // ═══ New fields ═══
    
    // Optional note/description
    val note: String? = null,
    
    // Timestamp for sorting
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Pre-defined expense category constants (for reference/defaults)
 * Note: Dynamic categories are stored in ExpenseCategory entity
 */
object ExpenseCategoryConstants {
    const val FUEL = "Fuel"
    const val LABOUR = "Labour"
    const val ELECTRICITY = "Electricity"
    const val HOME_EXPENSE = "Home Expense"
    const val TRANSPORT = "Transport"
    const val MAINTENANCE = "Maintenance"
    const val FEED = "Feed"
    const val MEDICAL = "Medical"
    const val MISC = "Misc"
    
    val allCategories = listOf(
        FUEL, LABOUR, ELECTRICITY, HOME_EXPENSE, 
        TRANSPORT, MAINTENANCE, FEED, MEDICAL, MISC
    )
}
