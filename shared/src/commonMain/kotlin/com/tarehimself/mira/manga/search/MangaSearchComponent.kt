package com.tarehimself.mira.screens.sources

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.consume
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.RealmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface MangaSearchComponent : KoinComponent {
    val state: MutableValue<State>

    val context: ComponentContext

    val api: MangaApi
    suspend fun loadInitialData()

    fun search(query: String, onSearchCompleted: (suspend () -> Unit)? = null)

    suspend fun tryLoadMoreData()

    fun onNewItemsLoaded(items: List<ApiMangaPreview>, replace: Boolean = false)

    @Parcelize
    data class State(
        var sourceId: String = "",
        var query: String = "",
        var items: LinkedHashSet<ApiMangaPreview> = LinkedHashSet(),
        var latestNext: String? = null,
        var hasLoadedMoreData: Boolean = false,
        var isLoadingData: Boolean = false,
        var wasViewingManga: Boolean = false
    ) : Parcelable

    val onItemSelected: (manga: ApiMangaPreview) -> Unit

    val realmRepository: RealmRepository
}

class DefaultMangaSearchComponent(
    componentContext: ComponentContext,
    sourceId: String,
    initialQuery: String,
    stateKey: String,
    onMangaSelected: (manga: ApiMangaPreview) -> Unit
) : MangaSearchComponent, ComponentContext by componentContext {
    override val state: MutableValue<MangaSearchComponent.State> = MutableValue(
        stateKeeper.consume(key = "SEARCH_COMP_STATE_${stateKey}") ?: MangaSearchComponent.State(
            sourceId = sourceId,
            query = initialQuery
        )
    )

    override val context: ComponentContext = componentContext

    override val api: MangaApi by inject()

    override val realmRepository: RealmRepository by inject()
    override fun search(query: String, onSearchCompleted: (suspend () -> Unit)?) {

        if (state.value.query == query && !state.value.hasLoadedMoreData) {
            return
        }

        state.update {
            it.query = query
            it.isLoadingData = true
            it
        }

        val scope = CoroutineScope(Dispatchers.Default)

        scope.launch {
            val response = api.search(query = query, source = state.value.sourceId)
            val data = response.data
            if (data != null) {
                state.update {
                    it.items = LinkedHashSet(data.items)
                    it.latestNext = data.next
                    it.hasLoadedMoreData = false
                    it
                }
            } else {
                state.update {
                    it.items = LinkedHashSet()
                    it.latestNext = null
                    it.hasLoadedMoreData = false
                    it
                }
            }
            state.update {
                it.isLoadingData = false
                it
            }
            if (onSearchCompleted != null) {
                onSearchCompleted()
            }
        }

    }

    override suspend fun loadInitialData() {
        if (state.value.wasViewingManga) {
            state.update {
                it.wasViewingManga = false
                it
            }
            return
        }

        state.update {
            it.isLoadingData = true
            it
        }

        val response = api.search(query = state.value.query, source = state.value.sourceId)
        val data = response.data
        if (data != null) {
            state.update {
                it.items = LinkedHashSet(data.items)
                it.latestNext = data.next
                it.hasLoadedMoreData = false
                it
            }
        }

        state.update {
            it.isLoadingData = false
            it
        }
    }

    override suspend fun tryLoadMoreData() {
        if (state.value.latestNext != null && !state.value.isLoadingData) {
            state.update {
                it.isLoadingData = true
                it
            }

            val response = api.search(
                query = state.value.query,
                source = state.value.sourceId,
                next = state.value.latestNext!!
            )
            val data = response.data
            if (data != null) {
                state.update {
                    it.items.addAll(data.items)
                    it.isLoadingData = false
                    it.latestNext = data.next
                    it.hasLoadedMoreData = true
                    it
                }
            }
        }
    }

    init {
        stateKeeper.register(key = "SEARCH_COMP_STATE_${stateKey}") { state.value }
    }

    override val onItemSelected: (manga: ApiMangaPreview) -> Unit = {
        state.update { ref ->
            ref.wasViewingManga = true
            ref
        }

        onMangaSelected(it)
    }

    override fun onNewItemsLoaded(items: List<ApiMangaPreview>, replace: Boolean) {

        state.update {
            if (replace) {
                it.items.clear()
            }

            it.items.addAll(items)

            it
        }
    }
}