package com.tarehimself.mira.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.common.ui.Pressable


@Composable
fun SettingsContentItem(
    onPressed: () -> Unit = {},
    onLongPressed: () -> Unit = {},
    content: @Composable RowScope.() -> Unit
) {

    val itemHeight = remember { 60.dp }

    Pressable(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = itemHeight),
        onClick = onPressed,
        onLongClick = onLongPressed,
        backgroundColor = MaterialTheme.colorScheme.surface
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            content()
        }
    }
}

@Composable
fun SettingsContentItem(
    content: @Composable RowScope.() -> Unit
) {

    val itemHeight = remember { 60.dp }

    Surface(
        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = itemHeight),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            content()
        }
    }
}