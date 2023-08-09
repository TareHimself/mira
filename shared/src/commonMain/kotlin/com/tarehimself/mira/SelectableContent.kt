package com.tarehimself.mira

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.common.pxToDp
import io.github.aakira.napier.Napier

enum class ESelectableContentStatus {
    EXPANDED,
    COLLAPSED,
    EXPANDING,
    COLLAPSING
}

@ExperimentalMaterialApi
@Stable
class SelectableContentState<T>(
    val selectedItems: SnapshotStateList<T>,
    val scaffoldState: ScaffoldState,
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
        if (status.value != ESelectableContentStatus.EXPANDED) {
            status.value = ESelectableContentStatus.EXPANDING
            if(initialItems != null)
            {
                selectedItems.addAll(initialItems)
            }
        }
    }

    fun collapse() {

        if (status.value != ESelectableContentStatus.COLLAPSED) {
            selectedItems.clear()
            status.value = ESelectableContentStatus.COLLAPSING
        }
    }

    fun select(item: T){
        if(selectedItems.size < maxSelectedItems.value){
            selectedItems.add(item)
            Napier.d { "Selected $item" }
        }
    }

    fun deselect(item: T){
        if((selectedItems.size - 1) < minSelectedItems.value){
            collapse()
        }
        else

        {
            selectedItems.remove(item)
        }
    }

    fun isSelected(item: T): Boolean{
        return selectedItems.contains(item)
    }
}

@Composable
@ExperimentalMaterialApi
fun <T> rememberSelectableContentState(
    selectedItems: SnapshotStateList<T> = remember { mutableStateListOf() },
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    minSelectedItems: MutableState<Int> = remember { mutableStateOf(1) },
    maxSelectedItems: MutableState<Int> = remember { mutableStateOf(Int.MAX_VALUE) },
): SelectableContentState<T> {
    return remember(selectedItems, scaffoldState) {

        SelectableContentState(
            selectedItems = selectedItems,
            scaffoldState = scaffoldState,
            topSheetDesiredHeight = mutableStateOf(999999),
            bottomSheetDesiredHeight = mutableStateOf(999999),
            status = mutableStateOf(ESelectableContentStatus.COLLAPSED),
            minSelectedItems = minSelectedItems,
            maxSelectedItems = maxSelectedItems
        )
    }
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
@Composable
fun <T> SelectableContent(
    topBar: @Composable() (() -> Unit) = {},
    topSheetContent: @Composable() (() -> Unit) = {},
    bottomSheetContent: @Composable() (() -> Unit) = {},
    modifier: Modifier = Modifier,
    state: SelectableContentState<T> = rememberSelectableContentState(),
    animationSpec: AnimationSpec<Float> = spring(),
    content: @Composable() (() -> Unit)
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
            Box() {
                topBar()
                Box(
                    modifier = Modifier.clip(
                        RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp)
                    ).offset(y=topSheetDesiredHeightDp * ((1.0f - sheetVisibilityAlpha) * -1)).onGloballyPositioned {
                        state.topSheetDesiredHeight.value = it.size.height
                    }
                ) {
                    topSheetContent()
                }
            }
        },
        modifier = modifier,
        backgroundColor = Color.Transparent,
        scaffoldState = state.scaffoldState,
    ) {padding ->
        Box(){
            Box(modifier = Modifier.padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + (sheetDesiredHeightDp * (sheetVisibilityAlpha)))) {
                content()
            }
            // Bottom sheet
            Box(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                .offset(y = sheetDesiredHeightDp * (1.0f - sheetVisibilityAlpha)).clip(
                    RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                ).onGloballyPositioned {
                    state.bottomSheetDesiredHeight.value = it.size.height
                }) {
                Box(modifier = Modifier) {
                    bottomSheetContent()
                }
            }
        }

    }
}