package com.example.qahwamanager.ui.inventory

import com.example.qahwamanager.data.db.InventoryDao
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.repository.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakeInventoryDao
    private lateinit var repository: InventoryRepository
    private lateinit var viewModel: InventoryViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeInventoryDao()
        repository = InventoryRepository(fakeDao)
        viewModel = InventoryViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun adjustQuantity_increasesQuantityCorrectly() = runTest {
        val item = InventoryItem(id = 1, name = "Coffee Beans", quantity = 5.0, unit = "kg")
        fakeDao.items.value = listOf(item)

        viewModel.adjustQuantity(item, 2.5)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(7.5, fakeDao.lastUpdated?.quantity ?: 0.0, 0.01)
    }

    @Test
    fun adjustQuantity_decreasesQuantityAndDoesNotDropBelowZero() = runTest {
        val item = InventoryItem(id = 1, name = "Milk", quantity = 1.0, unit = "L")
        fakeDao.items.value = listOf(item)

        viewModel.adjustQuantity(item, -2.0)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0.0, fakeDao.lastUpdated?.quantity ?: 0.0, 0.01)
    }

    @Test
    fun updateItem_updatesItemProperties() = runTest {
        val original = InventoryItem(id = 2, name = "Sugar", quantity = 10.0, unit = "kg", lowStockThreshold = 2.0)
        val modified = original.copy(name = "Brown Sugar", quantity = 8.0, lowStockThreshold = 3.0)

        viewModel.updateItem(modified)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Brown Sugar", fakeDao.lastUpdated?.name)
        assertEquals(8.0, fakeDao.lastUpdated?.quantity ?: 0.0, 0.01)
        assertEquals(3.0, fakeDao.lastUpdated?.lowStockThreshold ?: 0.0, 0.01)
    }

    private class FakeInventoryDao : InventoryDao {
        val items = MutableStateFlow<List<InventoryItem>>(emptyList())
        var lastUpdated: InventoryItem? = null

        override fun getAllItems(): Flow<List<InventoryItem>> = items
        override fun getLowStockItems(): Flow<List<InventoryItem>> = items

        override suspend fun insert(item: InventoryItem): Long {
            items.value = items.value + item
            return item.id
        }

        override suspend fun update(item: InventoryItem) {
            lastUpdated = item
            items.value = items.value.map { if (it.id == item.id) item else it }
        }

        override suspend fun delete(item: InventoryItem) {
            items.value = items.value.filter { it.id != item.id }
        }

        override suspend fun getById(id: Long): InventoryItem? = items.value.find { it.id == id }
    }
}
