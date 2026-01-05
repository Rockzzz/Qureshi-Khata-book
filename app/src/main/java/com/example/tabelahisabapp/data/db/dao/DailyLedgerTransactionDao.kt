package com.example.tabelahisabapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tabelahisabapp.data.db.entity.DailyLedgerTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyLedgerTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTransaction(transaction: DailyLedgerTransaction)

    @Update
    suspend fun updateTransaction(transaction: DailyLedgerTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: DailyLedgerTransaction)

    @Query("SELECT * FROM daily_ledger_transactions WHERE date = :date ORDER BY createdAt ASC")
    fun getTransactionsByDate(date: Long): Flow<List<DailyLedgerTransaction>>
    
    @Query("SELECT * FROM daily_ledger_transactions WHERE date = :date ORDER BY createdAt ASC")
    suspend fun getTransactionsForDateSync(date: Long): List<DailyLedgerTransaction>

    @Query("SELECT * FROM daily_ledger_transactions WHERE id = :transactionId")
    suspend fun getTransactionById(transactionId: Int): DailyLedgerTransaction?

    @Query("DELETE FROM daily_ledger_transactions WHERE date = :date")
    suspend fun deleteTransactionsForDate(date: Long)
    
    @Query("SELECT * FROM daily_ledger_transactions")
    suspend fun getAllTransactions(): List<DailyLedgerTransaction>

    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN mode = 'CASH_IN' THEN amount ELSE 0 END), 0.0) as totalCashIn,
            COALESCE(SUM(CASE WHEN mode = 'CASH_OUT' THEN amount ELSE 0 END), 0.0) as totalCashOut,
            COALESCE(SUM(CASE WHEN mode = 'BANK_IN' THEN amount ELSE 0 END), 0.0) as totalBankIn,
            COALESCE(SUM(CASE WHEN mode = 'BANK_OUT' THEN amount ELSE 0 END), 0.0) as totalBankOut
        FROM daily_ledger_transactions
        WHERE date = :date
    """)
    suspend fun getTotalsForDate(date: Long): TransactionTotals
    
    // Backup methods
    @Query("SELECT * FROM daily_ledger_transactions")
    suspend fun getAllTransactionsList(): List<DailyLedgerTransaction>
    
    @Query("DELETE FROM daily_ledger_transactions")
    suspend fun deleteAllTransactions()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: DailyLedgerTransaction)
    
    @Query("SELECT * FROM daily_ledger_transactions WHERE sourceType = :sourceType AND sourceId = :sourceId LIMIT 1")
    suspend fun getBySourceId(sourceType: String, sourceId: Int): DailyLedgerTransaction?
    
    @Query("SELECT * FROM daily_ledger_transactions WHERE customerTransactionId = :transactionId LIMIT 1")
    suspend fun getByCustomerTransactionId(transactionId: Int): DailyLedgerTransaction?
    
    @Query("SELECT DISTINCT date FROM daily_ledger_transactions ORDER BY date DESC")
    fun getAllDistinctDates(): Flow<List<Long>>
    
    @Query("SELECT DISTINCT date FROM daily_ledger_transactions WHERE date >= :fromDate ORDER BY date ASC")
    suspend fun getDistinctDatesFrom(fromDate: Long): List<Long>
}

data class TransactionTotals(
    val totalCashIn: Double = 0.0,
    val totalCashOut: Double = 0.0,
    val totalBankIn: Double = 0.0,
    val totalBankOut: Double = 0.0
)

