package com.example.cherrycharm.data.repository

import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.friendlyMessage
import com.example.cherrycharm.data.model.AppNotification
import com.example.cherrycharm.di.ApplicationScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class NotificationRepository @Inject constructor(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore,
    @ApplicationScope scope: CoroutineScope,
) {
    private fun ref(uid: String) =
        firestore.collection(Constants.USERS).document(uid).collection(Constants.NOTIFICATIONS)

    val notifications: StateFlow<Resource<List<AppNotification>>> = auth.userId.flatMapLatest { uid ->
        if (uid == null) flowOf<Resource<List<AppNotification>>>(Resource.Success(emptyList()))
        else ref(uid).orderBy("createdAt", Query.Direction.DESCENDING).limit(100).snapshotFlow()
            .map<QuerySnapshot, Resource<List<AppNotification>>> { Resource.Success(it.documents.mapNotNull(AppNotification::from)) }
            .onStart { emit(Resource.Loading) }
            .catch { emit(Resource.Error(it.friendlyMessage("Couldn't load notifications"))) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(10_000), Resource.Loading)

    val unreadCount: StateFlow<Int> = notifications
        .map { r -> (r as? Resource.Success)?.data?.count { !it.isRead } ?: 0 }
        .stateIn(scope, SharingStarted.WhileSubscribed(10_000), 0)

    suspend fun markRead(id: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching { ref(uid).document(id).update("isRead", true).await() }
    }

    suspend fun markAllRead() {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val unread = ref(uid).whereEqualTo("isRead", false).get().await().documents
            if (unread.isEmpty()) return
            val batch = firestore.batch()
            unread.forEach { batch.update(it.reference, "isRead", true) }
            batch.commit().await()
        }
    }

    suspend fun delete(id: String) {
        val uid = auth.currentUser?.uid ?: return
        runCatching { ref(uid).document(id).delete().await() }
    }

    suspend fun clearAll() {
        val uid = auth.currentUser?.uid ?: return
        runCatching {
            val docs = ref(uid).get().await().documents
            if (docs.isEmpty()) return
            val batch = firestore.batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
    }
}
