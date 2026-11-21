package com.example.qahwamanager.data.cloud

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Abstraction over cloud sign-in, active café workspace, and real-time sync lifecycle
 * so [com.example.qahwamanager.ui.root.RootViewModel] stays cleanly unit-testable.
 */
interface CloudSessionController {
    val activeCafe: StateFlow<CafeWorkspace?>
    fun observeCurrentUser(): Flow<CloudUser?>
    fun currentUser(): CloudUser?
    suspend fun refreshActiveCafeFromCloud(): CafeWorkspace?
    fun startSyncForCafe(cafeId: String)
    fun stopSync()
}

@Singleton
class FirebaseCloudSessionController @Inject constructor(
    private val authRepository: CloudAuthRepository,
    private val workspaceRepository: CafeWorkspaceRepository,
    private val syncEngine: FirestoreSyncEngine,
) : CloudSessionController {
    override val activeCafe: StateFlow<CafeWorkspace?> = workspaceRepository.activeCafe
    override fun observeCurrentUser(): Flow<CloudUser?> = authRepository.observeCurrentUser()
    override fun currentUser(): CloudUser? = authRepository.currentUser()
    override suspend fun refreshActiveCafeFromCloud(): CafeWorkspace? =
        workspaceRepository.refreshActiveCafeFromCloud()
    override fun startSyncForCafe(cafeId: String) = syncEngine.startForCafe(cafeId)
    override fun stopSync() = syncEngine.stop()
}

/** Used as a default in offline unit tests where cloud auth & café workspace are already satisfied. */
object AlwaysReadyCloudSession : CloudSessionController {
    private val readyUser = CloudUser(uid = "test-uid", email = "owner@qahwa.test", displayName = "Owner")
    override val activeCafe: StateFlow<CafeWorkspace?> =
        MutableStateFlow(CafeWorkspace(cafeId = "test-cafe", name = "Test Café"))
    override fun observeCurrentUser(): Flow<CloudUser?> = flowOf(readyUser)
    override fun currentUser(): CloudUser = readyUser
    override suspend fun refreshActiveCafeFromCloud(): CafeWorkspace? = activeCafe.value
    override fun startSyncForCafe(cafeId: String) = Unit
    override fun stopSync() = Unit
}
