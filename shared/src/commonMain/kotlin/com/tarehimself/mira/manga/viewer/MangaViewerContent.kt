package com.tarehimself.mira.manga.viewer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.SearchBarContent
import com.tarehimself.mira.VectorImage
import com.tarehimself.mira.common.AsyncImage
import com.tarehimself.mira.common.Constants
import com.tarehimself.mira.common.debug
import com.tarehimself.mira.data.MangaData
import com.tarehimself.mira.data.subscribeLibraryUpdate
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Bookmark
import compose.icons.fontawesomeicons.solid.Bookmark
import compose.icons.fontawesomeicons.solid.Share
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class, ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun MangaViewerContent(component: MangaViewerComponent) {
    val state by component.state.subscribeAsState(neverEqualPolicy())
    val mainContainerSize = remember { mutableStateOf(IntSize.Zero) }
    val scrollState = rememberLazyListState()
    val isBookmarked = subscribeLibraryUpdate({
        it.has(state.sourceId, state.preview.id)
    })
    val pullRefreshState =
        rememberPullRefreshState(state.isLoadingData || state.isLoadingChapters, {
            CoroutineScope(Dispatchers.Default).launch {
                async {
                    component.loadMangaData()
                }
                async {
                    component.loadChapters()
                }
            }
        })


    var backgroundAlpha by remember { mutableStateOf(0.0f) }

    val brush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colors.background.copy(alpha = .3f),
            MaterialTheme.colors.background
        ),
        startY = (0).toFloat(),
        endY = mainContainerSize.value.height.toFloat() / 2
    )

    LaunchedEffect(state.preview.id) {
        async {
            component.loadMangaData()
        }
        async {
            component.loadChapters()
        }
    }

    LaunchedEffect(Unit) {
        var lastFirstIndex = 0
        var lastFirstIndexSize = 0
        var lastFirstIndexOffset = 0

        snapshotFlow { scrollState.layoutInfo }.collect {

            if (it.visibleItemsInfo.isNotEmpty()) {
                val firstItem = it.visibleItemsInfo.first()
                backgroundAlpha = if (firstItem.index != 0) {
                    0.0f
                } else {
                    when (firstItem.offset == 0) {
                        true -> 1.0f
                        else -> 1.0f - (firstItem.offset.toFloat() * -1 / firstItem.size.toFloat())
                    }
                }
            }
        }
    }


    val commonModifier = Modifier.padding(vertical = 5.dp, horizontal = 20.dp)
    Box(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize().alpha(backgroundAlpha)) {
            AsyncImage(
                source = when (state.data != null) {
                    true -> state.data!!.cover
                    false -> state.preview.cover
                },
                contentDescription = "Big Background",
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f),
                contentScale = ContentScale.Crop
            )

            Box(modifier = Modifier.matchParentSize().background(brush))


        }

        Scaffold(topBar = {
            Spacer(modifier = Modifier.fillMaxWidth().height(70.dp))
        }, modifier = Modifier.fillMaxSize().onGloballyPositioned {
            mainContainerSize.value = it.size
        },
            floatingActionButton = {
                if (state.chapters.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = {
                            component.readChapter(state.chapters.size - 1)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(50.dp).padding(horizontal = 5.dp),
                        contentColor = Color.White
                    ) {
                        Text("Read")
                    }
                }
            }, backgroundColor = Color.Transparent
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(state = scrollState, modifier = Modifier.pullRefresh(pullRefreshState)) {
                    // Manga Cover and Title
                    item {
                        Row(
                            modifier = Modifier.then(commonModifier).fillMaxWidth().height(160.dp)
                        ) {
                            AsyncImage(
                                source = state.preview.cover,
                                contentDescription = "Manga Cover",
                                modifier = Modifier.fillMaxHeight().aspectRatio(
                                    Constants.mangaCoverRatio, matchHeightConstraintsFirst = true
                                ).clip(shape = RoundedCornerShape(5.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Column(
                                modifier = Modifier.fillMaxHeight().weight(1.0f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(state.preview.name, fontSize = 20.sp, maxLines = 3)
                                Spacer(modifier = Modifier.width(20.dp))
                                if (state.data != null) {
                                    Text(state.data!!.status)
                                }
                            }
                        }
                    }

                    // Bookmark and Share buttons
                    item {
                        Row(
                            modifier = Modifier.then(commonModifier).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconModifier = Modifier.size(18.dp)
                            val iconPressable = Modifier.width(100.dp)
                            val textFontSize = 13.sp

                            if (state.data is MangaData) {
                                Pressable(onClick = {
                                    debug("Bookmark Clicked")
                                    if (isBookmarked) {
                                        component.realmDatabase.removeFromLibrary(
                                            state.sourceId,
                                            state.data!!.id
                                        )
                                    } else {
                                        component.realmDatabase.addToLibrary(
                                            state.sourceId,
                                            state.data!!,
                                            state.chapters
                                        )

                                    }
                                }, modifier = Modifier.then(iconPressable)) {
                                    Column(
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Spacer(modifier = Modifier.height(5.dp))
                                        VectorImage(
                                            vector = when (isBookmarked) {
                                                true -> FontAwesomeIcons.Solid.Bookmark
                                                else -> FontAwesomeIcons.Regular.Bookmark
                                            },
                                            contentDescription = "Bookmark Icon",
                                            modifier = iconModifier
                                        )
                                        Spacer(modifier = Modifier.height(5.dp))
                                        Text("Bookmark", fontSize = textFontSize)
                                        Spacer(modifier = Modifier.height(5.dp))
                                    }
                                }
                            }

                            Pressable(onClick = {
                                debug("Share Clicked")
                            }, modifier = Modifier.then(iconPressable)) {
                                Column(
                                    verticalArrangement = Arrangement.SpaceBetween,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Spacer(modifier = Modifier.height(5.dp))
                                    VectorImage(
                                        vector = FontAwesomeIcons.Solid.Share,
                                        contentDescription = "Share Icon",
                                        modifier = iconModifier
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Text("Share")
                                    Spacer(modifier = Modifier.height(5.dp))
                                }
                            }
                        }
                    }

                    when (state.data) {
                        is MangaData -> {
                            val mangaData = state.data!!
                            // Description
                            if (mangaData.description.isNotEmpty()) {
                                item {
                                    Box(modifier = Modifier.then(commonModifier)) {
                                        Text(mangaData.description, fontSize = 15.sp)
                                    }
                                }
                            }

                            // Tags
                            if (mangaData.tags.isNotEmpty()) {
                                item {
                                    val tagHorizontalSpacing = 5.dp
                                    FlowRow(
                                        modifier = Modifier.padding(horizontal = 20.dp - tagHorizontalSpacing),
                                        horizontalArrangement = Arrangement.Start
                                    ) {
                                        mangaData.tags.forEach { tag ->
                                            Box(
                                                modifier = Modifier.padding(
                                                    vertical = 5.dp,
                                                    horizontal = tagHorizontalSpacing
                                                )
                                            ) {
                                                Pressable(
                                                    onClick = {
                                                        component.searchForTag(tag)
                                                    },
                                                    modifier = Modifier.clip(
                                                        shape = RoundedCornerShape(
                                                            5.dp
                                                        )
                                                    ),
                                                    backgroundColor = Color.DarkGray
                                                ) {

                                                    Box(
                                                        modifier = Modifier.padding(
                                                            vertical = 5.dp,
                                                            horizontal = 7.dp
                                                        )
                                                    ) {
                                                        Text(tag, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }


                        }

                        else -> {

                        }
                    }

                    // Chapter count and divider
                    item {
                        Column {
                            Row(modifier = Modifier.then(commonModifier)) {
                                Text("${state.chapters.size} chapters")
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.then(commonModifier).fillMaxWidth().height(2.dp)
                            ) {

                            }
                        }
                    }

                    // Chapters
                    items(state.chapters.size) { idx ->
                        MangaChapterContent(state.chapters[idx], onChapterSelected = {
                            component.readChapter(idx)
                        })
                    }
                }
                PullRefreshIndicator(
                    state.isLoadingData || state.isLoadingChapters,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }

}