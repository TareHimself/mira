package com.tarehimself.mangaz.screens.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mangaz.ui.search.LibraryComponent


@Composable
fun LibraryContent(component: LibraryComponent){
    val state by component.state.subscribeAsState()


    Surface (color = Color.Cyan, modifier = Modifier.fillMaxSize()){

    }
}