package com.example.cherrycharm.data.repository

import android.net.Uri
import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.resourceOf
import com.example.cherrycharm.data.model.UserProfile
import com.example.cherrycharm.di.ApplicationScope
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class UserRepository @Inject constructor(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    @ApplicationScope scope: CoroutineScope,
) {
    private fun userDoc(uid: String) = firestore.collection(Constants.USERS).document(uid)

    val profile: StateFlow<UserProfile?> = auth.userId.flatMapLatest { uid ->
        if (uid == null) flowOf<UserProfile?>(null)
        else userDoc(uid).snapshotFlow()
            .map<DocumentSnapshot, UserProfile?> { doc ->
                UserProfile.from(doc) ?: UserProfile(
                    id = uid,
                    name = auth.currentUser?.displayName.orEmpty(),
                    email = auth.currentUser?.email.orEmpty(),
                )
            }
            .catch { emit(null) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(10_000), null)

    val wishlistIds: StateFlow<Set<String>> = profile
        .map { it?.wishlist?.toSet() ?: emptySet() }
        .stateIn(scope, SharingStarted.WhileSubscribed(10_000), emptySet())

    suspend fun setWishlisted(productId: String, wishlisted: Boolean): Resource<Unit> =
        resourceOf("Couldn't update wishlist") {
            val uid = auth.requireUid()
            val op = if (wishlisted) FieldValue.arrayUnion(productId) else FieldValue.arrayRemove(productId)
            userDoc(uid).set(mapOf("wishlist" to op), SetOptions.merge()).await()
            Unit
        }

    /** Flips the heart; returns true if the product is now saved. */
    suspend fun toggleWishlist(productId: String): Resource<Boolean> {
        val nowSaved = productId !in wishlistIds.value
        return when (val r = setWishlisted(productId, nowSaved)) {
            is Resource.Success -> Resource.Success(nowSaved)
            is Resource.Error -> r
            else -> Resource.Error("Couldn't update wishlist")
        }
    }

    suspend fun updateProfile(name: String, phone: String): Resource<Unit> = resourceOf("Couldn't save profile") {
        val uid = auth.requireUid()
        userDoc(uid).set(
            mapOf("name" to name.trim(), "phone" to phone.trim(), "updatedAt" to System.currentTimeMillis()),
            SetOptions.merge(),
        ).await()
        auth.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name.trim()).build())?.await()
        Unit
    }

    suspend fun uploadAvatar(uri: Uri): Resource<String> = resourceOf("Couldn't upload photo") {
        val uid = auth.requireUid()
        val ref = storage.reference.child("profile_images/$uid.jpg")
        ref.putFile(uri).await()
        val url = ref.downloadUrl.await().toString()
        userDoc(uid).set(mapOf("profileImage" to url), SetOptions.merge()).await()
        auth.currentUser?.updateProfile(UserProfileChangeRequest.Builder().setPhotoUri(Uri.parse(url)).build())?.await()
        url
    }
}
