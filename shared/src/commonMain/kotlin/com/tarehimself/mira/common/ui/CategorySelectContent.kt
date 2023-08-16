package com.tarehimself.mira.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.VectorImage
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.StoredMangaCategory
import com.tarehimself.mira.data.rememberIsBookmarked
import com.tarehimself.mira.data.rememberMangaCategories
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ChevronDown
import compose.icons.fontawesomeicons.solid.ChevronUp
import compose.icons.fontawesomeicons.solid.Pen
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.Trash
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


enum class ECategorySelectStatus {
    Idle,
    Creating,
    Editing
}

@OptIn(ExperimentalMaterialApi::class)
@Stable
class CategorySelectContentState constructor(
    val sheetState: ModalBottomSheetState,
    val sourceId: MutableState<String>,
    val mangaId: MutableState<String>,
    val status: MutableState<ECategorySelectStatus>,
) : KoinComponent {

    val realmRepository: RealmRepository by inject()

    suspend fun selectCategories(sourceId: String, mangaId: String) {
        Napier.d { "$sourceId $mangaId ${realmRepository.has(sourceId, mangaId)}" }
        if (sourceId.isEmpty() || mangaId.isEmpty() || !realmRepository.has(sourceId, mangaId)) {
            return
        }
        Napier.d { "SELECTING PASS SHOWING SHEET" }
        this.sourceId.value = sourceId
        this.mangaId.value = mangaId
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
            sourceId = mutableStateOf(mangaId),
            mangaId = mutableStateOf(sourceId),
            status = mutableStateOf(ECategorySelectStatus.Idle)
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

    SlidableContent(modifier = Modifier.fillMaxWidth().height(70.dp),
        background = {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pressableModifier = Modifier.fillMaxHeight().aspectRatio(1.0f).clip(
                    RoundedCornerShape(5.dp)
                )

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

    val coroutineScope = rememberCoroutineScope()

    var status by state.status

    val isBookmarked by rememberIsBookmarked(state.sourceId.value, state.mangaId.value, false)

    val categories by if (isBookmarked) {
        rememberMangaCategories(state.sourceId.value, state.mangaId.value)
    } else {
        remember { mutableStateOf(listOf()) }
    }

    var textFieldValue by remember { mutableStateOf("") }

    var itemBeingEdited by remember { mutableStateOf("") }

    Box {
        ModalBottomSheetLayout(
            sheetState = state.sheetState,
            sheetShape = RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp),
            sheetBackgroundColor = Color.Transparent,
            sheetContentColor = contentColorFor(MaterialTheme.colorScheme.surface),
            sheetContent = {
                if (isBookmarked) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Pressable(modifier = Modifier.fillMaxWidth().height(70.dp), onClick = {
                            status = ECategorySelectStatus.Creating
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

                        LazyColumn {
                            items(categories.size, key = {
                                categories[it].first.id
                            }) { idx ->

                                CategorySelectContentSheetItem(
                                    data = categories[idx], onClick = {
                                        coroutineScope.launch {
                                            state.realmRepository.updateManga(
                                                state.sourceId.value,
                                                state.mangaId.value
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
                                        itemBeingEdited = categories[idx].first.id
                                        textFieldValue = categories[idx].first.name
                                        status = ECategorySelectStatus.Editing
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
        if (status != ECategorySelectStatus.Idle) {
            Surface(
                modifier = Modifier.matchParentSize(),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Column(modifier = Modifier.clip(RoundedCornerShape(5.dp)).fillMaxWidth(0.8f)) {
                        TextField(
                            value = textFieldValue,
                            onValueChange = { textFieldValue = it },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.height(40.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val buttonModifier = Modifier.fillMaxHeight().weight(1.0f)
                            Pressable(
                                onClick = {
                                    status = ECategorySelectStatus.Idle
                                },
                                backgroundColor = MaterialTheme.colorScheme.primary,
                                modifier = buttonModifier
                            ) {
                                Box {
                                    Text("Cancel", modifier = Modifier.align(Alignment.Center))
                                }

                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Pressable(
                                onClick = {
                                    when (status) {
                                        ECategorySelectStatus.Editing -> {
                                            coroutineScope.launch {
                                                status = ECategorySelectStatus.Idle
                                                state.realmRepository.updateCategoryName(
                                                    itemBeingEdited,
                                                    textFieldValue
                                                )
                                                textFieldValue = ""
                                            }
                                        }

                                        else -> {
                                            coroutineScope.launch {
                                                status = ECategorySelectStatus.Idle
                                                state.realmRepository.createCategory(textFieldValue)
                                                textFieldValue = ""
                                            }
                                        }
                                    }

                                },
                                backgroundColor = MaterialTheme.colorScheme.primary,
                                modifier = buttonModifier
                            ) {
                                Box {
                                    Text(
                                        when (status) {
                                            ECategorySelectStatus.Editing -> {
                                                "Edit"
                                            }

                                            else -> {
                                                "Create"
                                            }
                                        },
                                        modifier = Modifier.align(Alignment.Center)
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