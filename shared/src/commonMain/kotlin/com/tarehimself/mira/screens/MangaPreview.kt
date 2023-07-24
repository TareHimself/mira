@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")

package com.tarehimself.mira.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.common.AsyncImage
import com.tarehimself.mira.common.Constants
import com.tarehimself.mira.common.debug
import com.tarehimself.mira.data.ApiMangaPreview
import com.tarehimself.mira.data.MangaPreview
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.subscribeLibraryUpdate
import org.koin.compose.koinInject

//.border(width = 1.dp,
//color = Color.Transparent,
//shape = RoundedCornerShape(50.dp))
@Composable
fun <T>MangaPreviewContent(sourceId: String, data: T, onItemSelected: (item: T) -> Unit,realmRepository: RealmRepository = koinInject()) where T : MangaPreview {

    val textContainerSize = remember { mutableStateOf(IntSize.Zero) }
    val mainContainerSize = remember { mutableStateOf(IntSize.Zero) }
    val isBookmarked = subscribeLibraryUpdate({
        debug("Checking if ${data.id} is in library ${it.has(sourceId,data.id)}")
        it.has(sourceId,data.id)
    })

    val brush = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)),
        startY = (mainContainerSize.value.height - (textContainerSize.value.height * 3)).toFloat(),
        endY = mainContainerSize.value.height.toFloat()
    )


    Surface(
        modifier = Modifier.fillMaxWidth().border(
            width = 2.dp,
            color = Color.Transparent,
            shape = RoundedCornerShape(5.dp)
        ).padding(5.dp).aspectRatio(Constants.mangaCoverRatio),
        color = Color.Transparent,

        ) {
        Pressable(
            modifier = Modifier.fillMaxSize().clip(shape = RoundedCornerShape(5.dp))
                .onGloballyPositioned {
                    mainContainerSize.value = it.size
                }, onClick = {
                onItemSelected(data)
            },
        onLongClick = {
            if(!isBookmarked){
                realmRepository.addToLibrary(sourceId,data)
            }
        }) {

            Box {
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

                Crossfade(data is ApiMangaPreview && isBookmarked, modifier = Modifier.fillMaxSize(), animationSpec = tween(200)){
                    if(it){
                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black.copy(alpha = 0.7f)) {

                        }
                    }
                }
            }
        }
    }
}