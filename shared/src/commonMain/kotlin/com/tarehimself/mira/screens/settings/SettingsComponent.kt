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


interface SettingsComponent : KoinComponent {
    val state: MutableValue<State>

    val api: MangaApi
    suspend fun getSources()

    @Parcelize
    data class State(
        var sources: List<MangaSource> = listOf()
    ) : Parcelable

}

class DefaultSettingsComponent(componentContext: ComponentContext) : SettingsComponent,
    ComponentContext by componentContext {
    override val state: MutableValue<SettingsComponent.State> =
        MutableValue(SettingsComponent.State())

    override val api: MangaApi by inject<MangaApi>()

    override suspend fun getSources() {
        val sources = api.getSources()

        state.update {
            if (sources.data != null) {
                it.sources = sources.data
            }

            it
        }
    }
}