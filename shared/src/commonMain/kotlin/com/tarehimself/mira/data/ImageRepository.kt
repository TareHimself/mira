package com.tarehimself.mira.data

import androidx.compose.ui.graphics.ImageBitmap
import com.tarehimself.mira.common.Base64.decodeFromBase64
import com.tarehimself.mira.common.Cache
import com.tarehimself.mira.common.debug
import com.tarehimself.mira.common.toImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.koin.core.component.KoinComponent
import kotlin.coroutines.cancellation.CancellationException


interface ImageRepository : KoinComponent {

    val cache: Cache<String,ImageBitmap>

    val client: HttpClient

    suspend fun loadHttpImage(url: String,tries: Int = 0): ByteArray ?

    fun loadB64Image(url: String): ByteArray

    suspend fun loadBitmap(url:String): ImageBitmap?

    fun getCached(url:String): ImageBitmap?
}

class DefaultImageRepository : ImageRepository {

    override val cache = Cache<String,ImageBitmap>(20)

    override val client = HttpClient()


    override suspend fun loadHttpImage(url: String,tries: Int): ByteArray ? {
        try {
            val response = client.get(url)

            return if (response.status == HttpStatusCode.OK && response.contentType().toString()
                    .startsWith("image", true)
            ) {
                return response.body<ByteArray>()
            } else {
                null
            }
        } catch (_: CancellationException){
            return null
        }catch (e: Exception){
            debug("Exception while fetching http image ${e.message} $url")

            if(tries < 10){
                return loadHttpImage(url,tries + 1)
            }

            return null
        }
    }

    override fun loadB64Image(url: String): ByteArray {
        return url.substring(url.indexOf(",")  + 1).decodeFromBase64()
    }

    override suspend fun loadBitmap(url:String): ImageBitmap? {
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

    override fun getCached(url:String): ImageBitmap? {
        return cache[url]
    }
}

