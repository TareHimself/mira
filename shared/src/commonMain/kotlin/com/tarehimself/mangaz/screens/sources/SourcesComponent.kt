package com.tarehimself.mangaz.screens.sources

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.consume
import com.tarehimself.mangaz.data.MangaApi
import com.tarehimself.mangaz.data.MangaSource


interface SourcesComponent {
    val state: MutableValue<State>

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

    init {
        stateKeeper.register(key = "SOURCES_COMP_STATE") { state.value }
    }

    override val onItemSelected: (source: String) -> Unit = onSourceSelected

    override suspend fun getSources() {
        val sources = MangaApi.get().getSources()

        state.update {
            if(sources.data != null){
                it.sources = sources.data
            }

            it
        }
    }
}