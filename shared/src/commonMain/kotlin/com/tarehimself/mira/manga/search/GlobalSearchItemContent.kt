package com.tarehimself.mira.manga.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.MangaSource
import com.tarehimself.mira.data.rememberSetting
import com.tarehimself.mira.manga.preview.PreviewContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

@Composable
fun GlobalSearchItemContent(
    component: GlobalSearchComponent,
    idx: Int,
    onExpandSearch: () -> Unit,
    onResultSelected: (result: ApiMangaPreview) -> Unit
) {
    val state by component.state.subscribeAsState(neverEqualPolicy())
    val source = state.sourcesToSearch[idx]
    val results = state.searchResults[idx]



//    LaunchedEffect(source.id, state.searchTerm) {
//        withContext(Dispatchers.IO) {
//            component.setSearchResults(
//                idx,
//                mangaApi.search(source = source.id, query = state.searchTerm).data?.items
//                    ?: listOf()
//            )
//        }
//    }
    Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 10.dp)) {
        Row {
            Text(source.name)
        }

        if(state.isSearching[idx]){
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(20.dp))
        }

        Box(modifier = Modifier.fillMaxWidth().height(when(results.isNotEmpty()){
            true -> 200.dp
            else -> 0.dp
        })) {
            LazyRow {
                results.forEach { preview ->
                    item(key = preview.id) {
                        PreviewContent(preview, source.id, onPressed = {
                            onResultSelected(preview)
                        })
                    }
                }
            }
        }
    }

}