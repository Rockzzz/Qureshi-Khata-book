package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "companies")
data class Company(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String, // Company name (e.g., "Y.B.B.R.O.S.")
    val code: String? = null, // Company code
    val address: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val gstNumber: String? = null,
    val createdAt: Long
)

