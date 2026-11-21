package com.example.qahwamanager.testing

import com.example.qahwamanager.data.db.InventoryDao
import com.example.qahwamanager.data.db.MenuItemDao
import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.db.OrderItemDao
import com.example.qahwamanager.data.db.OwnerDao
import com.example.qahwamanager.data.db.ShiftAlreadyActiveException
import com.example.qahwamanager.data.db.ShiftDao
import com.example.qahwamanager.data.db.StaffDao
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.OrderItem
import com.example.qahwamanager.data.model.OrderStatus
import com.example.qahwamanager.data.model.OwnerCredentials
import com.example.qahwamanager.data.model.Shift
import com.example.qahwamanager.data.model.ShiftLeftOpen
import com.example.qahwamanager.data.model.ShiftStatus
import com.example.qahwamanager.data.model.ShiftWithStaff
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.time.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    override suspend fun markPaidIfOpen(id: Long, shiftId: Long, now: Long): Int {
        delay(IO_DELAY_MS)
        val target = orders.value.find { it.id == id && it.status == OrderStatus.OPEN } ?: return 0
        orders.update { list ->
            list.map {
                if (it.id == target.id) it.copy(status = OrderStatus.PAID, paidShiftId = shiftId, paidAt = now) else it
            }
        }
        return 1
    }

    override suspend fun cancelIfOpen(id: Long, shiftId: Long?, now: Long): Int {
        delay(IO_DELAY_MS)
        val target = orders.value.find { it.id == id && it.status == OrderStatus.OPEN } ?: return 0
        orders.update { list ->
            list.map {
                if (it.id == target.id) it.copy(status = OrderStatus.CANCELLED, cancelledAt = now, cancelledShiftId = shiftId) else it
            }
        }
        return 1
    }

    override fun observePaidInShift(shiftId: Long): Flow<List<Order>> =
        orders.map { list ->
            list.filter { it.status == OrderStatus.PAID && it.paidShiftId == shiftId }
                .sortedWith(compareByDescending<Order> { it.paidAt ?: 0 }.thenByDescending { it.id })
        }

    // Mirrors: status = 'CANCELLED' AND COALESCE(openedShiftId, cancelledShiftId) = :shiftId
    override fun observeCancelledForShift(shiftId: Long): Flow<List<Order>> =
        orders.map { list ->
            list.filter { it.status == OrderStatus.CANCELLED && (it.openedShiftId ?: it.cancelledShiftId) == shiftId }
                .sortedWith(compareByDescending<Order> { it.cancelledAt ?: 0 }.thenByDescending { it.id })
        }

    /** Set by [FakeShiftDao], which owns the shift_left_open rows. */
    internal var leftOpenRows: MutableStateFlow<List<ShiftLeftOpen>> = MutableStateFlow(emptyList())

    override fun observeLeftOpenOrders(shiftId: Long): Flow<List<Order>> =
        combine(orders, leftOpenRows) { list, rows ->
            val ids = rows.filter { it.shiftId == shiftId }.map { it.orderId }.toSet()
            list.filter { it.id in ids }.sortedBy { it.id }
        }

    override fun observeById(id: Long): Flow<Order?> = orders.map { list -> list.find { it.id == id } }

    override suspend fun getAllSnapshot(): List<Order> = orders.value
    override suspend fun getById(id: Long): Order? = orders.value.find { it.id == id }
    override suspend fun getByRemoteId(remoteId: String): Order? = orders.value.find { it.remoteId == remoteId }
    override suspend fun getItemsSnapshot(orderId: Long): List<OrderItem> = items.value.filter { it.orderId == orderId }
    override suspend fun deleteItemsForOrder(orderId: Long) {
        items.update { list -> list.filter { it.orderId != orderId } }
    }
    override suspend fun updateOrder(order: Order) {
        orders.update { list -> list.map { if (it.id == order.id) order else it } }
    }
}

