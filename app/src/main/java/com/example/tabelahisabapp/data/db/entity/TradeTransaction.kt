package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trade_transactions")
data class TradeTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val farmId: Int? = null,          // Link to Farm
    val entryNumber: String? = null,  // e.g., "HF-001"
    val date: Long,
    val deonar: String? = null, // Location/Name (e.g., "Deonar", "Allana")
    val type: String, // "BUY" or "SELL"
    val itemName: String, // Buffalo/Item name
    val quantity: Int, // Number of buffalo/items
    val buyRate: Double, // Buy rate per unit
    val weight: Double? = null, // Weight in kg
    val rate: Double? = null, // Rate per kg
    val extraBonus: Double? = null, // Extra bonus amount
    val netWeight: Double? = null, // Net weight after deduction (for Allana)
    val fee: Double? = null, // Fee per buffalo (for Allana)
    val tds: Double? = null, // TDS amount (for Allana)
    val totalAmount: Double, // Total amount
    val profit: Double? = null, // Profit (for SELL transactions)
    val pricePerUnit: Double, // Keep for backward compatibility
    val note: String?,
    val createdAt: Long
)

