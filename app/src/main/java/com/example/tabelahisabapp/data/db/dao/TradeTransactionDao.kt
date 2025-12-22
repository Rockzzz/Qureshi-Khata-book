package com.example.tabelahisabapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tabelahisabapp.data.db.entity.TradeTransaction
import com.example.tabelahisabapp.data.db.models.MonthlyProfitSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTrade(trade: TradeTransaction)

    @Delete
    suspend fun deleteTrade(trade: TradeTransaction)

    @Query("SELECT * FROM trade_transactions ORDER BY date DESC")
    fun getAllTrades(): Flow<List<TradeTransaction>>

    @Query("SELECT * FROM trade_transactions WHERE type = :type ORDER BY date DESC")
    fun getTradesByType(type: String): Flow<List<TradeTransaction>>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 'BUY' THEN totalAmount ELSE 0 END), 0.0) as totalBuy,
            COALESCE(SUM(CASE WHEN type = 'SELL' THEN totalAmount ELSE 0 END), 0.0) as totalSell
        FROM trade_transactions
        WHERE date >= :startTime AND date < :endTime
    """)
    fun getMonthlyProfitSummary(startTime: Long, endTime: Long): Flow<MonthlyProfitSummary>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 'BUY' THEN totalAmount ELSE 0 END), 0.0) as totalBuy,
            COALESCE(SUM(CASE WHEN type = 'SELL' THEN totalAmount ELSE 0 END), 0.0) as totalSell
        FROM trade_transactions
    """)
    fun getOverallProfitSummary(): Flow<MonthlyProfitSummary>

    @Query("SELECT * FROM trade_transactions WHERE id = :transactionId")
    fun getTradeTransactionById(transactionId: Int): Flow<TradeTransaction?>
    
    // Backup methods
    @Query("SELECT * FROM trade_transactions")
    suspend fun getAllTransactionsList(): List<TradeTransaction>
    
    @Query("DELETE FROM trade_transactions")
    suspend fun deleteAllTransactions()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TradeTransaction)
}
