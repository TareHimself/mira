package com.tarehimself.mira.screens.sources

import BottomSheetIcon
import BottomSheetIconRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ModalBottomSheetDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.common.ui.Pressable
import com.tarehimself.mira.common.borderRadius
import com.tarehimself.mira.common.ui.BottomSheetSelectionIcon
import com.tarehimself.mira.common.ui.SelectableContent
import com.tarehimself.mira.common.ui.rememberSelectableContentState
import com.tarehimself.mira.data.SettingsRepository
import com.tarehimself.mira.data.rememberSetting
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Search
import compose.icons.fontawesomeicons.solid.Trash
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourcesContent(
    component: SourcesComponent,
    settingsRepository: SettingsRepository = koinInject()
) {
    val state by component.state.subscribeAsState(neverEqualPolicy())

    val selectableContentState = rememberSelectableContentState<Int>()

    val showNsfwSources by rememberSetting(settingsRepository.keys.showNsfwSources, factory = {
        settingsRepository.settings.getBoolean(it, false)
    }, updated = { k, v ->
        settingsRepository.settings.putBoolean(k, v)
    })

    val sourcesToList = when (showNsfwSources) {
        true -> state.sources
        else -> state.sources.filter { source -> !source.nsfw }
    }


    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            component.getSources()
        }
    }

    LaunchedEffect(sourcesToList.hashCode()) {
        selectableContentState.collapse()
    }

    SelectableContent(
        state = selectableContentState,
        modifier = Modifier.fillMaxSize(),
        bottomSheetContent = {
            Surface(
                modifier = Modifier.height(70.dp).fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = ModalBottomSheetDefaults.Elevation
            ) {
                BottomSheetIconRow {
                    BottomSheetIcon(vector = FontAwesomeIcons.Solid.Search,
                        contentDescription = "Search",
                        onClick = {
                            component.onSearchMultiple(selectableContentState.selectedItems.value.map { sourceIndex -> sourcesToList[sourceIndex] })
                        })

                    BottomSheetSelectionIcon(onSelectAll = {

                        selectableContentState.select(sourcesToList.indices.toList())
                    }, onSelectInverse = {
                        selectableContentState.mutate {
                            val allItems = sourcesToList.indices
                            val toAdd = allItems.subtract(it)
                            it.clear()
                            it.addAll(toAdd)
                        }
                    }, onDeselect = {
                        selectableContentState.collapse()
                    })

                }

            }
        },
    ) {
        Box(modifier = Modifier.padding(it)){
            LazyColumn(modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(it).padding(horizontal = 10.dp)) {
                items(sourcesToList.size, key = { idx ->
                    sourcesToList[idx].id
                }) { idx ->

                    val source = sourcesToList[idx]
                    Surface(
                        color = when (selectableContentState.isSelected(idx)) {
                            true -> MaterialTheme.colorScheme.secondary
                            else -> Color.Transparent
                        }
                    ) {
                        Surface(
                            modifier = Modifier.height(60.dp).fillMaxWidth().padding(10.dp, 10.dp),
                            color = Color.Transparent
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxSize().borderRadius(5.dp)
                            ) {
                                Pressable(modifier = Modifier.fillMaxSize(), onClick = {
                                    when (selectableContentState.isExpanded || selectableContentState.isExpanding) {
                                        true -> when (selectableContentState.isSelected(idx)) {
                                            true -> selectableContentState.deselect(idx)
                                            else -> selectableContentState.select(idx)
                                        }

                                        else -> component.onItemSelected(source.id)
                                    }
                                }, onLongClick = {
                                    selectableContentState.expand(listOf(idx))
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
        }

    }

}