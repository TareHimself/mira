package com.tarehimself.mira.screens.sources

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.consume
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface SourcesComponent : KoinComponent {
    val state: MutableValue<State>

    val api: MangaApi
    suspend fun getSources()

    @Parcelize
    data class State(
        var sources: List<MangaSource> = listOf()
    ): Parcelable

    val onItemSelected: (source: String) -> Unit

}
class DefaultSourcesComponent(componentContext: ComponentContext, onSourceSelected: (source: String) -> Unit) : SourcesComponent,ComponentContext by componentContext {
    override val state: MutableValue<SourcesComponent.State> = MutableValue(
        stateKeeper.consume(key = "SOURCES_COMP_STATE") ?: SourcesComponent.State()
    )

    override val api: MangaApi by inject<MangaApi>()

    init {
        stateKeeper.register(key = "SOURCES_COMP_STATE") { state.value }
    }

    override val onItemSelected: (source: String) -> Unit = onSourceSelected

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