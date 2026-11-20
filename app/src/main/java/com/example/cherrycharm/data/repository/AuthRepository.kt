package com.example.cherrycharm.data.repository

import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.resourceOf
import com.example.cherrycharm.di.ApplicationScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationScope private val scope: CoroutineScope,
) {
    val currentUser: FirebaseUser? get() = auth.currentUser

    /** The signed-in user's uid, or null. Every user-scoped repository keys off this. */
    val userId: StateFlow<String?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.stateIn(scope, SharingStarted.Eagerly, auth.currentUser?.uid)

    val isLoggedIn: StateFlow<Boolean> =
        userId.map { it != null }.stateIn(scope, SharingStarted.Eagerly, auth.currentUser != null)

    fun requireUid(): String = auth.currentUser?.uid ?: error("Please sign in to continue.")

    suspend fun login(email: String, password: String): Resource<Unit> = resourceOf("Login failed") {
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        syncFcmToken()
    }

    suspend fun register(name: String, email: String, phone: String, password: String): Resource<Unit> =
        resourceOf("Registration failed") {
            val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user ?: error("Registration failed")
            user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build()).await()
            firestore.collection(Constants.USERS).document(user.uid).set(
                mapOf(
                    "id" to user.uid,
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "phone" to phone.trim(),
                    "profileImage" to "",
                    "wishlist" to emptyList<String>(),
                    "createdAt" to System.currentTimeMillis(),
                ),
                SetOptions.merge(),
            ).await()
            syncFcmToken()
        }

    suspend fun resetPassword(email: String): Resource<Unit> = resourceOf("Couldn't send reset email") {
        auth.sendPasswordResetEmail(email.trim()).await()
        Unit
    }

    suspend fun changePassword(current: String, new: String): Resource<Unit> = resourceOf("Couldn't change password") {
        val user = auth.currentUser ?: error("Not signed in")
        user.reauthenticate(EmailAuthProvider.getCredential(user.email.orEmpty(), current)).await()
        user.updatePassword(new).await()
        Unit
    }

    suspend fun deleteAccount(password: String): Resource<Unit> = resourceOf("Couldn't delete account") {
        val user = auth.currentUser ?: error("Not signed in")
        user.reauthenticate(EmailAuthProvider.getCredential(user.email.orEmpty(), password)).await()
        firestore.collection(Constants.USERS).document(user.uid).delete().await()
        user.delete().await()
        Unit
    }

    fun logout() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            // Stop pushes to this device for the signed-out account.
            scope.launch {
                runCatching {
                    firestore.collection(Constants.USERS).document(uid).update("fcmToken", "").await()
                }
            }
        }
        auth.signOut()
    }

    fun syncFcmToken() {
        val uid = auth.currentUser?.uid ?: return
        scope.launch {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                firestore.collection(Constants.USERS).document(uid)
                    .set(mapOf("fcmToken" to token), SetOptions.merge()).await()
            }
        }
    }
}
