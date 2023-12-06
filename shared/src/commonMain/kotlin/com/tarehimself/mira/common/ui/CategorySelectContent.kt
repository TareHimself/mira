package com.tarehimself.mira.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.contentColorFor
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.common.LocalBackHandler
import com.tarehimself.mira.common.borderRadius
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.StoredManga
import com.tarehimself.mira.data.StoredMangaCategory
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ChevronDown
import compose.icons.fontawesomeicons.solid.ChevronUp
import compose.icons.fontawesomeicons.solid.Pen
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.Trash
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


@OptIn(ExperimentalMaterialApi::class)
@Stable
class CategorySelectContentState constructor(
    val sheetState: ModalBottomSheetState,
    val sourceAndMangaId: MutableState<Pair<String,String>>,
    val mangaData: MutableState<StoredManga?>,
    val categories: MutableState<List<StoredMangaCategory>>
) : KoinComponent {

    val realmRepository: RealmRepository by inject()


    suspend fun selectCategories(sourceId: String, mangaId: String) {
        this.sourceAndMangaId.value = Pair(sourceId,mangaId)
        mangaData.value =
            realmRepository.getBookmark(RealmRepository.getBookmarkKey(sourceId,mangaId)).asFlow().first().obj
                ?: return
        categories.value = realmRepository.getCategories().asFlow().first().list

        this.sheetState.show()

        Napier.d { "SELECTING PASS SHOWING SHEET" }
    }

    suspend fun selectCategories() {
        this.sheetState.show()
    }


}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberCategorySelectContentState(
    sourceId: String = "",
    mangaId: String = "",
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        confirmValueChange = { it != ModalBottomSheetValue.HalfExpanded }
    ),
): CategorySelectContentState {
    return remember(sourceId, mangaId, sheetState) {

        CategorySelectContentState(
            sheetState = sheetState,
            sourceAndMangaId = mutableStateOf(Pair(sourceId,mangaId)),
            categories = mutableStateOf(listOf()),
            mangaData = mutableStateOf(null)
        )
    }
}

