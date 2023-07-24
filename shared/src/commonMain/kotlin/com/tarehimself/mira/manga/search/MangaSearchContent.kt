package com.tarehimself.mira.screens.sources

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.SearchBarContent
import com.tarehimself.mira.common.debug
import com.tarehimself.mira.screens.MangaPreviewContent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MangaSearchContent(component: MangaSearchComponent) {
    val state by component.state.subscribeAsState(neverEqualPolicy())

    val scrollState = rememberLazyGridState()

    val scope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(state.isLoadingData,{
        CoroutineScope(Dispatchers.Default).launch {
            component.loadInitialData()
        }
    })

    LaunchedEffect(Unit) {
        component.loadInitialData()
    }

    LaunchedEffect(state.items.size) {
        if (state.items.size > 0) {
            snapshotFlow { (scrollState.firstVisibleItemIndex + scrollState.layoutInfo.visibleItemsInfo.size + 6) >= state.items.size }
                .collect {
                    if (it && state.latestNext != null && !state.isLoadingData) {
                        component.tryLoadMoreData()
                    }
                }
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp)) {
            SearchBarContent(value = state.query, onChanged = {
                if (it != state.query) {
                    scope.launch {
                        scrollState.scrollToItem(0)
                    }
                    component.search(it)
                }
            })
        }
    }) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LazyVerticalGrid(
                state = scrollState,
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp).pullRefresh(pullRefreshState)
            ) {
                items(state.items) { result ->
                    MangaPreviewContent(state.sourceId, result, onItemSelected = { data ->
                        component.onItemSelected(data)
                    })
                }
            }
            PullRefreshIndicator(
                state.isLoadingData,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}