package com.tarehimself.mira

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.bringToFront
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.router.stack.push
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.benasher44.uuid.uuid4
import com.tarehimself.mira.common.HandlesBack
import com.tarehimself.mira.data.ApiMangaChapter
import com.tarehimself.mira.data.ApiMangaImage
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.data.MangaChapter
import com.tarehimself.mira.data.MangaPreview
import com.tarehimself.mira.data.MangaSource
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.manga.reader.DefaultReaderComponent
import com.tarehimself.mira.manga.reader.ReaderComponent
import com.tarehimself.mira.manga.search.DefaultGlobalSearchComponent
import com.tarehimself.mira.manga.viewer.DefaultViewerComponent
import com.tarehimself.mira.manga.viewer.ViewerComponent
import com.tarehimself.mira.screens.DefaultScreensComponent
import com.tarehimself.mira.screens.ScreensComponent
import com.tarehimself.mira.manga.search.DefaultSearchComponent
import com.tarehimself.mira.manga.search.GlobalSearchComponent
import com.tarehimself.mira.manga.search.SearchComponent
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface RootComponent : HandlesBack {
    val stack: Value<ChildStack<*, Child>>

    fun onBackClicked()

    fun navigateToScreens()

    fun navigateToSearch(sourceId: String,query: String = "")

    fun navigateToViewer(sourceId: String, preview: ApiMangaPreview)

    fun navigateToReader(sourceId: String, manga: String, chapterIndex: Int, chapters: List<MangaChapter>)

    fun navigateToGlobalSearch(searchTerm: String,sourcesToSearch: List<MangaSource>)

    sealed class Child {
        class ScreensChild(val component: ScreensComponent) : Child()
        class SearchChild(val component: SearchComponent) : Child()
        class ViewerChild(val component: ViewerComponent) : Child()
        class ReaderChild(val component: ReaderComponent) : Child()

        class GlobalSearchChild(val component: GlobalSearchComponent) : Child()
    }
}




class DefaultRootComponent(componentContext: ComponentContext) : RootComponent, ComponentContext by componentContext,KoinComponent {

    private val realmDatabase: RealmRepository by inject()

    init {
        lifecycle.subscribe (object : Lifecycle.Callbacks {
            override fun onCreate() {
                super.onCreate()
                Napier.base(DebugAntilog())
            }



            override fun onDestroy() {
                super.onDestroy()
            }
        })
    }

    @Parcelize
    data class PreviewDataParcel(
        override val id: String,
        override val name: String,
        override val cover: ApiMangaImage
    ) : Parcelable, MangaPreview


    private sealed class RootNavConfig : Parcelable {
        @Parcelize
        object Screens : RootNavConfig()

        @Parcelize
        data class Search(val sourceId: String,val query: String,val stateKey: String) : RootNavConfig()

        @Parcelize
        data class Viewer(val sourceId: String, val previewData: PreviewDataParcel) : RootNavConfig()

        @Parcelize
        data class Reader(val sourceId: String, val mangaId: String, val chapterIndex: Int, val chapters: List<MangaChapter>) : RootNavConfig()

        @Parcelize
        data class GlobalSearch(val searchTerm: String, val sourcesToSearch: List<MangaSource>) : RootNavConfig()
    }

    private val navigation = StackNavigation<RootNavConfig>()

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(source=navigation,initialConfiguration=RootNavConfig.Screens, handleBackButton = true, childFactory = ::createChild)

    private fun createChild(config: RootNavConfig, context: ComponentContext): RootComponent.Child = when(config){
        is RootNavConfig.Screens -> RootComponent.Child.ScreensChild(screensComponent(context))
        is RootNavConfig.Search -> RootComponent.Child.SearchChild(mangaSearchComponent(context,config))
        is RootNavConfig.Viewer -> RootComponent.Child.ViewerChild(mangaViewerComponent(context,config))
        is RootNavConfig.Reader -> RootComponent.Child.ReaderChild(mangaReaderComponent(context,config))
        is RootNavConfig.GlobalSearch -> RootComponent.Child.GlobalSearchChild(globalSearchComponent(context,config))
    }

    private fun screensComponent(context: ComponentContext): ScreensComponent =
        DefaultScreensComponent(
            componentContext = context,
            root = this
        )

    private fun mangaSearchComponent(context: ComponentContext,config: RootNavConfig.Search): SearchComponent =
        DefaultSearchComponent(
            componentContext = context,
            sourceId = config.sourceId,
            onMangaSelected = {
                navigateToViewer(config.sourceId,it)
            },
            initialQuery = config.query,
            stateKey = config.stateKey
        )

    private fun mangaViewerComponent(context: ComponentContext,config: RootNavConfig.Viewer): ViewerComponent =
        DefaultViewerComponent(
            componentContext = context,
            preview = ApiMangaPreview(id = config.previewData.id, name = config.previewData.name, cover = config.previewData.cover),
            root = this,
            sourceId = config.sourceId
        )

    private fun mangaReaderComponent(context: ComponentContext,config: RootNavConfig.Reader): ReaderComponent = DefaultReaderComponent(
        componentContext = context,
        mangaId = config.mangaId,
        initialChapterIndex = config.chapterIndex,
        chapters = config.chapters,
        sourceId = config.sourceId
    )

    private fun globalSearchComponent(context: ComponentContext,config: RootNavConfig.GlobalSearch): GlobalSearchComponent = DefaultGlobalSearchComponent(
        componentContext = context,
        sourcesToSearch = config.sourcesToSearch,
        onMangaSelected = { sourceId, preview ->
            navigateToViewer(sourceId,preview)
        },
        initialQuery = config.searchTerm
    )


    override fun onBackClicked() {
        navigation.pop()
    }

    override fun navigateToSearch(sourceId: String,query: String) {
        navigation.push(RootNavConfig.Search(sourceId = sourceId,query=query, stateKey = uuid4().toString()))
    }
    override fun navigateToReader(sourceId: String, manga: String, chapterIndex: Int, chapters: List<MangaChapter>) {
        navigation.push(RootNavConfig.Reader(mangaId=manga, sourceId = sourceId,chapterIndex = chapterIndex, chapters = chapters))
    }

    override fun navigateToViewer(sourceId: String, preview: ApiMangaPreview) {
        navigation.push(RootNavConfig.Viewer(sourceId = sourceId, previewData = PreviewDataParcel(id=preview.id, name = preview.name, cover = preview.cover)))
    }

    override fun navigateToGlobalSearch(searchTerm: String, sourcesToSearch: List<MangaSource>) {
        navigation.push(RootNavConfig.GlobalSearch(searchTerm = searchTerm,sourcesToSearch = sourcesToSearch))
    }

    override fun navigateToScreens() {
        navigation.bringToFront(RootNavConfig.Screens)
    }

    override fun registerBackHandler(handler: () -> Unit): () -> Unit {
        val callback = BackCallback(isEnabled = true,onBack = handler)
        backHandler.register(callback)
        return {
            backHandler.unregister(callback)
        }
    }
}