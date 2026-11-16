package com.example.qahwamanager.di

import android.content.Context
import com.example.qahwamanager.data.db.InventoryDao
import com.example.qahwamanager.data.db.MenuItemDao
import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.db.OrderItemDao
import com.example.qahwamanager.data.db.QahwaDatabase
import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.data.time.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// Repositories are provided through their @Inject constructors.
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): QahwaDatabase =
        QahwaDatabase.create(context)

    @Provides
    fun provideMenuItemDao(db: QahwaDatabase): MenuItemDao = db.menuItemDao()

    @Provides
    fun provideOrderDao(db: QahwaDatabase): OrderDao = db.orderDao()

    @Provides
    fun provideOrderItemDao(db: QahwaDatabase): OrderItemDao = db.orderItemDao()

    @Provides
    fun provideInventoryDao(db: QahwaDatabase): InventoryDao = db.inventoryDao()

    @Provides
    fun provideClock(): Clock = SystemClock
}
