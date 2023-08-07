package com.tarehimself.mira.screens.sources

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState
import com.tarehimself.mira.Pressable

@Composable
fun SourcesContent(component: SourcesComponent){
    val state by component.state.subscribeAsState(neverEqualPolicy())

    LaunchedEffect(Unit){
        component.getSources()
    }

    Surface (modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background){
        val sourcesToList = state.sources.filter { true }//{ !it.nsfw }
        LazyColumn(modifier = Modifier.fillMaxWidth()){
            items(sourcesToList.size, key = {
                sourcesToList[it].id
            }) { idx ->
                val source = sourcesToList[idx]
                Surface(modifier = Modifier.height(60.dp).fillMaxWidth().padding(10.dp,10.dp)) {
                    Surface(modifier = Modifier.fillMaxSize().clip(shape = RoundedCornerShape(5.dp)),color = Color.Blue) {
                        Pressable(modifier = Modifier.fillMaxSize(), onClick = {
                            component.onItemSelected(source.id)
                        }) {
                            Box {
                                Text(source.name, modifier = Modifier.align(Alignment.Center))
                            }
                        }
                    }
                }
            }
        }
    }
}