@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")
package com.tarehimself.mangaz

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.tarehimself.mangaz.data.ImageLoader
import com.tarehimself.mangaz.manga.reader.MangaReaderContent
import com.tarehimself.mangaz.manga.viewer.MangaViewerContent
import com.tarehimself.mangaz.screens.ScreensContent
import com.tarehimself.mangaz.screens.sources.MangaSearchContent


@Composable
fun RootContent(component: RootComponent){

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colors.background,

        ) {
        Children(stack = component.stack, modifier = Modifier.fillMaxSize(), animation = stackAnimation(
            slide())){
            when(val child = it.instance){
                is RootComponent.Child.ScreensChild -> ScreensContent(component = child.component)
                is RootComponent.Child.MangaSearchChild -> MangaSearchContent(component = child.component)
                is RootComponent.Child.MangaViewerChild -> MangaViewerContent(component = child.component)
                is RootComponent.Child.MangaReaderChild -> MangaReaderContent(component = child.component)
            }
        }
    }


//    CompositionLocalProvider(LocalKamelConfig provides kamelConfig) {
//        Surface(
//            modifier = Modifier.fillMaxSize(),
//            color = MaterialTheme.colors.background,
//
//            ) {
//            SearchScreen()
//        }
//    }
}