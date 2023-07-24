package com.tarehimself.mira.screens

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.consume
import com.tarehimself.mira.RootComponent
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.screens.sources.DefaultSourcesComponent
import com.tarehimself.mira.screens.sources.SourcesComponent
import com.tarehimself.mira.ui.search.DefaultLibraryComponent
import com.tarehimself.mira.ui.search.LibraryComponent


interface ScreensComponent {

    val stack: Value<ChildStack<*, Child>>

    val state: MutableValue<State>

    val root: RootComponent

    enum class EActiveScreen {
        Library,
        Sources
    }

    sealed class Child {
        class LibraryChild(val component: LibraryComponent) : Child()
        class SourcesChild(val component: SourcesComponent) : Child()
    }

    @Parcelize
    data class State(
        var activeScreen: EActiveScreen
    ) : Parcelable

    fun showLibrary()

    fun showSources()

}

class DefaultScreensComponent(
    componentContext: ComponentContext,
    override val root: RootComponent
) : ScreensComponent, ComponentContext by componentContext {

    @Parcelize // The `kotlin-parcelize` plugin must be applied if you are targeting Android
    private sealed interface Config : Parcelable {
        object Library : Config
        object Sources : Config
    }


    @OptIn(ExperimentalDecomposeApi::class)
    private val navigation = StackNavigation<Config>()

    override val state: MutableValue<ScreensComponent.State> = MutableValue(
        stateKeeper.consume(key = "SCREENS_COMP_STATE")
            ?: ScreensComponent.State(activeScreen = ScreensComponent.EActiveScreen.Library)
    )

    override val stack: Value<ChildStack<*, ScreensComponent.Child>> = childStack(
        source = navigation,
        initialConfiguration = when (state.value.activeScreen) {
            ScreensComponent.EActiveScreen.Library -> Config.Library
            ScreensComponent.EActiveScreen.Sources -> Config.Sources
        },
        handleBackButton = true,
        childFactory = ::createChild
    )


    private fun createChild(config: Config, context: ComponentContext): ScreensComponent.Child =
        when (config) {
            is Config.Library -> ScreensComponent.Child.LibraryChild(libraryComponent(context))
            is Config.Sources -> ScreensComponent.Child.SourcesChild(sourcesComponent(context))
        }

    private fun libraryComponent(context: ComponentContext): LibraryComponent =
        DefaultLibraryComponent(
            componentContext = context,
            onMangaSelected = {
                root.navigateToMangaViewer(
                    sourceId = it.source,
                    preview = ApiMangaPreview(id = it.id, name = it.name, cover = it.cover)
                )
            }
        )

    private fun sourcesComponent(context: ComponentContext): SourcesComponent =
        DefaultSourcesComponent(
            componentContext = context,
            onSourceSelected = {
                root.navigateToSearch(it)
            }
        )

    override fun showLibrary() {
        navigation.bringToFront(Config.Library)
        state.update {
            it.activeScreen = ScreensComponent.EActiveScreen.Library
            it
        }
    }

    override fun showSources() {
        navigation.bringToFront(Config.Sources)
        state.update {
            it.activeScreen = ScreensComponent.EActiveScreen.Sources
            it
        }
    }
}