class FakeStaffDao : StaffDao {
    val staff = MutableStateFlow<List<Staff>>(emptyList())

    private fun List<Staff>.ordered() = sortedWith(compareByDescending<Staff> { it.isOwner }.thenBy { it.name })

    override fun observeActive(): Flow<List<Staff>> = staff.map { list -> list.filter { it.isActive }.ordered() }
    override fun observeAll(): Flow<List<Staff>> = staff.map { it.ordered() }
    override suspend fun getAllSnapshot(): List<Staff> = staff.value
    override suspend fun getById(id: Long): Staff? = staff.value.find { it.id == id }
    override suspend fun getByRemoteId(remoteId: String): Staff? = staff.value.find { it.remoteId == remoteId }
    override suspend fun insert(staff: Staff): Long {
        val id = (this.staff.value.maxOfOrNull { it.id } ?: 0) + 1
        this.staff.update { it + staff.copy(id = id) }
        return id
    }
    override suspend fun update(staff: Staff) {
        this.staff.update { list -> list.map { if (it.id == staff.id) staff else it } }
    }
}

/**
 * Mirrors [ShiftDao] including its `@Transaction` methods, which are serialized with a [Mutex] and
 * pay [IO_DELAY_MS] so concurrent calls interleave the way they do against Room.
 */
class FakeShiftDao(private val orderDao: FakeOrderDao, private val staffDao: FakeStaffDao) : ShiftDao() {
    val shifts = MutableStateFlow<List<Shift>>(emptyList())
    val leftOpen = MutableStateFlow<List<ShiftLeftOpen>>(emptyList())
    private val transaction = Mutex()

    init {
        orderDao.leftOpenRows = leftOpen
    }

    private fun Shift.withStaff(): ShiftWithStaff {
        val person = staffDao.staff.value.find { it.id == staffId }
        return ShiftWithStaff(this, person?.name ?: "", person?.role ?: com.example.qahwamanager.data.model.StaffRole.WAITER)
    }
    private fun withStaff(list: List<Shift>): Flow<List<ShiftWithStaff>> =
        combine(shifts, staffDao.staff) { _, _ -> list.map { it.withStaff() } }

    override suspend fun getActive(): Shift? = shifts.value.find { it.status == ShiftStatus.ACTIVE }

    override fun observeActive(): Flow<ShiftWithStaff?> =
        combine(shifts, staffDao.staff) { list, _ -> list.find { it.status == ShiftStatus.ACTIVE }?.withStaff() }

    override fun observeById(id: Long): Flow<ShiftWithStaff?> =
        combine(shifts, staffDao.staff) { list, _ -> list.find { it.id == id }?.withStaff() }

    override fun observeToSettle(): Flow<List<ShiftWithStaff>> =
        combine(shifts, staffDao.staff) { list, _ ->
            list.filter { it.status == ShiftStatus.ENDED }.sortedBy { it.endedAt }.map { it.withStaff() }
        }

    override fun observeSettled(staffId: Long?): Flow<List<ShiftWithStaff>> =
        combine(shifts, staffDao.staff) { list, _ ->
            list.filter { it.status == ShiftStatus.SETTLED && (staffId == null || it.staffId == staffId) }
                .sortedByDescending { it.settledAt }.map { it.withStaff() }
        }

    override fun observeClosedShifts(): Flow<List<ShiftWithStaff>> =
        combine(shifts, staffDao.staff) { list, _ ->
            list.filter { it.status == ShiftStatus.ENDED || it.status == ShiftStatus.SETTLED }
                .sortedByDescending { it.startedAt }.map { it.withStaff() }
        }

    override fun observeExpectedCentimes(shiftId: Long): Flow<Long> =
        orderDao.orders.map { list -> list.filter { it.paidShiftId == shiftId && it.status == OrderStatus.PAID }.sumOf { it.totalCentimes } }

