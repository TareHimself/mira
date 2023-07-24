package com.tarehimself.mira.common

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.tarehimself.mira.data.ImageRepository
import org.koin.compose.koinInject


@Composable
fun AsyncImage(source: String, contentDescription: String, contentScale: ContentScale = ContentScale.None, crossFadeDuration: Int = 500, modifier: Modifier = Modifier, imageRepository: ImageRepository = koinInject(), placeholder: (@Composable() () -> Unit)? = null){

    val resource: MutableState<ImageBitmap?> = remember { mutableStateOf(imageRepository.getCached(source)) }

    LaunchedEffect(source) {
        resource.value = null

        val bitmap = imageRepository.loadBitmap(source)
        if (bitmap != null) {
            resource.value = bitmap
        }
    }

    Crossfade(resource.value, animationSpec = tween(crossFadeDuration)){
        if(it != null){
            Image(
                bitmap = it,
                contentDescription = contentDescription,
                modifier = modifier,
                contentScale = contentScale
            )
        }
        else if(placeholder != null){
            placeholder()
        }
    }

}