package com.example.qahwamanager.data.db

import androidx.room.TypeConverter
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.StaffRole

class Converters {
    @TypeConverter
    fun fromOrderStatus(value: OrderStatus): String = value.name

    @TypeConverter
    fun toOrderStatus(value: String): OrderStatus = OrderStatus.valueOf(value)

    @TypeConverter
    fun fromShiftStatus(value: ShiftStatus): String = value.name

    @TypeConverter
    fun toShiftStatus(value: String): ShiftStatus = ShiftStatus.valueOf(value)

    @TypeConverter
    fun fromStaffRole(value: StaffRole): String = value.name

    @TypeConverter
    fun toStaffRole(value: String): StaffRole = StaffRole.valueOf(value)
}
