package com.example.qahwamanager.data.cloud

import android.content.Context
import com.example.qahwamanager.data.db.QahwaDatabase
import com.example.qahwamanager.data.time.Clock
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class CafeWorkspace(
    val cafeId: String,
    val name: String,
)

data class CafeInvite(
    val code: String,
    val cafeId: String,
    val cafeName: String,
    val expiresAt: Long,
)

@Singleton
class CafeWorkspaceRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val firestore: FirebaseFirestore,
    private val authRepository: CloudAuthRepository,
    private val database: QahwaDatabase,
    private val syncEngine: FirestoreSyncEngine,
    private val clock: Clock,
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _activeCafe = MutableStateFlow(loadSavedWorkspace())
    val activeCafe: StateFlow<CafeWorkspace?> = _activeCafe.asStateFlow()

    private fun loadSavedWorkspace(): CafeWorkspace? {
        val id = prefs.getString(KEY_CAFE_ID, null) ?: return null
        val name = prefs.getString(KEY_CAFE_NAME, null) ?: return null
        return CafeWorkspace(id, name)
    }

    private fun saveWorkspace(workspace: CafeWorkspace?) {
        prefs.edit().apply {
            if (workspace == null) {
                remove(KEY_CAFE_ID)
                remove(KEY_CAFE_NAME)
            } else {
                putString(KEY_CAFE_ID, workspace.cafeId)
                putString(KEY_CAFE_NAME, workspace.name)
            }
        }.apply()
        _activeCafe.value = workspace
    }

    /** Looks up the signed-in user's linked café from `/users/{uid}` and caches it locally. */
    suspend fun refreshActiveCafeFromCloud(): CafeWorkspace? {
        val user = authRepository.currentUser() ?: return null
        val userDoc = firestore.collection("users").document(user.uid).get().await()
        val cafeId = userDoc.getString("cafeId") ?: return null
        val cafeDoc = firestore.collection("cafes").document(cafeId).get().await()
        val cafeName = cafeDoc.getString("name") ?: return null
        val workspace = CafeWorkspace(cafeId = cafeId, name = cafeName)
        saveWorkspace(workspace)
        syncEngine.startForCafe(cafeId)
        return workspace
    }

    /** Creates a new coffee shop tenant, links the current owner, and uploads any existing local rows. */
    suspend fun createCafe(cafeName: String): CafeWorkspace {
        val trimmed = cafeName.trim()
        require(trimmed.isNotEmpty()) { "Café name is blank" }
        val user = checkNotNull(authRepository.currentUser()) { "Not signed in" }
        val now = clock.nowMillis()
        val cafeId = UUID.randomUUID().toString()

        val batch = firestore.batch()
        val cafeRef = firestore.collection("cafes").document(cafeId)
        batch.set(
            cafeRef,
            mapOf(
                "name" to trimmed,
                "createdBy" to user.uid,
                "createdAt" to now,
            ),
        )
        batch.set(
            cafeRef.collection("members").document(user.uid),
            mapOf(
                "email" to user.email,
                "displayName" to user.displayName,
                "role" to "owner",
                "joinedAt" to now,
            ),
        )
        batch.set(
            firestore.collection("users").document(user.uid),
            mapOf(
                "email" to user.email,
                "displayName" to user.displayName,
                "cafeId" to cafeId,
                "createdAt" to now,
                "updatedAt" to now,
            ),
        )
        batch.commit().await()

        val workspace = CafeWorkspace(cafeId = cafeId, name = trimmed)
        saveWorkspace(workspace)
        syncEngine.startForCafe(cafeId)
        syncEngine.pushAllLocalData()
        return workspace
    }

    /** Generates a 24-hour single-use invite code so a co-owner can join the same coffee shop. */
    suspend fun generateInviteCode(): CafeInvite {
        val user = checkNotNull(authRepository.currentUser()) { "Not signed in" }
        val workspace = checkNotNull(_activeCafe.value) { "No active café" }
        val now = clock.nowMillis()
        val expiresAt = now + INVITE_TTL_MS
        val code = randomInviteCode()

        firestore.collection("cafe_invites").document(code).set(
            mapOf(
                "cafeId" to workspace.cafeId,
                "cafeName" to workspace.name,
                "createdBy" to user.uid,
                "createdAt" to now,
                "expiresAt" to expiresAt,
                "usedBy" to null,
            ),
        ).await()

        return CafeInvite(
            code = code,
            cafeId = workspace.cafeId,
            cafeName = workspace.name,
            expiresAt = expiresAt,
        )
    }

    /** Joins an existing coffee shop using a co-owner's invite code and replaces local seed data. */
    suspend fun joinCafeByCode(rawCode: String): CafeWorkspace {
        val user = checkNotNull(authRepository.currentUser()) { "Not signed in" }
        val code = rawCode.trim().uppercase().replace("-", "").replace(" ", "")
        require(code.length >= 8) { "Invalid invite code" }

        val inviteRef = firestore.collection("cafe_invites").document(code)
        val inviteSnap = inviteRef.get().await()
        if (!inviteSnap.exists()) throw IllegalArgumentException("Invite code not found")
        if (inviteSnap.getString("usedBy") != null) throw IllegalStateException("Invite code already used")

        val expiresAt = inviteSnap.getLong("expiresAt") ?: 0L
        val now = clock.nowMillis()
        if (expiresAt <= now) throw IllegalStateException("Invite code has expired")

        val cafeId = checkNotNull(inviteSnap.getString("cafeId")) { "Invite missing cafeId" }
        val cafeName = inviteSnap.getString("cafeName") ?: "Café"

        val batch = firestore.batch()
        batch.set(
            firestore.collection("cafes").document(cafeId).collection("members").document(user.uid),
            mapOf(
                "email" to user.email,
                "displayName" to user.displayName,
                "role" to "co_owner",
                "inviteCode" to code,
                "joinedAt" to now,
            ),
        )
        batch.update(
            inviteRef,
            mapOf(
                "usedBy" to user.uid,
                "usedAt" to now,
            ),
        )
        batch.set(
            firestore.collection("users").document(user.uid),
            mapOf(
                "email" to user.email,
                "displayName" to user.displayName,
                "cafeId" to cafeId,
                "createdAt" to now,
                "updatedAt" to now,
            ),
        )
        batch.commit().await()

        // Clear local seed/previous data before starting real-time sync for the joined café.
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }

        val workspace = CafeWorkspace(cafeId = cafeId, name = cafeName)
        saveWorkspace(workspace)
        syncEngine.startForCafe(cafeId)
        return workspace
    }

    /** Signs out of Firebase Auth, stops sync listeners, and wipes local Room data for tenant isolation. */
    suspend fun signOutAndClearLocal() {
        syncEngine.stop()
        withContext(Dispatchers.IO) {
            database.clearAllTables()
        }
        saveWorkspace(null)
        authRepository.signOut()
    }

    private fun randomInviteCode(): String {
        val random = SecureRandom()
        return buildString(8) {
            repeat(8) { append(INVITE_ALPHABET[random.nextInt(INVITE_ALPHABET.length)]) }
        }
    }

    companion object {
        private const val PREFS_NAME = "qahwa_cloud_workspace"
        private const val KEY_CAFE_ID = "cafe_id"
        private const val KEY_CAFE_NAME = "cafe_name"
        private const val INVITE_TTL_MS = 24L * 60L * 60L * 1000L
        private const val INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    }
}
