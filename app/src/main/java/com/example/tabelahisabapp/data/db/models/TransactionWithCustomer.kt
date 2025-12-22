package com.example.tabelahisabapp.data.db.models

import androidx.room.Embedded
import com.example.tabelahisabapp.data.db.entity.Customer
import com.example.tabelahisabapp.data.db.entity.CustomerTransaction

data class TransactionWithCustomer(
    @Embedded val transaction: CustomerTransaction,
    @Embedded(prefix = "cust_") val customer: Customer
)
