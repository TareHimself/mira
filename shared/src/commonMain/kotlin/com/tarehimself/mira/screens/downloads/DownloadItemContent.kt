package com.tarehimself.mira.screens.downloads

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.data.ChapterDownloadJob
import com.tarehimself.mira.data.ChapterDownloader
import com.tarehimself.mira.data.EChapterDownloadState
import com.tarehimself.mira.data.rememberChapterDownloadState
import org.koin.compose.koinInject

@Composable
fun DownloadItemContent(
    job: ChapterDownloadJob,
    chapterDownloader: ChapterDownloader = koinInject()
) {

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