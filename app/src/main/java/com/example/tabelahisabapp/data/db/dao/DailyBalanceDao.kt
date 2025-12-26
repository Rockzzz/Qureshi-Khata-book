package com.example.tabelahisabapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tabelahisabapp.data.db.entity.DailyBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyBalanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDailyBalance(dailyBalance: DailyBalance)

    @Delete
    suspend fun deleteDailyBalance(dailyBalance: DailyBalance)

    @Query("SELECT * FROM daily_balances WHERE date = :date")
    fun getDailyBalanceByDate(date: Long): Flow<DailyBalance?>
    
    @Query("SELECT * FROM daily_balances WHERE date = :date")
    suspend fun getBalanceByDateSync(date: Long): DailyBalance?

    @Query("SELECT * FROM daily_balances ORDER BY date DESC")
    fun getAllDailyBalances(): Flow<List<DailyBalance>>
    
    @Query("SELECT * FROM daily_balances WHERE date >= :fromDate ORDER BY date ASC")
    suspend fun getBalancesFromDate(fromDate: Long): List<DailyBalance>

    @Query("SELECT * FROM daily_balances WHERE date < :currentDate ORDER BY date DESC LIMIT 1")
    fun getPreviousDayBalance(currentDate: Long): Flow<DailyBalance?>
    
    @Query("SELECT * FROM daily_balances WHERE date < :currentDate ORDER BY date DESC LIMIT 1")
    suspend fun getPreviousDayBalanceSync(currentDate: Long): DailyBalance?
    
    // Backup methods
    @Query("SELECT * FROM daily_balances")
    suspend fun getAllBalancesList(): List<DailyBalance>
    
    @Query("DELETE FROM daily_balances")
    suspend fun deleteAllBalances()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBalance(balance: DailyBalance)
}
