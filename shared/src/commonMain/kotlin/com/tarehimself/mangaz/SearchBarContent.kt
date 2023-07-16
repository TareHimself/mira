package com.tarehimself.mangaz

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Surface
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchBarContent(
    value: String,
    onChanged: (change: String) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    textColor: Color = Color.White
) {

    val query: MutableState<String> = remember { mutableStateOf(value) }

    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(modifier = modifier, color = color) {
        Box(modifier = Modifier.fillMaxSize()) {
            TextField(query.value,
                onValueChange = {
                    query.value = it
                },
                modifier = modifier.fillMaxSize(),
                colors = TextFieldDefaults.textFieldColors(
                    textColor = textColor,
                    backgroundColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        onChanged(query.value)
                    }
                )
            )
        }
    }
}