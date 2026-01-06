package com.example.tabelahisabapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tabelahisabapp.data.db.entity.DailyExpense
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: DailyExpense)
    
    @Update
    suspend fun update(expense: DailyExpense)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseAndGetId(expense: DailyExpense): Long

    @Query("SELECT * FROM daily_expenses WHERE date = :date")
    fun getExpensesByDate(date: Long): Flow<List<DailyExpense>>
    
    @Query("SELECT * FROM daily_expenses WHERE id = :expenseId")
    suspend fun getExpenseById(expenseId: Int): DailyExpense?

    @Query("SELECT * FROM daily_expenses ORDER BY date DESC")
    fun getAllExpenses(): Flow<List<DailyExpense>>

    @Delete
    suspend fun deleteExpense(expense: DailyExpense)
    
    @Query("DELETE FROM daily_expenses WHERE id = :expenseId")
    suspend fun deleteExpenseById(expenseId: Int)

    @Query("DELETE FROM daily_expenses WHERE date = :date")
    suspend fun deleteExpensesForDate(date: Long)
    
    // Get all expenses for a specific date (sync version for derived ledger)
    // Uses date RANGE to catch expenses saved with any time on that day
    @Query("SELECT * FROM daily_expenses WHERE date >= :startOfDay AND date < :endOfDay ORDER BY createdAt ASC")
    suspend fun getExpensesByDateRangeSync(startOfDay: Long, endOfDay: Long): List<DailyExpense>
    
    // Backup methods
    @Query("SELECT * FROM daily_expenses")
    suspend fun getAllExpensesList(): List<DailyExpense>
    
    @Query("DELETE FROM daily_expenses")
    suspend fun deleteAllExpenses()
}
