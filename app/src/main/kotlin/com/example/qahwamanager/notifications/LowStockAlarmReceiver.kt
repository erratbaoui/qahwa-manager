package com.example.qahwamanager.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.qahwamanager.data.repository.InventoryRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LowStockAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var inventoryRepository: InventoryRepository

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            LowStockScheduler.scheduleBiHourlyCheck(context)
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val lowStockItems = inventoryRepository.getLowStockItems().first()
                LowStockNotifier.notifyIfLowStock(
                    context = appContext,
                    lowStockItems = lowStockItems,
                    force = intent?.action == ACTION_CHECK_LOW_STOCK,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_CHECK_LOW_STOCK = "com.example.qahwamanager.action.CHECK_LOW_STOCK"
    }
}