@Composable
fun CategorySelectContentSheetItem(
    data: Pair<StoredMangaCategory, Boolean>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    coroutineScope: CoroutineScope,
    realmRepository: RealmRepository = koinInject()
) {

    SideDrawerContent(modifier = Modifier.fillMaxWidth().height(70.dp),
        drawerContent = {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pressableModifier = Modifier.fillMaxHeight().aspectRatio(1.0f).borderRadius(5.dp)

                Pressable(
                    modifier = Modifier.then(pressableModifier),
                    backgroundColor = Color.Transparent,
                    onClick = onEdit
                ) {
                    Box(modifier = Modifier.fillMaxHeight()) {
                        VectorImage(
                            vector = FontAwesomeIcons.Solid.Pen,
                            contentDescription = "Edit",
                            modifier = Modifier.size(20.dp).align(Alignment.Center)
                        )
                    }
                }

                Pressable(
                    modifier = Modifier.then(pressableModifier),
                    backgroundColor = Color.Transparent,
                    onClick = {
                        coroutineScope.launch {
                            realmRepository.updateCategoryPosition(
                                data.first.id,
                                data.first.position - 1
                            )
                        }
                    }
                ) {
                    Box(modifier = Modifier.padding(5.dp)) {
                        VectorImage(
                            vector = FontAwesomeIcons.Solid.ChevronUp,
                            contentDescription = "Move Up",
                            modifier = Modifier.size(20.dp).align(Alignment.Center)
                        )
                    }
                }

                Pressable(
                    modifier = Modifier.then(pressableModifier),
                    backgroundColor = Color.Transparent,
                    onClick = {
                        coroutineScope.launch {
                            realmRepository.updateCategoryPosition(
                                data.first.id,
                                data.first.position + 1
                            )
                        }
                    }
                ) {
                    Box(modifier = Modifier.padding(5.dp)) {
                        VectorImage(
                            vector = FontAwesomeIcons.Solid.ChevronDown,
                            contentDescription = "Move Down",
                            modifier = Modifier.size(20.dp).align(Alignment.Center)
                        )
                    }
                }

                Pressable(
                    modifier = Modifier.then(pressableModifier),
                    backgroundColor = Color.Transparent,
                    onClick = {
                        coroutineScope.launch {
                            realmRepository.deleteCategory(data.first.id)
                        }
                    }
                ) {
                    Box(modifier = Modifier.padding(5.dp)) {
                        VectorImage(
                            vector = FontAwesomeIcons.Solid.Trash,
                            contentDescription = "Delete",
                            modifier = Modifier.size(20.dp).align(Alignment.Center)
                        )
                    }
                }

            }
        }) {
        Pressable(
            modifier = Modifier.matchParentSize(),
            onClick = onClick,
            backgroundColor = MaterialTheme.colorScheme.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = data.second,
                    onCheckedChange = null,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    data.first.name,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectContent(
    state: CategorySelectContentState = rememberCategorySelectContentState(),
    content: @Composable () -> Unit
) {

    val dialogController = LocalMiraDialogController.current
    val backHandler = LocalBackHandler.current

    val coroutineScope = rememberCoroutineScope()

    val sourceAndMangaId by state.sourceAndMangaId

    var categoriesData by state.categories

    var mangaData by state.mangaData

    val categories = remember(categoriesData.hashCode(),mangaData.hashCode()) {
        when(mangaData != null){
            true -> categoriesData.map {
                Pair(it, mangaData!!.categories.contains(it.id))
            }
            else -> listOf()
        }
    }

    Napier.d { "Categories ${categories.size} ${sourceAndMangaId.second} ${state.sheetState.isVisible}" }

    LaunchedEffect(sourceAndMangaId.first,sourceAndMangaId.second){
        state.realmRepository.getBookmark(RealmRepository.getBookmarkKey(sourceAndMangaId.first,sourceAndMangaId.second)).asFlow().collect{
            mangaData = it.obj
        }
    }

    LaunchedEffect(Unit){
        state.realmRepository.getCategories().asFlow().collect{
            categoriesData = it.list
        }
    }

    Box {
        ModalBottomSheetLayout(
            sheetState = state.sheetState,
            sheetShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
            sheetBackgroundColor = Color.Transparent,
            sheetContentColor = contentColorFor(MaterialTheme.colorScheme.surface),
            sheetContent = {
                Pressable(modifier = Modifier.fillMaxWidth().height(70.dp), onClick = {
                    dialogController.show(data = "",backHandler = backHandler) {initialText ->
                        TextInputCard(initialValue = initialText,
                            modifier = Modifier.align(
                                Alignment.Center
                            ),
                            commitText = "Create",
                            onCancelled = {
                                dialogController.hide()
                            },
                            onCommitted = { committedValue ->
                                dialogController.hide()
                                coroutineScope.launch {
                                    state.realmRepository.createCategory(committedValue)
                                }
                            })
                    }
                }, backgroundColor = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.padding(5.dp)) {
                            VectorImage(
                                vector = FontAwesomeIcons.Solid.Plus,
                                contentDescription = "Add",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "New Category",
                            modifier = Modifier.padding(vertical = 20.dp)
                        )
                    }
                }
                if (categories.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {

                        LazyColumn {
                            items(categories.size, key = {
                                categories[it].first.id
                            }) { idx ->

                                CategorySelectContentSheetItem(
                                    data = categories[idx], onClick = {
                                        coroutineScope.launch {
                                            state.realmRepository.updateBookmark(
                                                sourceAndMangaId.first,
                                                sourceAndMangaId.second
                                            ) {
                                                if (categories[idx].second) {
                                                    it.categories.remove(categories[idx].first.id)
                                                } else {
                                                    it.categories.add(categories[idx].first.id)
                                                }
                                            }
                                        }
                                    },
                                    onEdit = {
                                        dialogController.show(data = Pair(categories[idx].first.id,categories[idx].first.name),backHandler = backHandler) {initialData ->
                                            TextInputCard(initialValue = initialData.second,
                                                modifier = Modifier.align(
                                                    Alignment.Center
                                                ),
                                                commitText = "Edit",
                                                onCancelled = {
                                                    dialogController.hide()
                                                },
                                                onCommitted = { committedValue ->
                                                    dialogController.hide()
                                                    coroutineScope.launch {
                                                        state.realmRepository.updateCategoryName(
                                                            initialData.first,
                                                            committedValue
                                                        )
                                                    }
                                                })
                                        }
                                    },
                                    coroutineScope = coroutineScope
                                )
                            }
                        }
                    }
                }
            },
            content = content
        )
    }

}