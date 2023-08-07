@file:Suppress("INLINE_FROM_HIGHER_PLATFORM")
package com.tarehimself.mira

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

//    val windowInsetsController = rememberWindowInsetsController()
//
//
//    LaunchedEffect(Unit) {
//        // Hide the status bars
//        windowInsetsController?.setIsStatusBarsVisible(false)
//        // Hide the navigation bars
//        windowInsetsController?.setIsNavigationBarsVisible(false)
//        // Change an options for behavior when system bars are hidden
//        windowInsetsController?.setSystemBarsBehavior(SystemBarsBehavior.Immersive)
//
////        component.shareString("Please dont crash my app")
//    }

    Surface(

        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets(0.dp)),
        color = MaterialTheme.colors.background,
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