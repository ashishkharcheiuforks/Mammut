package io.github.koss.paging

import io.github.koss.paging.event.PagingRelay
import io.github.koss.paging.local.LocalDataSource
import io.github.koss.paging.network.LoadingState
import io.github.koss.paging.network.NetworkDataSource
import kotlinx.coroutines.flow.Flow

/**
 * The definition of a paging manager. Exposes domain models, is provided
 * generic methods of handling getting and setting of data.
 */
interface PagingManager<LocalModel, NetworkModel, DomainModel> {

    val data: Flow<List<DomainModel>>

    val loadingState: Flow<LoadingState>

    val pagingRelay: PagingRelay

    val localDataSource: LocalDataSource<LocalModel>

    val networkDataSource: NetworkDataSource<NetworkModel>

    val pagingMapper: PagingMapper<LocalModel, NetworkModel, DomainModel>

    /**
     * Simple lifecycle method for activating streaming and relay subscriptions
     */
    fun activate()

    /**
     * Simple lifecycle method for deactivating streaming and relay subscriptions
     */
    fun deactivate()

    /**
     * Function for reloading data backing this manager. This invalidates local storage and
     * reloads the latest data from network
     */
    fun reload()
}