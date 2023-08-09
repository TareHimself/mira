package com.tarehimself.mira.manga.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.VectorImage
import com.tarehimself.mira.common.AsyncImage
import com.tarehimself.mira.common.networkImagePainter
import compose.icons.Octicons
import compose.icons.octicons.Share24
import io.github.aakira.napier.Napier
import io.ktor.client.request.header
import kotlinx.coroutines.launch


@Composable
fun ReaderChapterLoading() {
    Box(
        modifier = Modifier.fillMaxWidth().height(40.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.align(
                Alignment.Center
            )
        )
    }
}

//@OptIn(ExperimentalTextApi::class)
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MangaReaderContent(component: MangaReaderComponent) {

    val state by component.state.subscribeAsState(neverEqualPolicy())

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded }
    )

    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberLazyListState()

    var wasLoading by remember { mutableStateOf(false) }

    var currentChapterPage by remember { mutableStateOf(0) }

    var currentChapterTotalPages by remember { mutableStateOf(0) }

    var currentBottomSheetTarget by remember { mutableStateOf(-1) }

    LaunchedEffect(Unit) {
        wasLoading = true
        component.loadInitialChapter()
    }

    // Track if we need to load more pages
    LaunchedEffect(Unit) {
        var lastPagesNum = state.pages.size

        snapshotFlow { scrollState.layoutInfo }.collect { layout ->
            if (layout.visibleItemsInfo.isEmpty()) {
                return@collect
            }

            if (state.pages.isNotEmpty() && layout.visibleItemsInfo.isNotEmpty()) {


                val currentPagesNum = state.pages.size


                if (lastPagesNum == 0 && currentPagesNum > 0) {

                    scrollState.scrollToItem(
                        when (val scrollTarget =
                            component.realmDatabase.getChaptersRead(component.realmDatabase.getMangaKey(
                                state.sourceId,
                                state.mangaId
                            )).firstOrNull()?.current) {
                            null -> {
                                1
                            }

                            else -> {
                                if ((state.chapters.lastIndex - scrollTarget.index) == state.initialChapterIndex) {
                                    scrollTarget.progress
                                } else {
                                    1
                                }
                            }
                        }
                    )

                    lastPagesNum = currentPagesNum

                    return@collect
                }


                // When we are at the bottom of the scroll
                val lastItemIndex = layout.visibleItemsInfo.last().index
                if (lastItemIndex >= state.pages.size - 1 && state.pages[lastItemIndex].chapterIndex != 0 && !state.isLoadingNext) { // latest chapter will have index 0
                    wasLoading = true
                    component.loadNextChapter()
                }


                // When we are at the top of the scroll
                val firstItemIndex = layout.visibleItemsInfo.first().index
                if (firstItemIndex == 0 && state.pages[firstItemIndex].chapterIndex != state.chapters.size - 1 && !state.isLoadingPrevious) { // first chapter will have index (size - 1)
                    wasLoading = true
                    component.loadPreviousChapter()
                }

                lastPagesNum = currentPagesNum
            }
        }
    }

    // Track if a chapter has been read
    LaunchedEffect(Unit) {
        var lastChapterIndex = state.initialChapterIndex
        snapshotFlow { scrollState.layoutInfo }.collect { layout ->

            if (layout.visibleItemsInfo.isEmpty()) {
                return@collect
            }

            val lastItemOnPage = layout.visibleItemsInfo.asReversed()
                .find { it.index < state.pages.size && state.pages[it.index] is MangaReaderComponent.ReaderChapterItem }

            if (lastItemOnPage != null) {

                val targetIdx = lastItemOnPage.index


                val readerItem = state.pages[targetIdx]

                if (readerItem.chapterIndex != -1) {
                    if (readerItem is MangaReaderComponent.ReaderChapterItem) {
                        if (currentChapterPage != readerItem.pageIndex || currentChapterTotalPages != readerItem.totalPages || lastChapterIndex != readerItem.chapterIndex) {

                            component.realmDatabase.updateMangaReadInfo(
                                state.sourceId,
                                state.mangaId,
                                state.chapters.lastIndex - readerItem.chapterIndex,
                                readerItem.pageIndex + 1,
                                readerItem.totalPages
                            )
                        }

                        currentChapterPage = readerItem.pageIndex
                        currentChapterTotalPages = readerItem.totalPages
                        if (currentChapterPage + 1 == currentChapterTotalPages && currentChapterTotalPages != 0 && readerItem.chapterIndex != -1) {
                            component.realmDatabase.markChapterAsRead(
                                state.sourceId,
                                state.mangaId,
                                state.chapters.lastIndex - lastChapterIndex
                            )
                        }
                    }

                    lastChapterIndex = readerItem.chapterIndex
                }
            }
        }
    }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
        sheetContent = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Pressable(modifier = Modifier.fillMaxWidth(), onClick = {
                    coroutineScope.launch {
                        if (sheetState.isVisible) {
                            sheetState.hide()
                        }
                    }

                }) {
                    Row(modifier = Modifier.padding(horizontal = 20.dp)) {
                        VectorImage(
                            vector = Octicons.Share24,
                            contentDescription = "Share",
                            modifier = Modifier.size(30.dp).align(Alignment.CenterVertically)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Share",
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                }
//                Pressable(modifier = Modifier.fillMaxWidth(), onClick = {
//                    coroutineScope.launch {
//                        if (sheetState.isVisible) {
//                            Napier.d { "Current Target Index $currentBottomSheetTarget" }
//                            when (val item = state.pages[currentBottomSheetTarget]) {
//                                is MangaReaderComponent.ReaderChapterItem -> {
//                                    val headers = ArrayList(item.data.headers.map {
//                                        listOf("X-TRANSLATOR-HEADER-${it.first()}", it.last())
//                                    })
//                                    headers.add(listOf("X-TRANSLATOR-Target", item.data.src))
//                                    component.setChapterItemForIndex(
//                                        currentBottomSheetTarget,
//                                        item.copy(
//                                            data = item.data.copy(
//                                                src = "https://manga-translator.oyintare.dev/translate?id=${item.id}",
//                                                headers = headers.toList()
//                                            )
//                                        )
//                                    )
//                                }
//                            }
//                            sheetState.hide()
//                            currentBottomSheetTarget = -1
//                        }
//                    }
//                }) {
//                    Row(modifier = Modifier.padding(horizontal = 20.dp)) {
//                        VectorImage(
//                            vector = FontAwesomeIcons.Solid.Language,
//                            contentDescription = "Translate",
//                            modifier = Modifier.size(30.dp).align(Alignment.CenterVertically)
//                        )
//                        Spacer(modifier = Modifier.width(10.dp))
//                        Text(
//                            "Translate",
//                            modifier = Modifier.padding(vertical = 20.dp)
//                        )
//                    }
//                }
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val boxWithConstraintsScope = this
            if (state.pages.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                }
            }


            LazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {


                items(state.pages.size, key = {
                    state.pages[it].id
                }) { idx ->
                    when (val item = state.pages[idx]) {
                        is MangaReaderComponent.ReaderChapterItem -> {
                            Pressable(
                                modifier = Modifier.fillMaxWidth().padding(0.dp, 10.dp),
                                onLongClick = {
                                    if (!sheetState.isVisible) {
                                        coroutineScope.launch {
                                            currentBottomSheetTarget = idx
                                            sheetState.show()
                                        }
                                    }

                                }) {

                                AsyncImage(
                                    painter = networkImagePainter(item.data.src) {
                                        item.data.headers.forEach {
                                            header(it.first(), it.last())
                                            Napier.i(tag = "async image") { "Adding image headers [${it.first()} : ${it.last()}]" }
                                        }
                                    },
                                    contentDescription = "Manga Page",
                                    modifier = Modifier.fillMaxWidth(),
                                    contentScale = ContentScale.FillWidth,
                                    onLoading = {
                                        Box(
                                            modifier = Modifier.height(boxWithConstraintsScope.maxHeight)
                                                .width(boxWithConstraintsScope.maxWidth)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(
                                                    Alignment.Center
                                                )
                                            )
                                        }
                                    },
                                    onFail = {
                                        Box(
                                            modifier = Modifier.height(boxWithConstraintsScope.maxHeight)
                                                .width(boxWithConstraintsScope.maxWidth)
                                        ) {
                                            Text(
                                                "Failed To Load Page",
                                                modifier = Modifier.align(
                                                    Alignment.Center
                                                )
                                            )
                                        }
                                    })

                            }
                        }

                        is MangaReaderComponent.ReaderDividerItem -> {
                            if (state.isLoadingPrevious && state.loadedPages.first() == item.chapterIndex) {
                                ReaderChapterLoading()
                            }
                            Surface(modifier = Modifier.fillMaxWidth().height(100.dp)) {
                                Box(
                                    modifier = Modifier.matchParentSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        item.data,
                                        modifier = Modifier,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            if (state.isLoadingNext && state.loadedPages.last() == item.chapterIndex) {
                                ReaderChapterLoading()
                            }
                        }
                    }
                }

            }


            if (state.pages.isNotEmpty() && currentChapterTotalPages > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)
                ) {
                    Box(modifier = Modifier.padding(vertical = 10.dp)) {
                        Text(
                            "${currentChapterPage + 1}/$currentChapterTotalPages",
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 17.sp
                        )
                    }
                }
            }

        }
    }

}