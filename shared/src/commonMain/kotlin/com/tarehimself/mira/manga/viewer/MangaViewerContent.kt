package com.tarehimself.mira.manga.viewer

import DropdownMenu
import DropdownMenuItem
import FileBridge
import ShareBridge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.common.ECacheType
import com.tarehimself.mira.common.ui.Pressable
import com.tarehimself.mira.common.ui.VectorImage
import com.tarehimself.mira.common.LocalWindowInsets
import com.tarehimself.mira.common.borderRadius
import com.tarehimself.mira.common.pxToDp
import com.tarehimself.mira.common.ui.AsyncImage
import com.tarehimself.mira.common.ui.CategorySelectContent
import com.tarehimself.mira.common.ui.SelectableContent
import com.tarehimself.mira.common.ui.rememberCategorySelectContentState
import com.tarehimself.mira.common.ui.rememberCoverPainter
import com.tarehimself.mira.common.ui.rememberSelectableContentState
import com.tarehimself.mira.data.ChapterDownloader
import com.tarehimself.mira.data.MangaData
import com.tarehimself.mira.data.MangaPreview
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
import compose.icons.evaicons.fill.MoreVertical
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun MangaViewerContentMainButton(name: String, icon: ImageVector, onClick: () -> Unit = {}) {

    Pressable(onClick = onClick, modifier = Modifier.width(100.dp).borderRadius(5.dp)) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(5.dp))
            VectorImage(
                vector = icon, contentDescription = "$name Icon", modifier = Modifier.size(18.dp), color = contentColorFor(MaterialTheme.colorScheme.background)
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(name, fontSize = 13.sp,color = contentColorFor(MaterialTheme.colorScheme.background))
            Spacer(modifier = Modifier.height(5.dp))
        }
    }
}