    override suspend fun expectedCentimes(shiftId: Long): Long =
        orderDao.orders.value.filter { it.paidShiftId == shiftId && it.status == OrderStatus.PAID }.sumOf { it.totalCentimes }

    override fun observeLeftOpen(shiftId: Long): Flow<List<ShiftLeftOpen>> =
        leftOpen.map { list -> list.filter { it.shiftId == shiftId }.sortedBy { it.orderId } }

    override suspend fun getLeftOpenSnapshot(shiftId: Long): List<ShiftLeftOpen> =
        leftOpen.value.filter { it.shiftId == shiftId }.sortedBy { it.orderId }

    override suspend fun deleteLeftOpenForShift(shiftId: Long) {
        leftOpen.update { list -> list.filter { it.shiftId != shiftId } }
    }

    override suspend fun getAllSnapshot(): List<Shift> = shifts.value
    override suspend fun getById(id: Long): Shift? = shifts.value.find { it.id == id }
    override suspend fun getByRemoteId(remoteId: String): Shift? = shifts.value.find { it.remoteId == remoteId }
    override suspend fun updateShift(shift: Shift) {
        shifts.update { list -> list.map { if (it.id == shift.id) shift else it } }
    }

    override suspend fun recordFailedOwnerPinOnActive(): Int {
        delay(IO_DELAY_MS)
        val active = getActive() ?: return 0
        shifts.update { list ->
            list.map { if (it.id == active.id) it.copy(failedOwnerPinAttempts = it.failedOwnerPinAttempts + 1) else it }
        }
        return 1
    }

    override suspend fun insertShift(shift: Shift): Long {
        delay(IO_DELAY_MS)
        val id = (shifts.value.maxOfOrNull { it.id } ?: 0) + 1
        shifts.update { it + shift.copy(id = id) }
        return id
    }

    override suspend fun insertLeftOpen(rows: List<ShiftLeftOpen>) {
        leftOpen.update { it + rows }
    }

    override suspend fun snapshotOpenOrders(shiftId: Long) {
        delay(IO_DELAY_MS)
        val rows = orderDao.orders.value.filter { it.status == OrderStatus.OPEN }
            .map { ShiftLeftOpen(shiftId = shiftId, orderId = it.id, totalCentimes = it.totalCentimes) }
        insertLeftOpen(rows)
    }

    override suspend fun markEnded(id: Long, now: Long, takeover: Boolean, countedCentimes: Long?): Int {
        val target = shifts.value.find { it.id == id && it.status == ShiftStatus.ACTIVE } ?: return 0
        val expectedCentimes = expectedCentimes(id)
        shifts.update { list ->
            list.map {
                if (it.id == target.id) {
                    it.copy(
                        status = ShiftStatus.ENDED,
                        endedAt = now,
                        endedByTakeover = takeover,
                        expectedCentimes = expectedCentimes,
                        countedCentimes = countedCentimes,
                    )
                } else it
            }
        }
        return 1
    }

    override suspend fun markSettled(id: Long, expected: Long, counted: Long, now: Long): Int {
        delay(IO_DELAY_MS)
        val target = shifts.value.find { it.id == id && it.status == ShiftStatus.ENDED } ?: return 0
        shifts.update { list ->
            list.map {
                if (it.id == target.id) it.copy(status = ShiftStatus.SETTLED, expectedCentimes = expected, countedCentimes = counted, settledAt = now) else it
            }
        }
        return 1
    }

    override suspend fun startShift(staffId: Long, now: Long): Long = transaction.withLock {
        if (getActive() != null) throw ShiftAlreadyActiveException()
        insertShift(Shift(staffId = staffId, status = ShiftStatus.ACTIVE, startedAt = now))
    }

    override suspend fun endActiveShift(now: Long, takeover: Boolean, countedCentimes: Long?): Long? = transaction.withLock {
        val active = getActive() ?: return@withLock null
        snapshotOpenOrders(active.id)
        markEnded(active.id, now, takeover, countedCentimes)
        active.id
    }

