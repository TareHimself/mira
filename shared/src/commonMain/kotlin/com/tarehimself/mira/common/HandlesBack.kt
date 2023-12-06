package com.tarehimself.mira.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.backhandler.BackCallback
import org.koin.core.component.KoinComponent


val LocalBackHandler = staticCompositionLocalOf<HandlesBack?> { null }

interface HandlesBack {
    fun registerBackHandler(handler: () -> Unit): () -> Unit
}