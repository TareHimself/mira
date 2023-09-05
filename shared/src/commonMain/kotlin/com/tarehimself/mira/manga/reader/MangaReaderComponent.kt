package com.tarehimself.mira.manga.reader

import FileBridge
import androidx.compose.ui.graphics.ImageBitmap
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.benasher44.uuid.uuid4
import com.tarehimself.mira.common.ECacheType
import com.tarehimself.mira.common.quickHash
import com.tarehimself.mira.data.ApiMangaImage
import com.tarehimself.mira.data.ChapterDownloader
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaChapter
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.SettingsRepository
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
import io.ktor.utils.io.ByteReadChannel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface MangaReaderComponent : KoinComponent {
    val state: MutableValue<State>

    val api: MangaApi

    val realmDatabase: RealmRepository

    val imageRepository: ImageRepository

    val settingsRepository: SettingsRepository

    val chapterDownloader: ChapterDownloader
    suspend fun loadPagesForIndex(chapterIndex: Int): List<ReaderItem<*>>?

    suspend fun loadNextChapter()

    suspend fun loadPreviousChapter(beforeUpdate: (() -> Unit)? = null)

    suspend fun loadInitialChapter()

    suspend fun markChapterAsRead(index: Int)

    fun setChapterItemForIndex(index: Int, item: NetworkChapterItem)

    suspend fun translatePage(index: Int): Boolean

    suspend fun loadLocalPageBitmap(item: LocalChapterItem): ImageBitmap?

    suspend fun loadLocalPage(item: LocalChapterItem): Pair<ByteReadChannel, Long>?

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

    data class TranslatedChapterItem<T>(override val data: ReaderChapterItem<T>) :
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
        var translationTasks: MutableSet<Int>
    )


}

