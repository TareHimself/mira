package com.tarehimself.mira.manga.viewer

import DropdownMenu
import DropdownMenuItem
import ShareBridge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.VectorImage
import com.tarehimself.mira.common.pxToDp
import com.tarehimself.mira.common.ui.AsyncImage
import com.tarehimself.mira.common.ui.CategorySelectContent
import com.tarehimself.mira.common.ui.SelectableContent
import com.tarehimself.mira.common.ui.rememberCategorySelectContentState
import com.tarehimself.mira.common.ui.rememberNetworkImagePainter
import com.tarehimself.mira.common.ui.rememberSelectableContentState
import com.tarehimself.mira.data.ChapterDownloader
import com.tarehimself.mira.data.MangaData
import com.tarehimself.mira.data.StoredManga
import com.tarehimself.mira.data.rememberCategories
import com.tarehimself.mira.data.rememberIsBookmarked
import com.tarehimself.mira.data.rememberReadInfo
import compose.icons.EvaIcons
import compose.icons.FontAwesomeIcons
import compose.icons.Octicons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.Outline
import compose.icons.evaicons.fill.Download
import compose.icons.evaicons.outline.DoneAll
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Bookmark
import compose.icons.fontawesomeicons.solid.ArrowDown
import compose.icons.fontawesomeicons.solid.Bookmark
import compose.icons.fontawesomeicons.solid.Tags
import compose.icons.octicons.Share24
import compose.icons.octicons.X16
import io.github.aakira.napier.Napier
import io.ktor.client.request.header
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun MangaViewerContentMainButton(name: String, icon: ImageVector, onClick: () -> Unit = {}) {

    Pressable(onClick = onClick, modifier = Modifier.width(100.dp).clip(RoundedCornerShape(5.dp))) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(5.dp))
            VectorImage(
                vector = icon,
                contentDescription = "$name Icon",
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(name, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

@OptIn(
    ExperimentalLayoutApi::class, ExperimentalMaterialApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun MangaViewerContent(
    component: MangaViewerComponent,
    chapterDownloader: ChapterDownloader = koinInject()
) {
    val state by component.state.subscribeAsState(neverEqualPolicy())

    val readInfo = rememberReadInfo(state.sourceId, state.preview.id)

    val isBookmarked by rememberIsBookmarked(state.sourceId, state.preview.id)

    val selectableContentState = rememberSelectableContentState<Int>()

    val scrollState = rememberLazyListState()

    val categorySelectState = rememberCategorySelectContentState()

    val allCategories by rememberCategories()

    val verticalPadding = remember { 5.dp }

    val horizontalPadding = remember { 20.dp }

    var backgroundImageHeight by remember { mutableStateOf(IntSize.Zero) }

    var minDescriptionHeight by remember { mutableStateOf(0) }

    var desiredDescriptionHeight by remember { mutableStateOf(1) }

    val minDescriptionHeightDp = minDescriptionHeight.pxToDp()

    val desiredDescriptionHeightDp = desiredDescriptionHeight.pxToDp()

    val willDescriptionOverflow = remember { desiredDescriptionHeight > minDescriptionHeight }

    var isDescriptionOpen by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current
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
                launch {
                    component.loadMangaData(true)
                }

                launch {
                    component.loadChapters(true)
                }

            }
        })


    var backgroundAlpha by remember { mutableStateOf(0.0f) }

    val backgroundBrushColor = MaterialTheme.colorScheme.background

    val backgroundBrush = remember(backgroundImageHeight.height) {
        Brush.verticalGradient(
            colors = listOf(
                backgroundBrushColor.copy(alpha = .5f),
                backgroundBrushColor.copy(alpha = .7f),
                backgroundBrushColor
            ), endY = backgroundImageHeight.height.toFloat() / 1.5f
        )
    }

    val descriptionBrush = remember(backgroundBrushColor) {
        Brush.verticalGradient(
            listOf(
                Color.Transparent,
                backgroundBrushColor.copy(alpha = 0.7f),
                backgroundBrushColor.copy(alpha = 0.9f)
            )
        )
    }

    val commonModifier = remember(verticalPadding, horizontalPadding) {
        Modifier.padding(vertical = verticalPadding, horizontal = horizontalPadding)
    }

    val descriptionFontSize = remember { 15.sp }

    val coroutineScope = rememberCoroutineScope()

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

    LaunchedEffect(Unit) {
        launch {
            if (state.data !is StoredManga) {
                component.loadMangaData()
            }
        }

        launch {
            if (state.chapters.isEmpty()) {
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

        // Big background image
        Box(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.5f).alpha(backgroundAlpha)
                .onGloballyPositioned {
                    backgroundImageHeight = it.size
                }) {

            val targetCover = remember(state.data) {
                when (state.data != null) {
                    true -> state.data!!.cover
                    false -> state.preview.cover
                }
            }
            Napier.d { "USING COVER ${targetCover?.src}" }
            AsyncImage(
                painter = rememberNetworkImagePainter(
                    targetCover!!.src
                ) {
                    targetCover.headers.forEach {
                        header(it.key, it.value)
                    }
                },
                contentDescription = "Big Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            Box(modifier = Modifier.matchParentSize().background(backgroundBrush))
        }

        val topBarMainModifier = remember { Modifier.fillMaxWidth().height(70.dp) }

        CategorySelectContent(state = categorySelectState) {
            SelectableContent(
                state = selectableContentState,
                topBar = {
                    Box(modifier = topBarMainModifier) {
                        Row(
                            modifier = Modifier.matchParentSize().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box {
                                var shouldShowContextMenu by remember { mutableStateOf(false) }
                                Pressable(onClick = {
                                    shouldShowContextMenu = true
                                }) {
                                    VectorImage(
                                        modifier = Modifier.size(30.dp),
                                        vector = EvaIcons.Fill.Download,
                                        contentDescription = "Download Manga"
                                    )
                                }
                                DropdownMenu(
                                    expanded = shouldShowContextMenu,
                                    onDismissRequest = { shouldShowContextMenu = false }) {
                                    DropdownMenuItem(text = {
                                        Text("All")
                                    }, onClick = {
                                        shouldShowContextMenu = false
                                        coroutineScope.launch {
                                            state.chapters.reversed().forEach {

                                                chapterDownloader.downloadChapter(
                                                    state.sourceId,
                                                    state.preview.id,
                                                    it.id,
                                                    "${state.preview.name} | ${it.name}"
                                                )
                                            }
                                        }

                                    })
                                    DropdownMenuItem(text = {
                                        Text("Not Downloaded")
                                    }, onClick = {
                                        shouldShowContextMenu = false
                                        coroutineScope.launch {
                                            state.chapters.reversed().forEach {
                                                if (!chapterDownloader.isDownloaded(
                                                        state.sourceId,
                                                        state.preview.id,
                                                        it.id
                                                    )
                                                ) {
                                                    chapterDownloader.downloadChapter(
                                                        state.sourceId,
                                                        state.preview.id,
                                                        it.id,
                                                        "${state.preview.name} | ${it.name}"
                                                    )
                                                }

                                            }
                                        }

                                    })
//                                    DropdownMenuItem(text = {
//                                        Text("Next 10 Chapters")
//                                    }, onClick = {
//
//                                    })
                                }
//                                androidx.compose.material.MaterialTheme {
//                                    DropdownMenu(expanded = shouldShowContextMenu, onDismissRequest = { shouldShowContextMenu = false}){
//                                        DropdownMenuItem(text = {
//                                            Text("All Chapters")
//                                        }, onClick = {
//
//                                        })
//                                        DropdownMenuItem(text = {
//                                            Text("Next 10 Chapters")
//                                        }, onClick = {
//
//                                        })
//                                    }
//                                }

                            }
                        }
                    }
                },
                topSheetContent = {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        modifier = topBarMainModifier,
                        tonalElevation = ModalBottomSheetDefaults.Elevation
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
                modifier = Modifier.fillMaxSize(),
                bottomSheetContent = {

                    Surface(
                        modifier = Modifier.height(80.dp).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = ModalBottomSheetDefaults.Elevation
                    ) {
                        val vectorModifier = remember { Modifier.size(25.dp) }
                        val pressableModifier = remember { Modifier.clip(RoundedCornerShape(5.dp)) }
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Pressable(onClick = {
                                coroutineScope.launch {
                                    selectableContentState.collapse()
                                }
                            }, modifier = pressableModifier) {
                                VectorImage(
                                    Octicons.X16,
                                    modifier = vectorModifier,
                                    contentDescription = "Cancel Action"
                                )
                            }
                            Pressable(onClick = {
                                coroutineScope.launch {
                                    selectableContentState.selectedItems.forEach {
                                        component.realmDatabase.markChapterAsRead(
                                            state.sourceId,
                                            state.preview.id,
                                            it
                                        )
                                    }
                                    selectableContentState.collapse()
                                }
                            }, modifier = pressableModifier) {
                                VectorImage(
                                    EvaIcons.Outline.DoneAll,
                                    modifier = vectorModifier,
                                    contentDescription = "Mark Read Action"
                                )
                            }
                            Pressable(onClick = {

                                coroutineScope.launch {
                                    selectableContentState.selectedItems.forEach {
                                        val targetIdx = state.chapters.lastIndex - it
                                        chapterDownloader.downloadChapter(
                                            state.sourceId,
                                            state.preview.id,
                                            state.chapters[targetIdx].id,
                                            "${component.state.value.preview.name} | ${state.chapters[targetIdx].name}"
                                        )
                                    }
                                    selectableContentState.collapse()
                                }
                            }, modifier = pressableModifier) {
                                VectorImage(
                                    FontAwesomeIcons.Solid.ArrowDown,
                                    modifier = vectorModifier,
                                    contentDescription = "Download Chapters"
                                )
                            }
                        }
                    }
                },
            ) {
                Box(modifier = Modifier.fillMaxSize()) {

                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.pullRefresh(pullRefreshState),
                    ) {
                        // Manga Cover and Title
                        item {
                            Row(
                                modifier = Modifier.then(commonModifier).fillMaxWidth()
                                    .height(160.dp)
                            ) {
//                                AsyncImage(
//                                    painter = rememberNetworkImagePainter(
//                                        state.preview.cover!!.src,
//                                        filterQuality = FilterQuality.None
//                                    ) {
//                                        state.preview.cover!!.headers.forEach {
//                                            header(it.key, it.value)
//                                        }
//                                    },
//                                    contentDescription = "Manga Cover",
//                                    modifier = Modifier.fillMaxHeight().aspectRatio(
//                                        Constants.mangaCoverRatio,
//                                        matchHeightConstraintsFirst = true
//                                    ).clip(shape = RoundedCornerShape(5.dp)),
//                                    contentScale = ContentScale.Crop,
//
//                                    onLoading = {
//                                        Box(
//                                            modifier = Modifier.fillMaxHeight().aspectRatio(
//                                                Constants.mangaCoverRatio,
//                                                matchHeightConstraintsFirst = true
//                                            ), contentAlignment = Alignment.Center
//                                        ) {
//                                            CircularProgressIndicator(it.progress.value)
//                                        }
//                                    },
//                                )

                                Spacer(modifier = Modifier.width(20.dp))

                                Column(
                                    modifier = Modifier.fillMaxHeight().weight(1.0f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        state.preview.name,
                                        fontSize = 23.sp,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.combinedClickable(
                                            enabled = true,
                                            onClickLabel = null,
                                            onLongClickLabel = "copy name",
                                            onClick = {},
                                            onLongClick = {
                                                clipboardManager.setText(AnnotatedString(state.preview.name))
                                            },
                                            role = Role.Button,
                                            indication = null,
                                            interactionSource = remember { MutableInteractionSource() }
                                        )
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

                                state.data?.let {
                                    MangaViewerContentMainButton(name = "Bookmark",
                                        icon = when (isBookmarked) {
                                            true -> FontAwesomeIcons.Solid.Bookmark
                                            else -> FontAwesomeIcons.Regular.Bookmark
                                        },
                                        onClick = {
                                            if (isBookmarked) {
                                                coroutineScope.launch {
                                                    component.unBookmark()
                                                    component.loadChapters(true)
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    component.bookmark()
                                                    if (allCategories.isNotEmpty()) {
                                                        categorySelectState.selectCategories(
                                                            state.sourceId,
                                                            state.preview.id
                                                        )
                                                    }
                                                }

                                            }
                                        })

                                    if (isBookmarked) {
                                        MangaViewerContentMainButton(
                                            name = "Categories",
                                            icon = FontAwesomeIcons.Solid.Tags,
                                            onClick = {
                                                coroutineScope.launch {
                                                    categorySelectState.selectCategories(
                                                        state.sourceId,
                                                        state.preview.id
                                                    )
                                                }
                                            })
                                    }

                                    MangaViewerContentMainButton(
                                        name = "Share",
                                        icon = Octicons.Share24,
                                        onClick = {
                                            ShareBridge.shareText(it.share)
                                        })
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
                                    val targetFirstChapter =
                                        state.chapters.lastIndex - (readInfo?.current?.index
                                            ?: 0)
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                                    ) {
                                        Pressable(
                                            onClick = {
                                                if (!state.isLoadingChapters) {
                                                    component.readChapter(targetFirstChapter)
                                                }
                                            },
                                            modifier = Modifier.clip(shape = RoundedCornerShape(5.dp))
                                                .width(100.dp).height(30.dp)
                                                .align(Alignment.Center),
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
                        items(state.chapters.size, key = { state.chapters[it].id }) { idx ->
                            val selectionIdx = state.chapters.lastIndex - idx
                            MangaChapterContent(
                                component = component,
                                index = idx,
                                total = state.chapters.size,
                                data = state.chapters[idx],
                                selectedState = selectableContentState.selectedItems,
                                onChapterSelected = {
                                    if (!selectableContentState.isExpanded && !selectableContentState.isExpanding) {
                                        if (!state.isLoadingChapters) {
                                            component.readChapter(idx)
                                        }

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

}