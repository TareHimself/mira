package com.tarehimself.mira.screens.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.SearchBarContent
import com.tarehimself.mira.SelectableContent
import com.tarehimself.mira.rememberSelectableContentState
import com.tarehimself.mira.screens.MangaPreviewContent
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryContent(component: LibraryComponent) {
    val state by component.state.subscribeAsState(neverEqualPolicy())

    var searchQuery by remember { mutableStateOf("") }

    val pagerState = rememberPagerState()

    val selectableContentState = rememberSelectableContentState<String>()

    val coroutineScope = rememberCoroutineScope()

    val testList = remember { mutableStateListOf(1,2,3) }
    LaunchedEffect(Unit) {
        component.loadLibrary()
    }

    LaunchedEffect(pagerState.currentPage) {
        selectableContentState.collapse()
    }

    LaunchedEffect(selectableContentState.selectedItems){
        snapshotFlow { testList }.collect{
            Napier.d { "Selected Items Updated IN MAIN" }
        }
    }

    Napier.d { "Items Selected ${selectableContentState.selectedItems} ${selectableContentState.selectedItems.size}" }
    SelectableContent(
        state = selectableContentState, topBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp)) {
                SearchBarContent(value = searchQuery, onChanged = {
                    searchQuery = it
                })
            }
        },
        modifier = Modifier.fillMaxSize(),
        bottomSheetContent = {
            Surface(modifier = Modifier.height(70.dp).fillMaxWidth()) {

                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                }
            }
        }
    ) {
        HorizontalPager(modifier = Modifier.fillMaxSize(), pageCount = 1, state = pagerState) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp)
            ) {

                items((when (searchQuery.isEmpty()) {
                    true -> state.library
                    else -> state.library.filter {
                        it.name.contains(
                            searchQuery,
                            true
                        ) || it.tags.any { tag -> tag.contains(searchQuery, false) }
                    }
                }).sortedBy { it.addedAt * -1 }, key = {
                    it.hashCode()
                }) { item ->

                    MangaPreviewContent(item.source,
                        item, onPressed = {
                            Napier.d { "Is Selected ${selectableContentState.isSelected(it.uniqueId)}" }
                            if (selectableContentState.isExpanding || selectableContentState.isExpanded) {
                                coroutineScope.launch {
                                    if (selectableContentState.isSelected(it.uniqueId)) {
                                        selectableContentState.deselect(it.uniqueId)
                                    } else {
                                        selectableContentState.select(it.uniqueId)
                                    }
                                }

                            } else {
                                component.onMangaSelected(it)
                            }
                        }, onLongPressed = {
                            if (selectableContentState.isCollapsed) {
                                coroutineScope.launch {
                                    selectableContentState.expand(listOf(it.uniqueId))
                                    testList.add(1)
                                }
                            }
                        }, selectedState = selectableContentState.selectedItems
                    )
                }
            }
        }

    }

}