package com.example.tabelahisabapp.data.db.dao

import androidx.room.*
import com.example.tabelahisabapp.data.db.entity.Farm
import kotlinx.coroutines.flow.Flow

@Dao
interface FarmDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateFarm(farm: Farm)
    
    @Delete
    suspend fun deleteFarm(farm: Farm)
    
    @Query("SELECT * FROM farms ORDER BY name ASC")
    fun getAllFarms(): Flow<List<Farm>>
    
    @Query("SELECT * FROM farms WHERE id = :farmId")
    suspend fun getFarmById(farmId: Int): Farm?
    
    @Query("SELECT * FROM farms WHERE id = :farmId")
    fun getFarmByIdFlow(farmId: Int): Flow<Farm?>
    
    @Query("UPDATE farms SET nextNumber = nextNumber + 1 WHERE id = :farmId")
    suspend fun incrementFarmNumber(farmId: Int)
    
    @Query("SELECT nextNumber FROM farms WHERE id = :farmId")
    suspend fun getNextNumber(farmId: Int): Int?
    
    // Backup methods
    @Query("SELECT * FROM farms")
    suspend fun getAllFarmsList(): List<Farm>
    
    @Query("DELETE FROM farms")
    suspend fun deleteAllFarms()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFarm(farm: Farm)
}
