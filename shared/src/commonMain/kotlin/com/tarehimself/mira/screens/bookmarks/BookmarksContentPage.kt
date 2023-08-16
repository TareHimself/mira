package com.tarehimself.mira.screens.bookmarks

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.common.ui.SelectableContentState
import com.tarehimself.mira.data.StoredManga
import com.tarehimself.mira.manga.preview.MangaPreviewContent
import kotlinx.coroutines.launch

@Composable
fun BookmarksContentPage(
    component: BookmarksComponent,
    category: String,
    bookmarks: List<StoredManga>,
    selectableContentState: SelectableContentState<String>
) {

    val coroutineScope = rememberCoroutineScope()

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

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp)
    ) {
        currentPageItems.forEach {manga ->
            item(key = manga.uniqueId) {
                MangaPreviewContent(
                    manga, manga.sourceId, onPressed = {
                        if (selectableContentState.isExpanding || selectableContentState.isExpanded) {
                            coroutineScope.launch {
                                if (selectableContentState.isSelected(manga.uniqueId)) {
                                    selectableContentState.deselect(manga.uniqueId)
                                } else {
                                    selectableContentState.select(manga.uniqueId)
                                }
                            }

                        } else {
                            component.onMangaSelected(manga)
                        }
                    }, onLongPressed = {
                        if (selectableContentState.isCollapsed) {
                            coroutineScope.launch {
                                selectableContentState.expand(listOf(manga.uniqueId))
                            }
                        }
                    },
                    selectedState = selectableContentState.selectedItems
                )
            }
        }
    }
}