package com.tarehimself.mira.common.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.essenty.backhandler.BackCallback



data class MiraDialog(val content: @Composable BoxScope.() -> Unit,val onDismissed: () -> Unit = {})

class MiraDialogController() {
    var current: MutableState<MiraDialog?> = mutableStateOf(null)

    fun show(onDismissed: () -> Unit = {},content: @Composable BoxScope.() -> Unit) {
        current.value = MiraDialog(content = content,onDismissed = onDismissed)
    }

    fun hide() {
        current.value?.let {
            it.onDismissed()
            current.value = null
        }
    }
}

val LocalMiraDialogController = staticCompositionLocalOf { MiraDialogController() }


@Composable
fun DialogPage(data: MiraDialog) {

    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f), modifier = Modifier.fillMaxSize()){
        Box(modifier = Modifier.fillMaxSize()){
            data.content(this)
        }
    }
}

@Composable
fun MiraDialogContainer(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMiraDialogController provides MiraDialogController()) {

        val dialog by LocalMiraDialogController.current.current

        BoxWithConstraints {
            content()
            dialog?.let {
                DialogPage(data = it)
            }
        }
    }
}