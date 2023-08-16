package com.tarehimself.mira

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.tarehimself.mira.manga.reader.MangaReaderContent
import com.tarehimself.mira.manga.viewer.MangaViewerContent
import com.tarehimself.mira.screens.ScreensContent
import com.tarehimself.mira.screens.sources.MangaSearchContent
import io.github.aakira.napier.Napier


@Composable
fun RootContent(component: RootComponent){


    Surface(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets(0.dp)),
        color = MaterialTheme.colorScheme.background,
    ) {
//        animation = stackAnimation( //Causing infinite loops on reader
//            slide())
        Children(stack = component.stack, modifier = Modifier.fillMaxSize(), animation = stackAnimation(slide())){
            when(val child = it.instance){
                is RootComponent.Child.ScreensChild -> ScreensContent(component = child.component)
                is RootComponent.Child.MangaSearchChild -> MangaSearchContent(component = child.component)
                is RootComponent.Child.MangaViewerChild -> MangaViewerContent(component = child.component)
                is RootComponent.Child.MangaReaderChild -> {
                    Napier.w("Creating new reader content", tag = "Infinite Reader bug")
                    MangaReaderContent(component = child.component)
                }
            }
        }
    }
}