class DefaultMangaReaderComponent(
    componentContext: ComponentContext,
    sourceId: String,
    mangaId: String,
    initialChapterIndex: Int,
    chapters: List<MangaChapter>
) : MangaReaderComponent, ComponentContext by componentContext {

    override val api: MangaApi by inject()

    override val realmDatabase: RealmRepository by inject()

    override val imageRepository: ImageRepository by inject()

    override val chapterDownloader: ChapterDownloader by inject()

    override val settingsRepository: SettingsRepository by inject()

    override val state: MutableValue<MangaReaderComponent.State> = MutableValue(
        MangaReaderComponent.State(
            mangaId = mangaId,
            initialChapterIndex = initialChapterIndex,
            chapters = when (realmDatabase.has(sourceId, mangaId)) {
                true -> {
                    realmDatabase.getBookmark(
                        realmDatabase.getBookmarkKey(
                            sourceId,
                            mangaId
                        )
                    ).find()?.chapters ?: listOf()
                }

                else -> {
                    chapters
                }
            },
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

    override suspend fun loadLocalPageBitmap(item: MangaReaderComponent.LocalChapterItem): ImageBitmap? {
        return FileBridge.getDownloadedChapterPageAsBitmap(
            realmDatabase.getBookmarkKey(
                state.value.sourceId,
                state.value.mangaId
            ).quickHash(),
            state.value.chapters[item.chapterIndex].id.quickHash(),
            item.pageIndex,
            imageRepository.deviceWidth.value
        )
    }

    override suspend fun loadLocalPage(item: MangaReaderComponent.LocalChapterItem): Pair<ByteReadChannel, Long>? {
        return FileBridge.getDownloadedChapterPage(
            realmDatabase.getBookmarkKey(
                state.value.sourceId,
                state.value.mangaId
            ).quickHash(),
            state.value.chapters[item.chapterIndex].id.quickHash(),
            item.pageIndex
        )
    }


    @OptIn(InternalAPI::class)
    override suspend fun translatePage(index: Int): Boolean {
        if(state.value.translationTasks.contains(index)){
            return false
        }

        state.update {
            it.translationTasks.add(index)
            it
        }

        try {
            state.value.pages.getOrNull(index)?.let {
                when (it is MangaReaderComponent.LocalChapterItem || it is MangaReaderComponent.NetworkChapterItem) {
                    true -> it
                    else -> null
                }
            }?.let { chapterItem ->
                val baseUrl by settingsRepository.translatorEndpoint
                val translatedImage = when (chapterItem) {
                    is MangaReaderComponent.LocalChapterItem -> {
                        loadLocalPage(chapterItem)?.let { file ->
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

                    is MangaReaderComponent.NetworkChapterItem -> {
                        imageRepository.loadHttpImage {
                            url(chapterItem.data.src)
                            chapterItem.data.headers.forEach {
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
                    FileBridge.cacheItem(
                        chapterItem.id.quickHash(),
                        channel,
                        type = ECacheType.Reader
                    )
                    Napier.d { "Saved Translated t0 ${chapterItem.id.quickHash()}" }
                    state.update { current ->
                        current.pages[index] =
                            MangaReaderComponent.TranslatedChapterItem(chapterItem as MangaReaderComponent.ReaderChapterItem<*>)
                        current.translationTasks.remove(index)
                        current
                    }

                    return true
                }
            }

            state.update {
                it.translationTasks.remove(index)
                it
            }
            return false
        } catch (e: Exception) {
            Napier.e("Error Translating Chapter", e)
            state.update {
                it.translationTasks.remove(index)
                it
            }

            return false
        }

    }


    override suspend fun loadPagesForIndex(chapterIndex: Int): List<MangaReaderComponent.ReaderItem<*>>? {

        if (state.value.chapters.lastIndex < chapterIndex) {
            return null
        }

        Napier.d { "Loading pages for chapter ${chapterIndex} ${state.value.chapters[chapterIndex].id}" }

        if (chapterDownloader.isDownloaded(
                state.value.sourceId,
                state.value.mangaId,
                state.value.chapters[chapterIndex].id
            )
        ) {
            val pagesDownloaded = chapterDownloader.getPagesDownloaded(
                state.value.sourceId,
                state.value.mangaId,
                state.value.chapters[chapterIndex].id
            )

            if (pagesDownloaded >= 0) {
                val chapter = state.value.chapters[chapterIndex]
                val result: ArrayList<MangaReaderComponent.ReaderItem<*>> = ArrayList()
                result.add(
                    MangaReaderComponent.ReaderDividerItem(
                        data = "Next:\n${chapter.name}",
                        chapterIndex = chapterIndex,
                        totalPages = pagesDownloaded
                    )
                )
                for (i in 0 until pagesDownloaded) {
                    result.add(
                        MangaReaderComponent.LocalChapterItem(
                            data = i,
                            chapterIndex = chapterIndex,
                            pageIndex = i,
                            totalPages = pagesDownloaded
                        )
                    )
                }

                result.add(
                    MangaReaderComponent.ReaderDividerItem(
                        data = "Previous:\n${chapter.name}",
                        chapterIndex = chapterIndex,
                        totalPages = pagesDownloaded
                    )
                )
                if (chapterIndex == 0) {
                    result.add(
                        MangaReaderComponent.ReaderDividerItem(
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
                val result: ArrayList<MangaReaderComponent.ReaderItem<*>> = ArrayList()
                result.add(
                    MangaReaderComponent.ReaderDividerItem(
                        data = "Next:\n${chapter.name}",
                        chapterIndex = chapterIndex,
                        totalPages = apiResponse.data.size
                    )
                )
                result.addAll(apiResponse.data.mapIndexed { idx, page ->
                    MangaReaderComponent.NetworkChapterItem(
                        data = page,
                        chapterIndex = chapterIndex,
                        pageIndex = idx,
                        totalPages = apiResponse.data.size
                    )
                })
                result.add(
                    MangaReaderComponent.ReaderDividerItem(
                        data = "Previous:\n${chapter.name}",
                        chapterIndex = chapterIndex,
                        totalPages = apiResponse.data.size
                    )
                )
                if (chapterIndex == 0) {
                    result.add(
                        MangaReaderComponent.ReaderDividerItem(
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

        var nextIndexToLoad = state.value.loadedPages.lastOrNull()

        if (nextIndexToLoad == null || nextIndexToLoad - 1 < 0) { // We have reached the latest chapter
            return
        }

        nextIndexToLoad -= 1

        state.update {
            it.isLoadingNext = true
            it
        }

        val pages = loadPagesForIndex(nextIndexToLoad)

        if (pages is ArrayList<MangaReaderComponent.ReaderItem<*>>) {
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

        val previousToLoad = state.value.loadedPages[0] + 1

        if (previousToLoad >= state.value.chapters.size) { // We have reached the latest chapter
            return
        }

        state.update {
            it.isLoadingPrevious = true
            it
        }

        val pages = loadPagesForIndex(previousToLoad)

        if (pages is ArrayList<MangaReaderComponent.ReaderItem<*>>) {
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

    override suspend fun loadInitialChapter() {
        try {
            val pages = loadPagesForIndex(state.value.initialChapterIndex)

            if (pages is ArrayList<MangaReaderComponent.ReaderItem<*>>) {
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
        item: MangaReaderComponent.NetworkChapterItem
    ) {
        state.update {
            it.pages[index] = item
            it
        }
    }
}