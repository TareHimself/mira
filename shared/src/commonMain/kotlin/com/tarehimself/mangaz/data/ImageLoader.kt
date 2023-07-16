package com.tarehimself.mangaz.data

import androidx.compose.ui.graphics.ImageBitmap
import com.tarehimself.mangaz.common.Base64.decodeFromBase64
import com.tarehimself.mangaz.common.SizedCache
import com.tarehimself.mangaz.common.toImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlin.jvm.Volatile

class ImageLoader private constructor(){
    companion object {
        @Volatile
        private var instance: ImageLoader? = null

        fun get() = instance ?: ImageLoader().also { instance = it}

    }

    private val cache = SizedCache<String,ImageBitmap>(20)

    private val client = HttpClient()

    private suspend fun loadHttpImage(url: String): ByteArray ? {
        val response = client.get(url)

        return if (response.status == HttpStatusCode.OK && response.contentType().toString()
                .startsWith("image", true)
        ) {
            return response.body<ByteArray>()
        } else {
            null
        }
    }

    private fun loadB64Image(url: String): ByteArray {
        return url.substring(url.indexOf(",")  + 1).decodeFromBase64()
    }

    suspend fun loadBitmap(url:String): ImageBitmap? {
        if(cache.contains(url)){
            return cache[url]
        }


        val bArray: ByteArray? = when {
            url.startsWith("data:image/png;base64,") -> loadB64Image(url)
            url.startsWith("http") -> loadHttpImage(url)
            else -> throw Exception("Unknown source $url")
        }

        return if (bArray != null
        ) {
            val bitmap = bArray.toImageBitmap()
            cache[url] = bitmap
            return bitmap
        } else {
            null
        }
    }

    fun getCached(url:String): ImageBitmap? {
        return cache[url]
    }
}

