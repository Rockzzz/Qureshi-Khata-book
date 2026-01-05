package com.example.tabelahisabapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.tabelahisabapp.data.db.entity.Customer
import com.example.tabelahisabapp.data.db.models.CustomerListResult
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer): Long
    
    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT * FROM customers WHERE id = :customerId")
    fun getCustomerById(customerId: Int): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE name = :name LIMIT 1")
    suspend fun getCustomerByName(name: String): Customer?
    
    @Query("SELECT * FROM customers WHERE id = :customerId")
    suspend fun getCustomerByIdSync(customerId: Int): Customer?

    @Query("""
        SELECT c.*, (
            COALESCE((SELECT SUM(CASE WHEN type = 'CREDIT' THEN amount ELSE -amount END) FROM customer_transactions WHERE customerId = c.id), 0.0)
        ) as balance
        FROM customers c
        ORDER BY c.createdAt DESC
    """)
    fun getAllCustomersWithBalance(): Flow<List<CustomerListResult>>
    
    // Backup methods
    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersList(): List<Customer>
    
    @Query("DELETE FROM customers")
    suspend fun deleteAllCustomers()
    
    @Query("SELECT * FROM customers")
    suspend fun getAllCustomersSync(): List<Customer>
}
