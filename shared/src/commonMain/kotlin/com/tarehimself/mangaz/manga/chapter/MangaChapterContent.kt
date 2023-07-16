package com.tarehimself.mangaz.manga.chapter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tarehimself.mangaz.data.MangaChapter


@Composable
fun MangaChapterContent(data: MangaChapter,onChapterSelected: (data: MangaChapter) -> Unit) {
    Surface(modifier = Modifier.height(60.dp).fillMaxWidth().padding(0.dp,10.dp)) {
        Surface(modifier = Modifier.fillMaxSize().clip(shape = RoundedCornerShape(5.dp)),color = Color.Blue) {
            Box(modifier = Modifier.fillMaxSize()){
                Text(data.name, modifier = Modifier.align(Alignment.Center))
                Button(onClick = {
                    onChapterSelected(data)
                }, modifier = Modifier.fillMaxSize(),colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent), elevation = ButtonDefaults.elevation(0.dp,0.dp,0.dp,0.dp,0.dp)){

                }
            }

        }
    }
}