package com.tarehimself.mira.screens.downloads

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface DownloadsComponent : KoinComponent {
    val state: MutableValue<State>

    val api: MangaApi
    suspend fun getSources()

    @Parcelize
    data class State(
        var sources: List<MangaSource> = listOf()
    ): Parcelable

}
class DefaultDownloadsComponent(componentContext: ComponentContext) : DownloadsComponent,ComponentContext by componentContext {
    override val state: MutableValue<DownloadsComponent.State> = MutableValue(
        DownloadsComponent.State()
    )

    override val api: MangaApi by inject<MangaApi>()

    init {
        stateKeeper.register(key = "SOURCES_COMP_STATE") { state.value }
    }

    override suspend fun getSources() {
        val sources = api.getSources()

        state.update {
            if(sources.data != null){
                it.sources = sources.data
            }

            it
        }
    }
}