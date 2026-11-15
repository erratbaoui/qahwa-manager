package com.example.qahwamanager

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object Home : NavKey
@Serializable data object Menu : NavKey
@Serializable data object Orders : NavKey
@Serializable data object NewOrder : NavKey
@Serializable data class OrderDetail(val orderId: Long) : NavKey
@Serializable data object Sales : NavKey
@Serializable data object Inventory : NavKey

