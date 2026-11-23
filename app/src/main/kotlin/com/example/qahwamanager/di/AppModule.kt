package com.example.qahwamanager.di

import android.content.Context
import com.example.qahwamanager.data.db.InventoryDao
import com.example.qahwamanager.data.db.LoyaltyCustomerDao
import com.example.qahwamanager.data.db.MenuItemDao
import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.db.OrderItemDao
import com.example.qahwamanager.data.db.OwnerDao
import com.example.qahwamanager.data.db.QahwaDatabase
import com.example.qahwamanager.data.db.ShiftDao
import com.example.qahwamanager.data.db.SpendingDao
import com.example.qahwamanager.data.db.StaffDao
import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.data.time.SystemClock
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
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
    fun provideStaffDao(db: QahwaDatabase): StaffDao = db.staffDao()

    @Provides
    fun provideShiftDao(db: QahwaDatabase): ShiftDao = db.shiftDao()

    @Provides
    fun provideOwnerDao(db: QahwaDatabase): OwnerDao = db.ownerDao()

    @Provides
    fun provideSpendingDao(db: QahwaDatabase): SpendingDao = db.spendingDao()

    @Provides
    fun provideLoyaltyCustomerDao(db: QahwaDatabase): LoyaltyCustomerDao = db.loyaltyCustomerDao()

    @Provides
    fun provideClock(): Clock = SystemClock

    @Provides
    @HashingDispatcher
    fun provideHashingDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
