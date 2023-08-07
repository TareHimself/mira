package com.tarehimself.mira.manga.viewer

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.tarehimself.mira.RootComponent
import com.tarehimself.mira.data.ApiMangaChapter
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaChapter
import com.tarehimself.mira.data.MangaData
import com.tarehimself.mira.data.MangaPreview
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.StoredManga
import com.tarehimself.mira.data.StoredMangaExtras
import io.realm.kotlin.ext.copyFromRealm
import io.realm.kotlin.ext.toRealmList
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface MangaViewerComponent : KoinComponent {
    val state: MutableValue<State>

    val root: RootComponent

    val api: MangaApi

    val realmDatabase: RealmRepository
    suspend fun loadMangaData()

    suspend fun loadChapters()

    suspend fun bookmark()

    suspend fun unBookmark()


    fun readChapter(index: Int)

    fun searchForTag(tag: String)

    fun onBookmarksUpdated()
    data class State(
        var sourceId: String,
        var preview: MangaPreview,
        var data: MangaData?,
        var chapters: List<MangaChapter>,
        var isLoadingData: Boolean,
        var isLoadingChapters: Boolean,
        var isBookmarked: Boolean,
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
            data = when (realmDatabase.has(sourceId, preview.id)) {
                true -> realmDatabase.bookmarks[realmDatabase.getMangaKey(sourceId, preview.id)]
                else -> null
            },
            chapters = when (realmDatabase.has(sourceId, preview.id)) {
                true -> realmDatabase.bookmarks[realmDatabase.getMangaKey(
                    sourceId,
                    preview.id
                )]!!.chapters

                else -> listOf()
            },
            isLoadingData = false,
            isLoadingChapters = false,
            isBookmarked = realmDatabase.has(sourceId,preview.id),
        )
    )

    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            var onBookmarksUpdatedSub: (() -> Unit) ? = null

            override fun onCreate() {
                super.onCreate()
                onBookmarksUpdatedSub = realmDatabase.subscribeOnBookmarksUpdated {
                    onBookmarksUpdated()
                }
            }

            override fun onDestroy() {
                super.onDestroy()
                onBookmarksUpdatedSub?.let { it() }
            }
        })
    }

    override suspend fun loadMangaData() {
        state.update {
            it.isLoadingData = true
            it
        }
        val mangaData =
            api.getManga(source = state.value.sourceId, mangaId = state.value.preview.id)
        if (mangaData.data != null) {
            if (state.value.data is StoredManga) {
                realmDatabase.updateManga(state.value.sourceId, state.value.preview.id) {
                    it.name = mangaData.data.name
                    it.description = mangaData.data.description
                    it.cover = mangaData.data.cover
                    it.status = mangaData.data.status
                    it.extras = mangaData.data.extras.map {
                        StoredMangaExtras().apply {
                            name = it.name
                            value = it.value
                        }
                    }.toRealmList()
                    it.tags = mangaData.data.tags.toRealmList()
                    it.share = mangaData.data.share

                    state.update { update ->
                        update.data = it.copyFromRealm()
                        update.isLoadingData = false
                        update
                    }
                }

            } else {
                state.update {
                    it.data = mangaData.data
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

    override suspend fun loadChapters() {
        state.update {
            it.isLoadingChapters = true
            it
        }
        val chaptersResponse =
            api.getChapters(source = state.value.sourceId, mangaId = state.value.preview.id)
        if (chaptersResponse.data != null) {
            state.update {
                it.chapters = chaptersResponse.data
                it.isLoadingChapters = false
                it
            }

            if (realmDatabase.has(state.value.sourceId, state.value.preview.id)) {
                realmDatabase.updateChapters(
                    state.value.sourceId,
                    state.value.preview.id,
                    chaptersResponse.data
                )
            }
        } else {
            state.update {
                it.isLoadingChapters = false
                it
            }
        }
    }

    override fun readChapter(index: Int) {
        root.navigateToMangaReader(
            state.value.sourceId, state.value.preview.id, index,
            when (realmDatabase.has(state.value.sourceId, state.value.preview.id)) {
                true -> listOf() // will be fetched in the reader
                else -> if (state.value.chapters[0] is ApiMangaChapter) {
                    state.value.chapters as List<ApiMangaChapter>
                } else {
                    listOf<ApiMangaChapter>()
                }
            }
        )
    }

    override fun searchForTag(tag: String) {
        root.navigateToSearch(state.value.sourceId, tag)
    }

    override suspend fun bookmark() {

        state.value.data?.let {
            realmDatabase.bookmark(state.value.sourceId,it,state.value.chapters)
        } ?: realmDatabase.bookmark(state.value.sourceId,state.value.preview)

    }

    override suspend fun unBookmark() {
        realmDatabase.removeBookmark(state.value.sourceId,state.value.preview.id)
    }

    override fun onBookmarksUpdated() {

        state.update {
            val isBookmarked = realmDatabase.has(state.value.sourceId,state.value.preview.id)
            it.isBookmarked = isBookmarked
            if(isBookmarked){
                it.data = realmDatabase.bookmarks[realmDatabase.getMangaKey(state.value.sourceId, state.value.preview.id)]
                if(state.value.data is StoredManga && state.value.chapters.isNotEmpty()){
                    it.chapters = (state.value.data as StoredManga).chapters
                }
            }
            it
        }

    }
}