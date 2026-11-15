package com.example.qahwamanager.data.db

import androidx.room.TypeConverter
import com.example.qahwamanager.data.model.OrderStatus

class Converters {
    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = OrderStatus.valueOf(value)
}

