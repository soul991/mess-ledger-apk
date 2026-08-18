package com.messledger.app.di

import android.content.Context
import androidx.room.Room
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.ktx.messaging
import com.messledger.app.data.local.MessLedgerDatabase
import com.messledger.app.data.local.dao.MemberDao
import com.messledger.app.data.local.dao.MessDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore
    
    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging = Firebase.messaging

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MessLedgerDatabase {
        return Room.databaseBuilder(
            context,
            MessLedgerDatabase::class.java,
            "messledger_db"
        ).build()
    }

    @Provides
    fun provideMessDao(database: MessLedgerDatabase): MessDao = database.messDao()

    @Provides
    fun provideMemberDao(database: MessLedgerDatabase): MemberDao = database.memberDao()
}
