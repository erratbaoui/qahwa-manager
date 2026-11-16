package com.example.qahwamanager.ui.inventory

import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.repository.InventoryRepository
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
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        fakeDao = FakeInventoryDao(listOf(milk))
        viewModel = InventoryViewModel(InventoryRepository(fakeDao))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun adjustQuantity_increasesStoredQuantity() = runTest {
        viewModel.adjustQuantity(milk, 2.5)
        advanceUntilIdle()

        assertEquals(7.5, fakeDao.quantityOf(1)!!, 0.0)
    }

    @Test
    fun adjustQuantity_rapidTapsFromTheSameSnapshotAllApply() = runTest {
        // The card still shows 5.0 when the second tap lands, before the first write is observed.
        viewModel.adjustQuantity(milk, 0.5)
        viewModel.adjustQuantity(milk, 0.5)
        advanceUntilIdle()

        assertEquals(6.0, fakeDao.quantityOf(1)!!, 0.0)
    }

    @Test
    fun adjustQuantity_doesNotDropBelowZero() = runTest {
        viewModel.adjustQuantity(milk, -8.0)
        advanceUntilIdle()

        assertEquals(0.0, fakeDao.quantityOf(1)!!, 0.0)
    }

    @Test
    fun updateItem_updatesItemProperties() = runTest {
        val modified = milk.copy(name = "Whole milk", quantity = 8.0, lowStockThreshold = 3.0)

        viewModel.updateItem(modified)
        advanceUntilIdle()

        assertEquals(listOf(modified), fakeDao.items.value)
    }
}
