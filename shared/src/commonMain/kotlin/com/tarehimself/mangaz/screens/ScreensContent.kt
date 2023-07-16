package com.tarehimself.mangaz.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import com.tarehimself.mangaz.screens.library.LibraryContent
import com.tarehimself.mangaz.screens.sources.SourcesContent


@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun ScreensContent(component: ScreensComponent) {
    Surface(modifier = Modifier.fillMaxSize().padding(20.dp,0.dp)) {
        Column {
            Surface(modifier = Modifier.weight(1.0f)) {
                Children(
                    stack = component.stack,
                    modifier = Modifier.fillMaxSize(),
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
            Surface(modifier = Modifier.height(70.dp).fillMaxWidth(), color = Color.Blue) {
                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { ->
                        component.showLibrary()
                    }, content = {
                        Text("Library")
                    })
                    Button(onClick = { ->
                        component.showSources()
                    }, content = {
                        Text("Search")
                    })
                }
            }
        }
    }

}