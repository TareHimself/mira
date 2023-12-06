package com.tarehimself.mira.manga.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.common.ui.SearchBarContent
import com.tarehimself.mira.common.useTopInsets
import com.tarehimself.mira.data.ApiMangaPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchContent(component: GlobalSearchComponent) {
    val state by component.state.subscribeAsState(neverEqualPolicy())

    val coroutineScope = rememberCoroutineScope()
    var activeJob by remember { mutableStateOf<Job?>(null) }
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        Box(Modifier.useTopInsets()) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp)) {
                SearchBarContent(value = state.searchTerm, onChanged = {
                    activeJob?.cancel("New Search")
                    activeJob = coroutineScope.launch {
                        component.updateSearchTerm(it)
                    }
                })
            }
        }
    }) {
        Box(modifier = Modifier.padding(it).fillMaxSize()) {
            LazyColumn {
                items(state.sourcesToSearch.size) { idx ->
                    GlobalSearchItemContent(component = component, idx = idx, onExpandSearch = {

                    }, onResultSelected = { preview ->
                        component.onItemSelected(state.sourcesToSearch[idx].id, preview)
                    })
                }
                state.sourcesToSearch.forEach { source ->
                    item(key = source.id) {

                    }
                }
            }
        }
    }
}