@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.tarehimself.mangaz.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tarehimself.mangaz.common.AsyncImage
import com.tarehimself.mangaz.common.Constants
import com.tarehimself.mangaz.data.MangaPreview

//.border(width = 1.dp,
//color = Color.Transparent,
//shape = RoundedCornerShape(50.dp))
@Composable
fun SearchResult(data: MangaPreview, onItemSelected: (item: MangaPreview) -> Unit) {

    val textContainerSize = remember { mutableStateOf(IntSize.Zero) }
    val mainContainerSize = remember { mutableStateOf(IntSize.Zero) }

    val brush = Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
        startY = (mainContainerSize.value.height - (textContainerSize.value.height * 3)).toFloat(),
        endY = mainContainerSize.value.height.toFloat()
    )



    Surface(
        modifier = Modifier.fillMaxWidth().border(
            width = 2.dp,
            color = Color.Transparent,
            shape = RoundedCornerShape(5.dp)
        ).padding(horizontal = 5.dp, vertical = 5.dp).aspectRatio(Constants.mangaCoverRatio),
        color = Color.Transparent,

        ) {
        Box {
            Box(modifier = Modifier.fillMaxSize().clip(shape = RoundedCornerShape(5.dp)).onGloballyPositioned {
                mainContainerSize.value = it.size
            }) {
                AsyncImage(
                    source = data.cover,
                    contentDescription = "Manga Cover",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier.matchParentSize().background(brush))
                Surface(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
                        .padding(5.dp).onGloballyPositioned {
                                                            textContainerSize.value = it.size
                        },
                    color = Color.Transparent
                ) {
                    Text(
                        data.name,
                        modifier = Modifier.align(Alignment.BottomStart).background(Color.Transparent),
                        maxLines = 2,
                        fontSize = 12.sp,

                    )

                }

            }

            Button(
                onClick = {
                    onItemSelected(data)
                },
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent),
                elevation = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
            ) {

            }
        }


    }
}