package com.tarehimself.mira.screens.bookmarks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ModalBottomSheetDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.common.ui.SearchBarContent
import com.tarehimself.mira.common.ui.SelectableContent
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.rememberCategories
import com.tarehimself.mira.common.ui.rememberSelectableContentState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import org.koin.compose.koinInject


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarksContent(component: BookmarksComponent) {

    val state by component.state.subscribeAsState(neverEqualPolicy())
    val savedState by component.savedState.subscribeAsState()

    var searchQuery by remember { mutableStateOf("") }


    val bookmarksSearched = remember(state.bookmarks, searchQuery) {
        if (searchQuery.trim().isEmpty()) {
            state.bookmarks
        } else {
            state.bookmarks.filter {
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

    LaunchedEffect(pagerState.currentPage,bookmarksSearched.size) {
        selectableContentState.collapse()
    }

    LaunchedEffect(pagerState.currentPage){
        component.updateSelectedPage(pagerState.currentPage)
    }

    SelectableContent(
        state = selectableContentState,
        topBar = {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 10.dp)) {
                SearchBarContent(value = searchQuery, onChanged = {
                    searchQuery = it
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

                }
            }
        },

        ) {
        if(bookmarksSearched.isEmpty()){
            Box(modifier = Modifier.fillMaxSize()) {

                Text(
                    when(state.bookmarks.isEmpty()) { true -> "(ඟ⍘ඟ)\n\nNo Bookmarks"
                            else -> "¯\\_(ツ)_/¯\n\nNo Results" },
                    modifier = Modifier.align(Alignment.Center).alpha(0.6f),
                    fontSize = 30.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        else
        {
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
                            BookmarksContentPage(
                                component = component,
                                category = categoriesToList[pageIdx].first,
                                bookmarks = bookmarksSearched,
                                selectableContentState = selectableContentState
                            )
                        }
                    }
                }
            }

        }

    }

}