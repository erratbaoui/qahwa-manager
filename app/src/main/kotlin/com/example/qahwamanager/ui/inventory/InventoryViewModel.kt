package com.example.qahwamanager.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.repository.InventoryRepository
import com.example.qahwamanager.security.OwnerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val inventoryRepository: InventoryRepository,
    private val ownerSession: OwnerSession,
) : ViewModel() {

    val inventoryItems: StateFlow<List<InventoryItem>> = inventoryRepository.getAllItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Stock edits are owner-only: the UI asks for the PIN, and these refuse if owner mode is off.
    fun addItem(item: InventoryItem) = ownerOnly { inventoryRepository.addItem(item) }

    fun updateItem(item: InventoryItem) = ownerOnly { inventoryRepository.updateItem(item) }

    /** Applies [delta] to the stored quantity (never below zero), not to the possibly stale [item] snapshot. */
    fun adjustQuantity(item: InventoryItem, delta: Double) = ownerOnly { inventoryRepository.adjustQuantity(item.id, delta) }

    fun deleteItem(item: InventoryItem) = ownerOnly { inventoryRepository.deleteItem(item) }

    private fun ownerOnly(block: suspend () -> Unit) {
        if (!ownerSession.isOwnerMode.value) return
        viewModelScope.launch { block() }
    }
}

