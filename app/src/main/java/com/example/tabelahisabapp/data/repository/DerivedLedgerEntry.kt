package com.example.tabelahisabapp.data.repository

/**
 * Represents a ledger entry derived from source transactions.
 * This is used by the Daily Ledger screen to display all transactions
 * from a single source of truth (CustomerTransaction + DailyExpense).
 */
data class DerivedLedgerEntry(
    val id: Int,
    val sourceType: String,  // "customer", "supplier", "expense"
    val sourceId: Int,
    val date: Long,
    val amount: Double,
    val party: String,
    val note: String?,
    val mode: String,  // CASH_IN, CASH_OUT, BANK_IN, BANK_OUT, PURCHASE
    val category: DerivedLedgerCategory,
    val paymentMethod: String,
    val createdAt: Long
)

/**
 * Categories for grouping transactions in Daily Ledger display
 */
enum class DerivedLedgerCategory {
    CASH_IN,         // Money received from customers (Paisa Aaya)
    CASH_OUT,        // Money given to customers
    EXPENSE,         // Daily expenses (Rozana Kharcha)
    PURCHASE,        // Supplier purchases - record only (Khareedari)
    SUPPLIER_PAYMENT // Payment to supplier (Payment Diya)
}
