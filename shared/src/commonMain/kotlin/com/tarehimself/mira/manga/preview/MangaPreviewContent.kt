package com.tarehimself.mira.manga.preview

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.common.Constants
import com.tarehimself.mira.common.ui.AsyncImage
import com.tarehimself.mira.common.ui.rememberMangaCoverPainter
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.data.MangaPreview
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.StoredManga
import com.tarehimself.mira.data.rememberIsBookmarked
import com.tarehimself.mira.data.rememberReadInfo
import io.ktor.client.request.header
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@Composable
fun <T> MangaPreviewContent(
    data: T,
    sourceId: String,
    onPressed: () -> Unit = {},
    onLongPressed: ((isBookmarked: Boolean) -> Unit)? = null,
    selectedState: SnapshotStateList<String>? = null,
    realmRepository: RealmRepository = koinInject()
) where T : MangaPreview {

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

    var isBookmarked by rememberIsBookmarked(sourceId, data.id)

    val readInfo = rememberReadInfo(sourceId, data.id)

    val isBookmarkedAlpha by animateFloatAsState(
        when (data is ApiMangaPreview && isBookmarked) {
            true -> 1.0f
            else -> 0.0f
        }, animationSpec = tween(200)
    )

    val pressableBackgroundColor by animateColorAsState(
        when (isSelected) {
            true -> MaterialTheme.colorScheme.tertiary
            else -> Color.Transparent
        }
    )

    val textBackgroundBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                Color.Black.copy(alpha = 0.5f),
                Color.Black.copy(alpha = 0.8f)
            ),
        )
    }

    LaunchedEffect(selectedState) {
        selectedState?.let {
            snapshotFlow { it.toList() }.collect {
                isSelected = it.contains(realmRepository.getMangaKey(sourceId, data.id))
            }
        }
    }

    Pressable(
        modifier = Modifier.fillMaxSize().padding(2.dp)
            .clip(shape = RoundedCornerShape(roundedCornerSize))
            .aspectRatio(Constants.mangaCoverRatio), onClick = {
            onPressed()
        },
        backgroundColor = pressableBackgroundColor,
        onLongClick = {
            coroutineScope.launch {
                if (data !is StoredManga) {
                    isBookmarked = if (!isBookmarked) {
                        realmRepository.bookmark(sourceId, data)
                        true
                    } else {
                        realmRepository.removeBookmark(sourceId, data.id)
                        false
                    }
                }
                onLongPressed?.let { it(isBookmarked) }
            }
        }) {

        Box(modifier = Modifier.padding(5.dp).clip(RoundedCornerShape(roundedCornerSize))) {
            AsyncImage(
                painter = rememberMangaCoverPainter(
                    data.cover!!.src,
                    filterQuality = FilterQuality.Low
                ) {
                    data.cover!!.headers.forEach {
                        header(it.key, it.value)
                    }
                },
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

            Box(
                modifier = Modifier.fillMaxWidth().background(textBackgroundBrush)
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
            ) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        data.name,
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        fontSize = 12.sp,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Grey out when bookmarked in search
            Surface(
                modifier = Modifier.fillMaxSize().alpha(isBookmarkedAlpha),
                color = Color.Black.copy(alpha = 0.7f)
            ) {

            }

            if (data is StoredManga) {
                if ((data as StoredManga).chapters.size - (readInfo?.read?.size ?: 0) != 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.clip(
                            shape = RoundedCornerShape(
                                bottomEnd = 5.dp
                            )
                        )
                    ) {
                        Text(
                            "${(data as StoredManga).chapters.size - (readInfo?.read?.size ?: 0)}",
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