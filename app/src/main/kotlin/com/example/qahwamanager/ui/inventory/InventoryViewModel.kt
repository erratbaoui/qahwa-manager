package com.example.qahwamanager.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.repository.InventoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
) : ViewModel() {

    val inventoryItems: StateFlow<List<InventoryItem>> = inventoryRepository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addItem(item: InventoryItem) = viewModelScope.launch {
        inventoryRepository.addItem(item)
    }

    fun updateItem(item: InventoryItem) = viewModelScope.launch {
        inventoryRepository.updateItem(item)
    }

    /** Applies [delta] to the stored quantity (never below zero), not to the possibly stale [item] snapshot. */
    fun adjustQuantity(item: InventoryItem, delta: Double) = viewModelScope.launch {
        inventoryRepository.adjustQuantity(item.id, delta)
    }

    fun deleteItem(item: InventoryItem) = viewModelScope.launch {
        inventoryRepository.deleteItem(item)
    }
}

