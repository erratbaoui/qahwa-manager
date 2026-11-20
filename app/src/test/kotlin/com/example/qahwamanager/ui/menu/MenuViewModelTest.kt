package com.example.qahwamanager.ui.menu

import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.repository.MenuRepository
import com.example.qahwamanager.security.OwnerSession
import com.example.qahwamanager.testing.FakeMenuItemDao
import com.example.qahwamanager.testing.testClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MenuViewModelTest {

    private val tea = MenuItem(id = 7, name = "Mint tea", nameAr = "أتاي", nameFr = "Thé", category = "tea", priceCentimes = 850)
    private val dao = FakeMenuItemDao(listOf(tea))
    private lateinit var session: OwnerSession

    @Before
    fun setup() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): MenuViewModel {
        session = OwnerSession(testClock(0))
        return MenuViewModel(MenuRepository(dao), session)
    }

    @Test
    fun toggleAvailability_locked_works() = runTest {
        val vm = viewModel()

        vm.toggleAvailability(tea)
        advanceUntilIdle()

        assertEquals(false, dao.items.value.single().isAvailable)
    }

    @Test
    fun addItem_locked_doesNothing() = runTest {
        val vm = viewModel()

        vm.addItem(tea.copy(id = 0, name = "Espresso"))
        advanceUntilIdle()

        assertEquals(listOf("Mint tea"), dao.items.value.map { it.name })
    }

    @Test
    fun addItem_ownerMode_adds() = runTest {
        val vm = viewModel()
        session.unlock()

        vm.addItem(tea.copy(id = 0, name = "Espresso"))
        advanceUntilIdle()

        assertEquals(listOf("Mint tea", "Espresso"), dao.items.value.map { it.name })
    }

    @Test
    fun updateItem_locked_doesNothing_ownerMode_updates() = runTest {
        val vm = viewModel()

        vm.updateItem(tea.copy(priceCentimes = 900))
        advanceUntilIdle()
        assertEquals(850L, dao.items.value.single().priceCentimes)

        session.unlock()
        vm.updateItem(tea.copy(priceCentimes = 900))
        advanceUntilIdle()
        assertEquals(900L, dao.items.value.single().priceCentimes)
    }

    @Test
    fun deleteItem_locked_doesNothing() = runTest {
        val vm = viewModel()

        vm.deleteItem(tea)
        advanceUntilIdle()

        assertEquals(1, dao.items.value.size)
    }
}
