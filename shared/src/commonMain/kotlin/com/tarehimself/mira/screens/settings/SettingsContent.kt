package com.tarehimself.mira.screens.settings

import FileBridge
import ShareBridge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarehimself.mira.common.EFilePaths
import com.tarehimself.mira.common.LocalBackHandler
import com.tarehimself.mira.common.toChannel
import com.tarehimself.mira.common.ui.LocalMiraDialogController
import com.tarehimself.mira.common.ui.MiraDialogContainer
import com.tarehimself.mira.common.ui.TextInputCard
import com.tarehimself.mira.data.ChapterDownloader
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.data.MiraRealmExport
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.SettingsRepository
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.serializer
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, InternalSerializationApi::class)
@Composable
fun SettingsContent(
    component: SettingsComponent,
    chapterDownloader: ChapterDownloader = koinInject(),
    imageRepository: ImageRepository = koinInject(),
    settingsRepository: SettingsRepository = koinInject(),
    realmRepository: RealmRepository = koinInject()
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
//            MiraDialogContainer {

                val dialogController = LocalMiraDialogController.current
                val backHandler = LocalBackHandler.current
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        SettingsContentItem(onPressed = {
                            coroutineScope.launch {
                                val exported = Json.encodeToString(MiraRealmExport::class.serializer(),realmRepository.export())
                                FileBridge.writeFile("export.json",exported.encodeToByteArray().toChannel(),EFilePaths.SharedFiles)
                                FileBridge.getFilePath("export.json",EFilePaths.SharedFiles)?.let {sharedPath ->
                                    ShareBridge.shareFile(sharedPath)
                                }
                            }

                        }) {
                            Text("Export Data")
                        }
                    }
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
                            dialogController.show(data = null,backHandler = backHandler) {
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
                            dialogController.show(data = null,backHandler = backHandler) {
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
//            }

        }
    }
}

