package com.tarehimself.mira.common.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.tarehimself.mira.common.HandlesBack
import com.tarehimself.mira.common.LocalBackHandler
import imePadding


class MiraDialog<T>(val data: T, private val dialogs: SnapshotStateList<MiraDialog<*>>, val onDismissed: () -> Unit = {},val backHandler: HandlesBack? = null, val content: @Composable BoxScope.(data: T) -> Unit){
    fun hide(){
        dialogs.remove(this)
        onDismissed()
    }
}

class MiraDialogController() {
    var dialogs: SnapshotStateList<MiraDialog<*>> = mutableStateListOf()




    fun <T>show(data: T, onDismissed: () -> Unit = {},backHandler: HandlesBack? = null, content: @Composable BoxScope.(data: T) -> Unit): MiraDialog<T> {
        val dialog = MiraDialog(content = content,onDismissed = onDismissed, dialogs = dialogs, data = data, backHandler = backHandler)
        dialogs.add(dialog)
        return dialog
    }

    fun hide() {
        if(dialogs.isNotEmpty()){
            dialogs.last().hide()
        }
    }
}

val LocalMiraDialogController = staticCompositionLocalOf { MiraDialogController() }


@Composable
fun <T>DialogPage(data: MiraDialog<T>) {
    val localBackHandler = data.backHandler ?: LocalBackHandler.current

    Surface(color = MaterialTheme.colorScheme.background.copy(alpha = 0.7f), modifier = Modifier.fillMaxSize()){
        Box(modifier = Modifier.fillMaxSize().imePadding()){
            data.content.invoke(this,data.data)
        }
    }

    DisposableEffect(Unit){
        val clearHandler = localBackHandler?.registerBackHandler {
            data.hide()
        }

        onDispose {
            clearHandler?.invoke()
        }
    }
}

@Composable
fun MiraDialogContainer(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMiraDialogController provides MiraDialogController()) {
        BoxWithConstraints {
            content()
            LocalMiraDialogController.current.dialogs.forEach {
                DialogPage(data = it)
            }
        }
    }
}