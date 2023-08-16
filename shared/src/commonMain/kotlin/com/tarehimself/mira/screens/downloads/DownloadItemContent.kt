package com.tarehimself.mira.screens.downloads

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.VectorImage
import com.tarehimself.mira.common.pxToDp
import com.tarehimself.mira.common.ui.SlidableContent
import com.tarehimself.mira.data.ChapterDownloadJob
import com.tarehimself.mira.data.ChapterDownloader
import com.tarehimself.mira.data.EChapterDownloadState
import com.tarehimself.mira.data.StoredChaptersRead
import com.tarehimself.mira.data.rememberChapterDownloadState
import com.tarehimself.mira.data.rememberReadInfo
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ArrowDown
import compose.icons.fontawesomeicons.solid.Check
import compose.icons.fontawesomeicons.solid.Clock
import compose.icons.fontawesomeicons.solid.Trash
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun DownloadItemContent(
    job: ChapterDownloadJob,
    chapterDownloader: ChapterDownloader = koinInject()
) {



    var itemWidth by remember { mutableStateOf(0) }

    val downloadState by rememberChapterDownloadState(job)

    val animatedDownloadProgress by animateFloatAsState(
        when (downloadState.first) {
            EChapterDownloadState.DOWNLOADING -> downloadState.second
            else -> 0.0f
        }
    )

    Column(
        modifier = Modifier.height(70.dp).fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            text = job.jobName, modifier = Modifier.fillMaxWidth(0.8f), maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (downloadState.first === EChapterDownloadState.DOWNLOADING) {
            LinearProgressIndicator(
                animatedDownloadProgress,
                modifier = Modifier.fillMaxWidth(0.9f)
            )
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.8f))
        }
    }
}