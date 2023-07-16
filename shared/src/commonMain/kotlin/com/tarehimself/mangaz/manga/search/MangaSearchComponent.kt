package com.tarehimself.mangaz.screens.sources

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.consume
import com.tarehimself.mangaz.common.debug
import com.tarehimself.mangaz.data.MangaApi
import com.tarehimself.mangaz.data.MangaPreview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


interface MangaSearchComponent {
    val state: MutableValue<State>

    suspend fun loadInitialData()

    fun search(query: String,onSearchCompleted: (suspend () -> Unit) ? = null)

    suspend fun tryLoadMoreData()

    fun onNewItemsLoaded(items: List<MangaPreview>,replace: Boolean = false)

    @Parcelize
    data class State(
        var sourceId: String = "",
        var query: String = "",
        var items: ArrayList<MangaPreview> = ArrayList(),
        var latestNext: String? = null,
        var hasLoadedMoreData: Boolean = false,
        var isLoadingData: Boolean = false,
        var wasViewingManga: Boolean = false
    ): Parcelable

    val onItemSelected: (manga: MangaPreview) -> Unit

}
class DefaultMangaSearchComponent(componentContext: ComponentContext,sourceId: String, onMangaSelected: (manga: MangaPreview) -> Unit) : MangaSearchComponent,ComponentContext by componentContext {
    override val state: MutableValue<MangaSearchComponent.State> = MutableValue(
        stateKeeper.consume(key = "SEARCH_COMP_STATE") ?: MangaSearchComponent.State(sourceId = sourceId)
    )


    override fun search(query: String,onSearchCompleted: (suspend () -> Unit) ?) {

        if(state.value.query == query && !state.value.hasLoadedMoreData){
            return
        }

        state.update {
            it.query = query
            it
        }

        val scope = CoroutineScope(Dispatchers.Default)

        scope.launch {
            val response = MangaApi.get().search(query = query, source = state.value.sourceId)
            val data = response.data
            if(data != null){
                debug("Fetched Initial Data")
                state.update {
                    it.items = ArrayList(data.items)
                    it.latestNext = data.next
                    it.hasLoadedMoreData = false
                    it
                }
            }
            if(onSearchCompleted != null){
                onSearchCompleted()
            }
        }

    }

    override suspend fun loadInitialData() {
        if(state.value.wasViewingManga){
            state.update {
                it.wasViewingManga = false
                it
            }
            return
        }
        val response = MangaApi.get().search(query = "", source = state.value.sourceId)
        val data = response.data
        if(data != null){
            debug("Fetched Initial Data")
            state.update {
                it.items = ArrayList(data.items)
                it.latestNext = data.next
                it.hasLoadedMoreData = false
                it
            }
        }
    }

    override suspend fun tryLoadMoreData() {
        debug("Loading more data")
        debug(when {
            state.value.latestNext == null -> "No next page"
            else -> state.value.latestNext!!
        })
        debug("Loading data state: ${state.value.isLoadingData}")
        if(state.value.latestNext != null && !state.value.isLoadingData){
            state.update {
                it.isLoadingData = true
                it
            }

            val response = MangaApi.get().search(query = state.value.query,source = state.value.sourceId, next = state.value.latestNext!!)
            val data = response.data
            if(data != null){
                state.update {
                    it.items.addAll(data.items)
                    it.isLoadingData =false
                    it.latestNext = data.next
                    it.hasLoadedMoreData = true
                    it
                }
            }
        }
    }

    init {
        stateKeeper.register(key = "SAVED_STATE") { state.value }
    }

    override val onItemSelected: (manga: MangaPreview) -> Unit = {
        state.update { ref ->
            ref.wasViewingManga = true
            ref
        }

        onMangaSelected(it)
    }

    override fun onNewItemsLoaded(items: List<MangaPreview>,replace: Boolean) {

        state.update {
            if(replace){
                it.items.clear()
            }

            it.items.addAll(items)

            it
        }
    }
}