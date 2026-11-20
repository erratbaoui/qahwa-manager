package com.example.qahwamanager.data.model

import androidx.room.Embedded

/** A shift with the name and role of the person who ran it, from `SELECT shifts.*, staff.name AS staffName, staff.role AS staffRole`. */
data class ShiftWithStaff(
    @Embedded val shift: Shift,
    val staffName: String,
    val staffRole: StaffRole = StaffRole.WAITER,
)
