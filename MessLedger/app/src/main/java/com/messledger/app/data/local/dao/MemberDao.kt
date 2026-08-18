package com.messledger.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.messledger.app.data.local.entity.CachedMember
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberDao {
    @Query("SELECT * FROM members WHERE messId = :messId AND deletedAt IS NULL")
    fun getActiveMembersByMess(messId: String): Flow<List<CachedMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: CachedMember)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<CachedMember>)

    @Query("DELETE FROM members WHERE messId = :messId")
    suspend fun deleteMembersByMess(messId: String)
}
