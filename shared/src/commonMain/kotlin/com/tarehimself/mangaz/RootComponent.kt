package com.tarehimself.mangaz

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.tarehimself.mangaz.data.MangaPreview
import com.tarehimself.mangaz.manga.reader.DefaultMangaReaderComponent
import com.tarehimself.mangaz.manga.reader.MangaReaderComponent
import com.tarehimself.mangaz.manga.viewer.DefaultMangaViewerComponent
import com.tarehimself.mangaz.manga.viewer.MangaViewerComponent
import com.tarehimself.mangaz.screens.DefaultScreensComponent
import com.tarehimself.mangaz.screens.ScreensComponent
import com.tarehimself.mangaz.screens.sources.DefaultMangaSearchComponent
import com.tarehimself.mangaz.screens.sources.MangaSearchComponent

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun onBackClicked()

    fun navigateToScreens()

    fun navigateToSearch(sourceId: String)

    fun navigateToMangaViewer(sourceId: String,preview: MangaPreview)

    fun navigateToMangaReader(sourceId: String,manga: String,chapterId: String)

    sealed class Child {
        class ScreensChild(val component: ScreensComponent) : Child()
        class MangaSearchChild(val component: MangaSearchComponent) : Child()
        class MangaViewerChild(val component: MangaViewerComponent) : Child()
        class MangaReaderChild(val component: MangaReaderComponent) : Child()
    }
}




class DefaultRootComponent(componentContext: ComponentContext) : RootComponent, ComponentContext by componentContext {

    init {

    }

    @Parcelize
    data class PreviewDataParcel(
        val id: String,
        val name: String,
        val cover: String
    ) : Parcelable

    private sealed class RootNavConfig : Parcelable {
        @Parcelize
        object Screens : RootNavConfig()

        @Parcelize
        data class Search(val sourceId: String) : RootNavConfig()

        @Parcelize
        data class MangaViewer(val sourceId: String,val previewData: PreviewDataParcel) : RootNavConfig()

        @Parcelize
        data class MangaReader(val sourceId: String,val mangaId: String,val chapterId: String) : RootNavConfig()
    }

    private val navigation = StackNavigation<RootNavConfig>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(source=navigation,initialConfiguration=RootNavConfig.Screens, handleBackButton = true, childFactory = ::createChild)

    private fun createChild(config: RootNavConfig, context: ComponentContext): RootComponent.Child = when(config){
        is RootNavConfig.Screens -> RootComponent.Child.ScreensChild(screensComponent(context))
        is RootNavConfig.Search -> RootComponent.Child.MangaSearchChild(mangaSearchComponent(context,config))
        is RootNavConfig.MangaViewer -> RootComponent.Child.MangaViewerChild(mangaViewerComponent(context,config))
        is RootNavConfig.MangaReader -> RootComponent.Child.MangaReaderChild(mangaReaderComponent(context,config))
    }

    private fun screensComponent(context: ComponentContext): ScreensComponent =
        DefaultScreensComponent(
            componentContext = context,
            root = this
        )

    private fun mangaSearchComponent(context: ComponentContext,config: RootNavConfig.Search): MangaSearchComponent =
        DefaultMangaSearchComponent(
            componentContext = context,
            sourceId = config.sourceId,
            onMangaSelected = {
                navigateToMangaViewer(config.sourceId,it)
            }
        )

    private fun mangaViewerComponent(context: ComponentContext,config: RootNavConfig.MangaViewer): MangaViewerComponent =
        DefaultMangaViewerComponent(
            componentContext = context,
            preview = MangaPreview(id = config.previewData.id, name = config.previewData.name, cover = config.previewData.cover),
            root = this,
            sourceId = config.sourceId
        )

    private fun mangaReaderComponent(context: ComponentContext,config: RootNavConfig.MangaReader): MangaReaderComponent = DefaultMangaReaderComponent(
        componentContext = context,
        mangaId = config.mangaId,
        initialChapter = config.chapterId,
        sourceId = config.sourceId
    )


    override fun onBackClicked() {
        navigation.pop()
    }

    override fun navigateToSearch(sourceId: String) {
        navigation.push(RootNavConfig.Search(sourceId = sourceId))
    }
    override fun navigateToMangaReader(sourceId: String,manga: String, chapterId: String) {
        navigation.push(RootNavConfig.MangaReader(mangaId=manga,chapterId=chapterId, sourceId = sourceId))
    }

    override fun navigateToMangaViewer(sourceId: String,preview: MangaPreview) {
        navigation.push(RootNavConfig.MangaViewer(sourceId = sourceId, previewData = PreviewDataParcel(id=preview.id, name = preview.name, cover = preview.cover)))
    }

    override fun navigateToScreens() {
        navigation.bringToFront(RootNavConfig.Screens)
    }
}