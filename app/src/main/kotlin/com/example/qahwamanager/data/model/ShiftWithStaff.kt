package com.example.qahwamanager.data.model

import androidx.room.Embedded

/** A shift with the name of the person who ran it, from `SELECT shifts.*, staff.name AS staffName`. */
data class ShiftWithStaff(
    @Embedded val shift: Shift,
    val staffName: String
)
