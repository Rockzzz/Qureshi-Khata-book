package com.example.tabelahisabapp.data.db.models

data class MonthlyProfitSummary(
    val totalBuy: Double,
    val totalSell: Double,
    val totalProfit: Double = 0.0
)
