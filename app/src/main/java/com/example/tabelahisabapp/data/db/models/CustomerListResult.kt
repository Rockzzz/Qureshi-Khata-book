package com.example.tabelahisabapp.data.db.models

import androidx.room.Embedded
import com.example.tabelahisabapp.data.db.entity.Customer

data class CustomerListResult(
    @Embedded
    val customer: Customer,
    val balance: Double
)
