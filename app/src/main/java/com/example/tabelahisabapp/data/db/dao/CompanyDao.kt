package com.example.tabelahisabapp.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tabelahisabapp.data.db.entity.Company
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanyDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateCompany(company: Company)
    
    @Delete
    suspend fun deleteCompany(company: Company)
    
    @Query("SELECT * FROM companies ORDER BY createdAt DESC")
    fun getAllCompanies(): Flow<List<Company>>
    
    @Query("SELECT * FROM companies WHERE id = :companyId")
    fun getCompanyById(companyId: Int): Flow<Company?>
    
    // Backup methods
    @Query("SELECT * FROM companies")
    suspend fun getAllCompaniesList(): List<Company>
    
    @Query("DELETE FROM companies")
    suspend fun deleteAllCompanies()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompany(company: Company)
}

