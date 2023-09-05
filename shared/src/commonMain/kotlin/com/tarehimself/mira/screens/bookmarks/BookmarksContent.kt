package com.tarehimself.mira.screens.bookmarks

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.common.borderRadius
import com.tarehimself.mira.common.ui.Pressable
import com.tarehimself.mira.common.ui.SearchBarContent
import com.tarehimself.mira.common.ui.SelectableContent
import com.tarehimself.mira.common.ui.VectorImage
import com.tarehimself.mira.common.ui.rememberSelectableContentState
import com.tarehimself.mira.data.rememberBookmarks
import com.tarehimself.mira.data.rememberCategories
import com.tarehimself.mira.manga.preview.MangaPreviewContent
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Trash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch


@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterialApi::class)
@Composable
fun BookmarksContent(component: BookmarksComponent) {

    val bookmarks by rememberBookmarks()

    val savedState by component.state.subscribeAsState()

    var searchQuery by remember { mutableStateOf("") }

    val bookmarksSearched = remember(bookmarks, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            bookmarks
        } else {
            bookmarks.filter {
                it.name.contains(
                    searchQuery,
                    true
                ) || it.tags.any { tag -> tag.contains(searchQuery, false) }
            }
        }
    }

    val pagerState = rememberPagerState(initialPage = savedState.selectedPage)

    val selectableContentState = rememberSelectableContentState<String>()

    val coroutineScope = rememberCoroutineScope()

    val categories by rememberCategories(false)

    val categoriesToList = remember(categories, bookmarksSearched) {
        val ids = categories.map { Pair(it.id, it.name) }
        val result = arrayListOf<Pair<String, String>>()

        if (bookmarksSearched.any { manga -> manga.categories.isEmpty() }) {
            result.add(Pair("DEFAULT", "Default"))
        }

        result.addAll(ids.filter { cat ->
            bookmarksSearched.any { manga ->
                manga.categories.contains(
                    cat.first
                )
            }
        })
        result
    }

    LaunchedEffect(pagerState.currentPage, bookmarksSearched.size) {
        selectableContentState.collapse()
    }

    LaunchedEffect(pagerState.currentPage) {
        component.updateSelectedPage(pagerState.currentPage)
    }

    SelectableContent(
        state = selectableContentState,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp)) {
                SearchBarContent(value = searchQuery, onChanged = {
                    searchQuery = it.trim()
                })
            }
        },
        modifier = Modifier.fillMaxSize(),
        bottomSheetContent = {
            Surface(
                modifier = Modifier.height(70.dp).fillMaxWidth(),
                color = MaterialTheme.colorScheme.background,
                tonalElevation = ModalBottomSheetDefaults.Elevation
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Pressable(
                        modifier = Modifier.fillMaxHeight().aspectRatio(1.0f).borderRadius(5.dp),
                        backgroundColor = Color.Transparent,
                        onClick = {
                            coroutineScope.launch {
                                component.realmDatabase.removeBookmarks(selectableContentState.selectedItems.toList())
                                selectableContentState.collapse()
                            }
                        }
                    ) {
                        Box(modifier = Modifier.fillMaxHeight()) {
                            VectorImage(
                                vector = FontAwesomeIcons.Solid.Trash,
                                contentDescription = "Delete",
                                modifier = Modifier.size(20.dp).align(Alignment.Center)
                            )
                        }
                    }
                }
            }
        },

        ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            if (bookmarksSearched.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize()) {

                    Text(
                        when (bookmarks.isEmpty()) {
                            true -> "(ඟ⍘ඟ)\n\nNo Bookmarks"
                            else -> "¯\\_(ツ)_/¯\n\nNo Results"
                        },
                        modifier = Modifier.align(Alignment.Center).alpha(0.6f),
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (categoriesToList.isNotEmpty() && !(categoriesToList.size == 1 && categoriesToList.first().first == "DEFAULT")) {
                        ScrollableTabRow(
                            selectedTabIndex = pagerState.currentPage.coerceIn(
                                0,
                                categoriesToList.lastIndex
                            ),
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            edgePadding = 0.dp,
                        ) {
                            categoriesToList.forEachIndexed { idx, data ->
                                Tab(selected = idx == pagerState.currentPage, onClick = {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(idx)
                                    }
                                },
                                    text = { Text(data.second) })
                            }
                        }
                    }

                    if (categoriesToList.isNotEmpty()) {
                        HorizontalPager(
                            modifier = Modifier.fillMaxSize(),
                            pageCount = categoriesToList.size,
                            state = pagerState,

                            ) { pageIdx ->

                            if (pageIdx < categoriesToList.size) {
                                val category = categoriesToList[pageIdx].first

                                var isRefreshingCategory by remember { mutableStateOf(false) }

                                val currentPageItems = remember(bookmarks.hashCode(), category) {
                                    bookmarks.filter {
                                        when (category) {
                                            "DEFAULT" -> {
                                                it.categories.isEmpty()
                                            }

                                            else -> {
                                                it.categories.contains(category)
                                            }
                                        }
                                    }
                                }

                                val pullRefreshState =
                                    rememberPullRefreshState(isRefreshingCategory, {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            if (!isRefreshingCategory) {
                                                isRefreshingCategory = true
                                                component.realmDatabase.updateBookmarksFromApi(
                                                    currentPageItems.map { it.uniqueId })
                                                isRefreshingCategory = false
                                            }
                                        }
                                    })

                                Box {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp)
                                            .pullRefresh(pullRefreshState)
                                    ) {
                                        currentPageItems.forEach { manga ->
                                            item(key = manga.uniqueId) {
                                                Box {
                                                    Crossfade(selectableContentState.isSelected(manga.uniqueId), modifier = Modifier.matchParentSize()){
                                                        if(it){
                                                            Box(
                                                                modifier = Modifier.fillMaxSize()
                                                                    .padding(2.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier.matchParentSize()
                                                                        .borderRadius(5.dp)
                                                                ) {
                                                                    Box(
                                                                        modifier = Modifier.matchParentSize()
                                                                            .background(MaterialTheme.colorScheme.tertiary)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                    MangaPreviewContent(
                                                        manga, manga.sourceId, onPressed = {
                                                            if (selectableContentState.isExpanding || selectableContentState.isExpanded) {
                                                                coroutineScope.launch {
                                                                    if (selectableContentState.isSelected(
                                                                            manga.uniqueId
                                                                        )
                                                                    ) {
                                                                        selectableContentState.deselect(
                                                                            manga.uniqueId
                                                                        )
                                                                    } else {
                                                                        selectableContentState.select(
                                                                            manga.uniqueId
                                                                        )
                                                                    }
                                                                }

                                                            } else {
                                                                component.onMangaSelected(manga)
                                                            }
                                                        }, onLongPressed = {
                                                            if (selectableContentState.isCollapsed) {
                                                                coroutineScope.launch {
                                                                    selectableContentState.expand(
                                                                        listOf(manga.uniqueId)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    )
                                                }

                                            }
                                        }
                                    }

                                    PullRefreshIndicator(
                                        isRefreshingCategory,
                                        state = pullRefreshState,
                                        modifier = Modifier.align(Alignment.TopCenter)
                                    )
                                }
                            }
                        }
                    }
                }

            }
        }

    }

}