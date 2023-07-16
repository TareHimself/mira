package com.tarehimself.mangaz.manga.viewer

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.tarehimself.mangaz.RootComponent
import com.tarehimself.mangaz.data.MangaApi
import com.tarehimself.mangaz.data.MangaChapter
import com.tarehimself.mangaz.data.MangaData
import com.tarehimself.mangaz.data.MangaPreview


interface MangaViewerComponent {
    val state: MutableValue<State>

    val root: RootComponent

    suspend fun loadMangaData()

    suspend fun loadChapters()

    fun readChapter(data: MangaChapter)

    data class State(
        var sourceId: String,
        var preview: MangaPreview,
        var data: MangaData?,
        var chapters: ArrayList<MangaChapter>
    )
}
class DefaultMangaViewerComponent(componentContext: ComponentContext,
                                  override val root: RootComponent, sourceId:String, preview: MangaPreview) : MangaViewerComponent,ComponentContext by componentContext{
    override val state: MutableValue<MangaViewerComponent.State> = MutableValue(
        MangaViewerComponent.State(
            sourceId = sourceId,
            preview = preview,
            data = null,
            chapters = ArrayList()
        )
    )

    override suspend fun loadMangaData() {
        val mangaData = MangaApi.get().getManga(source = state.value.sourceId,mangaId = state.value.preview.id)
        if(mangaData.data != null){
            state.update {
                it.data = mangaData.data
                it
            }
        }
    }

    override suspend fun loadChapters() {
        val chaptersResponse = MangaApi.get().getChapters(source = state.value.sourceId,mangaId = state.value.preview.id)
        if(chaptersResponse.data != null){
            state.update {
                it.chapters = ArrayList(chaptersResponse.data)
                it
            }
        }
    }

    override fun readChapter(data: MangaChapter) {
        root.navigateToMangaReader(state.value.sourceId,state.value.preview.id,data.id)
    }
}