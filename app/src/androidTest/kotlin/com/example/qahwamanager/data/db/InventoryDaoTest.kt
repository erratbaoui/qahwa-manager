package com.example.qahwamanager.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.qahwamanager.data.model.InventoryItem
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryDaoTest {

    private lateinit var db: QahwaDatabase
    private lateinit var dao: InventoryDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            QahwaDatabase::class.java,
        ).build()
        dao = db.inventoryDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun adjustQuantity_appliesEachDeltaToTheStoredValue() = runTest {
        val id = dao.insert(InventoryItem(name = "Milk", quantity = 5.0, unit = "L"))

        dao.adjustQuantity(id, 0.5)
        dao.adjustQuantity(id, 0.5)

        assertEquals(6.0, dao.getById(id)!!.quantity, 0.0)
    }

    @Test
    fun adjustQuantity_neverGoesBelowZero() = runTest {
        val id = dao.insert(InventoryItem(name = "Mint", quantity = 1.0, unit = "kg"))

        dao.adjustQuantity(id, -2.0)

        assertEquals(0.0, dao.getById(id)!!.quantity, 0.0)
    }
}
