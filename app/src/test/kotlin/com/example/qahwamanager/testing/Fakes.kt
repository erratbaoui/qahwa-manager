package com.example.qahwamanager.testing

import com.example.qahwamanager.data.db.InventoryDao
import com.example.qahwamanager.data.db.MenuItemDao
import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.db.OrderItemDao
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.TestScope
import java.util.TimeZone

/** Simulated database latency, so concurrent calls interleave the way they do against Room. */
private const val IO_DELAY_MS = 10L

/** A clock that starts at [startMillis] and advances with the test's virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
fun TestScope.testClock(startMillis: Long, zone: TimeZone = TimeZone.getTimeZone("UTC")): Clock =
    object : Clock {
        override fun nowMillis() = startMillis + testScheduler.currentTime
        override fun timeZone() = zone
    }

class FakeOrderDao : OrderDao() {
    val orders = MutableStateFlow<List<Order>>(emptyList())
    val items = MutableStateFlow<List<OrderItem>>(emptyList())
    private var nextOrderId = 1L
    private var nextItemId = 1L

    fun seed(vararg seeded: Order) {
        orders.update { it + seeded }
        nextOrderId = (orders.value.maxOfOrNull { it.id } ?: 0) + 1
    }

    fun statusOf(id: Long): OrderStatus? = orders.value.find { it.id == id }?.status

    override fun getAllOrders(): Flow<List<Order>> =
        orders.map { list -> list.sortedByDescending { it.createdAt } }

    override fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>> =
        orders.map { list -> list.filter { it.status == status } }

    override fun getPaidOrdersSince(startMs: Long): Flow<List<Order>> =
        orders.map { list ->
            list.filter { it.status == OrderStatus.PAID && it.createdAt >= startMs }
                .sortedByDescending { it.createdAt }
        }

    override fun getRevenueSince(startMs: Long): Flow<Long> =
        getPaidOrdersSince(startMs).map { list -> list.sumOf { it.totalCentimes } }

    override suspend fun insertOrder(order: Order): Long {
        delay(IO_DELAY_MS)
        val id = nextOrderId++
        orders.update { it + order.copy(id = id) }
        return id
    }

    override suspend fun insertOrderItems(items: List<OrderItem>) {
        delay(IO_DELAY_MS)
        this.items.update { current -> current + items.map { it.copy(id = nextItemId++) } }
    }

    override suspend fun updateStatusIfOpen(id: Long, status: OrderStatus): Int {
        delay(IO_DELAY_MS)
        val target = orders.value.find { it.id == id && it.status == OrderStatus.OPEN } ?: return 0
        orders.update { list -> list.map { if (it.id == target.id) it.copy(status = status) else it } }
        return 1
    }

    override fun observeById(id: Long): Flow<Order?> = orders.map { list -> list.find { it.id == id } }
}

class FakeOrderItemDao(private val orderDao: FakeOrderDao) : OrderItemDao {
    override fun getItemsForOrder(orderId: Long): Flow<List<OrderItem>> =
        orderDao.items.map { list -> list.filter { it.orderId == orderId } }
}

class FakeMenuItemDao(initial: List<MenuItem> = emptyList()) : MenuItemDao {
    val items = MutableStateFlow(initial)

    override fun getAllItems(): Flow<List<MenuItem>> = items
    override fun getItemsByCategory(category: String): Flow<List<MenuItem>> =
        items.map { list -> list.filter { it.category == category } }
    override fun getAvailableItems(): Flow<List<MenuItem>> =
        items.map { list -> list.filter { it.isAvailable } }
    override suspend fun insert(item: MenuItem): Long {
        val id = (items.value.maxOfOrNull { it.id } ?: 0) + 1
        items.update { it + item.copy(id = id) }
        return id
    }
    override suspend fun update(item: MenuItem) {
        items.update { list -> list.map { if (it.id == item.id) item else it } }
    }
    override suspend fun delete(item: MenuItem) {
        items.update { list -> list.filter { it.id != item.id } }
    }
}

class FakeInventoryDao(initial: List<InventoryItem> = emptyList()) : InventoryDao {
    val items = MutableStateFlow(initial)

    fun quantityOf(id: Long): Double? = items.value.find { it.id == id }?.quantity

    override fun getAllItems(): Flow<List<InventoryItem>> = items
    override fun getLowStockItems(): Flow<List<InventoryItem>> =
        items.map { list -> list.filter { it.quantity <= it.lowStockThreshold } }
    override suspend fun insert(item: InventoryItem): Long {
        items.update { it + item }
        return item.id
    }
    override suspend fun update(item: InventoryItem) {
        delay(IO_DELAY_MS)
        items.update { list -> list.map { if (it.id == item.id) item else it } }
    }
    // Mirrors: UPDATE inventory_items SET quantity = MAX(0, quantity + :delta) WHERE id = :id
    override suspend fun adjustQuantity(id: Long, delta: Double) {
        delay(IO_DELAY_MS)
        items.update { list ->
            list.map { if (it.id == id) it.copy(quantity = maxOf(0.0, it.quantity + delta)) else it }
        }
    }
    override suspend fun delete(item: InventoryItem) {
        items.update { list -> list.filter { it.id != item.id } }
    }
    override suspend fun getById(id: Long): InventoryItem? = items.value.find { it.id == id }
}
