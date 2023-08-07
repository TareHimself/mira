@file:Suppress("INLINE_FROM_HIGHER_PLATFORM") // https://github.com/Kamel-Media/Kamel/issues/38

package com.tarehimself.mira.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.common.AsyncImage
import com.tarehimself.mira.common.Constants
import com.tarehimself.mira.common.mangaCoverPainter
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.data.MangaPreview
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.StoredManga
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

//.border(width = 1.dp,
//color = Color.Transparent,
//shape = RoundedCornerShape(50.dp))
@Composable
fun <T> MangaPreviewContent(
    sourceId: String,
    data: T,
    onPressed: (item: T) -> Unit = {},
    onLongPressed: ((item: T) -> Unit)? = null,
    selectedState: SnapshotStateList<String>? = null,
    realmRepository: RealmRepository = koinInject()
) where T : MangaPreview {

    val textContainerSize = remember { mutableStateOf(IntSize.Zero) }

    val mainContainerSize = remember { mutableStateOf(IntSize.Zero) }

    val roundedCornerSize = remember { 5.dp }

    val coroutineScope = rememberCoroutineScope()

    var isSelected by remember {
        mutableStateOf(
            selectedState?.contains(
                realmRepository.getMangaKey(
                    sourceId,
                    data.id
                )
            ) ?: false
        )
    }

    var isBookmarked by remember(sourceId, data.id) {
        mutableStateOf(
            realmRepository.has(
                sourceId,
                data.id
            )
        )
    }

    val brush = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
        startY = (mainContainerSize.value.height - (textContainerSize.value.height * 3)).toFloat(),
        endY = mainContainerSize.value.height.toFloat()
    )

    val backgroundColor by animateColorAsState(
        when (isSelected) {
            true -> Color.Blue
            else -> Color.Transparent
        }
    )

    LaunchedEffect(selectedState) {
        selectedState?.let {
            snapshotFlow { it.toList() }.collect {
                isSelected = it.contains(realmRepository.getMangaKey(sourceId, data.id))
            }
        }
    }
    DisposableEffect(sourceId, data.id, isBookmarked) {
        if (isBookmarked && data !is StoredManga) {
            val unsubscribe = realmRepository.subscribeOnBookmarksUpdated {
                isBookmarked = realmRepository.has(sourceId, data.id)
            }

            onDispose {
                unsubscribe()
            }
        } else {
            onDispose {

            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(2.dp)
            .clip(shape = RoundedCornerShape(roundedCornerSize))
            .aspectRatio(Constants.mangaCoverRatio),
        color = backgroundColor,
    ) {
        Pressable(
            modifier = Modifier.fillMaxSize().clip(shape = RoundedCornerShape(roundedCornerSize))
                .onGloballyPositioned {
                    mainContainerSize.value = it.size
                }, onClick = {
                onPressed(data)
            },
            onLongClick = {
                onLongPressed?.let { it(data) } ?: run {
                    if (data !is StoredManga) {
                        if (!isBookmarked) {
                            coroutineScope.launch {
                                realmRepository.bookmark(sourceId, data)
                                isBookmarked = true
                            }
                        } else {
                            coroutineScope.launch {
                                realmRepository.removeBookmark(sourceId, data.id)
                                isBookmarked = false
                            }
                        }
                    }
                }
            }) {

            Box(modifier = Modifier.padding(5.dp).clip(RoundedCornerShape(roundedCornerSize))) {
                AsyncImage(
                    painter = mangaCoverPainter(data.cover, filterQuality = FilterQuality.Low),
                    contentDescription = "Manga Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onLoading = {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CircularProgressIndicator(
                                it.progress.value,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                    }
                )
                Box(modifier = Modifier.matchParentSize().background(brush))
                Surface(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .padding(5.dp).onGloballyPositioned {
                            textContainerSize.value = it.size
                        },
                    color = Color.Transparent
                ) {
                    Text(
                        data.name,
                        modifier = Modifier.align(Alignment.BottomStart)
                            .background(Color.Transparent),
                        maxLines = 2,
                        fontSize = 12.sp,
                        overflow = TextOverflow.Ellipsis
                    )

                }

                Crossfade(
                    data is ApiMangaPreview && isBookmarked,
                    modifier = Modifier.fillMaxSize(),
                    animationSpec = tween(200)
                ) {
                    if (it) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {

                        }
                    }
                }
                if (data is StoredManga) {
                    if (data.chapters.size - data.chaptersRead.size != 0) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.clip(
                                shape = RoundedCornerShape(
                                    bottomEnd = 5.dp
                                )
                            )
                        ) {
                            Text(
                                "${data.chapters.size - data.chaptersRead.size}",
                                color = Color.White,
                                modifier = Modifier.padding(5.dp),
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}