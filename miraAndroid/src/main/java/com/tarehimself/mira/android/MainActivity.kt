package com.tarehimself.mira.android

import FileBridge
import ShareBridge
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import com.arkivanov.decompose.defaultComponentContext
import com.tarehimself.mira.DefaultRootComponent
import com.tarehimself.mira.RootContent
import com.tarehimself.mira.initKoin
import org.koin.android.ext.koin.androidContext


class MainActivity : ComponentActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        ShareBridge.setContext(applicationContext)
        FileBridge.setContext(applicationContext)

        initKoin {
            androidContext(applicationContext)
        }



        val root = DefaultRootComponent(
            componentContext = defaultComponentContext(),
        )

//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//

        setContent {
            MyApplicationTheme {
                RootContent(component = root)
            }

//        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
//            if(window.insetsController != null){
//                WindowCompat.setDecorFitsSystemWindows(window, false)
//                window.insetsController?.hide(WindowInsets.Type.statusBars())
//                window.insetsController?.hide(WindowInsets.Type.navigationBars())
//            }
//        }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        ShareBridge.clearContext()
        FileBridge.clearContext()
    }
}
