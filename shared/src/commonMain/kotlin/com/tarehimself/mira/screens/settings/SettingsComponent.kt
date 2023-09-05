package com.tarehimself.mira.screens.settings

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.russhwolf.settings.Settings
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaSource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface SettingsComponent : KoinComponent {
    val state: MutableValue<State>

    val api: MangaApi
    @Parcelize
    data class State(
        var updates: Int
    ) : Parcelable

}

class DefaultSettingsComponent(componentContext: ComponentContext) : SettingsComponent,
    ComponentContext by componentContext {
    override val state: MutableValue<SettingsComponent.State> =
        MutableValue(SettingsComponent.State(updates = 0))

    override val api: MangaApi by inject()

}