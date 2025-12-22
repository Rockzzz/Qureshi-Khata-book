package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "daily_balances",
    indices = [Index(value = ["date"], unique = true)]
)
data class DailyBalance(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Long,
    val openingCash: Double,
    val openingBank: Double,
    val closingCash: Double,
    val closingBank: Double,
    val note: String?,
    val createdAt: Long
)
