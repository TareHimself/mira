package com.tarehimself.mira

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

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