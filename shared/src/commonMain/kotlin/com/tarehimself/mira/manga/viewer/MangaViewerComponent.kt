package com.tarehimself.mira.manga.viewer

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.tarehimself.mira.RootComponent
import com.tarehimself.mira.data.ApiMangaChapter
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaChapter
import com.tarehimself.mira.data.MangaData
import com.tarehimself.mira.data.MangaPreview
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.StoredManga
import io.realm.kotlin.ext.copyFromRealm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface MangaViewerComponent : KoinComponent {
    val state: MutableValue<State>

    val root: RootComponent

    val api: MangaApi

    val realmDatabase: RealmRepository
    suspend fun loadMangaData(isRefresh: Boolean = false)

    suspend fun loadChapters(isRefresh: Boolean = false)

    suspend fun bookmark()

    suspend fun unBookmark()


    fun readChapter(index: Int)

    fun searchForTag(tag: String)

    data class State(
        var sourceId: String,
        var preview: MangaPreview,
        var data: MangaData?,
        var chapters: List<MangaChapter>,
        var isLoadingData: Boolean,
        var isLoadingChapters: Boolean,
    )


}

class DefaultMangaViewerComponent(
    componentContext: ComponentContext,
    override val root: RootComponent, sourceId: String, preview: MangaPreview
) : MangaViewerComponent, ComponentContext by componentContext {


    override val realmDatabase: RealmRepository by inject()

    override val api: MangaApi by inject()

    override val state: MutableValue<MangaViewerComponent.State> = MutableValue(
        MangaViewerComponent.State(
            sourceId = sourceId,
            preview = preview,
            data = null,
            chapters = listOf(),
            isLoadingData = false,
            isLoadingChapters = false,
        )
    )

//    init {
//        lifecycle.subscribe(object : Lifecycle.Callbacks {
//
//            override fun onCreate() {
//                super.onCreate()
//            }
//
//            override fun onDestroy() {
//                super.onDestroy()
//            }
//        })
//    }

    override suspend fun loadMangaData(isRefresh: Boolean) {

        withContext(Dispatchers.IO){
            state.update {
                it.isLoadingData = true
                it
            }


            var loadedData: MangaData?
            val isBookmarked = realmDatabase.has(state.value.sourceId, state.value.preview.id)
            val usedStored = !isRefresh && isBookmarked
            loadedData = if (usedStored) {
                realmDatabase.getBookmark(
                    realmDatabase.getBookmarkKey(
                        state.value.sourceId,
                        state.value.preview.id
                    )
                ).asFlow().firstOrNull()?.obj
            } else {
                api.getManga(source = state.value.sourceId, mangaId = state.value.preview.id).data
            }

            if (loadedData?.let { usedStored && it.status.isEmpty() } != false) {
                loadedData =
                    api.getManga(source = state.value.sourceId, mangaId = state.value.preview.id).data
            }

            if (loadedData != null) {
                if (isBookmarked && loadedData !is StoredManga) {
                    realmDatabase.updateBookmark(state.value.sourceId, state.value.preview.id) {
                        it.updateFromApiData(loadedData)

                        state.update { update ->
                            update.data = it.copyFromRealm()
                            update.isLoadingData = false
                            update
                        }
                    }
                } else {
                    state.update {
                        it.data = loadedData
                        it.isLoadingData = false
                        it
                    }
                }
            } else {
                state.update {
                    it.isLoadingData = false
                    it
                }
            }
        }
    }

    override suspend fun loadChapters(isRefresh: Boolean) {
        withContext(Dispatchers.IO){
            state.update {
                it.isLoadingChapters = true
                it
            }

            var loadedData: List<MangaChapter>?
            val isBookmarked = realmDatabase.has(state.value.sourceId, state.value.preview.id)
            val usedStored = !isRefresh && isBookmarked

            loadedData = if (usedStored) {
                realmDatabase.getBookmark(
                    realmDatabase.getBookmarkKey(
                        state.value.sourceId,
                        state.value.preview.id
                    )
                ).asFlow().first().obj?.chapters
            } else {
                api.getChapters(source = state.value.sourceId, mangaId = state.value.preview.id).data
            }

            if (usedStored && loadedData.isNullOrEmpty()) {
                loadedData = api.getChapters(
                    source = state.value.sourceId,
                    mangaId = state.value.preview.id
                ).data?.let { it.distinctBy { chap -> chap.id } }
            }

            if (loadedData != null) {
                if (isBookmarked && (loadedData.isEmpty() || loadedData.first() is ApiMangaChapter)) {
                    realmDatabase.updateChapters(
                        state.value.sourceId,
                        state.value.preview.id,
                        loadedData
                    )
                }

                state.update {
                    it.chapters = loadedData
                    it.isLoadingChapters = false
                    it
                }
            } else {
                state.update {
                    it.isLoadingChapters = false
                    it
                }
            }
        }
    }

    override fun readChapter(index: Int) {
        root.navigateToMangaReader(
            state.value.sourceId, state.value.preview.id, index,
            when (realmDatabase.has(state.value.sourceId, state.value.preview.id)) {
                true -> listOf() // will be fetched in the reader
                else -> if (state.value.chapters.first() is ApiMangaChapter) {
                    state.value.chapters as List<ApiMangaChapter>
                } else {
                    listOf()
                }
            }
        )
    }

    override fun searchForTag(tag: String) {
        root.navigateToSearch(state.value.sourceId, tag)
    }

    override suspend fun bookmark() {

        state.value.data?.let {
            realmDatabase.bookmark(state.value.sourceId, it, state.value.chapters)
        } ?: realmDatabase.bookmark(state.value.sourceId, state.value.preview)

    }

    override suspend fun unBookmark() {
        realmDatabase.removeBookmark(state.value.sourceId, state.value.preview.id)
    }
}