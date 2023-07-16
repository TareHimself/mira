package com.tarehimself.mangaz.screens.sources

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mangaz.SearchBarContent
import com.tarehimself.mangaz.screens.SearchResult
import kotlinx.coroutines.launch

@Composable
fun MangaSearchContent(component: MangaSearchComponent){
    val state by component.state.subscribeAsState( neverEqualPolicy())

    val scrollState = rememberLazyGridState()

    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit){
        component.loadInitialData()
    }

    LaunchedEffect(state.items.size){
        if(state.items.size > 0){
            snapshotFlow { (scrollState.firstVisibleItemIndex + scrollState.layoutInfo.visibleItemsInfo.size + 6) >= state.items.size }
                .collect {
                    if(it && state.latestNext != null && !state.isLoadingData){
                        component.tryLoadMoreData()
                    }
                }
        }
    }

    Surface (modifier = Modifier.fillMaxSize()){
        Column {
            SearchBarContent(value=state.query, onChanged = {
                if(it != state.query){
                    scope.launch {
                        scrollState.scrollToItem(0)
                    }
                    component.search(it)
                }
            }, modifier = Modifier.fillMaxWidth().height(50.dp))

            LazyVerticalGrid( state = scrollState,columns = GridCells.Fixed(2), modifier = Modifier.fillMaxWidth()){
                items(state.items) { result ->
                    SearchResult(result, onItemSelected = {
                        component.onItemSelected(it)
                    })
                }
            }
        }
    }
}