    override suspend fun takeOver(newStaffId: Long, now: Long): Long = transaction.withLock {
        val active = getActive()
        if (active != null) {
            snapshotOpenOrders(active.id)
            markEnded(active.id, now, takeover = true)
        }
        insertShift(Shift(staffId = newStaffId, status = ShiftStatus.ACTIVE, startedAt = now))
    }

    override suspend fun settle(shiftId: Long, countedCentimes: Long, now: Long): Boolean = transaction.withLock {
        val expected = expectedCentimes(shiftId)
        markSettled(shiftId, expected, countedCentimes, now) == 1
    }
}

class FakeOwnerDao(private val staffDao: FakeStaffDao) : OwnerDao() {
    val credentials = MutableStateFlow<OwnerCredentials?>(null)

    override fun observeExists(): Flow<Boolean> = credentials.map { it != null }
    override suspend fun getCredentials(): OwnerCredentials? = credentials.value
    override suspend fun upsertCredentials(c: OwnerCredentials) {
        delay(IO_DELAY_MS)
        credentials.value = c.copy(id = 1)
    }
    override suspend fun insertStaff(staff: Staff): Long = staffDao.insert(staff)
    override suspend fun createOwner(owner: Staff, credentials: OwnerCredentials): Long {
        val id = insertStaff(owner)
        upsertCredentials(credentials)
        return id
    }
}

class FakeOrderItemDao(private val orderDao: FakeOrderDao) : OrderItemDao {
    override fun getItemsForOrder(orderId: Long): Flow<List<OrderItem>> =
        orderDao.items.map { list -> list.filter { it.orderId == orderId } }
}

class FakeMenuItemDao(initial: List<MenuItem> = emptyList()) : MenuItemDao {
    val items = MutableStateFlow(initial)

    override fun getAllItems(): Flow<List<MenuItem>> = items.map { list -> list.filter { !it.isDeleted } }
    override fun getItemsByCategory(category: String): Flow<List<MenuItem>> =
        items.map { list -> list.filter { !it.isDeleted && it.category == category } }
    override fun getAvailableItems(): Flow<List<MenuItem>> =
        items.map { list -> list.filter { !it.isDeleted && it.isAvailable } }
    override suspend fun getAllSnapshot(): List<MenuItem> = items.value
    override suspend fun getById(id: Long): MenuItem? = items.value.find { it.id == id }
    override suspend fun getByRemoteId(remoteId: String): MenuItem? = items.value.find { it.remoteId == remoteId }
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

    override fun getAllItems(): Flow<List<InventoryItem>> = items.map { list -> list.filter { !it.isDeleted } }
    override fun getLowStockItems(): Flow<List<InventoryItem>> =
        items.map { list -> list.filter { !it.isDeleted && it.quantity <= it.lowStockThreshold } }
    override suspend fun getAllSnapshot(): List<InventoryItem> = items.value
    override suspend fun insert(item: InventoryItem): Long {
        items.update { it + item }
        return item.id
    }
    override suspend fun update(item: InventoryItem) {
        delay(IO_DELAY_MS)
        items.update { list -> list.map { if (it.id == item.id) item else it } }
    }
    // Mirrors: UPDATE inventory_items SET quantity = MAX(0, quantity + :delta) WHERE id = :id
    override suspend fun adjustQuantity(id: Long, delta: Double, updatedAt: Long) {
        delay(IO_DELAY_MS)
        items.update { list ->
            list.map { if (it.id == id) it.copy(quantity = maxOf(0.0, it.quantity + delta), updatedAt = updatedAt) else it }
        }
    }
    override suspend fun getByRemoteId(remoteId: String): InventoryItem? =
        items.value.find { it.remoteId == remoteId }
    override suspend fun delete(item: InventoryItem) {
        items.update { list -> list.filter { it.id != item.id } }
    }
    override suspend fun getById(id: Long): InventoryItem? = items.value.find { it.id == id }
}
