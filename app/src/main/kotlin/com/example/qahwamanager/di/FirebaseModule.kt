package com.example.qahwamanager.di

import com.example.qahwamanager.data.cloud.CloudSessionController
import com.example.qahwamanager.data.cloud.FirebaseCloudSessionController
import com.example.qahwamanager.data.cloud.FirestoreSyncEngine
import com.example.qahwamanager.data.cloud.SyncPublisher
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.firestoreSettings
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = Firebase.auth

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = Firebase.firestore.apply {
        firestoreSettings = firestoreSettings {
            setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SyncBindingModule {

    @Binds
    @Singleton
    abstract fun bindSyncPublisher(engine: FirestoreSyncEngine): SyncPublisher

    @Binds
    @Singleton
    abstract fun bindCloudSessionController(
        impl: FirebaseCloudSessionController
    ): CloudSessionController
}
