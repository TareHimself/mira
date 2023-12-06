package com.tarehimself.mira.manga.reader

import FileBridge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.benasher44.uuid.uuid4
import com.tarehimself.mira.common.EFilePaths
import com.tarehimself.mira.data.ApiMangaImage
import com.tarehimself.mira.data.ChapterDownloader
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaChapter
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.SettingsRepository
import com.tarehimself.mira.storage.MediaStorage
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.util.InternalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface ReaderComponent : KoinComponent {
    val state: MutableValue<State>

    val api: MangaApi

    val realmDatabase: RealmRepository

    val imageRepository: ImageRepository

    val settingsRepository: SettingsRepository

    val chapterDownloader: ChapterDownloader
    suspend fun loadPagesForIndex(chapterIndex: Int): List<ReaderItem<*>>?

    suspend fun loadNextChapter()

    suspend fun loadPreviousChapter(beforeUpdate: (() -> Unit)? = null)

    suspend fun start()

    suspend fun markChapterAsRead(index: Int)

    fun setChapterItemForIndex(index: Int, item: NetworkChapterItem)

    suspend fun translatePage(page: ReaderComponent.ReaderItem<*>): Boolean

    interface ReaderItem<T> {
        val data: T
        val chapterIndex: Int
        val totalPages: Int
        val id: String
    }

    interface ReaderChapterItem<T> : ReaderItem<T> {
        val pageIndex: Int
    }

    data class NetworkChapterItem(
        override val data: ApiMangaImage, override val chapterIndex: Int,
        override val totalPages: Int, override val pageIndex: Int
    ) : ReaderChapterItem<ApiMangaImage> {
        override val id: String = uuid4().toString()
    }

    data class LocalChapterItem(
        override val data: Int, override val chapterIndex: Int,
        override val totalPages: Int, override val pageIndex: Int
    ) : ReaderChapterItem<Int> {
        override val id: String = uuid4().toString()
    }

    data class TranslatedChapterItem<T>(val translatedFileName: String,override val data: ReaderChapterItem<T>) :
        ReaderChapterItem<ReaderChapterItem<T>> {
        override val pageIndex: Int = data.pageIndex
        override val chapterIndex: Int = data.chapterIndex
        override val id: String = data.id
        override val totalPages: Int = data.totalPages
    }


    data class ReaderDividerItem(
        override val data: String,
        override val chapterIndex: Int,
        override val totalPages: Int
    ) : ReaderItem<String> {
        override val id: String = uuid4().toString()
    }

    data class State(
        var sourceId: String,
        var mangaId: String,
        var initialChapterIndex: Int,
        var chapters: List<MangaChapter>,
        var pages: ArrayList<ReaderItem<*>>,
        var loadedPages: ArrayList<Int>,
        var isLoadingNext: Boolean,
        var isLoadingPrevious: Boolean,
        var initialLoadError: String?,
        var translationTasks: MutableSet<String>
    )


}


@Composable
fun rememberReaderPainter(sourceId: String,
                          mangaId: String,
                          chapter: MangaChapter,
                          item: ReaderComponent.ReaderChapterItem<*>): AsyncReaderPainter {

    val painter = remember(sourceId,mangaId,chapter.id,item) {
        AsyncReaderPainter(sourceId,mangaId,chapter,item)
    }

    DisposableEffect(painter) {
        onDispose {
            painter.onDisposed()
        }
    }

    LaunchedEffect(painter) {
        withContext(Dispatchers.IO) {
            painter.load()
        }
    }

    return painter
}

