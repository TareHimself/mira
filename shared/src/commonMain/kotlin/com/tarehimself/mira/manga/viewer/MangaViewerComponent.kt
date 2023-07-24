package com.tarehimself.mira.manga.viewer

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.tarehimself.mira.RootComponent
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaChapter
import com.tarehimself.mira.data.MangaData
import com.tarehimself.mira.data.MangaPreview
import com.tarehimself.mira.data.RealmRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface MangaViewerComponent : KoinComponent {
    val state: MutableValue<State>

    val root: RootComponent

    val api: MangaApi
    suspend fun loadMangaData()

    suspend fun loadChapters()

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


    val realmDatabase: RealmRepository
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
                true -> realmDatabase.libraryCache[realmDatabase.getMangaKey(sourceId, preview.id)]
                else -> null
            },
            chapters = when (realmDatabase.has(sourceId, preview.id)) {
                true -> realmDatabase.getLibrary()[realmDatabase.getMangaKey(
                    sourceId,
                    preview.id
                )]!!.chapters

                else -> listOf()
            },
            isLoadingData = false,
            isLoadingChapters = false
        )
    )

    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {

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
            state.update {
                it.data = mangaData.data
                it.isLoadingData = false
                it
            }

        }
        else{
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
        }
        else{
            state.update {
                it.isLoadingChapters = false
                it
            }
        }
    }

    override fun readChapter(index: Int) {
        root.navigateToMangaReader(state.value.sourceId, state.value.preview.id, index,state.value.chapters)
    }

    override fun searchForTag(tag: String) {
        root.navigateToSearch(state.value.sourceId, tag)
    }
}