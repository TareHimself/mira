package com.tarehimself.mira.manga.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.Scaffold
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.tarehimself.mira.common.LocalWindowInsets
import com.tarehimself.mira.common.ui.CategorySelectContent
import com.tarehimself.mira.common.ui.SearchBarContent
import com.tarehimself.mira.common.ui.rememberCategorySelectContentState
import com.tarehimself.mira.common.useTopInsets
import com.tarehimself.mira.data.rememberCategories
import com.tarehimself.mira.manga.preview.MangaPreviewContent
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MangaSearchContent(component: MangaSearchComponent) {
    val state by component.state.subscribeAsState(neverEqualPolicy())

    val scrollState = rememberLazyGridState()

    val scope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(state.isLoadingData, {
        CoroutineScope(Dispatchers.Default).launch {
            component.loadInitialData()
        }
    })

    val categories by rememberCategories(false)

    val categorySelectContentState = rememberCategorySelectContentState()

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        component.loadInitialData()
    }

    LaunchedEffect(Unit) {
        snapshotFlow { scrollState.layoutInfo }
            .collect {
                if (state.items.isEmpty()) {
                    return@collect
                }

                if ((scrollState.firstVisibleItemIndex + scrollState.layoutInfo.visibleItemsInfo.size + 6) >= state.items.size && state.latestNext != null && !state.isLoadingData) {
                    component.tryLoadMoreData()
                }
            }
    }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        Box(Modifier.useTopInsets()){
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp)) {
                SearchBarContent(value = state.query, onChanged = {
                    if (it != state.query) {
                        scope.launch {
                            scrollState.scrollToItem(0)
                        }
                        component.search(it.trim())
                    }
                })
            }
        }
    }) {
        Box(modifier = Modifier.padding(it).fillMaxWidth()) {
            CategorySelectContent(state = categorySelectContentState) {
                LazyVerticalGrid(
                    state = scrollState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp)
                        .pullRefresh(pullRefreshState)
                ) {
                    state.items.forEach { data ->
                        item(key = data.hashCode()) {
                            MangaPreviewContent(data, state.sourceId, onPressed = {
                                component.onItemSelected(data)
                            }, onLongPressed = { isBookmarked ->
                                coroutineScope.launch {
                                    Napier.d { "Selecting category ${categories.size} $isBookmarked" }
                                    if (isBookmarked) {
                                        categorySelectContentState.selectCategories(
                                            state.sourceId,
                                            data.id
                                        )
                                    }
                                }

                            })
                        }
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
}