class DefaultReaderComponent(
    componentContext: ComponentContext,
    sourceId: String,
    mangaId: String,
    initialChapterIndex: Int,
    chapters: List<MangaChapter>
) : ReaderComponent, ComponentContext by componentContext {

    override val api: MangaApi by inject()

    override val realmDatabase: RealmRepository by inject()

    override val imageRepository: ImageRepository by inject()

    override val chapterDownloader: ChapterDownloader by inject()

    override val settingsRepository: SettingsRepository by inject()

    override val state: MutableValue<ReaderComponent.State> = MutableValue(
        ReaderComponent.State(
            mangaId = mangaId,
            initialChapterIndex = initialChapterIndex,
            chapters = chapters.asReversed(), // Move first chapter to index 0
            sourceId = sourceId,
            pages = ArrayList(),
            loadedPages = ArrayList(),
            isLoadingNext = false,
            isLoadingPrevious = false,
            initialLoadError = null,
            translationTasks = mutableSetOf()
        )
    )


    init {
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onCreate() {
                super.onCreate()
            }

            override fun onDestroy() {
                super.onDestroy()
            }
        })
    }

    @OptIn(InternalAPI::class)
    override suspend fun translatePage(page: ReaderComponent.ReaderItem<*>): Boolean {
        if(state.value.translationTasks.contains(page.id) || page is ReaderComponent.TranslatedChapterItem<*>){
            return false
        }

        state.update {
            it.translationTasks.add(page.id)
            it
        }

        try {
            val baseUrl by settingsRepository.translatorEndpoint
            val translatedFileName = "${uuid4()}.png"

            Napier.d { "Using url $baseUrl" }

            val translatedImage = when (page) {
                is ReaderComponent.LocalChapterItem -> {
                    MediaStorage.getChapterPagePath(state.value.sourceId,state.value.mangaId,page.chapterIndex,page.pageIndex)
                        ?.let { pagePath ->
                            FileBridge.readFile(pagePath)?.let { file ->
                                val request = HttpClient().submitFormWithBinaryData(
                                    url = baseUrl,
                                    formData = formData {
                                        append("file", ChannelProvider {
                                            file.first
                                        },Headers.build {
                                            append(HttpHeaders.ContentType, "image/png")
                                            append(HttpHeaders.ContentDisposition, "filename=\"image.png\"")
                                        })
                                    }
                                )

                                if(request.status == HttpStatusCode.OK){
                                    request.bodyAsChannel()
                                }
                                else{
                                    null
                                }
                            }
                        }
                }

                is ReaderComponent.NetworkChapterItem -> {
                    imageRepository.loadHttpImage {
                        url(page.data.src)
                        page.data.headers.forEach {
                            header(key = it.key, value = it.value)
                        }
                    }?.let { file ->
                        val request = HttpClient().submitFormWithBinaryData(
                            url = baseUrl,
                            formData = formData {
                                append("file", ChannelProvider {
                                    file.first
                                },Headers.build {
                                    append(HttpHeaders.ContentType, "image/png")
                                    append(HttpHeaders.ContentDisposition, "filename=\"image.png\"")
                                })
                            }
                        )
                        Napier.d { "Request done with status ${request.status.toString()}" }
                        if(request.status == HttpStatusCode.OK){
                            Napier.d { "Got body with size ${request.contentLength()}" }
                            request.bodyAsChannel()
                        }
                        else{
                            null
                        }
                    }

                }

                else -> {
                    null
                }
            }

            translatedImage?.let {channel ->
                FileBridge.writeFile(
                    translatedFileName,
                    channel,
                    type = EFilePaths.ReaderPageCache
                )
                Napier.d { "Saved Translated to $translatedImage" }
                state.update { current ->
                    val pageIndex = current.pages.indexOfFirst { x -> x.id == page.id }
                    current.pages[pageIndex] =
                        ReaderComponent.TranslatedChapterItem(translatedFileName,page as ReaderComponent.ReaderChapterItem<*>)
                    current.translationTasks.remove(page.id)
                    current
                }

                return true
            }

            state.update {
                it.translationTasks.remove(page.id)
                it
            }
            return false
        } catch (e: Exception) {
            Napier.e("Error Translating Chapter", e)
            state.update {
                it.translationTasks.remove(page.id)
                it
            }

            return false
        }

    }


    override suspend fun loadPagesForIndex(chapterIndex: Int): List<ReaderComponent.ReaderItem<*>>? {


        if (state.value.chapters.lastIndex < chapterIndex) {
            return null
        }

        Napier.d { "Loading pages for chapter $chapterIndex ${state.value.chapters[chapterIndex].id}" }


        if (chapterDownloader.isDownloaded(
                state.value.sourceId,
                state.value.mangaId,
                chapterIndex
            )
        ) {
            val pagesDownloaded = chapterDownloader.getPagesDownloaded(
                state.value.sourceId,
                state.value.mangaId,
                chapterIndex
            )

            if (pagesDownloaded >= 0) {
                val chapter = state.value.chapters[chapterIndex]
                val result: ArrayList<ReaderComponent.ReaderItem<*>> = ArrayList()
                result.add(
                    ReaderComponent.ReaderDividerItem(
                        data = "Next:\n${chapter.name}",
                        chapterIndex = chapterIndex,
                        totalPages = pagesDownloaded
                    )
                )
                for (i in 0 until pagesDownloaded) {
                    result.add(
                        ReaderComponent.LocalChapterItem(
                            data = i,
                            chapterIndex = chapterIndex,
                            pageIndex = i,
                            totalPages = pagesDownloaded
                        )
                    )
                }

                result.add(
                    ReaderComponent.ReaderDividerItem(
                        data = "Previous:\n${chapter.name}",
                        chapterIndex = chapterIndex,
                        totalPages = pagesDownloaded
                    )
                )
                if (chapterIndex == 0) {
                    result.add(
                        ReaderComponent.ReaderDividerItem(
                            data = "No More Chapters",
                            chapterIndex = -1,
                            totalPages = 0
                        )
                    )
                }
                return result
            }
        } else {
            val apiResponse = api.getChapter(
                source = state.value.sourceId,
                mangaId = state.value.mangaId,
                chapterId = state.value.chapters[chapterIndex].id
            )

//            apiResponse.error?.let {
//                throw Exception(it)
//            }

            if (apiResponse.data != null) {
                val chapter = state.value.chapters[chapterIndex]
                val result: ArrayList<ReaderComponent.ReaderItem<*>> = ArrayList()
                result.add(
                    ReaderComponent.ReaderDividerItem(
                        data = "Next:\n${chapter.name}",
                        chapterIndex = chapterIndex,
                        totalPages = apiResponse.data.size
                    )
                )
                result.addAll(apiResponse.data.mapIndexed { idx, page ->
                    ReaderComponent.NetworkChapterItem(
                        data = page,
                        chapterIndex = chapterIndex,
                        pageIndex = idx,
                        totalPages = apiResponse.data.size
                    )
                })
                result.add(
                    ReaderComponent.ReaderDividerItem(
                        data = "Previous:\n${chapter.name}",
                        chapterIndex = chapterIndex,
                        totalPages = apiResponse.data.size
                    )
                )
                if (chapterIndex == 0) {
                    result.add(
                        ReaderComponent.ReaderDividerItem(
                            data = "No More Chapters",
                            chapterIndex = -1,
                            totalPages = 0
                        )
                    )
                }
                return result
            }
        }

        return null
    }

    override suspend fun loadNextChapter() {
        if (state.value.isLoadingNext) {
            return
        }

        if (state.value.loadedPages.size == 0) {
            return
        }

        val nextIndexToLoad = state.value.loadedPages.lastOrNull()?.let { it + 1 } ?: 0

        if (nextIndexToLoad > state.value.chapters.lastIndex) { // We have reached the latest chapter
            return
        }

        state.update {
            it.isLoadingNext = true
            it
        }

        val pages = loadPagesForIndex(nextIndexToLoad)

        if (pages is ArrayList<ReaderComponent.ReaderItem<*>>) {
            state.update {
                it.pages.addAll(pages)
                it.loadedPages.add(nextIndexToLoad)
                it.isLoadingNext = false
                it
            }
            return
        }

        state.update {
            it.isLoadingNext = false
            it
        }
    }

    override suspend fun loadPreviousChapter(beforeUpdate: (() -> Unit)?) {
        if (state.value.isLoadingPrevious) {
            return
        }

        if (state.value.loadedPages.size == 0) {
            return
        }

        val previousToLoad = state.value.loadedPages.firstOrNull()?.let { it - 1 } ?: 0

        if (previousToLoad < 0 || state.value.loadedPages.contains(previousToLoad)) { // We have reached the latest chapter
            return
        }

        state.update {
            it.isLoadingPrevious = true
            it
        }

        val pages = loadPagesForIndex(previousToLoad)

        if (pages is ArrayList<ReaderComponent.ReaderItem<*>>) {
            state.update {
                it.pages.addAll(0, pages)
                it.loadedPages.add(0, previousToLoad)
                it.isLoadingPrevious = false
                it
            }
            return
        }

        state.update {
            it.isLoadingPrevious = false
            it
        }
    }

    override suspend fun start() {

        try {

            if(realmDatabase.has(state.value.sourceId,state.value.mangaId)){
                state.update {
                    it.chapters = realmDatabase.getBookmark(
                        RealmRepository.getBookmarkKey(
                            state.value.sourceId,
                            state.value.mangaId
                        )
                    ).find()?.chapters?.asReversed() ?: listOf() // Move first chapter to index 0
                    it
                }
            }

            state.update {
                it.initialChapterIndex = it.chapters.lastIndex - it.initialChapterIndex
                it
            }

            val pages = loadPagesForIndex(state.value.initialChapterIndex)

            if (pages is ArrayList<ReaderComponent.ReaderItem<*>>) {
                state.update {
                    it.pages.addAll(pages)
                    it.loadedPages.add(state.value.initialChapterIndex)
                    it
                }
            }
        } catch (e: Exception) {
            state.update {
                it.initialLoadError = e.message
                it
            }
        }
    }

    override suspend fun markChapterAsRead(index: Int) {

    }

    override fun setChapterItemForIndex(
        index: Int,
        item: ReaderComponent.NetworkChapterItem
    ) {
        state.update {
            it.pages[index] = item
            it
        }
    }
}