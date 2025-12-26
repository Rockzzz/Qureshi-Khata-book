package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single transaction in the Daily Ledger (Cash Book)
 * Each transaction records money movement: Cash In, Cash Out, Bank In, or Bank Out
 * 
 * This entity acts as the MIRROR - it reflects data from source ledgers:
 * - Customer transactions (Money Received / Money Given)
 * - Supplier transactions (Payment Kiya)
 * - Expenses
 * 
 * The sourceType and sourceId fields track where this transaction originated,
 * enabling bidirectional sync.
 */
@Entity(
    tableName = "daily_ledger_transactions",
    indices = [
        Index(value = ["date"]), 
        Index(value = ["date", "createdAt"]),
        Index(value = ["sourceType", "sourceId"])
    ]
)
data class DailyLedgerTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    // Midnight normalized date for grouping
    val date: Long,
    
    // Transaction mode: CASH_IN, CASH_OUT, BANK_IN, BANK_OUT
    val mode: String,
    
    // Transaction amount
    val amount: Double,
    
    // Party name (Customer/Vendor name)
    val party: String? = null,
    
    // Transaction description/note
    val note: String? = null,
    
    // Timestamp for sorting by time within a day
    val createdAt: Long = System.currentTimeMillis(),
    
    // ═══ Linking fields for bidirectional sync ═══
    
    // Link to CustomerTransaction for customer/seller transactions
    // (kept for backward compatibility)
    val customerTransactionId: Int? = null,
    
    // Source type: "customer", "supplier", "expense"
    // Used to identify which ledger this transaction came from
    val sourceType: String? = null,
    
    // Source entity ID - references the original transaction ID
    // For customer: CustomerTransaction.id
    // For expense: DailyExpense.id
    val sourceId: Int? = null
)

/**
 * Transaction mode constants
 */
object LedgerMode {
    const val CASH_IN = "CASH_IN"
    const val CASH_OUT = "CASH_OUT"
    const val BANK_IN = "BANK_IN"
    const val BANK_OUT = "BANK_OUT"
}

/**
 * Source type constants
 */
object SourceType {
    const val CUSTOMER = "customer"
    const val SUPPLIER = "supplier"
    const val EXPENSE = "expense"
}
