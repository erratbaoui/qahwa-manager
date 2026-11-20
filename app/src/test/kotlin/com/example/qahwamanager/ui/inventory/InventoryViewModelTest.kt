package com.example.qahwamanager.ui.inventory

import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.repository.InventoryRepository
import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.data.time.Clock
import com.example.qahwamanager.testing.FakeInventoryDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val milk = InventoryItem(id = 1, name = "Milk", quantity = 5.0, unit = "L")
    private lateinit var fakeDao: FakeInventoryDao
    private lateinit var session: OwnerSession
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        fakeDao = FakeInventoryDao(listOf(milk))
        session = OwnerSession(object : Clock { override fun nowMillis() = 0L })
        viewModel = InventoryViewModel(InventoryRepository(fakeDao), session)
    }

    @Test
    fun adjustQuantity_locked_doesNothing() = runTest {
        viewModel.adjustQuantity(milk, 2.5)
        advanceUntilIdle()

        assertEquals(5.0, fakeDao.quantityOf(1)!!, 0.0)
    }

    @Test
    fun adjustQuantity_ownerMode_adjusts() = runTest {
        session.unlock()

        viewModel.adjustQuantity(milk, 2.5)
        advanceUntilIdle()

        assertEquals(7.5, fakeDao.quantityOf(1)!!, 0.0)
    }

    @Test
    fun addAndDelete_locked_doNothing() = runTest {
        viewModel.addItem(InventoryItem(id = 2, name = "Sugar", quantity = 1.0, unit = "kg"))
        viewModel.deleteItem(milk)
        advanceUntilIdle()

        assertEquals(listOf(milk), fakeDao.items.value)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun adjustQuantity_increasesStoredQuantity() = runTest {
        session.unlock()
        viewModel.adjustQuantity(milk, 2.5)
        advanceUntilIdle()

        assertEquals(7.5, fakeDao.quantityOf(1)!!, 0.0)
    }

    @Test
    fun adjustQuantity_rapidTapsFromTheSameSnapshotAllApply() = runTest {
        session.unlock()
        // The card still shows 5.0 when the second tap lands, before the first write is observed.
        viewModel.adjustQuantity(milk, 0.5)
        viewModel.adjustQuantity(milk, 0.5)
        advanceUntilIdle()

        assertEquals(6.0, fakeDao.quantityOf(1)!!, 0.0)
    }

    @Test
    fun adjustQuantity_doesNotDropBelowZero() = runTest {
        session.unlock()
        viewModel.adjustQuantity(milk, -8.0)
        advanceUntilIdle()

        assertEquals(0.0, fakeDao.quantityOf(1)!!, 0.0)
    }

    @Test
    fun updateItem_updatesItemProperties() = runTest {
        val modified = milk.copy(name = "Whole milk", quantity = 8.0, lowStockThreshold = 3.0)
        session.unlock()

        viewModel.updateItem(modified)
        advanceUntilIdle()

        assertEquals(listOf(modified), fakeDao.items.value)
    }
}
