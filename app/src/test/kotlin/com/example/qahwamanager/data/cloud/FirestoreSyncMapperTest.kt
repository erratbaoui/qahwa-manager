package com.example.qahwamanager.data.cloud

import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.model.LoyaltyConfig
import com.example.qahwamanager.data.model.LoyaltyCustomer
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.Shift
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.Spending
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.model.StaffRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreSyncMapperTest {

    @Test
    fun menuItem_roundTripsCleanly() {
        val original = MenuItem(
            name = "Café Cassé",
            nameAr = "كافيه كاسيه",
            nameFr = "Café Cassé",
            category = "coffee",
            priceCentimes = 900L,
            description = "Moroccan style",
            isAvailable = true,
            remoteId = "menu-uuid-1",
            updatedAt = 123456L,
            isDeleted = false,
        )

        val restored = FirestoreSyncMapper.mapToMenuItem(original.remoteId, FirestoreSyncMapper.menuItemToMap(original))

        assertNotNull(restored)
        assertEquals(original.copy(id = 0), restored)
    }

    @Test
    fun inventoryItem_roundTripsSoftDelete() {
        val original = InventoryItem(
            name = "Coffee Beans",
            quantity = 4.5,
            unit = "kg",
            lowStockThreshold = 1.0,
            remoteId = "inv-uuid-1",
            updatedAt = 555L,
            isDeleted = true,
        )

        val restored = FirestoreSyncMapper.mapToInventoryItem(original.remoteId, FirestoreSyncMapper.inventoryItemToMap(original))

        assertEquals(original.copy(id = 0), restored)
    }

    @Test
    fun staff_roundTripsAllRolesIncludingCleaning() {
        val original = Staff(
            name = "Fatima",
            role = StaffRole.CLEANING,
            isOwner = false,
            isActive = true,
            createdAt = 100L,
            remoteId = "staff-uuid-1",
            updatedAt = 200L,
        )

        val restored = FirestoreSyncMapper.mapToStaff(original.remoteId, FirestoreSyncMapper.staffToMap(original))

        assertEquals(original.copy(id = 0), restored)
    }

    @Test
    fun shift_roundTripsWithEmbeddedLeftOpen() {
        val shift = Shift(
            staffId = 5L,
            status = ShiftStatus.SETTLED,
            startedAt = 1_000L,
            endedAt = 2_000L,
            endedByTakeover = false,
            settledAt = 2_500L,
            expectedCentimes = 45_000L,
            countedCentimes = 45_000L,
            failedOwnerPinAttempts = 1,
            remoteId = "shift-uuid-1",
            updatedAt = 2_500L,
        )
        val leftOpen = listOf(RemoteShiftLeftOpen("order-uuid-9", 1_800L))

        val restored = FirestoreSyncMapper.mapToShift(
            shift.remoteId,
            FirestoreSyncMapper.shiftToMap(shift, "staff-uuid-1", leftOpen),
        )

        assertNotNull(restored)
        assertEquals("staff-uuid-1", restored!!.staffRemoteId)
        assertEquals(ShiftStatus.SETTLED, restored.status)
        assertEquals(45_000L, restored.countedCentimes)
        assertEquals(leftOpen, restored.leftOpen)
    }

    @Test
    fun order_roundTripsWithEmbeddedItems() {
        val order = Order(
            tableNumber = "4",
            status = OrderStatus.PAID,
            createdAt = 10_000L,
            totalCentimes = 2_400L,
            openedShiftId = 1L,
            paidShiftId = 1L,
            paidAt = 10_500L,
            remoteId = "order-uuid-1",
            updatedAt = 10_500L,
        )
        val items = listOf(
            OrderItem(
                orderId = 0L,
                menuItemId = 2L,
                menuItemName = "Atay Naa'naa",
                menuItemNameAr = "أتاي بالنعناع",
                menuItemNameFr = "Thé à la Menthe",
                quantity = 3,
                unitPriceCentimes = 800L,
            )
        )

        val restored = FirestoreSyncMapper.mapToOrder(
            order.remoteId,
            FirestoreSyncMapper.orderToMap(order, items, "shift-uuid-1", "shift-uuid-1", null),
        )

        assertNotNull(restored)
        assertEquals("4", restored!!.tableNumber)
        assertEquals(OrderStatus.PAID, restored.status)
        assertEquals("shift-uuid-1", restored.paidShiftRemoteId)
        assertEquals(1, restored.items.size)
        assertEquals("Atay Naa'naa", restored.items.first().menuItemName)
        assertEquals(3, restored.items.first().quantity)
    }

    @Test
    fun shouldApplyRemote_enforcesLastWriteWins() {
        assertTrue(FirestoreSyncMapper.shouldApplyRemote(localUpdatedAt = null, remoteUpdatedAt = 100L))
        assertTrue(FirestoreSyncMapper.shouldApplyRemote(localUpdatedAt = 100L, remoteUpdatedAt = 200L))
        assertFalse(FirestoreSyncMapper.shouldApplyRemote(localUpdatedAt = 200L, remoteUpdatedAt = 200L))
        assertFalse(FirestoreSyncMapper.shouldApplyRemote(localUpdatedAt = 300L, remoteUpdatedAt = 200L))
    }

    @Test
    fun spending_roundTripsWithShiftLink() {
        val spending = Spending(
            shiftId = 3L,
            title = "Milk & Mint",
            amountCentimes = 4_500L,
            spentAt = 9_000L,
            remoteId = "spending-uuid-1",
            updatedAt = 9_500L,
            isDeleted = false,
        )

        val restored = FirestoreSyncMapper.mapToSpending(
            spending.remoteId,
            FirestoreSyncMapper.spendingToMap(spending, "shift-uuid-3"),
        )

        assertNotNull(restored)
        assertEquals("Milk & Mint", restored!!.title)
        assertEquals(4_500L, restored.amountCentimes)
        assertEquals("shift-uuid-3", restored.shiftRemoteId)
        assertFalse(restored.isDeleted)
    }

    @Test
    fun loyaltyCustomerAndConfig_roundTripCleanly() {
        val customer = LoyaltyCustomer(
            name = "Salma",
            phone = "+212611223344",
            currentStamps = 5,
            lifetimeStamps = 13,
            rewardsRedeemed = 1,
            lastVisitAt = 50_000L,
            createdAt = 10_000L,
            remoteId = "loyalty-uuid-1",
            updatedAt = 50_000L,
            isDeleted = false,
        )
        val restoredCustomer = FirestoreSyncMapper.mapToLoyaltyCustomer(
            customer.remoteId,
            FirestoreSyncMapper.loyaltyCustomerToMap(customer),
        )
        assertEquals(customer.copy(id = 0), restoredCustomer)

        val config = LoyaltyConfig(
            stampsRequired = 8,
            rewardTitle = "1 Free Mint Tea",
            googleReviewUrl = "https://g.page/r/test/review",
            updatedAt = 77_000L,
        )
        val restoredConfig = FirestoreSyncMapper.mapToLoyaltyConfig(
            FirestoreSyncMapper.loyaltyConfigToMap(config, "Café Atlas"),
        )
        assertEquals(config.copy(cafeName = "Café Atlas"), restoredConfig)
    }
}
