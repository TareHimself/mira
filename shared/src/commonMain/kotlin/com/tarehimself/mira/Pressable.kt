package com.tarehimself.mira

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Pressable(
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onDoubleClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    content: @Composable (interactionSource: MutableInteractionSource) -> Unit
) {

//    Box(modifier = modifier){
//        Button(
//            onClick = onClick,
//            modifier = Modifier.matchParentSize().padding(vertical = buttonVerticalPadding, horizontal = buttonHorizontalPadding),
//            colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
//            elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
//        ) {
//            content(
//        }
//    }
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }


    Surface(
        modifier = modifier.combinedClickable(
            enabled = true,
            onClickLabel = null,
            onLongClickLabel = null,
            onClick = onClick,
            onLongClick = onLongClick,
            onDoubleClick = onDoubleClick,
            role = null,
            indication = LocalIndication.current,
            interactionSource = interactionSource
        ), color = backgroundColor
    ) {
        content(interactionSource)
    }

}