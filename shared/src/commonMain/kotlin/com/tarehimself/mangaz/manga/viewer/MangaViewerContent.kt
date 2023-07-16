package com.tarehimself.mangaz.manga.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mangaz.common.AsyncImage
import com.tarehimself.mangaz.common.Constants
import com.tarehimself.mangaz.manga.chapter.MangaChapterContent
import com.tarehimself.mangaz.screens.SearchResult

@Composable
fun MangaViewerContent(component: MangaViewerComponent) {
    val state by component.state.subscribeAsState(neverEqualPolicy())

    LaunchedEffect(state.preview.id){
        component.loadChapters()
    }

    Surface(modifier = Modifier.fillMaxSize().padding(20.dp,0.dp)) {
        Column{
            Spacer(modifier = Modifier.fillMaxWidth().height(70.dp))
            LazyColumn {
                item {
                    Row(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                        AsyncImage(
                            source = when (state.data != null) {
                                true -> state.data!!.cover
                                false -> state.preview.cover
                            },
                            contentDescription = "Manga Cover",
                            modifier = Modifier.fillMaxHeight().aspectRatio(Constants.mangaCoverRatio, matchHeightConstraintsFirst = true).clip(shape = RoundedCornerShape(5.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(20.dp))
                        Column(modifier = Modifier.fillMaxHeight().weight(1.0f), verticalArrangement = Arrangement.Center) {
                            Text(state.preview.name, fontSize = 24.sp)
                            if(state.data != null){
                                Text(state.data!!.status)
                            }
                        }
                    }
                }

                items(state.chapters.size) { idx ->
                    MangaChapterContent(state.chapters[idx], onChapterSelected = {
                        component.readChapter(it)
                    })
                }
            }
        }
    }
}