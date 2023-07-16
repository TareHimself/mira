package com.tarehimself.mangaz.manga.reader

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mangaz.common.AsyncImage


@Composable
fun MangaReaderColumn(
    component: MangaReaderComponent,
    chapterId: String,
    constraintsScope: BoxWithConstraintsScope
) {

    val pages: MutableState<List<String>> = remember { mutableStateOf(listOf()) }

    LaunchedEffect(Unit) {

        val pagesResponse = component.loadPages(chapterId)

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
}

@Composable
fun MangaReaderContent(component: MangaReaderComponent) {

    val state by component.state.subscribeAsState(neverEqualPolicy())

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val boxWithConstraintsScope = this
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                MangaReaderColumn(component, state.initialChapter, boxWithConstraintsScope)
            }
//            items(state.pages.size,key={
//                state.pages[it]
//            }){
//
//            }
        }
    }

}