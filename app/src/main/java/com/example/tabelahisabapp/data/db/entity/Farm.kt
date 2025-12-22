package com.example.tabelahisabapp.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Farm entity for organizing trades by farm location
 * Each farm has a unique numbering series (e.g., HF-001, HF-002)
 */
@Entity(tableName = "farms")
data class Farm(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,           // "Hindustani Farm"
    val shortCode: String,      // "HF" for entry numbers like HF-001
    val nextNumber: Int = 1,    // Auto-increment counter for entry series
    val createdAt: Long
)
