package com.tarehimself.mira.manga.viewer

import ShareBridge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.SelectableContent
import com.tarehimself.mira.VectorImage
import com.tarehimself.mira.common.AsyncImage
import com.tarehimself.mira.common.Constants
import com.tarehimself.mira.common.networkImagePainter
import com.tarehimself.mira.common.pxToDp
import com.tarehimself.mira.data.MangaData
import com.tarehimself.mira.data.StoredManga
import com.tarehimself.mira.data.rememberIsBookmarked
import com.tarehimself.mira.data.rememberReadInfo
import com.tarehimself.mira.rememberSelectableContentState
import compose.icons.FontAwesomeIcons
import compose.icons.Octicons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Bookmark
import compose.icons.fontawesomeicons.solid.Bookmark
import compose.icons.octicons.Download16
import compose.icons.octicons.Share24
import compose.icons.octicons.X16
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch


@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterialApi::class)
@Composable
fun MangaViewerContent(component: MangaViewerComponent) {
    val state by component.state.subscribeAsState(neverEqualPolicy())

    val readInfo = rememberReadInfo(state.sourceId,state.preview.id)

    val isBookmarked = rememberIsBookmarked(state.sourceId,state.preview.id)

    val verticalPadding by remember { mutableStateOf(5.dp) }

    val horizontalPadding by remember { mutableStateOf(20.dp) }

    val mainContainerSize = remember { mutableStateOf(IntSize.Zero) }

    val scrollState = rememberLazyListState()

    var minDescriptionHeight by remember { mutableStateOf(0) }

    var desiredDescriptionHeight by remember { mutableStateOf(1) }

    val minDescriptionHeightDp = minDescriptionHeight.pxToDp()

    val desiredDescriptionHeightDp = desiredDescriptionHeight.pxToDp()

    val willDescriptionOverflow = remember { desiredDescriptionHeight > minDescriptionHeight }

    val selectableContentState = rememberSelectableContentState<Int>()

    var isDescriptionOpen by remember { mutableStateOf(false) }

    val descriptionHeight by animateDpAsState(
        if (!willDescriptionOverflow) {
            desiredDescriptionHeightDp
        } else if (isDescriptionOpen) {
            desiredDescriptionHeightDp + minDescriptionHeightDp
        } else if (desiredDescriptionHeight <= minDescriptionHeight) {
            desiredDescriptionHeightDp + minDescriptionHeightDp
        } else {
            minDescriptionHeightDp
        }, animationSpec = tween(200)
    )

    val pullRefreshState =
        rememberPullRefreshState(state.isLoadingData || state.isLoadingChapters, {
            CoroutineScope(Dispatchers.Default).launch {
                async {
                    component.loadMangaData(true)
                }
                async {
                    component.loadChapters(true)
                }

            }
        })


    var backgroundAlpha by remember { mutableStateOf(0.0f) }

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color.Transparent,
            MaterialTheme.colors.background.copy(alpha = .7f),
            MaterialTheme.colors.background
        ), startY = (0).toFloat(), endY = mainContainerSize.value.height.toFloat() / 3
    )

    val descriptionBrush = Brush.verticalGradient(
        listOf(
            Color.Transparent,
            MaterialTheme.colors.background.copy(alpha = 0.7f),
            MaterialTheme.colors.background.copy(alpha = 0.9f)
        )
    )

    val coroutineScope = rememberCoroutineScope()

    val commonModifier =
        Modifier.padding(vertical = verticalPadding, horizontal = horizontalPadding)

    val descriptionFontSize = remember { 15.sp }

    LaunchedEffect(Unit) {
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

    LaunchedEffect(state.preview.id) {
        if(state.data !is StoredManga){
            async {
                component.loadMangaData()
            }
        }

        if (state.chapters.isEmpty()) {
            async {
                component.loadChapters()
            }
        }
    }



    Box(modifier = Modifier.fillMaxSize()) {

        if (state.data != null) { // dummy for height calculations
            Box(
                modifier = Modifier.padding(horizontal = horizontalPadding).alpha(0.0f)
            ) {
                Text(
                    "\n", modifier = Modifier.fillMaxWidth().onGloballyPositioned {
                        minDescriptionHeight = it.size.height
                    }, fontSize = descriptionFontSize
                )
                Text(
                    state.data!!.description, modifier = Modifier.onGloballyPositioned {
                        desiredDescriptionHeight = it.size.height
                    }, fontSize = descriptionFontSize
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize().alpha(backgroundAlpha)) {

            AsyncImage(
                painter = networkImagePainter(when (state.data != null) {
                    true -> state.data!!.cover
                    false -> state.preview.cover
                }),
                contentDescription = "Big Background",
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f),
                contentScale = ContentScale.Crop
            )

            Box(modifier = Modifier.matchParentSize().background(backgroundBrush))
        }
        val topBarMainModifier = Modifier.fillMaxWidth().height(70.dp)
        SelectableContent(
            state = selectableContentState,
            topBar = {
                Box(modifier = topBarMainModifier) {

                }
            },
            topSheetContent = {
                Surface(
                    color = MaterialTheme.colors.surface,
                    modifier = topBarMainModifier,
                    elevation = ModalBottomSheetDefaults.Elevation
                ) {
                    Row(
                        modifier = Modifier.matchParentSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text(
                            "${selectableContentState.selectedItems.size} Selected",
                            fontSize = 20.sp
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize().onGloballyPositioned {
                mainContainerSize.value = it.size
            },
            bottomSheetContent = {

                Surface(
                    modifier = Modifier.height(80.dp).fillMaxWidth(),
                    color = MaterialTheme.colors.surface,
                    elevation = ModalBottomSheetDefaults.Elevation
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Pressable(onClick = {
                            coroutineScope.launch {
                                selectableContentState.collapse()
                            }
                        }) {
                            VectorImage(
                                Octicons.X16,
                                modifier = Modifier.size(40.dp),
                                contentDescription = "Cancel Action"
                            )
                        }
                        Pressable(onClick = {

                        }) {
                            VectorImage(
                                Octicons.Download16,
                                modifier = Modifier.size(40.dp),
                                contentDescription = "Download Chapters"
                            )
                        }
                    }
                }
            },
        ) {
            Box(modifier = Modifier.fillMaxSize()) {

                LazyColumn(state = scrollState, modifier = Modifier.pullRefresh(pullRefreshState)) {
                    // Manga Cover and Title
                    item {
                        Row(
                            modifier = Modifier.then(commonModifier).fillMaxWidth().height(160.dp)
                        ) {
                            AsyncImage(
                                painter = networkImagePainter(state.preview.cover, filterQuality = FilterQuality.None),
                                contentDescription = "Manga Cover",
                                modifier = Modifier.fillMaxHeight().aspectRatio(
                                    Constants.mangaCoverRatio, matchHeightConstraintsFirst = true
                                ).clip(shape = RoundedCornerShape(5.dp)),
                                contentScale = ContentScale.Crop,

                                onLoading = {
                                    Box(contentAlignment = Alignment.Center){
                                        CircularProgressIndicator(it.progress.value)
                                    }
                                },
                            )

                            Spacer(modifier = Modifier.width(20.dp))

                            Column(
                                modifier = Modifier.fillMaxHeight().weight(1.0f),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    state.preview.name,
                                    fontSize = 23.sp,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(20.dp))
                                if (state.data != null) {
                                    Text(
                                        state.data!!.status,
                                        fontSize = 16.sp,
                                        color = Color.White.copy(alpha = 0.7f)
                                    )
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

                            state.data?.let {
                                Pressable(onClick = {
                                    if (isBookmarked) {
                                        coroutineScope.launch {
                                            component.unBookmark()
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            component.bookmark()
                                        }

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

                                Pressable(onClick = {
                                    ShareBridge.shareText(it.share)
                                }, modifier = Modifier.then(iconPressable)) {
                                    Column(
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Spacer(modifier = Modifier.height(5.dp))
                                        VectorImage(
                                            vector = Octicons.Share24,
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
                    }

                    when (state.data) {
                        is MangaData -> {
                            val mangaData = state.data!!
                            // Description
                            if (mangaData.description.isNotEmpty()) {
                                item {
                                    Pressable(onClick = {
                                        isDescriptionOpen = !isDescriptionOpen
                                    }) {
                                        Box(modifier = Modifier.then(commonModifier)
                                            .height(descriptionHeight).fillMaxWidth()
                                            .graphicsLayer {
                                                clip = true
                                            }) {

                                            Text(
                                                mangaData.description,
                                                modifier = Modifier.fillMaxWidth(),
                                                fontSize = descriptionFontSize,
                                            )

                                            if (willDescriptionOverflow) {
                                                Canvas(modifier = Modifier.height(
                                                    minDescriptionHeightDp / 2
                                                ).align(Alignment.BottomStart).fillMaxWidth(),
                                                    onDraw = {
                                                        drawRect(descriptionBrush)
                                                    })
                                            }
                                        }
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
                                                    }, modifier = Modifier.clip(
                                                        shape = RoundedCornerShape(
                                                            5.dp
                                                        )
                                                    ), backgroundColor = Color.DarkGray
                                                ) {

                                                    Box(
                                                        modifier = Modifier.padding(
                                                            vertical = 5.dp, horizontal = 7.dp
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
                        Column(modifier = Modifier.then(commonModifier)) {
                            if (state.chapters.isNotEmpty()) {
                                val targetFirstChapter = when (val curData = state.data) {
                                    is StoredManga -> {
                                        state.chapters.lastIndex - (readInfo?.current?.index ?: 0)
                                    }

                                    else -> {
                                        state.chapters.lastIndex
                                    }
                                }
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                                    Pressable(
                                        onClick = {
                                            component.readChapter(targetFirstChapter)
                                        },
                                        modifier = Modifier.clip(shape = RoundedCornerShape(5.dp))
                                            .width(100.dp).height(30.dp).align(Alignment.Center),
                                        backgroundColor = Color.DarkGray
                                    ) {
                                        Box {
                                            Text(
                                                if (targetFirstChapter == state.chapters.lastIndex) {
                                                    "Read"
                                                } else {
                                                    "Resume"
                                                }, modifier = Modifier.align(Alignment.Center)
                                            )
                                            Spacer(modifier = Modifier.height(verticalPadding))
                                        }
                                    }
                                }
                            }
                            Row {
                                Text("${state.chapters.size} chapters")
                            }
                            Surface(
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth().height(2.dp)
                            ) {

                            }
                        }
                    }

                    // Chapters
                    items(state.chapters.size) { idx ->
                        val selectionIdx = state.chapters.lastIndex - idx
                        MangaChapterContent(
                            component = component,
                            index = idx,
                            total = state.chapters.size,
                            data = state.chapters[idx],
                            sourceId = state.sourceId,
                            mangaId = state.preview.id,
                            isSelected = selectableContentState.isSelected(selectionIdx),
                            onChapterSelected = {
                                if (!selectableContentState.isExpanded && !selectableContentState.isExpanding) {
                                    component.readChapter(idx)
                                } else {
                                    if (selectableContentState.isSelected(selectionIdx)) {
                                        selectableContentState.deselect(selectionIdx)
                                    } else {
                                        selectableContentState.select(selectionIdx)
                                    }
                                }

                            },
                            onChapterLongPressed = {
                                if (selectableContentState.isCollapsed) {
                                    selectableContentState.expand(listOf(selectionIdx))
                                }
                            })
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
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