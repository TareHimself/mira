package com.tarehimself.mangaz.screens

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.tarehimself.mangaz.RootComponent
import com.tarehimself.mangaz.screens.sources.DefaultSourcesComponent
import com.tarehimself.mangaz.screens.sources.SourcesComponent
import com.tarehimself.mangaz.ui.search.DefaultLibraryComponent
import com.tarehimself.mangaz.ui.search.LibraryComponent


interface ScreensComponent {

    val stack: Value<ChildStack<*, Child>>

    val root: RootComponent

    sealed class Child {
        class LibraryChild(val component: LibraryComponent) : Child()
        class SourcesChild(val component: SourcesComponent) : Child()
    }

    fun showLibrary()

    fun showSources()

}

class DefaultScreensComponent(componentContext: ComponentContext, override val root: RootComponent) : ScreensComponent,ComponentContext by componentContext {

    @Parcelize // The `kotlin-parcelize` plugin must be applied if you are targeting Android
    private sealed interface Config : Parcelable {
        object Library : Config
        object Sources : Config
    }


    @OptIn(ExperimentalDecomposeApi::class)
    private val navigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<*, ScreensComponent.Child>> = childStack(source=navigation,initialConfiguration= Config.Library, handleBackButton = true, childFactory = ::createChild)

    private fun createChild(config: Config, context: ComponentContext): ScreensComponent.Child = when(config){
        is Config.Library -> ScreensComponent.Child.LibraryChild(libraryComponent(context))
        is Config.Sources -> ScreensComponent.Child.SourcesChild(sourcesComponent(context))
    }

    private fun libraryComponent(context: ComponentContext): LibraryComponent =
        DefaultLibraryComponent(
            componentContext = context,
        )

    private fun sourcesComponent(context: ComponentContext): SourcesComponent =
        DefaultSourcesComponent(
            componentContext = context,
            onSourceSelected = {
                root.navigateToSearch(it)
            }
        )

    override fun showLibrary(){
        navigation.bringToFront(Config.Library)
    }

    override fun showSources(){
        navigation.bringToFront(Config.Sources)
    }
}