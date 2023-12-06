package com.tarehimself.mira.common.ui

import BottomSheetIcon
import DropdownMenu
import DropdownMenuItem
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.common.borderRadius
import com.tarehimself.mira.common.pxToDp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.LayerGroup
import io.github.aakira.napier.Napier
import kotlin.math.min

enum class ESelectableContentStatus {
    EXPANDED,
    COLLAPSED,
    EXPANDING,
    COLLAPSING
}

@Stable
class SelectableContentState<T>(
    val selectedItems: MutableState<LinkedHashSet<T>>,
    var bottomSheetDesiredHeight: MutableState<Int>,
    var topSheetDesiredHeight: MutableState<Int>,
    val status: MutableState<ESelectableContentStatus>,
    val minSelectedItems: MutableState<Int>,
    val maxSelectedItems: MutableState<Int>
) {
    val isExpanded: Boolean
        get() = status.value == ESelectableContentStatus.EXPANDED

    val isCollapsed: Boolean
        get() = status.value == ESelectableContentStatus.COLLAPSED

    val isExpanding: Boolean
        get() = status.value == ESelectableContentStatus.EXPANDING

    val isCollapsing: Boolean
        get() = status.value == ESelectableContentStatus.COLLAPSING

    fun expand(initialItems: List<T>?) {
        mutate {
            if (status.value != ESelectableContentStatus.EXPANDED) {
                status.value = ESelectableContentStatus.EXPANDING
                if (initialItems != null) {
                    it.addAll(initialItems)
                }
            }
        }
    }

    fun collapse() {
        if (status.value != ESelectableContentStatus.COLLAPSED) {
            if (selectedItems.value.isNotEmpty()) {
                mutate(autoCollapse = false) {
                    it.clear()
                }
            }
            status.value = ESelectableContentStatus.COLLAPSING
        }
    }

    fun select(item: T) {
        mutate {
            if (it.size < maxSelectedItems.value) {
                it.add(item)
            }
        }
    }

    fun select(items: List<T>) {
        mutate {
            if (it.size < maxSelectedItems.value) {
                it.addAll(
                    items.subList(
                        0, min(items.size,maxSelectedItems.value - (items.size + it.size))
                    )
                )
            }
        }
    }

    fun deselect(item: T) {
        mutate {
            it.remove(item)
        }
    }

    fun deselect(items: List<T>) {
        mutate {
            it.removeAll(items)
        }
    }

    fun mutate(autoCollapse: Boolean = true,block: (items: LinkedHashSet<T>) -> Unit) {
        block(selectedItems.value)
        if(autoCollapse && selectedItems.value.size < minSelectedItems.value){
            selectedItems.value.clear()
            status.value = ESelectableContentStatus.COLLAPSING
        }

        selectedItems.value = selectedItems.value
    }

    fun isSelected(item: T): Boolean {
        return selectedItems.value.contains(item)
    }
}

@Composable
fun <T> rememberSelectableContentState(
    selectedItems: LinkedHashSet<T> = remember { linkedSetOf() },
    minSelectedItems: MutableState<Int> = remember { mutableStateOf(1) },
    maxSelectedItems: MutableState<Int> = remember { mutableStateOf(Int.MAX_VALUE) },
): SelectableContentState<T> {
    return remember(selectedItems, minSelectedItems, maxSelectedItems) {

        SelectableContentState(
            selectedItems = mutableStateOf(selectedItems, neverEqualPolicy()),
            topSheetDesiredHeight = mutableStateOf(999999),
            bottomSheetDesiredHeight = mutableStateOf(999999),
            status = mutableStateOf(ESelectableContentStatus.COLLAPSED),
            minSelectedItems = minSelectedItems,
            maxSelectedItems = maxSelectedItems
        )
    }
}

@OptIn(
    ExperimentalMaterial3Api::class
)
@Composable
fun <T> SelectableContent(
    topBar: @Composable() (() -> Unit) = {},
    topSheetContent: @Composable() (() -> Unit) = {},
    bottomSheetContent: @Composable() (() -> Unit) = {},
    modifier: Modifier = Modifier,
    state: SelectableContentState<T> = rememberSelectableContentState(),
    animationSpec: AnimationSpec<Float> = spring(),
    content: @Composable() (BoxScope.(padding: PaddingValues) -> Unit)
) {

    val sheetDesiredHeightDp = (state.bottomSheetDesiredHeight.value).pxToDp()
    val topSheetDesiredHeightDp = (state.topSheetDesiredHeight.value).pxToDp()

    val sheetVisibilityAlpha by animateFloatAsState(
        when (state.status.value) {
            ESelectableContentStatus.COLLAPSED -> 0.0f
            ESelectableContentStatus.EXPANDED -> 1.0f
            ESelectableContentStatus.COLLAPSING -> 0.0f
            ESelectableContentStatus.EXPANDING -> 1.0f
        },
        animationSpec = animationSpec,
        finishedListener = {
            state.status.value = if (state.isExpanding) {
                ESelectableContentStatus.EXPANDED
            } else {
                ESelectableContentStatus.COLLAPSED
            }
        }
    )

    Scaffold(
        topBar = {
            Box {
                topBar()
                Box(
                    modifier = Modifier.borderRadius(bottomStart = 5.dp, bottomEnd = 5.dp)
                        .offset(y = topSheetDesiredHeightDp * ((1.0f - sheetVisibilityAlpha) * -1))
                        .onGloballyPositioned {
                            state.topSheetDesiredHeight.value = it.size.height
                        }
                ) {
                    topSheetContent()
                }
            }
        },
        modifier = modifier,
        containerColor = Color.Transparent,
    ) { padding ->
        Box {
            content(
                PaddingValues(
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + (sheetDesiredHeightDp * (sheetVisibilityAlpha))
                )
            )
            // Bottom sheet
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .offset(y = sheetDesiredHeightDp * (1.0f - sheetVisibilityAlpha))
                .borderRadius(topStart = 5.dp, topEnd = 5.dp).onGloballyPositioned {
                    state.bottomSheetDesiredHeight.value = it.size.height
                }) {
                Box(modifier = Modifier) {
                    bottomSheetContent()
                }
            }
        }

    }
}

@Composable
fun BottomSheetSelectionIcon(
    onSelectAll: () -> Unit,
    onSelectInverse: () -> Unit,
    onDeselect: () -> Unit,
    extras: @Composable ColumnScope.(performAction: (action: () -> Unit) -> Unit) -> Unit = {}
) {
    var shouldShowSelectDropdown by remember { mutableStateOf(false) }
    val dropdownAction: (action: () -> Unit) -> Unit = remember {
        {
            shouldShowSelectDropdown = false
            it()
        }
    }

    BottomSheetIcon(vector = FontAwesomeIcons.Solid.LayerGroup,
        contentDescription = "Select",
        onClick = {
            shouldShowSelectDropdown = !shouldShowSelectDropdown
        },
        content = {
            DropdownMenu(expanded = shouldShowSelectDropdown,
                onDismissRequest = { shouldShowSelectDropdown = false }) {
                DropdownMenuItem(text = {
                    Text("All")
                }, onClick = {
                    dropdownAction {
                        onSelectAll()
                    }
                })
                DropdownMenuItem(text = {
                    Text("Inverse")
                }, onClick = {
                    dropdownAction {
                        onSelectInverse()
                    }
                })
                extras(dropdownAction)
                DropdownMenuItem(text = {
                    Text("Deselect")
                }, onClick = {
                    dropdownAction {
                        onDeselect()
                    }
                })

            }
        })
}