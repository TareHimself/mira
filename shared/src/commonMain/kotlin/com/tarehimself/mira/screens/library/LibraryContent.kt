package com.tarehimself.mira.screens.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.SearchBarContent
import com.tarehimself.mira.common.debug
import com.tarehimself.mira.screens.MangaPreviewContent
import com.tarehimself.mira.ui.search.LibraryComponent


@Composable
fun LibraryContent(component: LibraryComponent) {
    val state by component.state.subscribeAsState(neverEqualPolicy())
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        component.loadLibrary()
    }

    Scaffold(topBar = {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp)){
            SearchBarContent(value = searchQuery, onChanged = {
                searchQuery = it
            })
        }
    }, modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp)
        ) {

            items((when(searchQuery.isEmpty()){
                true -> state.library
                else -> state.library.filter { it.name.contains(searchQuery,true) || it.tags.any { tag -> tag.contains(searchQuery,false) } }
            }).sortedBy { it.addedAt * -1 }, key = {
                it.uniqueId
            }) { item ->

                MangaPreviewContent(item.source,
                    item, onItemSelected = {
                        component.onMangaSelected(it)
                    })
            }
        }
    }

}