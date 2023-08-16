package com.tarehimself.mira.common.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.VectorImage
import com.tarehimself.mira.common.pxToDp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.ChevronDown
import compose.icons.fontawesomeicons.solid.ChevronUp
import compose.icons.fontawesomeicons.solid.Pen
import compose.icons.fontawesomeicons.solid.Trash
import io.github.aakira.napier.Napier
import kotlinx.coroutines.launch
import kotlin.math.abs


@Composable
fun SlidableContent(modifier: Modifier = Modifier,background: @Composable BoxScope.() -> Unit,content: @Composable BoxScope.() -> Unit){

    var pressableOffset by remember { mutableStateOf(0.0f) }

    var backgroundWidth by remember { mutableStateOf(0) }

    val maxOffset =
        remember(backgroundWidth) { (backgroundWidth * -1.0f) }

    val pressableOffsetAnimated by animateDpAsState(pressableOffset.toInt().pxToDp())

    val indicatorAnimated by animateFloatAsState(when(abs(maxOffset.toInt()) == 0){
        true -> 0.0f
        else -> when(abs(pressableOffset.toInt()) == 0){
            true -> 1.0f
            else -> {
                (maxOffset - pressableOffset) / maxOffset
            }
        }
    })

    Napier.d { "Max offset $maxOffset $indicatorAnimated" }

    LaunchedEffect(maxOffset){
        if(pressableOffset < maxOffset){
            pressableOffset = 0.0f
        }
    }

    Box(modifier = Modifier.then(modifier).pointerInput(maxOffset) {
        detectHorizontalDragGestures(onDragEnd = {
            val shouldExpand = pressableOffset < (maxOffset / 2)
            pressableOffset = if (shouldExpand) {
                maxOffset
            } else {
                0.0f
            }
        }) { _, dragAmount ->
            pressableOffset = (pressableOffset + dragAmount).coerceIn(maxOffset.toFloat(), 0.0f)
        }
    }) {
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.primary).fillMaxHeight()
                .align(Alignment.CenterEnd).onGloballyPositioned {
                    backgroundWidth = it.size.width
                }) {
            background(this)
        }
        Box(modifier = Modifier.matchParentSize().offset(x=pressableOffsetAnimated)){
            content(this)
        }

        Box(modifier = Modifier.fillMaxHeight().width(2.dp).align(Alignment.CenterEnd).alpha(indicatorAnimated)){
            Surface(modifier = Modifier.matchParentSize(),color = MaterialTheme.colorScheme.primary) {

            }
        }
    }
}