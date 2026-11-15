package com.example.qahwamanager.di

import android.content.Context
import com.example.qahwamanager.data.db.InventoryDao
import com.example.qahwamanager.data.db.MenuItemDao
import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.db.OrderItemDao
import com.example.qahwamanager.data.db.QahwaDatabase
import com.example.qahwamanager.data.repository.InventoryRepository
import com.example.qahwamanager.data.repository.MenuRepository
import com.example.qahwamanager.data.repository.OrderRepository
import com.example.qahwamanager.data.repository.SalesRepository
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
    fun provideDatabase(@ApplicationContext context: Context): QahwaDatabase =
        QahwaDatabase.create(context)

    @Provides
    @Singleton
    fun provideMenuItemDao(db: QahwaDatabase): MenuItemDao = db.menuItemDao()

    @Provides
    @Singleton
    fun provideOrderDao(db: QahwaDatabase): OrderDao = db.orderDao()

    @Provides
    @Singleton
    fun provideOrderItemDao(db: QahwaDatabase): OrderItemDao = db.orderItemDao()

    @Provides
    @Singleton
    fun provideInventoryDao(db: QahwaDatabase): InventoryDao = db.inventoryDao()

    @Provides
    @Singleton
    fun provideMenuRepository(dao: MenuItemDao): MenuRepository = MenuRepository(dao)

    @Provides
    @Singleton
    fun provideOrderRepository(orderDao: OrderDao, orderItemDao: OrderItemDao): OrderRepository =
        OrderRepository(orderDao, orderItemDao)

    @Provides
    @Singleton
    fun provideInventoryRepository(dao: InventoryDao): InventoryRepository =
        InventoryRepository(dao)

    @Provides
    @Singleton
    fun provideSalesRepository(orderDao: OrderDao): SalesRepository = SalesRepository(orderDao)
}

