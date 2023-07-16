package com.tarehimself.mangaz.screens.sources

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.jetbrains.subscribeAsState

@Composable
fun SourcesContent(component: SourcesComponent){
    val state by component.state.subscribeAsState()

    LaunchedEffect(Unit){
        component.getSources()
    }

    Surface (modifier = Modifier.fillMaxSize()){
        LazyColumn(modifier = Modifier.fillMaxWidth()){
            items(state.sources.size, key = {
                state.sources[it]
            }) { idx ->
                val source = state.sources[idx]
                Surface(modifier = Modifier.height(60.dp).fillMaxWidth().padding(0.dp,10.dp)) {
                    Surface(modifier = Modifier.fillMaxSize().clip(shape = RoundedCornerShape(5.dp)),color = Color.Blue) {
                        Box(modifier = Modifier.fillMaxSize()){
                            Text(source.name, modifier = Modifier.align(Alignment.Center))
                            Button(onClick = {
                                component.onItemSelected(source.id)
                            }, modifier = Modifier.fillMaxSize(),colors = ButtonDefaults.buttonColors(backgroundColor = Color.Transparent), elevation = ButtonDefaults.elevation(0.dp,0.dp,0.dp,0.dp,0.dp)){

                            }
                        }

                    }
                }
            }
        }
    }
}