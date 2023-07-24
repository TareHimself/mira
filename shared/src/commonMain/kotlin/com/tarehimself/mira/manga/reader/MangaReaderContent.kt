package com.tarehimself.mira.manga.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.common.AsyncImage
import com.tarehimself.mira.common.debug


/*@Composable
fun MangaReaderColumn(
    component: MangaReaderComponent,
    chapterId: String,
    constraintsScope: BoxWithConstraintsScope
) {

    val pages: MutableState<List<String>> = remember { mutableStateOf(listOf()) }

    LaunchedEffect(Unit) {

        val pagesResponse = component.loadPagesForIndex(chapterId)

        if (pagesResponse != null) {
            pages.value = pagesResponse
        }
    }

    Column {
        pages.value.forEach { page ->
            Box(modifier = Modifier.fillMaxWidth().padding(0.dp, 10.dp)) {
                AsyncImage(
                    page,
                    contentDescription = "Manga Page",
                    modifier = Modifier.fillMaxWidth(),
                    contentScale = ContentScale.FillWidth,
                    placeholder = {
                        Box(
                            modifier = Modifier.height(constraintsScope.maxHeight)
                                .width(constraintsScope.maxWidth)
                        ) {
                            Text("Loading...", modifier = Modifier.align(Alignment.Center))
                        }
                    })
            }
        }
    }
}*/

//@Composable
//fun MangaReaderContent(component: MangaReaderComponent) {
//
//    val state by component.state.subscribeAsState(neverEqualPolicy())
//
//    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
//        val boxWithConstraintsScope = this
//        LazyColumn(modifier = Modifier.fillMaxSize()) {
//            item {
//                MangaReaderColumn(component, state.initialChapter, boxWithConstraintsScope)
//            }
////            items(state.pages.size,key={
////                state.pages[it]
////            }){
////
////            }
//        }
//    }
//
//}

data class ScrollInfo(
    var index: Int,
    var offset: Int,
)

@Composable
fun LoadingReaderChapterPlaceholder(){
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
fun MangaReaderContent(component: MangaReaderComponent) {

    val state by component.state.subscribeAsState(neverEqualPolicy())

    val scrollState = rememberLazyListState()

    var lastPagesNum by remember { mutableStateOf(state.pages.size) }
    var lastScrollInfo by remember {
        mutableStateOf(
            ScrollInfo(
                scrollState.firstVisibleItemIndex,
                scrollState.firstVisibleItemScrollOffset
            )
        )
    }
    var lastFirstPageItem by remember { mutableStateOf<MangaReaderComponent.ReaderItem?>(null) }

    val restartSnapshotIdx: MutableState<Int> = remember { mutableStateOf(0) }
    var wasLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        wasLoading = true
        component.loadInitialChapter()
        restartSnapshotIdx.value += 1
    }

    LaunchedEffect(restartSnapshotIdx.value) {
        if (state.pages.size > 0) {

            snapshotFlow { scrollState.layoutInfo }.collect { layout ->

                if (state.pages.isNotEmpty() && layout.visibleItemsInfo.isNotEmpty()) {


                    val currentPagesNum = state.pages.size


                    if (lastPagesNum == 0 && currentPagesNum > 0) {
                        scrollState.scrollToItem(1)

                        lastPagesNum = currentPagesNum

                        return@collect
                    }


                    // When we are at the bottom of the scroll
                    val lastItemIndex = layout.visibleItemsInfo.last().index
                    if (lastItemIndex >= state.pages.size - 1 && state.pages[lastItemIndex].chapterIndex != 0 && !state.isLoadingNext) { // latest chapter will have index 0
                        wasLoading = true
                        debug("Loading next chapter")
                        component.loadNextChapter()
                    }


                    // When we are at the top of the scroll
                    val firstItemIndex = layout.visibleItemsInfo.first().index
                    if (firstItemIndex == 0 && state.pages[firstItemIndex].chapterIndex != state.chapters.size - 1 && !state.isLoadingPrevious) { // first chapter will have index (size - 1)
                        wasLoading = true
                        debug("Loading previous Chapter")
                        component.loadPreviousChapter()
                    }

                    if (lastPagesNum != currentPagesNum) {
                        lastFirstPageItem = state.pages.first()
                    }

                    lastPagesNum = currentPagesNum
                }
            }
        }
    }



    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val boxWithConstraintsScope = this
        LazyColumn(modifier = Modifier.fillMaxSize(), state = scrollState) {

            if(state.isLoadingPrevious){
                item {
                    LoadingReaderChapterPlaceholder()
                }
            }
            items(state.pages, key = {
                it.hashCode()
            }) { item ->
                when (item) {
                    is MangaReaderComponent.ReaderChapterItem -> {
                        Pressable(modifier = Modifier.fillMaxWidth().padding(0.dp, 10.dp)) {
                            AsyncImage(
                                item.data,
                                contentDescription = "Manga Page",
                                modifier = Modifier.fillMaxWidth(),
                                contentScale = ContentScale.FillWidth,
                                placeholder = {
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
                                })
                        }
                    }

                    is MangaReaderComponent.ReaderDividerItem -> {
                        Surface(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                            Box(
                                modifier = Modifier.matchParentSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(item.data, modifier = Modifier)
                            }
                        }
                    }
                }
            }

            if(state.isLoadingNext){
                item {
                    LoadingReaderChapterPlaceholder()
                }
            }
        }
    }

}