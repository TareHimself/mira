package com.tarehimself.mira.manga.reader

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.benasher44.uuid.uuid4
import com.tarehimself.mira.data.ApiMangaChapterPage
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaChapter
import com.tarehimself.mira.data.RealmRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface MangaReaderComponent : KoinComponent {
    val state: MutableValue<State>

    val api: MangaApi

    val realmDatabase: RealmRepository
    suspend fun loadPagesForIndex(chapterIndex: Int): List<ReaderItem<*>>?

    suspend fun loadNextChapter()

    suspend fun loadPreviousChapter(beforeUpdate: (() -> Unit)? = null)

    suspend fun loadInitialChapter()

    suspend fun markChapterAsRead(index: Int)

    fun setChapterItemForIndex(index: Int,item: ReaderChapterItem)

    enum class EReaderItemType {
        Page,
        Divider
    }

    interface ReaderItem<T> {
        val type: EReaderItemType
        val data: T
        val chapterIndex: Int
        val totalPages: Int
        val id: String
    }

    data class ReaderChapterItem(
        override val data: ApiMangaChapterPage, override val chapterIndex: Int,
        override val totalPages: Int, val pageIndex: Int
    ) : ReaderItem<ApiMangaChapterPage> {
        override val type: EReaderItemType = EReaderItemType.Page
        override val id: String = uuid4().toString()
    }



    data class ReaderDividerItem(
        override val data: String,
        override val chapterIndex: Int,
        override val totalPages: Int
    ) : ReaderItem<String>{
        override val type: EReaderItemType = EReaderItemType.Divider
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

    override val state: MutableValue<MangaReaderComponent.State> = MutableValue(
        MangaReaderComponent.State(
            mangaId = mangaId,
            initialChapterIndex = initialChapterIndex,
            chapters = when (realmDatabase.has(sourceId, mangaId)) {
                true -> {
                    realmDatabase.getManga(realmDatabase.getMangaKey(
                        sourceId,
                        mangaId
                    )).first().chapters
                }

                else -> {
                    chapters
                }
            },
            sourceId = sourceId,
            pages = ArrayList(),
            loadedPages = ArrayList(),
            isLoadingNext = false,
            isLoadingPrevious = false
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


    override suspend fun loadPagesForIndex(chapterIndex: Int): List<MangaReaderComponent.ReaderItem<*>>? {
        val apiResponse = api.getChapter(
            source = state.value.sourceId,
            mangaId = state.value.mangaId,
            chapterId = state.value.chapters[chapterIndex].id
        )

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
                MangaReaderComponent.ReaderChapterItem(
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
        val pages = loadPagesForIndex(state.value.initialChapterIndex)

        if (pages is ArrayList<MangaReaderComponent.ReaderItem<*>>) {
            state.update {
                it.pages.addAll(pages)
                it.loadedPages.add(state.value.initialChapterIndex)
                it
            }
        }
    }

    override suspend fun markChapterAsRead(index: Int) {

    }

    override fun setChapterItemForIndex(
        index: Int,
        item: MangaReaderComponent.ReaderChapterItem
    ) {
        state.update {
            it.pages[index] = item
            it
        }
    }
}