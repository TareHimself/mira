package com.tarehimself.mira.manga.viewer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.VectorImage
import com.tarehimself.mira.common.debug
import com.tarehimself.mira.data.MangaChapter
import compose.icons.Octicons
import compose.icons.octicons.Download16


@Composable
fun MangaChapterContent(data: MangaChapter,onChapterSelected: (data: MangaChapter) -> Unit) {
    val itemHeight = 60.dp

    Pressable(modifier = Modifier.fillMaxWidth().height(itemHeight), onClick = {
        onChapterSelected(data)
    }) {

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.padding(horizontal = 20.dp)) {
            Box {
                Text(data.name, modifier = Modifier.align(Alignment.CenterStart))
            }

            Pressable(modifier = Modifier.size(30.dp), onClick = {
                debug("Downloading Chapter ${data.name}")
            }) {
                VectorImage(vector = Octicons.Download16, contentDescription = "Download Chapter")
            }
        }
    }
}