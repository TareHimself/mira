package com.tarehimself.mangaz.manga.reader

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.tarehimself.mangaz.common.SizedCache
import com.tarehimself.mangaz.data.MangaApi

interface MangaReaderComponent {
    val state: MutableValue<State>

    suspend fun loadPages(chapterId: String) : List<String> ?

    data class State(
        var sourceId: String,
        var mangaId: String,
        var initialChapter: String,
        var cachedPages: SizedCache<String,List<String>>
    )
}
class DefaultMangaReaderComponent(componentContext: ComponentContext,sourceId:String, mangaId: String,initialChapter: String) : MangaReaderComponent,ComponentContext by componentContext{

    override val state: MutableValue<MangaReaderComponent.State> = MutableValue(
        MangaReaderComponent.State(
            mangaId = mangaId,
            initialChapter = initialChapter,
            sourceId = sourceId,
            cachedPages = SizedCache(20)
        )
    )

    override suspend fun loadPages(chapterId: String) : List<String> ? {
        if(state.value.cachedPages.contains(chapterId)){
            return  state.value.cachedPages[chapterId]
        }

        val apiResponse = MangaApi.get().getChapter(source = state.value.sourceId,mangaId = state.value.mangaId, chapterId = chapterId)

        if(apiResponse.data != null){
            state.update {
                it.cachedPages[chapterId] = apiResponse.data
                it
            }
        }

        return apiResponse.data
    }


}