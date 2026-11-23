package com.example.qahwamanager.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.qahwamanager.data.model.LoyaltyCustomer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Dao
interface LoyaltyCustomerDao {
    @Query("SELECT * FROM loyalty_customers WHERE isDeleted = 0 ORDER BY lastVisitAt DESC, id DESC")
    fun observeAll(): Flow<List<LoyaltyCustomer>>

    @Query("SELECT * FROM loyalty_customers")
    suspend fun getAllSnapshot(): List<LoyaltyCustomer>

    @Query("SELECT * FROM loyalty_customers WHERE id = :id")
    suspend fun getById(id: Long): LoyaltyCustomer?

    @Query("SELECT * FROM loyalty_customers WHERE remoteId = :remoteId")
    suspend fun getByRemoteId(remoteId: String): LoyaltyCustomer?

    @Query("SELECT * FROM loyalty_customers WHERE isDeleted = 0 AND phone = :phone LIMIT 1")
    suspend fun getByPhone(phone: String): LoyaltyCustomer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(customer: LoyaltyCustomer): Long

    @Update
    suspend fun update(customer: LoyaltyCustomer)
}

object EmptyLoyaltyCustomerDao : LoyaltyCustomerDao {
    override fun observeAll(): Flow<List<LoyaltyCustomer>> = flowOf(emptyList())
    override suspend fun getAllSnapshot(): List<LoyaltyCustomer> = emptyList()
    override suspend fun getById(id: Long): LoyaltyCustomer? = null
    override suspend fun getByRemoteId(remoteId: String): LoyaltyCustomer? = null
    override suspend fun getByPhone(phone: String): LoyaltyCustomer? = null
    override suspend fun insert(customer: LoyaltyCustomer): Long = 0L
    override suspend fun update(customer: LoyaltyCustomer) = Unit
}
