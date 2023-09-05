package com.tarehimself.mira.android

import FileBridge
import ShareBridge
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
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

        WindowCompat.setDecorFitsSystemWindows(window,false)


//        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//
//        if (Environment.isExternalStorageManager()) {
//            //todo when permission is granted
//        } else {
//            //request for the permission
//            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//            val uri = Uri.fromParts("package", packageName, null)
//            intent.data = uri
//            startActivity(intent)
//        }

        setContent {

            MyApplicationTheme(applicationContext) {
                Box(Modifier.windowInsetsPadding(WindowInsets(0,0,0,0))){
                    RootContent(component = root)
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        ShareBridge.clearContext()
        FileBridge.clearContext()
    }
}
