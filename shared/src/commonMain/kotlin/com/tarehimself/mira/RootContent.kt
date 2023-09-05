package com.tarehimself.mira

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.tarehimself.mira.common.LocalWindowInsets
import com.tarehimself.mira.common.dpToPx
import com.tarehimself.mira.common.ui.MiraDialogContainer
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.manga.reader.MangaReaderContent
import com.tarehimself.mira.manga.viewer.MangaViewerContent
import com.tarehimself.mira.screens.ScreensContent
import com.tarehimself.mira.manga.search.MangaSearchContent
import io.github.aakira.napier.Napier
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootContent(component: RootComponent) {


    val imageRepository = koinInject<ImageRepository>()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) {
        CompositionLocalProvider(
            LocalWindowInsets provides WindowInsets(
                top = it.calculateTopPadding(),
                bottom = it.calculateBottomPadding()
            )
        ) {

            BoxWithConstraints {
                imageRepository.deviceWidth.value = this.maxWidth.dpToPx().toInt()
                Children(
                    stack = component.stack,
                    modifier = Modifier.fillMaxSize(),
                    animation = stackAnimation(slide())
                ) {
                    when (val child = it.instance) {
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
    }
//    Surface(
//        modifier = Modifier.fillMaxSize(),
//        color = ,
//    ) {
//
//
//    }
}