package com.example.qahwamanager.data.cloud

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class CloudUser(
    val uid: String,
    val email: String,
    val displayName: String,
)

@Singleton
class CloudAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
) {
    fun currentUser(): CloudUser? = auth.currentUser?.toCloudUser()

    fun observeCurrentUser(): Flow<CloudUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser?.toCloudUser())
        }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser?.toCloudUser())
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun signIn(email: String, password: String): CloudUser {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        val user = checkNotNull(result.user) { "Sign-in returned no user" }
        return user.toCloudUser()
    }

    suspend fun signUp(email: String, password: String, displayName: String): CloudUser {
        val trimmedName = displayName.trim()
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        val user = checkNotNull(result.user) { "Sign-up returned no user" }
        if (trimmedName.isNotEmpty()) {
            user.updateProfile(userProfileChangeRequest { this.displayName = trimmedName }).await()
        }
        return CloudUser(
            uid = user.uid,
            email = user.email.orEmpty(),
            displayName = trimmedName.ifEmpty { user.displayName.orEmpty() },
        )
    }

    fun signOut() {
        auth.signOut()
    }

    private fun FirebaseUser.toCloudUser(): CloudUser = CloudUser(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName.orEmpty(),
    )
}
