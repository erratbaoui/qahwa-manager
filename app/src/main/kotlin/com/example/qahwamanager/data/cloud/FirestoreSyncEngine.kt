package com.example.qahwamanager.data.cloud

import android.util.Log
import com.example.qahwamanager.data.db.InventoryDao
import com.example.qahwamanager.data.db.MenuItemDao
import com.example.qahwamanager.data.db.OrderDao
import com.example.qahwamanager.data.db.ShiftDao
import com.example.qahwamanager.data.db.StaffDao
import com.example.qahwamanager.data.model.InventoryItem
import com.example.qahwamanager.data.model.MenuItem
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.Shift
import com.example.qahwamanager.data.model.ShiftLeftOpen
import com.example.qahwamanager.data.model.Staff
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bidirectional offline-first sync bridge between local Room SQLite and Cloud Firestore
 * scoped strictly to `/cafes/{cafeId}/...`.
 */
@Singleton
class FirestoreSyncEngine @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val menuItemDao: MenuItemDao,
    private val inventoryDao: InventoryDao,
    private val staffDao: StaffDao,
    private val shiftDao: ShiftDao,
    private val orderDao: OrderDao,
) : SyncPublisher {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inboundMutex = Mutex()
    private val listeners = mutableListOf<ListenerRegistration>()

    @Volatile
    private var activeCafeId: String? = null

    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    fun startForCafe(cafeId: String) {
        if (activeCafeId == cafeId && listeners.isNotEmpty()) return
        stop()
        activeCafeId = cafeId
        _syncStatus.value = SyncStatus.SYNCING
        Log.i(TAG, "Starting Firestore sync for cafe=$cafeId")

        val cafeRef = firestore.collection("cafes").document(cafeId)

        listeners += cafeRef.collection("staff").addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) {
                Log.w(TAG, "Staff listener error", err)
                _syncStatus.value = SyncStatus.ERROR
                return@addSnapshotListener
            }
            val docs = snap?.documents.orEmpty()
            val fromCache = snap?.metadata?.isFromCache == true
            Log.i(TAG, "Staff snapshot: count=${docs.size}, fromCache=$fromCache")
            scope.launch {
                inboundMutex.withLock {
                    docs.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        val remote = FirestoreSyncMapper.mapToStaff(doc.id, data) ?: return@forEach
                        val local = staffDao.getByRemoteId(remote.remoteId)
                        if (FirestoreSyncMapper.shouldApplyRemote(local?.updatedAt, remote.updatedAt)) {
                            if (local == null) staffDao.insert(remote)
                            else staffDao.update(remote.copy(id = local.id))
                        }
                    }
                    if (!fromCache) {
                        val remoteIds = docs.mapTo(hashSetOf()) { it.id }
                        staffDao.getAllSnapshot()
                            .filter { it.remoteId !in remoteIds }
                            .forEach { pushStaff(it) }
                    }
                }
                _syncStatus.value = if (fromCache) SyncStatus.OFFLINE else SyncStatus.SYNCED
            }
        }

        listeners += cafeRef.collection("menu_items").addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) {
                Log.w(TAG, "Menu listener error", err)
                return@addSnapshotListener
            }
            val docs = snap?.documents.orEmpty()
            val fromCache = snap?.metadata?.isFromCache == true
            scope.launch {
                inboundMutex.withLock {
                    docs.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        val remote = FirestoreSyncMapper.mapToMenuItem(doc.id, data) ?: return@forEach
                        val local = menuItemDao.getByRemoteId(remote.remoteId)
                        if (FirestoreSyncMapper.shouldApplyRemote(local?.updatedAt, remote.updatedAt)) {
                            if (local == null) menuItemDao.insert(remote)
                            else menuItemDao.update(remote.copy(id = local.id))
                        }
                    }
                    if (!fromCache) {
                        val remoteIds = docs.mapTo(hashSetOf()) { it.id }
                        menuItemDao.getAllSnapshot()
                            .filter { it.remoteId !in remoteIds }
                            .forEach { pushMenuItem(it) }
                    }
                }
            }
        }

        listeners += cafeRef.collection("inventory_items").addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) {
                Log.w(TAG, "Inventory listener error", err)
                return@addSnapshotListener
            }
            val docs = snap?.documents.orEmpty()
            val fromCache = snap?.metadata?.isFromCache == true
            scope.launch {
                inboundMutex.withLock {
                    docs.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        val remote = FirestoreSyncMapper.mapToInventoryItem(doc.id, data) ?: return@forEach
                        val local = inventoryDao.getByRemoteId(remote.remoteId)
                        if (FirestoreSyncMapper.shouldApplyRemote(local?.updatedAt, remote.updatedAt)) {
                            if (local == null) inventoryDao.insert(remote)
                            else inventoryDao.update(remote.copy(id = local.id))
                        }
                    }
                    if (!fromCache) {
                        val remoteIds = docs.mapTo(hashSetOf()) { it.id }
                        inventoryDao.getAllSnapshot()
                            .filter { it.remoteId !in remoteIds }
                            .forEach { pushInventoryItem(it) }
                    }
                }
            }
        }

        listeners += cafeRef.collection("shifts").addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) {
                Log.w(TAG, "Shifts listener error", err)
                return@addSnapshotListener
            }
            val docs = snap?.documents.orEmpty()
            val fromCache = snap?.metadata?.isFromCache == true
            scope.launch {
                inboundMutex.withLock {
                    docs.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        val remote = FirestoreSyncMapper.mapToShift(doc.id, data) ?: return@forEach
                        val staff = staffDao.getByRemoteId(remote.staffRemoteId) ?: return@forEach
                        val local = shiftDao.getByRemoteId(remote.remoteId)
                        if (FirestoreSyncMapper.shouldApplyRemote(local?.updatedAt, remote.updatedAt)) {
                            val resolvedLeftOpen = remote.leftOpen.mapNotNull { lo ->
                                val order = orderDao.getByRemoteId(lo.orderRemoteId) ?: return@mapNotNull null
                                ShiftLeftOpen(shiftId = 0L, orderId = order.id, totalCentimes = lo.totalCentimes)
                            }
                            shiftDao.upsertRemoteShift(
                                shift = Shift(
                                    staffId = staff.id,
                                    status = remote.status,
                                    startedAt = remote.startedAt,
                                    endedAt = remote.endedAt,
                                    endedByTakeover = remote.endedByTakeover,
                                    settledAt = remote.settledAt,
                                    expectedCentimes = remote.expectedCentimes,
                                    countedCentimes = remote.countedCentimes,
                                    failedOwnerPinAttempts = remote.failedOwnerPinAttempts,
                                    remoteId = remote.remoteId,
                                    updatedAt = remote.updatedAt,
                                ),
                                leftOpen = resolvedLeftOpen,
                            )
                        }
                    }
                    if (!fromCache) {
                        val remoteIds = docs.mapTo(hashSetOf()) { it.id }
                        shiftDao.getAllSnapshot()
                            .filter { it.remoteId !in remoteIds }
                            .forEach { pushShift(it.id) }
                    }
                }
            }
        }

        listeners += cafeRef.collection("orders").addSnapshotListener(MetadataChanges.INCLUDE) { snap, err ->
            if (err != null) {
                Log.w(TAG, "Orders listener error", err)
                return@addSnapshotListener
            }
            val docs = snap?.documents.orEmpty()
            val fromCache = snap?.metadata?.isFromCache == true
            scope.launch {
                inboundMutex.withLock {
                    docs.forEach { doc ->
                        val data = doc.data ?: return@forEach
                        val remote = FirestoreSyncMapper.mapToOrder(doc.id, data) ?: return@forEach
                        val local = orderDao.getByRemoteId(remote.remoteId)
                        if (FirestoreSyncMapper.shouldApplyRemote(local?.updatedAt, remote.updatedAt)) {
                            val openedShiftId = remote.openedShiftRemoteId?.let { shiftDao.getByRemoteId(it)?.id }
                            val paidShiftId = remote.paidShiftRemoteId?.let { shiftDao.getByRemoteId(it)?.id }
                            val cancelledShiftId = remote.cancelledShiftRemoteId?.let { shiftDao.getByRemoteId(it)?.id }
                            orderDao.upsertRemoteOrderWithItems(
                                order = Order(
                                    tableNumber = remote.tableNumber,
                                    status = remote.status,
                                    createdAt = remote.createdAt,
                                    totalCentimes = remote.totalCentimes,
                                    openedShiftId = openedShiftId,
                                    paidShiftId = paidShiftId,
                                    paidAt = remote.paidAt,
                                    cancelledAt = remote.cancelledAt,
                                    cancelledShiftId = cancelledShiftId,
                                    remoteId = remote.remoteId,
                                    updatedAt = remote.updatedAt,
                                ),
                                items = remote.items,
                            )
                        }
                    }
                    if (!fromCache) {
                        val remoteIds = docs.mapTo(hashSetOf()) { it.id }
                        orderDao.getAllSnapshot()
                            .filter { it.remoteId !in remoteIds }
                            .forEach { pushOrder(it.id) }
                    }
                }
            }
        }
    }

    fun stop() {
        listeners.forEach { it.remove() }
        listeners.clear()
        activeCafeId = null
        _syncStatus.value = SyncStatus.IDLE
    }

    /** Uploads all existing local Room rows to the active café (used when creating a new café). */
    suspend fun pushAllLocalData() {
        val cafeId = activeCafeId ?: return
        val cafeRef = firestore.collection("cafes").document(cafeId)
        val batch = firestore.batch()
        var opCount = 0

        val staffList = staffDao.getAllSnapshot()
        val staffById = staffList.associateBy { it.id }
        staffList.forEach { s ->
            batch.set(cafeRef.collection("staff").document(s.remoteId), FirestoreSyncMapper.staffToMap(s))
            opCount++
        }

        menuItemDao.getAllSnapshot().forEach { m ->
            batch.set(cafeRef.collection("menu_items").document(m.remoteId), FirestoreSyncMapper.menuItemToMap(m))
            opCount++
        }

        inventoryDao.getAllSnapshot().forEach { inv ->
            batch.set(cafeRef.collection("inventory_items").document(inv.remoteId), FirestoreSyncMapper.inventoryItemToMap(inv))
            opCount++
        }

        val shifts = shiftDao.getAllSnapshot()
        val shiftById = shifts.associateBy { it.id }
        val orders = orderDao.getAllSnapshot()
        val orderById = orders.associateBy { it.id }

        shifts.forEach { shift ->
            val staffRemoteId = staffById[shift.staffId]?.remoteId ?: return@forEach
            val leftOpen = shiftDao.getLeftOpenSnapshot(shift.id).mapNotNull { lo ->
                val orderRemoteId = orderById[lo.orderId]?.remoteId ?: return@mapNotNull null
                RemoteShiftLeftOpen(orderRemoteId, lo.totalCentimes)
            }
            batch.set(
                cafeRef.collection("shifts").document(shift.remoteId),
                FirestoreSyncMapper.shiftToMap(shift, staffRemoteId, leftOpen),
            )
            opCount++
        }

        orders.forEach { order ->
            val items = orderDao.getItemsSnapshot(order.id)
            batch.set(
                cafeRef.collection("orders").document(order.remoteId),
                FirestoreSyncMapper.orderToMap(
                    order = order,
                    items = items,
                    openedShiftRemoteId = order.openedShiftId?.let { shiftById[it]?.remoteId },
                    paidShiftRemoteId = order.paidShiftId?.let { shiftById[it]?.remoteId },
                    cancelledShiftRemoteId = order.cancelledShiftId?.let { shiftById[it]?.remoteId },
                ),
            )
            opCount++
        }

        if (opCount > 0) {
            batch.commit().await()
        }
    }

    override fun pushMenuItem(item: MenuItem) {
        val cafeId = activeCafeId ?: return
        scope.launch {
            runCatching {
                firestore.collection("cafes").document(cafeId)
                    .collection("menu_items").document(item.remoteId)
                    .set(FirestoreSyncMapper.menuItemToMap(item))
                    .await()
            }.onFailure { Log.w(TAG, "Failed to push menu item ${item.remoteId}", it) }
        }
    }

    override fun pushInventoryItem(item: InventoryItem) {
        val cafeId = activeCafeId ?: return
        scope.launch {
            runCatching {
                firestore.collection("cafes").document(cafeId)
                    .collection("inventory_items").document(item.remoteId)
                    .set(FirestoreSyncMapper.inventoryItemToMap(item))
                    .await()
            }.onFailure { Log.w(TAG, "Failed to push inventory item ${item.remoteId}", it) }
        }
    }

    override fun pushStaff(staff: Staff) {
        val cafeId = activeCafeId ?: return
        scope.launch {
            runCatching {
                firestore.collection("cafes").document(cafeId)
                    .collection("staff").document(staff.remoteId)
                    .set(FirestoreSyncMapper.staffToMap(staff))
                    .await()
                Log.i(TAG, "Pushed staff ${staff.name} (${staff.remoteId})")
            }.onFailure { Log.w(TAG, "Failed to push staff ${staff.remoteId}", it) }
        }
    }

    override fun pushShift(shiftId: Long) {
        val cafeId = activeCafeId ?: return
        scope.launch {
            runCatching {
                val shift = shiftDao.getById(shiftId) ?: return@launch
                val staff = staffDao.getById(shift.staffId) ?: return@launch
                val leftOpen = shiftDao.getLeftOpenSnapshot(shift.id).mapNotNull { lo ->
                    val order = orderDao.getById(lo.orderId) ?: return@mapNotNull null
                    RemoteShiftLeftOpen(order.remoteId, lo.totalCentimes)
                }
                firestore.collection("cafes").document(cafeId)
                    .collection("shifts").document(shift.remoteId)
                    .set(FirestoreSyncMapper.shiftToMap(shift, staff.remoteId, leftOpen))
                    .await()
                Log.i(TAG, "Pushed shift $shiftId (${shift.remoteId})")
            }.onFailure { Log.w(TAG, "Failed to push shift $shiftId", it) }
        }
    }

    override fun pushOrder(orderId: Long) {
        val cafeId = activeCafeId ?: return
        scope.launch {
            runCatching {
                val order = orderDao.getById(orderId) ?: return@launch
                val items = orderDao.getItemsSnapshot(order.id)
                val openedShiftRemote = order.openedShiftId?.let { shiftDao.getById(it)?.remoteId }
                val paidShiftRemote = order.paidShiftId?.let { shiftDao.getById(it)?.remoteId }
                val cancelledShiftRemote = order.cancelledShiftId?.let { shiftDao.getById(it)?.remoteId }
                firestore.collection("cafes").document(cafeId)
                    .collection("orders").document(order.remoteId)
                    .set(
                        FirestoreSyncMapper.orderToMap(
                            order = order,
                            items = items,
                            openedShiftRemoteId = openedShiftRemote,
                            paidShiftRemoteId = paidShiftRemote,
                            cancelledShiftRemoteId = cancelledShiftRemote,
                        )
                    )
                    .await()
            }.onFailure { Log.w(TAG, "Failed to push order $orderId", it) }
        }
    }

    companion object {
        private const val TAG = "FirestoreSyncEngine"
    }
}
