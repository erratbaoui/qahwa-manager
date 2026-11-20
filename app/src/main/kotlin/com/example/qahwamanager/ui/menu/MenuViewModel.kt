package com.example.qahwamanager.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.repository.MenuRepository
import com.example.qahwamanager.security.OwnerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val ownerSession: OwnerSession
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow("coffee")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val menuItems: StateFlow<List<MenuItem>> = _selectedCategory
        .flatMapLatest { category ->
            if (category == "all") menuRepository.getAllItems()
            else menuRepository.getItemsByCategory(category)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    // Changing the menu and prices is the owner's job; the UI asks for the PIN first, and the
    // view model refuses anyway if owner mode is off.
    fun addItem(item: MenuItem) = ownerOnly { menuRepository.addItem(item) }

    fun updateItem(item: MenuItem) = ownerOnly { menuRepository.updateItem(item) }

    fun deleteItem(item: MenuItem) = ownerOnly { menuRepository.deleteItem(item) }

    /** "We ran out": any waiter can flip availability. */
    fun toggleAvailability(item: MenuItem) = viewModelScope.launch {
        menuRepository.updateItem(item.copy(isAvailable = !item.isAvailable))
    }

    private fun ownerOnly(block: suspend () -> Unit) {
        if (!ownerSession.isOwnerMode.value) return
        viewModelScope.launch { block() }
    }
}

