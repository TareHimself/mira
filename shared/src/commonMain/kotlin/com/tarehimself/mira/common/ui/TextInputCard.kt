package com.tarehimself.mira.common.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tarehimself.mira.common.borderRadius


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInputCard(initialValue: String,modifier: Modifier = Modifier, cancelText: String = "Cancel", commitText: String = "Done", onCancelled: () -> Unit = {}, onCommitted: (text: String) -> Unit = {}) {

    var currentValue by remember { mutableStateOf(initialValue) }

    Column(modifier = Modifier.borderRadius(5.dp).fillMaxWidth(0.8f).then(modifier),horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,) {
        TextField(
            value = currentValue,
            onValueChange = { currentValue = it },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.height(40.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val buttonModifier = Modifier.fillMaxHeight().weight(1.0f)
            Pressable(
                onClick = {
                    onCancelled()
                },
                backgroundColor = MaterialTheme.colorScheme.primary,
                modifier = buttonModifier
            ) {
                Box {
                    Text(cancelText, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Pressable(
                onClick = {
                    onCommitted(currentValue)
                },
                backgroundColor = MaterialTheme.colorScheme.primary,
                modifier = buttonModifier
            ) {
                Box {
                    Text(commitText,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

}