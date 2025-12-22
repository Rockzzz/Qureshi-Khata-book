package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "customer_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Customer::class,
            parentColumns = ["id"],
            childColumns = ["customerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["customerId"])]
)
data class CustomerTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val customerId: Int,
    val type: String, // "CREDIT" or "DEBIT"
    val amount: Double,
    val date: Long,
    val note: String?,
    val voiceNotePath: String?,
    val paymentMethod: String = "CASH", // "CASH" or "BANK"
    val createdAt: Long
)
