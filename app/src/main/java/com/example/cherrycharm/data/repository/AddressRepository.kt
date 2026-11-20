package com.example.cherrycharm.data.repository

import com.example.cherrycharm.core.Constants
import com.example.cherrycharm.core.Resource
import com.example.cherrycharm.core.friendlyMessage
import com.example.cherrycharm.core.resourceOf
import com.example.cherrycharm.data.model.Address
import com.example.cherrycharm.di.ApplicationScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
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
class AddressRepository @Inject constructor(
    private val auth: AuthRepository,
    private val firestore: FirebaseFirestore,
    @ApplicationScope scope: CoroutineScope,
) {
    private fun ref(uid: String) =
        firestore.collection(Constants.USERS).document(uid).collection(Constants.ADDRESSES)

    val addresses: StateFlow<Resource<List<Address>>> = auth.userId.flatMapLatest { uid ->
        if (uid == null) flowOf<Resource<List<Address>>>(Resource.Success(emptyList()))
        else ref(uid).snapshotFlow()
            .map<QuerySnapshot, Resource<List<Address>>> { snap ->
                Resource.Success(
                    snap.documents.mapNotNull { Address.from(it) }
                        .sortedWith(compareByDescending<Address> { it.isDefault }.thenBy { it.label })
                )
            }
            .onStart { emit(Resource.Loading) }
            .catch { emit(Resource.Error(it.friendlyMessage("Couldn't load addresses"))) }
    }.stateIn(scope, SharingStarted.WhileSubscribed(10_000), Resource.Loading)

    fun find(id: String): Address? = (addresses.value as? Resource.Success)?.data?.firstOrNull { it.id == id }

    /** Saves (creates or updates). The first address — or one marked default — becomes the only default. */
    suspend fun save(address: Address): Resource<String> = resourceOf("Couldn't save address") {
        val col = ref(auth.requireUid())
        val doc = if (address.id.isBlank()) col.document() else col.document(address.id)
        val existing = col.get().await().documents
        val makeDefault = address.isDefault || existing.none { it.id != doc.id }
        val batch = firestore.batch()
        if (makeDefault) {
            existing.filter { it.id != doc.id && it.getBoolean("isDefault") == true }
                .forEach { batch.update(it.reference, "isDefault", false) }
        }
        batch.set(doc, address.copy(id = doc.id, isDefault = makeDefault).toMap())
        batch.commit().await()
        doc.id
    }

    suspend fun setDefault(id: String): Resource<Unit> = resourceOf("Couldn't update default") {
        val col = ref(auth.requireUid())
        val batch = firestore.batch()
        col.get().await().documents.forEach { batch.update(it.reference, "isDefault", it.id == id) }
        batch.commit().await()
        Unit
    }

    suspend fun delete(id: String): Resource<Unit> = resourceOf("Couldn't delete address") {
        val col = ref(auth.requireUid())
        val wasDefault = find(id)?.isDefault == true
        col.document(id).delete().await()
        if (wasDefault) {
            col.limit(1).get().await().documents.firstOrNull()?.reference?.update("isDefault", true)?.await()
        }
        Unit
    }
}
