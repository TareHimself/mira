package com.tarehimself.mira

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.tarehimself.mira.common.LocalBackHandler
import com.tarehimself.mira.common.LocalWindowInsets
import com.tarehimself.mira.common.dpToPx
import com.tarehimself.mira.common.ui.MiraDialogContainer
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.manga.reader.ReaderContent
import com.tarehimself.mira.manga.search.GlobalSearchContent
import com.tarehimself.mira.manga.viewer.ViewerContent
import com.tarehimself.mira.screens.ScreensContent
import com.tarehimself.mira.manga.search.SearchContent
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootContent(component: RootComponent) {


    val imageRepository = koinInject<ImageRepository>()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) {
        CompositionLocalProvider(LocalBackHandler provides component){
            CompositionLocalProvider(
                LocalWindowInsets provides WindowInsets(
                    top = it.calculateTopPadding(),
                    bottom = it.calculateBottomPadding()
                )
            ) {
                MiraDialogContainer {
                    BoxWithConstraints {
                        imageRepository.deviceWidth.value = this.maxWidth.dpToPx().toInt()
                        Children(
                            stack = component.stack,
                            modifier = Modifier.fillMaxSize(),
                            animation = stackAnimation(slide())
                        ) {
                            when (val child = it.instance) {
                                is RootComponent.Child.ScreensChild -> ScreensContent(component = child.component)
                                is RootComponent.Child.SearchChild -> SearchContent(component = child.component)
                                is RootComponent.Child.ViewerChild -> ViewerContent(component = child.component)
                                is RootComponent.Child.ReaderChild -> ReaderContent(component = child.component)
                                is RootComponent.Child.GlobalSearchChild -> GlobalSearchContent(component = child.component)
                            }
                        }
                    }
                }
            }
        }
    }
//    Surface(
//        modifier = Modifier.fillMaxSize(),
//        color = ,
//    ) {
//
//
//    }
}