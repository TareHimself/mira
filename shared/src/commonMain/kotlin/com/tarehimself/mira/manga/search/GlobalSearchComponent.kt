package com.tarehimself.mira.manga.search

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.consume
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaSource
import com.tarehimself.mira.data.RealmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface GlobalSearchComponent : KoinComponent {
    val state: MutableValue<State>
    val mangaApi: MangaApi
    @Parcelize
    data class State(
        var sourcesToSearch: List<MangaSource>,
        var searchTerm: String,
        var searchResults: ArrayList<List<ApiMangaPreview>>,
        var isSearching: ArrayList<Boolean>
    ) : Parcelable

    val onItemSelected: (sourceId: String, manga: ApiMangaPreview) -> Unit

    suspend fun updateSearchTerm(term: String)

    fun setSearchResults(idx: Int, results: List<ApiMangaPreview>)
}

class DefaultGlobalSearchComponent(
    componentContext: ComponentContext,
    initialQuery: String,
    sourcesToSearch: List<MangaSource>,
    onMangaSelected: (sourceId: String, manga: ApiMangaPreview) -> Unit
) : GlobalSearchComponent, ComponentContext by componentContext {
    override val state: MutableValue<GlobalSearchComponent.State> = MutableValue(
        stateKeeper.consume(key = "SEARCH_COMP_STATE_${initialQuery}_${sourcesToSearch.hashCode()}")
            ?: GlobalSearchComponent.State(
                sourcesToSearch = sourcesToSearch,
                searchTerm = initialQuery,
                searchResults = ArrayList(List(sourcesToSearch.size) { listOf() }),
                isSearching = ArrayList(List(sourcesToSearch.size){ false })
            )
    )

    override val mangaApi: MangaApi by inject()

    override val onItemSelected: (sourceId: String, manga: ApiMangaPreview) -> Unit =
        onMangaSelected

    override suspend fun updateSearchTerm(term: String) {
        withContext(Dispatchers.IO){
            state.update {
                it.searchTerm = term
                it.searchResults = ArrayList(List(it.sourcesToSearch.size) { listOf() })
                it.isSearching = ArrayList(List(it.sourcesToSearch.size){ true })
                it
            }

            awaitAll(*state.value.sourcesToSearch.mapIndexed{ idx, source ->
                async {
                    setSearchResults(idx,mangaApi.search(source = source.id, query = state.value.searchTerm).data?.items
                        ?: listOf())
                }
            }.toTypedArray())
        }
    }

    override fun setSearchResults(idx: Int, results: List<ApiMangaPreview>) {
        state.update {
            it.searchResults[idx] = results
            it
        }
    }
}