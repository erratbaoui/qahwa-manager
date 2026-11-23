package com.example.qahwamanager.ui.shift

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qahwamanager.data.db.EmptySpendingDao
import com.example.qahwamanager.data.db.ShiftAlreadyActiveException
import com.example.qahwamanager.data.model.Order
import com.example.qahwamanager.data.model.ShiftWithStaff
import com.example.qahwamanager.data.model.Spending
import com.example.qahwamanager.data.model.Staff
import com.example.qahwamanager.data.repository.OrderRepository
import com.example.qahwamanager.data.repository.ShiftRepository
import com.example.qahwamanager.data.repository.SpendingRepository
import com.example.qahwamanager.data.repository.StaffRepository
import com.example.qahwamanager.data.time.SystemClock
import com.example.qahwamanager.security.OwnerSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The waiter's side of shifts: start, take over, collect from open tables, record spendings, end. */
@HiltViewModel
class ShiftViewModel @Inject constructor(
    private val shiftRepository: ShiftRepository,
    staffRepository: StaffRepository,
    private val orderRepository: OrderRepository,
    ownerSession: OwnerSession,
    private val spendingRepository: SpendingRepository = SpendingRepository(EmptySpendingDao, SystemClock),
) : ViewModel() {

    val activeShift: StateFlow<ShiftWithStaff?> = shiftRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val activeStaff: StateFlow<List<Staff>> = staffRepository.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val openOrders: StateFlow<List<Order>> = orderRepository.getOpenOrders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Cash collected in the running shift; 0 when none is running. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val collectedCentimes: StateFlow<Long> = shiftRepository.observeActive()
        .flatMapLatest { shift -> if (shift == null) flowOf(0L) else shiftRepository.observeCollectedCentimes(shift.shift.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /** Spendings recorded during the running shift. */
    @OptIn(ExperimentalCoroutinesApi::class)
    val shiftSpendings: StateFlow<List<Spending>> = shiftRepository.observeActive()
        .flatMapLatest { shift -> if (shift == null) flowOf(emptyList()) else spendingRepository.observeForShift(shift.shift.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val shiftSpendingsTotalCentimes: StateFlow<Long> = shiftRepository.observeActive()
        .flatMapLatest { shift -> if (shift == null) flowOf(0L) else spendingRepository.observeTotalForShift(shift.shift.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val isOwnerMode: StateFlow<Boolean> = ownerSession.isOwnerMode

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    fun start(staffId: Long) = save {
        try {
            shiftRepository.start(staffId)
        } catch (e: ShiftAlreadyActiveException) {
            // Someone else's tap won; the card now shows their shift.
        }
    }

    fun takeOver(staffId: Long) = save { shiftRepository.takeOver(staffId) }

    /** Records a spending paid out during the active shift. */
    fun addSpending(title: String, amountCentimes: Long, onDone: () -> Unit = {}) = save {
        val shiftId = shiftRepository.activeShiftId() ?: return@save
        spendingRepository.addSpending(
            title = title,
            amountCentimes = amountCentimes,
            shiftId = shiftId,
        )
        onDone()
    }

    /** Ends the running shift (saving optional [countedCentimes] handed over), then calls [onDone]. */
    fun endShift(countedCentimes: Long? = null, onDone: () -> Unit) = save {
        shiftRepository.endActive(countedCentimes)
        onDone()
    }

    /** Marks an open order paid in the running shift. Does nothing without a shift. */
    fun markPaid(orderId: Long) = save {
        val shiftId = shiftRepository.activeShiftId() ?: return@save
        orderRepository.markPaid(orderId, shiftId)
    }

    /** Runs one write at a time; taps that arrive while a write is in flight are ignored. */
    private fun save(block: suspend () -> Unit) {
        if (_isSaving.value) return
        _isSaving.value = true
        viewModelScope.launch {
            try {
                block()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
