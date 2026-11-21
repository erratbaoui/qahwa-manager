package com.example.qahwamanager.data.cloud

import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.Shift
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.model.StaffRole

data class RemoteShiftLeftOpen(
    val orderRemoteId: String,
    val totalCentimes: Long,
)

data class RemoteShiftRecord(
    val remoteId: String,
    val staffRemoteId: String,
    val status: ShiftStatus,
    val startedAt: Long,
    val endedAt: Long?,
    val endedByTakeover: Boolean,
    val settledAt: Long?,
    val expectedCentimes: Long?,
    val countedCentimes: Long?,
    val failedOwnerPinAttempts: Int,
    val updatedAt: Long,
    val leftOpen: List<RemoteShiftLeftOpen>,
)

data class RemoteOrderRecord(
    val remoteId: String,
    val tableNumber: String,
    val status: OrderStatus,
    val createdAt: Long,
    val totalCentimes: Long,
    val openedShiftRemoteId: String?,
    val paidShiftRemoteId: String?,
    val paidAt: Long?,
    val cancelledAt: Long?,
    val cancelledShiftRemoteId: String?,
    val updatedAt: Long,
    val items: List<OrderItem>,
)

/**
 * Pure Kotlin serialization between Room entities and Cloud Firestore document maps.
 * Kept free of Android/Firebase SDK classes so it can be thoroughly unit tested.
 */
object FirestoreSyncMapper {

    fun menuItemToMap(item: MenuItem): Map<String, Any?> = mapOf(
        "name" to item.name,
        "nameAr" to item.nameAr,
        "nameFr" to item.nameFr,
        "category" to item.category,
        "priceCentimes" to item.priceCentimes,
        "description" to item.description,
        "isAvailable" to item.isAvailable,
        "isDeleted" to item.isDeleted,
        "updatedAt" to item.updatedAt,
    )

    fun mapToMenuItem(remoteId: String, data: Map<String, Any?>): MenuItem? {
        val name = data["name"] as? String ?: return null
        return MenuItem(
            name = name,
            nameAr = (data["nameAr"] as? String).orEmpty(),
            nameFr = (data["nameFr"] as? String).orEmpty(),
            category = (data["category"] as? String) ?: "coffee",
            priceCentimes = data.longValue("priceCentimes") ?: 0L,
            description = (data["description"] as? String).orEmpty(),
            isAvailable = (data["isAvailable"] as? Boolean) ?: true,
            remoteId = remoteId,
            updatedAt = data.longValue("updatedAt") ?: 0L,
            isDeleted = (data["isDeleted"] as? Boolean) ?: false,
        )
    }

    fun inventoryItemToMap(item: InventoryItem): Map<String, Any?> = mapOf(
        "name" to item.name,
        "quantity" to item.quantity,
        "unit" to item.unit,
        "lowStockThreshold" to item.lowStockThreshold,
        "isDeleted" to item.isDeleted,
        "updatedAt" to item.updatedAt,
    )

    fun mapToInventoryItem(remoteId: String, data: Map<String, Any?>): InventoryItem? {
        val name = data["name"] as? String ?: return null
        return InventoryItem(
            name = name,
            quantity = data.doubleValue("quantity") ?: 0.0,
            unit = (data["unit"] as? String) ?: "pcs",
            lowStockThreshold = data.doubleValue("lowStockThreshold") ?: 1.0,
            remoteId = remoteId,
            updatedAt = data.longValue("updatedAt") ?: 0L,
            isDeleted = (data["isDeleted"] as? Boolean) ?: false,
        )
    }

    fun staffToMap(staff: Staff): Map<String, Any?> = mapOf(
        "name" to staff.name,
        "role" to staff.role.name,
        "isOwner" to staff.isOwner,
        "isActive" to staff.isActive,
        "createdAt" to staff.createdAt,
        "updatedAt" to staff.updatedAt,
    )

    fun mapToStaff(remoteId: String, data: Map<String, Any?>): Staff? {
        val name = data["name"] as? String ?: return null
        val role = runCatching { StaffRole.valueOf((data["role"] as? String).orEmpty()) }
            .getOrDefault(StaffRole.WAITER)
        return Staff(
            name = name,
            role = role,
            isOwner = (data["isOwner"] as? Boolean) ?: false,
            isActive = (data["isActive"] as? Boolean) ?: true,
            createdAt = data.longValue("createdAt") ?: 0L,
            remoteId = remoteId,
            updatedAt = data.longValue("updatedAt") ?: 0L,
        )
    }

    fun shiftToMap(
        shift: Shift,
        staffRemoteId: String,
        leftOpen: List<RemoteShiftLeftOpen>,
    ): Map<String, Any?> = mapOf(
        "staffId" to staffRemoteId,
        "staffRemoteId" to staffRemoteId,
        "status" to shift.status.name,
        "startedAt" to shift.startedAt,
        "endedAt" to shift.endedAt,
        "endedByTakeover" to shift.endedByTakeover,
        "settledAt" to shift.settledAt,
        "expectedCentimes" to shift.expectedCentimes,
        "countedCentimes" to shift.countedCentimes,
        "failedOwnerPinAttempts" to shift.failedOwnerPinAttempts,
        "updatedAt" to shift.updatedAt,
        "leftOpen" to leftOpen.map {
            mapOf("orderRemoteId" to it.orderRemoteId, "totalCentimes" to it.totalCentimes)
        },
    )

