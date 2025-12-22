package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a single transaction in the Daily Ledger (Cash Book)
 * Each transaction records money movement: Cash In, Cash Out, Bank In, or Bank Out
 */
@Entity(
    tableName = "daily_ledger_transactions",
    indices = [Index(value = ["date"]), Index(value = ["date", "createdAt"])]
)
data class DailyLedgerTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long, // Midnight normalized date
    val mode: String, // "CASH_IN", "CASH_OUT", "BANK_IN", "BANK_OUT"
    val amount: Double,
    val party: String? = null, // Optional: Customer/Vendor name
    val note: String? = null, // Optional: Transaction description
    val createdAt: Long = System.currentTimeMillis(), // Timestamp for sorting by time
    val customerTransactionId: Int? = null // Link to CustomerTransaction for two-way sync
)

