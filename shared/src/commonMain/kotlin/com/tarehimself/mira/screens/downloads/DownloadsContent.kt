package com.tarehimself.mira.screens.sources

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.data.ChapterDownloader
import com.tarehimself.mira.screens.downloads.DownloadItemContent
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsContent(
    component: DownloadsComponent,
    chapterDownloader: ChapterDownloader = koinInject()
) {
//    val state by component.state.subscribeAsState(neverEqualPolicy())

    var downloadQueue by remember { mutableStateOf(chapterDownloader.downloadQueue.toList()) }

    LaunchedEffect(Unit) {
        snapshotFlow { chapterDownloader.queueUpdates.value }.collect {
            downloadQueue = chapterDownloader.downloadQueue.toList()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(modifier = Modifier.height(70.dp)) {

            }
        }) {
        Box(modifier = Modifier.padding(it)) {
            if (downloadQueue.isNotEmpty()) {
                LazyColumn {
                    downloadQueue.forEach {
                        item(key = it.hashCode()) {
                            DownloadItemContent(it)
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "(～﹃～)~zZ\n\nNo Downloads",
                        modifier = Modifier.align(Alignment.Center).alpha(0.6f),
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}