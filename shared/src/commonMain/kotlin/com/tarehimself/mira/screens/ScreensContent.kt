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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.common.LocalBackHandler
import com.tarehimself.mira.common.ui.Pressable
import com.tarehimself.mira.common.ui.VectorImage
import com.tarehimself.mira.common.LocalWindowInsets
import com.tarehimself.mira.common.borderRadius
import com.tarehimself.mira.common.useBottomInsets
import com.tarehimself.mira.screens.bookmarks.BookmarksContent
import com.tarehimself.mira.screens.downloads.DownloadsContent
import com.tarehimself.mira.screens.settings.SettingsContent
import com.tarehimself.mira.screens.sources.SourcesContent
import compose.icons.FontAwesomeIcons
import compose.icons.Octicons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Bookmark
import compose.icons.fontawesomeicons.solid.Bookmark
import compose.icons.fontawesomeicons.solid.Cog
import compose.icons.octicons.Search24


@Composable
fun ScreensContentBottomBarItem(
    vector: ImageVector,
    vectorSelected: ImageVector = vector,
    onClick: () -> Unit,
    label: String,
    isActive: Boolean
) {


    val iconSize = 20.dp


    Pressable(
        modifier = Modifier.height(60.dp).width(80.dp).borderRadius(5.dp),
        onClick = onClick
    ) {
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
                        true -> LocalContentColor.current
                        false -> LocalContentColor.current.copy(alpha = 0.3f)
                    }
                )
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                label, fontSize = 12.sp, color = when (isActive) {
                    true -> LocalContentColor.current
                    false -> LocalContentColor.current.copy(alpha = 0.3f)
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreensContent(component: ScreensComponent) {

    val state by component.state.subscribeAsState(policy = neverEqualPolicy())

    CompositionLocalProvider(LocalBackHandler provides component) {
        Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
            Surface(modifier = Modifier.useBottomInsets()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(70.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ScreensContentBottomBarItem(
                        vector = FontAwesomeIcons.Regular.Bookmark,
                        vectorSelected = FontAwesomeIcons.Solid.Bookmark,
                        onClick = {
                            component.showBookmarks()
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

                    ScreensContentBottomBarItem(
                        vector = FontAwesomeIcons.Solid.Cog,
                        onClick = {
                            component.showSettings()
                        },
                        label = "Settings",
                        isActive = state.activeScreen == ScreensComponent.EActiveScreen.Settings
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
                    is ScreensComponent.Child.BookmarksChild -> BookmarksContent(component = child.component)
                    is ScreensComponent.Child.SourcesChild -> SourcesContent(component = child.component)
                    is ScreensComponent.Child.DownloadsChild -> DownloadsContent(component = child.component)
                    is ScreensComponent.Child.SettingsChild -> SettingsContent(component = child.component)
                }
            }
        }
    }
}