package com.example.qahwamanager.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository
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

    fun addItem(item: MenuItem) = viewModelScope.launch {
        menuRepository.addItem(item)
    }

    fun updateItem(item: MenuItem) = viewModelScope.launch {
        menuRepository.updateItem(item)
    }

    fun deleteItem(item: MenuItem) = viewModelScope.launch {
        menuRepository.deleteItem(item)
    }

    fun toggleAvailability(item: MenuItem) = viewModelScope.launch {
        menuRepository.updateItem(item.copy(isAvailable = !item.isAvailable))
    }
}

