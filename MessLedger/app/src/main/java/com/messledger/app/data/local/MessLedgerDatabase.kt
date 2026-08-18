package com.messledger.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.messledger.app.data.local.dao.MemberDao
import com.messledger.app.data.local.dao.MessDao
import com.messledger.app.data.local.entity.CachedMember
import com.messledger.app.data.local.entity.CachedMess

@Database(
    entities = [CachedMess::class, CachedMember::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MessLedgerDatabase : RoomDatabase() {
    abstract fun messDao(): MessDao
    abstract fun memberDao(): MemberDao
}
