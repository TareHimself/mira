package com.tarehimself.mira.screens

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.Pressable
import com.tarehimself.mira.VectorImage
import com.tarehimself.mira.screens.library.LibraryContent
import com.tarehimself.mira.screens.sources.SourcesContent
import compose.icons.FontAwesomeIcons
import compose.icons.Octicons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Bookmark
import compose.icons.fontawesomeicons.solid.Bookmark
import compose.icons.octicons.Search24


@Composable
fun ScreensContentBottomBarItem(
    vector: ImageVector,
    vectorSelected: ImageVector = vector,
    onClick: () -> Unit,
    label: String,
    isActive: Boolean
) {


    val pressableModifier = Modifier.height(50.dp).width(80.dp)
    val iconSize = 20.dp


    Pressable(modifier = pressableModifier, onClick = onClick) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Crossfade(isActive) {
                VectorImage(
                    vector = when (it) {
                        true -> {
                            vectorSelected
                        }

                        false -> vector
                    },
                    contentDescription = label,
                    modifier = Modifier.size(iconSize),
                    color = when (it) {
                        true -> Color.White
                        false -> Color.White.copy(alpha = 0.3f)
                    }
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                label, fontSize = 12.sp, color = when (isActive) {
                    true -> Color.White
                    false -> Color.White.copy(alpha = 0.3f)
                }
            )
        }
    }
}


@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun ScreensContent(component: ScreensComponent) {

    val state by component.state.subscribeAsState(policy = neverEqualPolicy())

    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
        Surface(modifier = Modifier.height(70.dp).fillMaxWidth()) {

            Row(
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScreensContentBottomBarItem(
                    vector = FontAwesomeIcons.Regular.Bookmark,
                    vectorSelected = FontAwesomeIcons.Solid.Bookmark,
                    onClick = {
                        component.showLibrary()
                    },
                    label = "Bookmarks",
                    isActive = state.activeScreen == ScreensComponent.EActiveScreen.Library
                )

                ScreensContentBottomBarItem(
                    vector = Octicons.Search24,
                    onClick = {
                        component.showSources()
                    },
                    label = "Search",
                    isActive = state.activeScreen == ScreensComponent.EActiveScreen.Sources
                )
            }
        }
    }) { padding ->
        Children(
            stack = component.stack,
            modifier = Modifier.fillMaxSize().padding(padding),
            animation = stackAnimation(
                fade()
            )
        ) {
            when (val child = it.instance) {
                is ScreensComponent.Child.LibraryChild -> LibraryContent(component = child.component)
                is ScreensComponent.Child.SourcesChild -> SourcesContent(component = child.component)
            }
        }
    }
}