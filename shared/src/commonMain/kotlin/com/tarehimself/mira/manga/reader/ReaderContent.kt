package com.tarehimself.mira.manga.reader

import ShareBridge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.common.EFilePaths
import com.tarehimself.mira.common.ui.Pressable
import com.tarehimself.mira.common.ui.VectorImage
import com.tarehimself.mira.common.hash
import com.tarehimself.mira.common.ui.AsyncBitmapPainter
import com.tarehimself.mira.common.ui.AsyncPainter
import com.tarehimself.mira.common.ui.ErrorContent
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.rememberIsBookmarked
import compose.icons.FontAwesomeIcons
import compose.icons.Octicons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.File
import compose.icons.fontawesomeicons.solid.Language
import compose.icons.octicons.Share24
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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

@Composable
fun ReaderPageLoading(
    ratioKey: String,
    constraints: BoxWithConstraintsScope,
    painter: AsyncBitmapPainter,
    imageRepository: ImageRepository = koinInject()
) {

    val modifier = remember {
        when (val imageRatio = imageRepository.coverRatios[ratioKey]) {
            is Float -> {
                Modifier.aspectRatio(imageRatio).fillMaxWidth()
            }

            else -> {
                Modifier.width(constraints.maxWidth).height(constraints.maxHeight)
            }
        }
    }

    val progress by painter.progress

    Box(modifier = Modifier.fillMaxWidth().padding(0.dp, 10.dp)) {
        Box(
            modifier = modifier
        ) {
            Crossfade(progress > 0.1, modifier = Modifier.matchParentSize()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (it) {
                        CircularProgressIndicator(
                            progress, modifier = Modifier.align(
                                Alignment.Center
                            )
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.align(
                                Alignment.Center
                            )
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun ChapterPage(
    component: ReaderComponent,
    itemIndex: Int,
    page: ReaderComponent.ReaderChapterItem<*>,
    onLongClick: (painter: AsyncReaderPainter) -> Unit,
    constraints: BoxWithConstraintsScope,
    imageRepository: ImageRepository = koinInject()
) {

    val state by component.state.subscribeAsState(neverEqualPolicy())

//    val deviceWidth by imageRepository.deviceWidth

    val asyncPainter = rememberReaderPainter(state.sourceId,state.mangaId,state.chapters[page.chapterIndex],page)

    val imageRatioKey =
        remember { "${state.sourceId}${state.mangaId}${page.chapterIndex}${page.pageIndex}".hash() }

    val painterStatus by asyncPainter.status


    Crossfade(painterStatus, animationSpec = tween(500)) {
        when (it) {
            AsyncPainter.EAsyncPainterStatus.LOADING -> {
                ReaderPageLoading(
                    ratioKey = imageRatioKey, painter = asyncPainter, constraints = constraints
                )
            }

            AsyncPainter.EAsyncPainterStatus.SUCCESS -> {
                val painter = asyncPainter.beforePaint()
                if (painter != null) {
                    Pressable(modifier = Modifier.fillMaxWidth().padding(0.dp, 10.dp),
                        onLongClick = {
                            onLongClick(asyncPainter)
                        }) {
                        Box {
                            Image(
                                painter = painter,
                                contentDescription = "Chapter ${page.chapterIndex + 1} Page ${page.pageIndex + 1}",
                                modifier = Modifier.fillMaxWidth().onGloballyPositioned { layout ->

                                    if (!imageRepository.coverRatios.contains(imageRatioKey)) {
                                        imageRepository.coverRatios[imageRatioKey] =
                                            layout.size.width.toFloat() / layout.size.height.toFloat()
                                    }

                                },
                                contentScale = ContentScale.FillWidth,
                            )
                            Crossfade(
                                state.translationTasks.contains(page.id),
                                modifier = Modifier.matchParentSize()
                            ) { shouldShowTranslating ->
                                if (shouldShowTranslating) {
                                    Box(
                                        modifier = Modifier.fillMaxSize().background(
                                            MaterialTheme.colorScheme.background.copy(
                                                alpha = 0.6f
                                            )
                                        )
                                    ) {
                                        Napier.d { "Translating chapter indicator" }
                                        Column(
                                            modifier = Modifier.align(Alignment.Center)
                                                .fillMaxSize(0.8f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                "Translating",
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        }

                                    }
                                }
                            }

                        }

                    }
                }


            }

            AsyncPainter.EAsyncPainterStatus.FAIL -> {

                Box(
                    modifier = Modifier.height(constraints.maxHeight).width(constraints.maxWidth)
                ) {
                    ErrorContent("Failed To Load Page")
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ReaderContent(component: ReaderComponent) {

    val state by component.state.subscribeAsState(neverEqualPolicy())

    val sheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded })

    val coroutineScope = rememberCoroutineScope()

    val scrollState = rememberLazyListState()

    var wasLoading by remember { mutableStateOf(false) }

    var currentChapterPageIndex by remember { mutableStateOf(0) }

    var currentChapterTotalPages by remember { mutableStateOf(0) }

    var currentBottomSheetTarget by remember { mutableStateOf<Pair<Int, AsyncReaderPainter>?>(null) }

    val isBookmarked by rememberIsBookmarked(state.sourceId, state.mangaId)

    var hasScrolledToLast by remember { mutableStateOf(false) }

    var currentChapterIndex by remember { mutableStateOf(state.initialChapterIndex) }

    LaunchedEffect(Unit) {
        wasLoading = true
        component.start()
    }

    // Track if we need to load more pages
    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.layoutInfo }.collect { layout ->
            if (state.chapters.isEmpty()) {
                return@collect
            }

            if (state.pages.isEmpty()) {
                return@collect
            }

            if (layout.visibleItemsInfo.isEmpty()) {
                return@collect
            }

            if (!hasScrolledToLast) {
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
        }
    }

    // Track if a chapter has been read
    LaunchedEffect(Unit) {

        snapshotFlow { scrollState.layoutInfo }.collect { layout ->

            if (state.chapters.isEmpty()) {
                return@collect
            }

            if (state.pages.isEmpty()) {
                return@collect
            }

            if (layout.visibleItemsInfo.isEmpty()) {
                return@collect
            }

            if (!hasScrolledToLast) {

                scrollState.scrollToItem(
                    when (val scrollTarget = component.realmDatabase.getChaptersRead(
                        RealmRepository.getBookmarkKey(
                            state.sourceId, state.mangaId
                        )
                    ).asFlow().first().obj?.current) {
                        null -> {
                            1
                        }

                        else -> {
                            if (scrollTarget.index == state.initialChapterIndex) {
                                scrollTarget.progress
                            } else {
                                1
                            }
                        }
                    }
                )

                hasScrolledToLast = true
                return@collect
            }


            val lastItemOnPage = layout.visibleItemsInfo.asReversed()
                .find { it.index < state.pages.size && state.pages[it.index] is ReaderComponent.ReaderChapterItem }
                ?: return@collect

            val targetChapterItem = state.pages[lastItemOnPage.index]

            if (targetChapterItem !is ReaderComponent.ReaderChapterItem) {
                return@collect
            }

            val lastPage = currentChapterPageIndex
            val lastTotalPages = currentChapterTotalPages
            val lastChapterIndex = currentChapterIndex

            currentChapterPageIndex = targetChapterItem.pageIndex
            currentChapterTotalPages = targetChapterItem.totalPages
            currentChapterIndex = targetChapterItem.chapterIndex

            if (!isBookmarked) { // Only show visual tracker if it is not bookmarked
                return@collect
            }
            val isAtLastPage = (currentChapterPageIndex + 1) == currentChapterTotalPages
            val chapterDelta = currentChapterIndex - lastChapterIndex
            val pageDelta = currentChapterPageIndex - lastPage
            val totalPagesDelta = currentChapterTotalPages - lastTotalPages

            if (chapterDelta < 0 || isAtLastPage) {
                component.realmDatabase.markChapterAsRead(
                    state.sourceId, state.mangaId, when (isAtLastPage) {
                        true -> currentChapterIndex
                        else -> lastChapterIndex
                    }
                )
            } else if (pageDelta != 0 || totalPagesDelta != 0) {
                component.realmDatabase.updateBookmarkReadInfo(
                    state.sourceId,
                    state.mangaId,
                    currentChapterIndex,
                    currentChapterPageIndex + 1,
                    currentChapterTotalPages
                )
            }

        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ModalBottomSheetLayout(
            sheetState = sheetState,
            sheetShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
            sheetBackgroundColor = MaterialTheme.colorScheme.surface,
            sheetContent = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {

                    Pressable(modifier = Modifier.fillMaxWidth().height(70.dp), onClick = {
                        coroutineScope.launch {
                            if (sheetState.isVisible) {
                                currentBottomSheetTarget?.second?.let {
                                    it.saveTo("${currentChapterPageIndex.toString().padStart(5,'0')}.png",EFilePaths.SharedFiles)?.let { path -> ShareBridge.shareFile(path) }
                                    currentBottomSheetTarget = null
                                    sheetState.hide()
                                }
                            }
                        }
                    }) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VectorImage(
                                vector = Octicons.Share24,
                                contentDescription = "Share",
                                modifier = Modifier.size(30.dp).align(Alignment.CenterVertically)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Share", modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    }
                    currentBottomSheetTarget?.let { target ->
                        val chapterItem = state.pages[target.first]
                        if (chapterItem !is ReaderComponent.TranslatedChapterItem<*> && !state.translationTasks.contains(chapterItem.id) && target.second.status.value == AsyncPainter.EAsyncPainterStatus.SUCCESS
                        ) {
                            Pressable(modifier = Modifier.fillMaxWidth().height(70.dp), onClick = {
                                coroutineScope.launch {
                                    if (sheetState.isVisible) {
                                        sheetState.hide()
                                        component.translatePage(chapterItem)
                                    }
                                }
                            }) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    VectorImage(
                                        vector = FontAwesomeIcons.Solid.Language,
                                        contentDescription = "Translate",
                                        modifier = Modifier.size(30.dp)
                                            .align(Alignment.CenterVertically)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        "Translate", modifier = Modifier.padding(vertical = 20.dp)
                                    )
                                }
                            }
                        }
                    }
                    Pressable(modifier = Modifier.fillMaxWidth().height(70.dp), onClick = {
                        coroutineScope.launch {
                            if (sheetState.isVisible) {
                                currentBottomSheetTarget?.second?.let {
                                    it.saveTo(
                                        "${
                                            currentChapterPageIndex.toString().padStart(5, '0')
                                        }.png", EFilePaths.SharedFiles
                                    )?.let { path -> ShareBridge.saveImage(path) }


                                    currentBottomSheetTarget = null
                                    sheetState.hide()
                                }
                            }
                        }
                    }) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            VectorImage(
                                vector = FontAwesomeIcons.Regular.File,
                                contentDescription = "Save",
                                modifier = Modifier.size(20.dp).align(Alignment.CenterVertically)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Save", modifier = Modifier.padding(vertical = 20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(30.dp))
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxWithConstraintsScope = this
                if (state.pages.isEmpty() && state.initialLoadError == null) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(
                                Alignment.Center
                            )
                        )
                    }
                } else if (state.initialLoadError != null) {
                    ErrorContent("Failed To Load Chapter")
                }


                LazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {
                    items(state.pages.size, key = {
                        state.pages[it].id
                    }) { idx ->
                        when (val item = state.pages[idx]) {

                            is ReaderComponent.ReaderChapterItem -> {
                                ChapterPage(
                                    component = component,
                                    itemIndex = idx,
                                    page = item,
                                    onLongClick = { painter ->
                                        if (!sheetState.isVisible) {
                                            coroutineScope.launch {
                                                currentBottomSheetTarget = Pair(idx, painter)
                                                sheetState.show()
                                            }
                                        }

                                    },
                                    constraints = boxWithConstraintsScope
                                )
                            }

                            is ReaderComponent.ReaderDividerItem -> {
                                if (state.isLoadingPrevious && idx == 0) {
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
                                if (state.isLoadingNext && idx == state.pages.lastIndex) {
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
                                "${currentChapterPageIndex + 1}/$currentChapterTotalPages",
                                modifier = Modifier.align(Alignment.Center),
                                fontSize = 17.sp
                            )
                        }
                    }
                }

            }
        }
    }


}