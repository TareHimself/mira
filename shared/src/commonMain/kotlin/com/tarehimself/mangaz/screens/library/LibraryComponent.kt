package com.tarehimself.mangaz.ui.search

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value


interface LibraryComponent  {
    val state: Value<State>


    data class State(
        val dummy: String
    )
}
class DefaultLibraryComponent (componentContext: ComponentContext) : LibraryComponent,ComponentContext by componentContext {
    override val state: Value<LibraryComponent.State> = MutableValue(LibraryComponent.State(dummy = ""))

}