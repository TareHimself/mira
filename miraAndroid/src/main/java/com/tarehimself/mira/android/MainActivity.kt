package com.tarehimself.mira.android

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material.*
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.arkivanov.decompose.defaultComponentContext
import com.tarehimself.mira.DefaultRootComponent
import com.tarehimself.mira.RootContent
import com.tarehimself.mira.initKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initKoin {
            androidContext(applicationContext)
        }

        val root = DefaultRootComponent(
            componentContext = defaultComponentContext(),
        )

        setContent {
            MyApplicationTheme {
                Surface {
                    RootContent(component = root)
                }
            }

//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//                if(window.insetsController != null){
//                    WindowCompat.setDecorFitsSystemWindows(window, false)
//                    window.insetsController?.hide(WindowInsets.Type.statusBars())
//                    window.insetsController?.hide(WindowInsets.Type.navigationBars())
//                }
//            }
        }

    }
}
