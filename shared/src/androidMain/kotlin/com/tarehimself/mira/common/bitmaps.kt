package com.tarehimself.mira.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.aakira.napier.Napier
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

actual fun ByteArray.toImageBitmap(): ImageBitmap ? {
    val options = BitmapFactory.Options()
    options.inSampleSize = 1
    return BitmapFactory.decodeByteArray(this,0,size,options)?.let { it.asImageBitmap() }
}

actual fun ImageBitmap.sizeBytes(): Int {
    return this.asAndroidBitmap().byteCount
}

actual fun ByteArray.toImageBitmap(maxWidth: Int): ImageBitmap ? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeByteArray(this,0,size,options)

    val originalBitmap = BitmapFactory.decodeByteArray(this,0,size) ?: return null

    if(options.outWidth <= maxWidth){
        return originalBitmap.asImageBitmap()
    }

    val bitmap = Bitmap.createScaledBitmap(originalBitmap,maxWidth,(maxWidth * (options.outHeight.toFloat() / options.outWidth.toFloat())).toInt(),true)

    return bitmap.asImageBitmap()
}