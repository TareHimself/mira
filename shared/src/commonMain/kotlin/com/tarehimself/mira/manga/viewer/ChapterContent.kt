package com.tarehimself.mira.manga.viewer

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarehimself.mira.common.ui.Pressable
import com.tarehimself.mira.common.ui.VectorImage
import com.tarehimself.mira.common.borderRadius
import com.tarehimself.mira.common.brightness
import com.tarehimself.mira.common.pxToDp
import com.tarehimself.mira.common.ui.SelectableContentState
import com.tarehimself.mira.common.ui.SideDrawerContent
import com.tarehimself.mira.data.ChapterDownloader
import com.tarehimself.mira.data.EChapterDownloadState
import com.tarehimself.mira.data.MangaChapter
import com.tarehimself.mira.data.StoredChapterReadInfo
import com.tarehimself.mira.data.StoredChaptersRead
import com.tarehimself.mira.data.rememberChapterDownloadState
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowDown
import compose.icons.fontawesomeicons.solid.Check
import compose.icons.fontawesomeicons.solid.Clock
import compose.icons.fontawesomeicons.solid.Trash
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun MangaChapterContent(
    component: ViewerComponent,
    index: Int,
    total: Int,
    data: MangaChapter,
    selectedState: SelectableContentState<Int>,
    onChapterSelected: (data: MangaChapter) -> Unit,
    onChapterLongPressed: (data: MangaChapter) -> Unit,
    readInfoState: MutableState<StoredChaptersRead?>,
    downloader: ChapterDownloader = koinInject()
) {

    val selectionIdx = remember(index, total) { total - 1 - index }

    val itemHeight = remember { 60.dp }

    var itemWidth by remember { mutableStateOf(0) }

    val itemWidthDp = itemWidth.pxToDp()

    val selectedItems by selectedState.selectedItems

    val isSelected by remember(selectionIdx,selectedState,selectedItems.hashCode()) { mutableStateOf(selectedState.isSelected(selectionIdx)) }

    val readInfo by readInfoState

    val hasBeenRead =
        remember(readInfo,selectionIdx) { readInfo?.read?.contains(selectionIdx) == true }

    val backgroundColor by animateColorAsState(
        when (isSelected) {
            true -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surface
        }
    )

    val downloadState by rememberChapterDownloadState(
        component.state.value.sourceId,
        component.state.value.preview.id,
        data.id,
        selectionIdx
    )

    val animatedDownloadProgress by animateFloatAsState(
        when (downloadState.first) {
            EChapterDownloadState.DOWNLOADING -> downloadState.second
            else -> 0.0f
        }
    )

    val coroutineScope = rememberCoroutineScope()


//    LaunchedEffect(index, total) {
//        snapshotFlow {  }.collect {
//            isSelected = selectedState.isSelected(selectionIdx)
//        }
//    }

    SideDrawerContent(modifier = Modifier.fillMaxWidth().height(itemHeight), drawerContent = {
        if (downloadState.first == EChapterDownloadState.DOWNLOADED) {
            val pressableModifier = Modifier.fillMaxHeight().aspectRatio(1.0f).borderRadius(5.dp)

            Pressable(
                modifier = Modifier.then(pressableModifier),
                backgroundColor = Color.Transparent,
                onClick = {
                    coroutineScope.launch {
                        downloader.deleteChapter(
                            component.state.value.sourceId,
                            component.state.value.preview.id,
                            selectionIdx,

                        )
                    }
                }
            ) {
                Box(modifier = Modifier.fillMaxHeight()) {
                    VectorImage(
                        vector = FontAwesomeIcons.Solid.Trash,
                        contentDescription = "Delete",
                        modifier = Modifier.size(20.dp).align(Alignment.Center)
                    )
                }
            }
        }
    }) {
        Pressable(modifier = Modifier.matchParentSize(), onClick = {
            onChapterSelected(data)
        }, onLongClick = {
            onChapterLongPressed(data)
        },
            backgroundColor = backgroundColor
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 20.dp).onGloballyPositioned {
                    itemWidth = it.size.width
                }
            ) {
                Box(
                    modifier = remember(itemWidthDp) { Modifier.width(itemWidthDp - 30.dp - 20.dp) }
                ) {
                    Column(modifier = Modifier.align(Alignment.CenterStart)) {
                        Text(
                            data.name,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = when (hasBeenRead && !isSelected) {
                                true -> contentColorFor(backgroundColor).brightness(0.5f)
                                else -> contentColorFor(backgroundColor)
                            }
                        )
                        Row {
                            Text(
                                remember(data.released) {
                                    when (val released = data.released) {
                                        is String -> released
                                        else -> "Unknown"
                                    }
                                },
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = contentColorFor(backgroundColor).brightness(0.5f)
//                                color = Color.DarkGray
                            )
                            when (val currentPage = readInfo?.current) {
                                is StoredChapterReadInfo -> {

                                    if ((component.state.value.chapters.lastIndex - currentPage.index) == index) {
                                        Text(
                                            "  |  ",
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = contentColorFor(backgroundColor).brightness(0.5f)
//                                            color = Color.DarkGray
                                        )
                                        Text(
                                            "Page ${currentPage.progress}",
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = contentColorFor(backgroundColor).brightness(0.5f)
//                                            color = Color.DarkGray
                                        )
                                    }
                                }

                                else -> {

                                }
                            }

                        }

                    }
                }

                if (!isSelected) {
                    Box {
                        Pressable(
                            modifier = Modifier.fillMaxHeight(0.6f).aspectRatio(1.0f)
                                .borderRadius(5.dp), onClick = {
                                coroutineScope.launch {
                                    downloader.downloadChapter(
                                        component.state.value.sourceId,
                                        component.state.value.preview.id,
                                        data.id,
                                        selectionIdx,
                                        "${component.state.value.preview.name} | ${data.name}"
                                    )
                                }
                            }) {
                            Crossfade(downloadState.first, modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    when (it) {
                                        EChapterDownloadState.NONE -> {
                                            VectorImage(
                                                vector = FontAwesomeIcons.Solid.ArrowDown,
                                                modifier = Modifier.align(Alignment.Center)
                                                    .fillMaxSize(0.5f),
                                                contentDescription = "Download Chapter",
                                                color = contentColorFor(backgroundColor)
                                            )
                                        }

                                        EChapterDownloadState.PENDING -> {
                                            VectorImage(
                                                vector = FontAwesomeIcons.Solid.Clock,
                                                modifier = Modifier.align(Alignment.Center)
                                                    .fillMaxSize(0.5f),
                                                contentDescription = "Download Chapter",
                                                color = contentColorFor(backgroundColor)
                                            )
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(Alignment.Center)
                                                    .size(30.dp),
                                                strokeWidth = 2.dp
                                            )
                                        }

                                        EChapterDownloadState.DOWNLOADING -> {
                                            VectorImage(
                                                vector = FontAwesomeIcons.Solid.Clock,
                                                modifier = Modifier.align(Alignment.Center)
                                                    .fillMaxSize(0.5f),
                                                contentDescription = "Download Chapter",
                                                color = contentColorFor(backgroundColor)
                                            )
                                            CircularProgressIndicator(
                                                modifier = Modifier.align(Alignment.Center)
                                                    .size(30.dp),
                                                strokeWidth = 2.dp,
                                                progress = animatedDownloadProgress
                                            )
                                        }

                                        EChapterDownloadState.DOWNLOADED -> {
                                            VectorImage(
                                                vector = FontAwesomeIcons.Solid.Check,
                                                modifier = Modifier.align(Alignment.Center)
                                                    .fillMaxSize(0.5f),
                                                contentDescription = "Download Chapter",
                                                color = contentColorFor(backgroundColor)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }
    }
}