package com.tarehimself.mira.screens

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.active
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.consume
import com.tarehimself.mira.RootComponent
import com.tarehimself.mira.data.ApiMangaHeader
import com.tarehimself.mira.data.ApiMangaImage
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.screens.bookmarks.DefaultBookmarksComponent
import com.tarehimself.mira.screens.bookmarks.BookmarksComponent
import com.tarehimself.mira.screens.downloads.DefaultDownloadsComponent
import com.tarehimself.mira.screens.settings.DefaultSettingsComponent
import com.tarehimself.mira.screens.sources.DefaultSourcesComponent
import com.tarehimself.mira.screens.downloads.DownloadsComponent
import com.tarehimself.mira.screens.settings.SettingsComponent
import com.tarehimself.mira.screens.sources.SourcesComponent


interface ScreensComponent {

    val stack: Value<ChildStack<*, Child>>

    val state: MutableValue<State>

    val root: RootComponent

    enum class EActiveScreen {
        Library,
        Sources,
        Downloads,
        Settings
    }

    sealed class Child {
        class BookmarksChild(val component: BookmarksComponent) : Child()
        class SourcesChild(val component: SourcesComponent) : Child()

        class DownloadsChild(val component: DownloadsComponent) : Child()
        class SettingsChild(val component: SettingsComponent) : Child()
    }

    @Parcelize
    data class State(
        var activeScreen: EActiveScreen
    ) : Parcelable

    fun showBookmarks()

    fun showSources()

    fun showDownloads()

    fun showSettings()

    val backCallback: BackCallback
}

class DefaultScreensComponent(
    componentContext: ComponentContext,
    override val root: RootComponent
) : ScreensComponent, ComponentContext by componentContext {

    @Parcelize // The `kotlin-parcelize` plugin must be applied if you are targeting Android
    private sealed interface Config : Parcelable {
        object Library : Config
        object Sources : Config

        object Downloads : Config

        object Settings : Config
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
            ScreensComponent.EActiveScreen.Downloads -> Config.Downloads
            ScreensComponent.EActiveScreen.Settings -> Config.Settings
        },
        handleBackButton = true,
        childFactory = ::createChild,
    )


    override val backCallback: BackCallback = BackCallback(isEnabled = true) {
        navigation.pop {
            state.update {
                it.activeScreen = when (stack.active.instance) {
                    is ScreensComponent.Child.BookmarksChild -> ScreensComponent.EActiveScreen.Library
                    is ScreensComponent.Child.SourcesChild -> ScreensComponent.EActiveScreen.Sources
                    is ScreensComponent.Child.DownloadsChild -> ScreensComponent.EActiveScreen.Downloads
                    is ScreensComponent.Child.SettingsChild -> ScreensComponent.EActiveScreen.Settings
                }
                it
            }
        }
    }

    init {
        backHandler.register(backCallback)
    }


    private fun createChild(config: Config, context: ComponentContext): ScreensComponent.Child =
        when (config) {
            is Config.Library -> ScreensComponent.Child.BookmarksChild(bookmarksComponent(context))
            is Config.Sources -> ScreensComponent.Child.SourcesChild(sourcesComponent(context))
            is Config.Downloads -> ScreensComponent.Child.DownloadsChild(downloadsComponent(context))
            is Config.Settings -> ScreensComponent.Child.SettingsChild(settingsComponent(context))
        }

    private fun bookmarksComponent(context: ComponentContext): BookmarksComponent =
        DefaultBookmarksComponent(
            componentContext = context,
            onMangaSelected = {
                root.navigateToMangaViewer(
                    sourceId = it.sourceId,
                    preview = ApiMangaPreview(
                        id = it.id,
                        name = it.name,
                        cover = ApiMangaImage(
                            src = it.cover!!.src,
                            headers = it.cover!!.headers.map { header ->
                                ApiMangaHeader(
                                    key = header.key,
                                    value = header.value
                                )
                            })
                    )
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

    private fun downloadsComponent(context: ComponentContext): DownloadsComponent =
        DefaultDownloadsComponent(
            componentContext = context,
        )

    private fun settingsComponent(context: ComponentContext): SettingsComponent =
        DefaultSettingsComponent(
            componentContext = context,
        )

    override fun showBookmarks() {
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

    override fun showDownloads() {
        navigation.bringToFront(Config.Downloads)
        state.update {
            it.activeScreen = ScreensComponent.EActiveScreen.Downloads
            it
        }
    }

    override fun showSettings() {
        navigation.bringToFront(Config.Settings)
        state.update {
            it.activeScreen = ScreensComponent.EActiveScreen.Settings
            it
        }
    }
}