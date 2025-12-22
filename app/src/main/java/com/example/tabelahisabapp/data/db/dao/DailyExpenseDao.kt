package com.example.tabelahisabapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tabelahisabapp.data.db.entity.DailyExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: DailyExpense)

    @Query("SELECT * FROM daily_expenses WHERE date = :date")
    fun getExpensesByDate(date: Long): Flow<List<DailyExpense>>

    @Delete
    suspend fun deleteExpense(expense: DailyExpense)

    @Query("DELETE FROM daily_expenses WHERE date = :date")
    suspend fun deleteExpensesForDate(date: Long)
    
    // Backup methods
    @Query("SELECT * FROM daily_expenses")
    suspend fun getAllExpensesList(): List<DailyExpense>
    
    @Query("DELETE FROM daily_expenses")
    suspend fun deleteAllExpenses()
}
