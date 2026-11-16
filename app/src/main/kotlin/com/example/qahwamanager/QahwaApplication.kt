package com.example.qahwamanager

import android.app.Application
import com.example.qahwamanager.notifications.LowStockScheduler
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class QahwaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        LowStockScheduler.scheduleBiHourlyCheck(this)
    }
}
