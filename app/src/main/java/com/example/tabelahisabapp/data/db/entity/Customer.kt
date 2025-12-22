package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Type: CUSTOMER, SELLER, or BOTH
enum class ContactType {
    CUSTOMER,  // Someone who owes you money (udhaar)
    SELLER,    // Someone you buy from (like Gaffar for buffalo)
    BOTH       // Both customer and seller
}

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phone: String?,
    val type: String = "CUSTOMER", // CUSTOMER, SELLER, or BOTH
    val createdAt: Long
)
