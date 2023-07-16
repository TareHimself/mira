package com.tarehimself.mangaz.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import com.arkivanov.decompose.defaultComponentContext
import com.tarehimself.mangaz.DefaultRootComponent
import com.tarehimself.mangaz.RootContent


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = DefaultRootComponent(
            componentContext = defaultComponentContext()
        )
        setContent {
            MyApplicationTheme {
                Surface {
                    RootContent(component = root)
                }
            }
        }
    }
}
