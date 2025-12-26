package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Contact type enum representing different roles a contact can have:
 * - CUSTOMER: Someone who owes you money (udhaar) - Money may come back
 * - SELLER: Someone you buy from (supplier) - Money paid for purchases
 * - BOTH: Acts as both customer and seller
 */
enum class ContactType {
    CUSTOMER,  // Someone who owes you money (udhaar)
    SELLER,    // Someone you buy from (like Gaffar for buffalo)
    BOTH       // Both customer and seller
}

/**
 * Customer/Supplier entity - Unified model for both customers and suppliers
 * 
 * For CUSTOMER type:
 *   - Positive balance = They owe you (You will get)
 *   - Negative balance = You owe them (You will give)
 *   
 * For SELLER type:
 *   - Tracks purchases and payments
 *   - Balance shows pending payables
 */
@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    
    val name: String,
    
    val phone: String? = null,
    
    // Contact type: CUSTOMER, SELLER, or BOTH
    val type: String = "CUSTOMER",
    
    // ═══ New fields for enhanced functionality ═══
    
    // Email address (optional, for reminders)
    val email: String? = null,
    
    // Business name (used mainly for suppliers)
    val businessName: String? = null,
    
    // Category for suppliers: Feed, Milk, Equipment, etc.
    val category: String? = null,
    
    // Opening balance for migrating existing ledgers
    // Positive = They owe you, Negative = You owe them
    val openingBalance: Double = 0.0,
    
    // Notes about this customer/supplier
    val notes: String? = null,
    
    val createdAt: Long
)
