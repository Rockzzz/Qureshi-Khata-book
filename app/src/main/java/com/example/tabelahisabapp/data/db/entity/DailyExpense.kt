package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_expenses",
    indices = [Index(value = ["date"])]
)
data class DailyExpense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long, // Midnight normalized date
    val category: String, // e.g., "Milk", "Petrol"
    val amount: Double,
    val paymentMethod: String = "CASH",
    val createdAt: Long = System.currentTimeMillis()
)
