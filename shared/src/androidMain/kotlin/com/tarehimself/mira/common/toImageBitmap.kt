package com.tarehimself.mira.common

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun ByteArray.toImageBitmap(): ImageBitmap {
    val options = BitmapFactory.Options()
    options.inSampleSize = 1

    return BitmapFactory.decodeByteArray(this,0,size,options).asImageBitmap()
}