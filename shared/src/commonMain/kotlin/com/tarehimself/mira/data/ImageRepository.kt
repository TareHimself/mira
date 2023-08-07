package com.tarehimself.mira.data

import androidx.compose.ui.graphics.ImageBitmap
import com.goncalossilva.murmurhash.MurmurHash3
import com.tarehimself.mira.common.Base64.decodeFromBase64
import com.tarehimself.mira.common.Cache
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import org.koin.core.component.KoinComponent
import kotlin.coroutines.cancellation.CancellationException


interface ImageRepository : KoinComponent {

    val cache: Cache<String, ImageBitmap>

    val hashCache: Cache<String, String>

    val client: HttpClient

    suspend fun loadHttpImage(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {},
        tries: Int = 0
    ): Pair<Any?, Long>

    fun loadB64Image(url: String): ByteArray

    fun getCached(url: String): ImageBitmap?

    fun hashData(data: String): String
}

class DefaultImageRepository : ImageRepository {

    override val cache = Cache<String, ImageBitmap>(10) // Number of in-memory images at any given time

    override val hashCache = Cache<String, String>(70)

    override val client = HttpClient()


    override suspend fun loadHttpImage(
        url: String,
        block: HttpRequestBuilder.() -> Unit,
        tries: Int
    ): Pair<Any?, Long> {
        try {
            val response = client.get(url, block)

            return if (response.status == HttpStatusCode.OK
            ) {
                var dataLength = response.contentLength() ?: 0

                if (dataLength.toInt() == 0) {
                    val data = response.body<ByteArray>()
                    dataLength = data.size.toLong()

                    Napier.w(tag = "image") { "Some BOZO website did not send content length, winging it, Derived size of $dataLength" }

                    return Pair(data, dataLength)
                } else {
                    return Pair(response.bodyAsChannel(), dataLength)
                }
            } else {
                Pair(null, 0)
            }
        } catch (_: CancellationException) {
            return Pair(null, 0)
        } catch (e: Exception) {
            Napier.e(
                "Exception while fetching http image ${e.message} $url",
                tag = "Image Repository"
            )

            if (tries < 10) {
                return loadHttpImage(url, block, tries + 1)
            }

            return Pair(null, 0)
        }
    }

    override fun loadB64Image(url: String): ByteArray {
        return url.substring(url.indexOf(",") + 1).decodeFromBase64()
    }

    override fun hashData(data: String): String {
        return hashCache[data] ?: run {
            val hashed = "${MurmurHash3().hash32x86(data.encodeToByteArray())}"
            hashCache[data] = hashed
            hashed
        }
    }

//    override suspend fun loadBitmap(url:String): ImageBitmap? {
//        if(cache.contains(url)){
//            return cache[url]
//        }
//
//
//        val bArray: ByteArray? = when {
//            url.startsWith("data:image/png;base64,") -> loadB64Image(url)
//            url.startsWith("http") -> loadHttpImage(url)
//            else -> throw Exception("Unknown source $url")
//        }
//
//        return if (bArray != null
//        ) {
//            val bitmap = bArray.toImageBitmap()
//            cache[url] = bitmap
//            return bitmap
//        } else {
//            null
//        }
//    }

    override fun getCached(
        url: String
    ): ImageBitmap? {
        return when (val data = cache[url]) {
            is ImageBitmap -> {
                data
            }

            else -> {
                null
            }
        }
    }
}

