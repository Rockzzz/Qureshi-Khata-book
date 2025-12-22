package com.example.tabelahisabapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tabelahisabapp.data.db.entity.CustomerTransaction
import com.example.tabelahisabapp.data.db.models.CustomerBalanceSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerTransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateTransaction(transaction: CustomerTransaction)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAndGetId(transaction: CustomerTransaction): Long
    
    @Query("SELECT * FROM customer_transactions WHERE id = :transactionId")
    suspend fun getTransactionByIdSync(transactionId: Int): CustomerTransaction?

    @Delete
    suspend fun deleteTransaction(transaction: CustomerTransaction)

    @Query("SELECT * FROM customer_transactions WHERE id = :transactionId")
    fun getTransactionById(transactionId: Int): Flow<CustomerTransaction?>

    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId ORDER BY date DESC")
    fun getTransactionsForCustomer(customerId: Int): Flow<List<CustomerTransaction>>
    
    @Query("SELECT * FROM customer_transactions WHERE customerId = :customerId")
    suspend fun getTransactionsForCustomerDirect(customerId: Int): List<CustomerTransaction>

    @Query("""
        SELECT
            COALESCE(SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE 0 END), 0.0) as totalCredit,
            COALESCE(SUM(CASE WHEN type = 'DEBIT' THEN amount ELSE 0 END), 0.0) as totalDebit
        FROM customer_transactions
        WHERE customerId = :customerId
    """)
    fun getBalanceSummaryForCustomer(customerId: Int): Flow<CustomerBalanceSummary>

    @Query("""
        SELECT 
            t.*,
            c.id as cust_id,
            c.name as cust_name,
            c.phone as cust_phone,
            c.type as cust_type,
            c.createdAt as cust_createdAt
        FROM customer_transactions t
        INNER JOIN customers c ON t.customerId = c.id
        WHERE t.date = :date
    """)
    fun getTransactionsByDateWithCustomer(date: Long): Flow<List<com.example.tabelahisabapp.data.db.models.TransactionWithCustomer>>
    
    // Backup methods
    @Query("SELECT * FROM customer_transactions")
    suspend fun getAllTransactionsList(): List<CustomerTransaction>
    
    @Query("DELETE FROM customer_transactions")
    suspend fun deleteAllTransactions()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: CustomerTransaction)
}
