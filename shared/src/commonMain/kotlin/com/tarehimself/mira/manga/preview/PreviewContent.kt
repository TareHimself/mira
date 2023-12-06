package com.tarehimself.mira.manga.preview

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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarehimself.mira.common.ui.Pressable
import com.tarehimself.mira.common.Constants
import com.tarehimself.mira.common.borderRadius
import com.tarehimself.mira.common.ui.AsyncImage
import com.tarehimself.mira.common.ui.ErrorContent
import com.tarehimself.mira.common.ui.rememberCoverPreviewPainter
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.data.MangaPreview
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.StoredManga
import com.tarehimself.mira.data.rememberIsBookmarked
import com.tarehimself.mira.data.rememberReadInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject


@Composable
fun <T> PreviewContent(
    data: T,
    sourceId: String,
    onPressed: () -> Unit = {},
    onLongPressed: ((isBookmarked: Boolean) -> Unit)? = null,
    realmRepository: RealmRepository = koinInject()
) where T : MangaPreview {

    val coroutineScope = rememberCoroutineScope()

    var isBookmarked by rememberIsBookmarked(sourceId, data.id)

    val isBookmarkedAlpha by animateFloatAsState(
        when (data is ApiMangaPreview && isBookmarked) {
            true -> 1.0f
            else -> 0.0f
        }, animationSpec = tween(200)
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


    Pressable(
        modifier = Modifier.fillMaxSize().padding(2.dp)
            .borderRadius(5.dp)
            .aspectRatio(Constants.mangaCoverRatio), onClick = {
            onPressed()
        },
        backgroundColor = Color.Transparent,
        onLongClick = {
            coroutineScope.launch {
                withContext(Dispatchers.IO){
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
            }
        }) {

        Box(modifier = Modifier.padding(5.dp).borderRadius(5.dp)) {
            Box(modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.background)) {  }
            AsyncImage(
                asyncPainter = rememberCoverPreviewPainter(sourceId=sourceId,
                    mangaId = data.id,
                    filterQuality = FilterQuality.Low
                ) {
                    this.fromMangaImage(data.cover!!)
                },
                contentDescription = "Manga Cover",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onLoading = {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                },
                onFail = {
                    ErrorContent(it.request.hashString)
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
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
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

                val readInfo by rememberReadInfo(sourceId, data.id)

                if ((data as StoredManga).chapters.size - (readInfo?.read?.size ?: 0) != 0) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f), modifier = Modifier.borderRadius(bottomEnd = 5.dp)
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