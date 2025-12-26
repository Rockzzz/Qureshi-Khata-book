package com.example.tabelahisabapp.data.db.dao

import androidx.room.*
import com.example.tabelahisabapp.data.db.entity.ExpenseCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseCategoryDao {
    @Query("SELECT * FROM expense_categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<ExpenseCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: ExpenseCategory)

    @Delete
    suspend fun deleteCategory(category: ExpenseCategory)
    
    @Query("SELECT COUNT(*) FROM expense_categories")
    suspend fun getCount(): Int
    
    @Insert
    suspend fun insertAll(categories: List<ExpenseCategory>)
}
