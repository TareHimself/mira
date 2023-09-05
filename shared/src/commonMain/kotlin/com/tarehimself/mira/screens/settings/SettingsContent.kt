package com.tarehimself.mira.screens.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Surface
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarehimself.mira.common.ui.LocalMiraDialogController
import com.tarehimself.mira.common.ui.MiraDialogContainer
import com.tarehimself.mira.common.ui.TextInputCard
import com.tarehimself.mira.data.ChapterDownloader
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.data.SettingsRepository
import compose.icons.AllIcons
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.CompactDisc
import compose.icons.fontawesomeicons.solid.EyeSlash
import compose.icons.fontawesomeicons.solid.Language
import compose.icons.fontawesomeicons.solid.Memory
import compose.icons.fontawesomeicons.solid.Plus
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
    component: SettingsComponent,
    chapterDownloader: ChapterDownloader = koinInject(),
    imageRepository: ImageRepository = koinInject(),
    settingsRepository: SettingsRepository = koinInject()
) {
//    val state by component.state.subscribeAsState(neverEqualPolicy())


    val coroutineScope = rememberCoroutineScope()



    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Box(modifier = Modifier.height(70.dp)) {

            }
        }) {
        Box(modifier = Modifier.padding(it)) {
//            Box(modifier = Modifier.fillMaxSize()) {
//                Text(
//                    "ಠಿ_ಠ\n\nComing Soon",
//                    modifier = Modifier.align(Alignment.Center).alpha(0.6f),
//                    fontSize = 30.sp,
//                    textAlign = TextAlign.Center
//                )
//            }
            MiraDialogContainer {

                val dialogController = LocalMiraDialogController.current

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        SettingsContentItem(onPressed = {
                            coroutineScope.launch {
                                chapterDownloader.deleteAllChapters()
                            }
                        }) {
                            Text("Delete All Downloaded Chapters")
                        }
                    }
                    item {
                        val memoryUsed by remember { imageRepository.cache.size }
                        SettingsContentItem(onPressed = {
                            coroutineScope.launch {
                                imageRepository.cache.clear()
                            }
                        }) {
                            Column {
                                Text("Clear Memory Cache")
                                Text("${(memoryUsed / 1024).roundToInt()} MB", fontSize = 8.sp)
                            }
                        }
                    }
                    item {

                        var showNsfwSources by settingsRepository.showNsfwSources

                        SettingsContentItem {
                            Text("Show NSFW Sources")
                            Switch(checked = showNsfwSources, onCheckedChange = { checked ->
                                showNsfwSources = checked
                            })
                        }
                    }

                    item {
                        var sourcesApi by settingsRepository.sourcesApi

                        SettingsContentItem(onPressed = {
                            dialogController.show {
                                TextInputCard(initialValue = sourcesApi,
                                    modifier = Modifier.align(
                                        Alignment.Center
                                    ),
                                    onCancelled = {
                                        dialogController.hide()
                                    },
                                    onCommitted = { newEndpoint ->
                                        dialogController.hide()
                                        sourcesApi = newEndpoint.trim()
                                    })
                            }
                        }) {
                            Column {
                                Text("Sources API")
                                Text(
                                    when (sourcesApi.isEmpty()) {
                                        true -> "No Api Url Set"
                                        else -> sourcesApi
                                    }, fontSize = 10.sp
                                )
                            }

                        }
                    }

                    item {

                        var translatorEndpoint by settingsRepository.translatorEndpoint

                        SettingsContentItem(onPressed = {
                            dialogController.show {
                                TextInputCard(initialValue = translatorEndpoint,
                                    modifier = Modifier.align(
                                        Alignment.Center
                                    ),
                                    onCancelled = {
                                        dialogController.hide()
                                    },
                                    onCommitted = { newEndpoint ->
                                        dialogController.hide()
                                        translatorEndpoint = newEndpoint.trim()
                                    })
                            }
                        }) {
                            Column {
                                Text("Translator Endpoint")
                                Text(
                                    when (translatorEndpoint.isEmpty()) {
                                        true -> "No Api Url Set"
                                        else -> translatorEndpoint
                                    }, fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

