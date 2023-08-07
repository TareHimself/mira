package com.tarehimself.mira

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.benasher44.uuid.uuid4
import com.tarehimself.mira.data.ApiMangaChapter
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.data.MangaPreview
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.manga.reader.DefaultMangaReaderComponent
import com.tarehimself.mira.manga.reader.MangaReaderComponent
import com.tarehimself.mira.manga.viewer.DefaultMangaViewerComponent
import com.tarehimself.mira.manga.viewer.MangaViewerComponent
import com.tarehimself.mira.screens.DefaultScreensComponent
import com.tarehimself.mira.screens.ScreensComponent
import com.tarehimself.mira.screens.sources.DefaultMangaSearchComponent
import com.tarehimself.mira.screens.sources.MangaSearchComponent
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    fun onBackClicked()

    fun navigateToScreens()

    fun navigateToSearch(sourceId: String,query: String = "")

    fun navigateToMangaViewer(sourceId: String,preview: ApiMangaPreview)

    fun navigateToMangaReader(sourceId: String,manga: String,chapterIndex: Int,chapters: List<ApiMangaChapter>)

    sealed class Child {
        class ScreensChild(val component: ScreensComponent) : Child()
        class MangaSearchChild(val component: MangaSearchComponent) : Child()
        class MangaViewerChild(val component: MangaViewerComponent) : Child()
        class MangaReaderChild(val component: MangaReaderComponent) : Child()
    }
}




class DefaultRootComponent(componentContext: ComponentContext) : RootComponent, ComponentContext by componentContext,KoinComponent {

    private val realmDatabase: RealmRepository by inject()

    init {
        lifecycle.subscribe (object : Lifecycle.Callbacks {
            override fun onCreate() {
                super.onCreate()
                Napier.base(DebugAntilog())
                realmDatabase.manageData()
            }

            override fun onDestroy() {
                super.onDestroy()
                realmDatabase.stopActiveManaging()
            }
        })
    }

    @Parcelize
    data class PreviewDataParcel(
        override val id: String,
        override val name: String,
        override val cover: String
    ) : Parcelable, MangaPreview


    private sealed class RootNavConfig : Parcelable {
        @Parcelize
        object Screens : RootNavConfig()

        @Parcelize
        data class Search(val sourceId: String,val query: String,val stateKey: String) : RootNavConfig()

        @Parcelize
        data class MangaViewer(val sourceId: String,val previewData: PreviewDataParcel) : RootNavConfig()

        @Parcelize
        data class MangaReader(val sourceId: String,val mangaId: String,val chapterIndex: Int,val chapters: List<ApiMangaChapter>) : RootNavConfig()
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
            },
            initialQuery = config.query,
            stateKey = config.stateKey
        )

    private fun mangaViewerComponent(context: ComponentContext,config: RootNavConfig.MangaViewer): MangaViewerComponent =
        DefaultMangaViewerComponent(
            componentContext = context,
            preview = ApiMangaPreview(id = config.previewData.id, name = config.previewData.name, cover = config.previewData.cover),
            root = this,
            sourceId = config.sourceId
        )

    private fun mangaReaderComponent(context: ComponentContext,config: RootNavConfig.MangaReader): MangaReaderComponent = DefaultMangaReaderComponent(
        componentContext = context,
        mangaId = config.mangaId,
        initialChapterIndex = config.chapterIndex,
        chapters = config.chapters,
        sourceId = config.sourceId
    )


    override fun onBackClicked() {
        navigation.pop()
    }

    override fun navigateToSearch(sourceId: String,query: String) {
        navigation.push(RootNavConfig.Search(sourceId = sourceId,query=query, stateKey = uuid4().toString()))
    }
    override fun navigateToMangaReader(sourceId: String,manga: String,chapterIndex: Int,chapters: List<ApiMangaChapter>) {
        navigation.push(RootNavConfig.MangaReader(mangaId=manga, sourceId = sourceId,chapterIndex = chapterIndex, chapters = chapters))
    }

    override fun navigateToMangaViewer(sourceId: String,preview: ApiMangaPreview) {
        navigation.push(RootNavConfig.MangaViewer(sourceId = sourceId, previewData = PreviewDataParcel(id=preview.id, name = preview.name, cover = preview.cover)))
    }

    override fun navigateToScreens() {
        navigation.bringToFront(RootNavConfig.Screens)
    }
}