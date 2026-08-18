package com.messledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.messledger.app.data.local.entity.CachedMess
import kotlinx.coroutines.flow.Flow

@Dao
interface MessDao {
    @Query("SELECT * FROM messes")
    fun getAllMesses(): Flow<List<CachedMess>>

    @Query("SELECT * FROM messes WHERE id = :id")
    fun getMessById(id: String): Flow<CachedMess?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMess(mess: CachedMess)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMesses(messes: List<CachedMess>)

    @Query("DELETE FROM messes WHERE id = :id")
    suspend fun deleteMess(id: String)
    
    @Query("DELETE FROM messes")
    suspend fun deleteAll()
}
