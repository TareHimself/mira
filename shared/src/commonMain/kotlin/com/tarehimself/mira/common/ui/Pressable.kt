package com.tarehimself.mira.common.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Pressable(
    onClick: () -> Unit = { },
    onLongClick: (() -> Unit)? = null,
    onDoubleClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    tonalElevation: Dp =  0.dp,
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
            role = Role.Button,
            indication = LocalIndication.current,
            interactionSource = interactionSource,

        ), color = backgroundColor, tonalElevation = tonalElevation
    ) {
        content(interactionSource)
    }

}