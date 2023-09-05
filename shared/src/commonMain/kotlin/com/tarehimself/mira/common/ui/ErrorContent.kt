package com.tarehimself.mira.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

@Composable
fun ErrorContent(message: String, fontSize: TextUnit = 20.sp) {

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "(ˉ﹃ˉ)\n\n$message",
            Modifier.align(Alignment.Center).alpha(0.6f),
            fontSize = fontSize,
            textAlign = TextAlign.Center
        )
    }
}