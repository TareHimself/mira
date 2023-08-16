package com.tarehimself.mira.common.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.VectorImage
import compose.icons.Octicons
import compose.icons.octicons.Search24


@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SearchBarContent(
    value: String,
    onChanged: (change: String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Transparent,
    textColor: Color = Color.White,
    height: Dp = 50.dp
) {

    val scrollState = rememberScrollState()
    val query: MutableState<String> = remember { mutableStateOf(value) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(modifier = modifier.height(height).clip(shape = RoundedCornerShape(height)), color = color) {
        TextField(query.value,
            onValueChange = {
                query.value = it
            },
            modifier = modifier.fillMaxSize().horizontalScroll(scrollState),
            maxLines = 1,
            colors = TextFieldDefaults.textFieldColors(
                textColor = textColor,
//                backgroundColor = Color.Transparent,

            focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor =  Color.Transparent
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                    onChanged(query.value)
                }
            ),
            leadingIcon = {
                VectorImage(vector = Octicons.Search24, contentDescription = "Search Icon", color = textColor)
            }
        )
    }
}