    fun mapToShift(remoteId: String, data: Map<String, Any?>): RemoteShiftRecord? {
        val staffRemoteId = (data["staffRemoteId"] as? String) ?: (data["staffId"] as? String) ?: return null
        val status = runCatching { ShiftStatus.valueOf((data["status"] as? String).orEmpty()) }
            .getOrDefault(ShiftStatus.ENDED)
        val rawLeftOpen = (data["leftOpen"] as? List<*>).orEmpty()
        val leftOpen = rawLeftOpen.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val orderRemoteId = map["orderRemoteId"] as? String ?: return@mapNotNull null
            val totalCentimes = (map["totalCentimes"] as? Number)?.toLong() ?: 0L
            RemoteShiftLeftOpen(orderRemoteId, totalCentimes)
        }
        return RemoteShiftRecord(
            remoteId = remoteId,
            staffRemoteId = staffRemoteId,
            status = status,
            startedAt = data.longValue("startedAt") ?: 0L,
            endedAt = data.longValue("endedAt"),
            endedByTakeover = (data["endedByTakeover"] as? Boolean) ?: false,
            settledAt = data.longValue("settledAt"),
            expectedCentimes = data.longValue("expectedCentimes"),
            countedCentimes = data.longValue("countedCentimes"),
            failedOwnerPinAttempts = (data["failedOwnerPinAttempts"] as? Number)?.toInt() ?: 0,
            updatedAt = data.longValue("updatedAt") ?: 0L,
            leftOpen = leftOpen,
        )
    }

    fun orderToMap(
        order: Order,
        items: List<OrderItem>,
        openedShiftRemoteId: String?,
        paidShiftRemoteId: String?,
        cancelledShiftRemoteId: String?,
    ): Map<String, Any?> = mapOf(
        "tableNumber" to order.tableNumber,
        "status" to order.status.name,
        "createdAt" to order.createdAt,
        "totalCentimes" to order.totalCentimes,
        "openedShiftId" to openedShiftRemoteId,
        "paidShiftId" to paidShiftRemoteId,
        "paidAt" to order.paidAt,
        "cancelledAt" to order.cancelledAt,
        "cancelledShiftId" to cancelledShiftRemoteId,
        "updatedAt" to order.updatedAt,
        "items" to items.map { item ->
            mapOf(
                "menuItemId" to item.menuItemId,
                "menuItemName" to item.menuItemName,
                "menuItemNameAr" to item.menuItemNameAr,
                "menuItemNameFr" to item.menuItemNameFr,
                "quantity" to item.quantity,
                "unitPriceCentimes" to item.unitPriceCentimes,
            )
        },
    )

    fun mapToOrder(remoteId: String, data: Map<String, Any?>): RemoteOrderRecord? {
        val tableNumber = data["tableNumber"] as? String ?: return null
        val status = runCatching { OrderStatus.valueOf((data["status"] as? String).orEmpty()) }
            .getOrDefault(OrderStatus.OPEN)
        val rawItems = (data["items"] as? List<*>).orEmpty()
        val items = rawItems.mapNotNull { entry ->
            val map = entry as? Map<*, *> ?: return@mapNotNull null
            val menuItemName = map["menuItemName"] as? String ?: return@mapNotNull null
            OrderItem(
                orderId = 0L,
                menuItemId = (map["menuItemId"] as? Number)?.toLong() ?: 0L,
                menuItemName = menuItemName,
                menuItemNameAr = (map["menuItemNameAr"] as? String).orEmpty(),
                menuItemNameFr = (map["menuItemNameFr"] as? String).orEmpty(),
                quantity = (map["quantity"] as? Number)?.toInt() ?: 1,
                unitPriceCentimes = (map["unitPriceCentimes"] as? Number)?.toLong() ?: 0L,
            )
        }
        return RemoteOrderRecord(
            remoteId = remoteId,
            tableNumber = tableNumber,
            status = status,
            createdAt = data.longValue("createdAt") ?: 0L,
            totalCentimes = data.longValue("totalCentimes") ?: 0L,
            openedShiftRemoteId = (data["openedShiftId"] as? String) ?: (data["openedShiftRemoteId"] as? String),
            paidShiftRemoteId = (data["paidShiftId"] as? String) ?: (data["paidShiftRemoteId"] as? String),
            paidAt = data.longValue("paidAt"),
            cancelledAt = data.longValue("cancelledAt"),
            cancelledShiftRemoteId = (data["cancelledShiftId"] as? String) ?: (data["cancelledShiftRemoteId"] as? String),
            updatedAt = data.longValue("updatedAt") ?: 0L,
            items = items,
        )
    }

    /** Last-Write-Wins check: apply incoming remote row when no local row exists or remote is newer. */
    fun shouldApplyRemote(localUpdatedAt: Long?, remoteUpdatedAt: Long): Boolean =
        localUpdatedAt == null || remoteUpdatedAt > localUpdatedAt

    private fun Map<String, Any?>.longValue(key: String): Long? = (this[key] as? Number)?.toLong()
    private fun Map<String, Any?>.doubleValue(key: String): Double? = (this[key] as? Number)?.toDouble()
}