@Composable
fun BackgroundImage(
    preview: MangaPreview, data: MangaData?, modifier: Modifier, onDisposed: () -> Unit = {}
) {

    DisposableEffect(Unit) {

        onDispose {
            onDisposed()
        }
    }
    Box(modifier = modifier) {

        val targetCover = remember(preview, data) {
            when (data != null) {
                true -> data.cover
                false -> preview.cover
            }
        }

        AsyncImage(
            asyncPainter = rememberCoverPainter {
                fromMangaImage(targetCover!!)
            },
            contentDescription = "Big Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )

        Box(
            modifier = Modifier.matchParentSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.1f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
        )
    }
}

@OptIn(
    ExperimentalLayoutApi::class, ExperimentalMaterialApi::class, ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class
)
@Composable
fun MangaViewerContent(
    component: MangaViewerComponent, chapterDownloader: ChapterDownloader = koinInject()
) {
    val state by component.state.subscribeAsState(neverEqualPolicy())

    val verticalPadding = remember { 5.dp }

    val horizontalPadding = remember { 20.dp }

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

    val scrollState = rememberLazyListState()

    val backgroundBrushColor = MaterialTheme.colorScheme.background

    val descriptionFontSize = remember { 15.sp }

    val coroutineScope = rememberCoroutineScope()

    var topItemYPosition by remember { mutableStateOf(0.0f) }

    var topItemHeight by remember { mutableStateOf(1) }

    val chapters =
        remember(state.chapters) { state.chapters }

    val readInfoState = rememberReadInfo(state.sourceId, state.preview.id)

    val readInfo by readInfoState

    val isBookmarked by rememberIsBookmarked(state.sourceId, state.preview.id)

    val selectableContentState = rememberSelectableContentState<Int>()

    val categorySelectState = rememberCategorySelectContentState(
        sourceId = state.sourceId, mangaId = state.preview.id
    )

    val allCategories by rememberCategories()


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

    LaunchedEffect(Unit){
        snapshotFlow { scrollState.layoutInfo }.collect{
            if(it.visibleItemsInfo.isEmpty()){
                return@collect
            }

            it.visibleItemsInfo.firstOrNull()?.let {
                item ->
                if(item.index != 0 && topItemHeight <= 0){
                    topItemYPosition = (topItemHeight * -1).toFloat()
                }
            }
        }
    }
    LaunchedEffect(Unit){
        withContext(Dispatchers.IO){
            FileBridge.clearCache(type = ECacheType.Reader)
        }
    }

    val windowInsets = LocalWindowInsets.current

    val density = LocalDensity.current

//    Scaffold {scaffoldPadding ->
//
//    }
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

        // Content

        val topBarMainModifier = remember { Modifier.fillMaxWidth().height(70.dp) }

        val commonModifier = remember(verticalPadding, horizontalPadding) {
            Modifier.padding(vertical = verticalPadding, horizontal = horizontalPadding)
        }
        SelectableContent(
            state = selectableContentState,
            topBar = {
                val surfaceOpacity by animateFloatAsState(
                    when (topItemHeight) {
                        1 -> 1.0f
                        0 -> 0.0f
                        else -> {
                            when (val diff = (topItemYPosition * -1.0f)) {
                                0.0f -> 0.0f
                                else -> {
                                    (diff / topItemHeight).coerceIn(0.0f, 1.0f)
                                }
                            }
                        }
                    }
                )

                Surface(
                    color = MaterialTheme.colorScheme.background.copy(alpha = surfaceOpacity),
                ) {
                    Box(modifier = Modifier.padding(top = windowInsets.getTop(density).pxToDp()).then(topBarMainModifier)) {
                        Row(
                            modifier = Modifier.matchParentSize()
                                .padding(horizontal = 20.dp),
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
                                        contentDescription = "Download Manga",
                                        color = contentColorFor(MaterialTheme.colorScheme.background)
                                    )
                                }
                                DropdownMenu(expanded = shouldShowContextMenu,
                                    onDismissRequest = { shouldShowContextMenu = false }) {
                                    DropdownMenuItem(text = {
                                        Text("All")
                                    }, onClick = {
                                        shouldShowContextMenu = false
                                        coroutineScope.launch {
                                            chapters.reversed().forEach {

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
                                        Text("Unread")
                                    }, onClick = {
                                        shouldShowContextMenu = false
                                        coroutineScope.launch {
                                            chapters.filterIndexed { idx, _ ->
                                                readInfo?.read?.let {
                                                    !it.contains(
                                                        chapters.lastIndex - idx
                                                    )
                                                } ?: true
                                            }.reversed().forEach {
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
                                    DropdownMenuItem(text = {
                                        Text("Not Downloaded")
                                    }, onClick = {
                                        shouldShowContextMenu = false
                                        coroutineScope.launch {
                                            chapters.reversed().forEach {
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
                                }
//

                            }
                            Box {
                                var shouldShowContextMenu by remember { mutableStateOf(false) }
                                Pressable(onClick = {
                                    shouldShowContextMenu = true
                                }) {
                                    VectorImage(
                                        modifier = Modifier.size(30.dp),
                                        vector = EvaIcons.Fill.MoreVertical,
                                        contentDescription = "More Options",
                                        color = contentColorFor(MaterialTheme.colorScheme.background)
                                    )
                                }
                                DropdownMenu(expanded = shouldShowContextMenu,
                                    onDismissRequest = { shouldShowContextMenu = false }) {
                                    DropdownMenuItem(text = {
                                        Text("Migrate")
                                    }, onClick = {
                                        shouldShowContextMenu = false
                                        coroutineScope.launch {

                                        }
                                    })
                                }
//

                            }
                        }
                    }
                }
            },
            topSheetContent = {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = ModalBottomSheetDefaults.Elevation
                ) {
                    Row(
                        modifier = Modifier.padding(top = windowInsets.getTop(density).pxToDp()).then(topBarMainModifier),
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
                    val pressableModifier = remember { Modifier.borderRadius(5.dp) }
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
                                        state.sourceId, state.preview.id, it
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
                                    val targetIdx = chapters.lastIndex - it
                                    chapterDownloader.downloadChapter(
                                        state.sourceId,
                                        state.preview.id,
                                        chapters[targetIdx].id,
                                        "${component.state.value.preview.name} | ${chapters[targetIdx].name}"
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
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding())
            ) {
                CategorySelectContent(state = categorySelectState) {
                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.pullRefresh(pullRefreshState),
                    ) {
                        // Manga Title And Buttons (Takes account of top bar padding)
                        item {
                            val itemScope = this
                            Box(modifier = Modifier.onGloballyPositioned {
                                topItemHeight = it.size.height
                                topItemYPosition = it.positionInParent().y
                            }) {
                                BackgroundImage(preview = state.preview,
                                    data = state.data,
                                    modifier = Modifier.matchParentSize(),
                                    onDisposed = {
                                        topItemYPosition = (topItemHeight * -1).toFloat()
                                    })
                                // Foreground content , name , status and icons
                                Column {
                                    Spacer(modifier = Modifier.height(padding.calculateTopPadding() + windowInsets.getTop(density).pxToDp()))
                                    Row(
                                        modifier = Modifier.then(commonModifier).fillMaxWidth()
                                            .height(160.dp)
                                    ) {

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
                                                color = MaterialTheme.colorScheme.onBackground,
                                                modifier = Modifier.combinedClickable(enabled = true,
                                                    onClickLabel = null,
                                                    onLongClickLabel = "copy name",
                                                    onClick = {},
                                                    onLongClick = {
                                                        clipboardManager.setText(
                                                            AnnotatedString(
                                                                state.preview.name
                                                            )
                                                        )
                                                    },
                                                    role = Role.Button,
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() })
                                            )
                                            Spacer(modifier = Modifier.width(20.dp))
                                            if (state.data != null) {
                                                Text(
                                                    state.data!!.status,
                                                    fontSize = 16.sp,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(
                                                        alpha = 0.7f
                                                    )
                                                )
                                            }
                                        }
                                    }

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
                                                                categorySelectState.selectCategories()
                                                            }
                                                        }

                                                    }
                                                })

                                            if (isBookmarked) {
                                                MangaViewerContentMainButton(name = "Categories",
                                                    icon = FontAwesomeIcons.Solid.Tags,
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            categorySelectState.selectCategories()
                                                        }
                                                    })
                                            }

                                            MangaViewerContentMainButton(name = "Share",
                                                icon = Octicons.Share24,
                                                onClick = {
                                                    ShareBridge.shareText(it.share)
                                                })
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
                                        val descriptionBrush = remember(backgroundBrushColor) {
                                            Brush.verticalGradient(
                                                listOf(
//                                                    Color.Transparent,
                                                    backgroundBrushColor.copy(alpha = 0.1f),
                                                    backgroundBrushColor.copy(alpha = 0.7f),
                                                    backgroundBrushColor.copy(alpha = 0.9f)
                                                )
                                            )
                                        }

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
                                                    ).align(Alignment.BottomStart)
                                                        .fillMaxWidth(), onDraw = {
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
                                                        },
                                                        modifier = Modifier.borderRadius(5.dp),
                                                        backgroundColor = MaterialTheme.colorScheme.surface,
                                                        tonalElevation = 10.dp
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
                            Column(modifier = Modifier.then(commonModifier)) {
                                if (chapters.isNotEmpty()) {
                                    val targetFirstChapter =
                                        chapters.lastIndex - (readInfo?.current?.index ?: readInfo?.read?.max() ?: 0)

                                    Box(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(vertical = 20.dp)
                                    ) {
                                        Pressable(
                                            onClick = {
                                                if (!state.isLoadingChapters) {
                                                    component.readChapter(targetFirstChapter)
                                                }
                                            },
                                            modifier = Modifier.borderRadius(5.dp).width(120.dp)
                                                .height(40.dp).align(Alignment.Center),
                                            backgroundColor = MaterialTheme.colorScheme.surface,
                                            tonalElevation = 16.dp
                                        ) {
                                            Box {
                                                Text(
                                                    if (targetFirstChapter == chapters.lastIndex) {
                                                        "Read"
                                                    } else {
                                                        "Resume"
                                                    },
                                                    modifier = Modifier.align(Alignment.Center),
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(
                                                    modifier = Modifier.height(
                                                        verticalPadding
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                                Row {
                                    Text("${chapters.size} chapters")
                                }
                                Surface(
                                    color = Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth().height(2.dp)
                                ) {

                                }
                            }
                        }

                        // Chapters

                        items(chapters.size, key = { chapters[it].id }) { idx ->
                            val data = chapters[idx]
                            val selectionIdx = chapters.lastIndex - idx

                            MangaChapterContent(
                                component = component,
                                index = idx,
                                total = chapters.size,
                                data = data,
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
                                },
                                readInfoState = readInfoState
                            )
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