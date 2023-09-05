package com.tarehimself.mira.screens.sources

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.common.ui.Pressable
import com.tarehimself.mira.common.borderRadius
import com.tarehimself.mira.data.SettingsRepository
import com.tarehimself.mira.data.rememberSetting
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesContent(
    component: SourcesComponent,
    settingsRepository: SettingsRepository = koinInject()
) {
    val state by component.state.subscribeAsState(neverEqualPolicy())

    LaunchedEffect(Unit) {
        component.getSources()
    }

    val showNsfwSources by rememberSetting(settingsRepository.keys.showNsfwSources, factory = {
        settingsRepository.settings.getBoolean(it, false)
    }, updated = { k, v ->
        settingsRepository.settings.putBoolean(k, v)
    })

    val sourcesToList = when (showNsfwSources) {
        true -> state.sources
        else -> state.sources.filter { source -> !source.nsfw }
    }

    LazyColumn(modifier = Modifier.fillMaxWidth()) {
        items(sourcesToList.size, key = { idx ->
            sourcesToList[idx].id
        }) { idx ->

            val source = sourcesToList[idx]
            Surface(modifier = Modifier.height(60.dp).fillMaxWidth().padding(10.dp, 10.dp)) {
                Surface(
                    modifier = Modifier.fillMaxSize().borderRadius(5.dp)
                ) {
                    Pressable(modifier = Modifier.fillMaxSize(), onClick = {
                        component.onItemSelected(source.id)
                    }, backgroundColor = MaterialTheme.colorScheme.primary) {
                        Box {
                            Text(source.name, modifier = Modifier.align(Alignment.Center))
                        }
                    }
                }
            }
        }
    }
}