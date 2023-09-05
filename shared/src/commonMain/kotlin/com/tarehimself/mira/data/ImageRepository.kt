package com.tarehimself.mira.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import com.tarehimself.mira.common.Base64.decodeFromBase64
import com.tarehimself.mira.common.Cache
import com.tarehimself.mira.common.quickHash
import free
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.component.KoinComponent
import sizeBytes
import usable
import kotlin.coroutines.cancellation.CancellationException
import kotlin.jvm.Synchronized
import kotlin.math.max
import kotlin.math.roundToInt

class MiraBitmap(private val bitmap: ImageBitmap, var refs: Int = 0){


    init {
        activeBitmaps.value++
    }

    companion object {
        val activeBitmaps: MutableState<Int> = mutableStateOf(0)
    }

    fun use(){
        refs++
        Napier.d { "$refs Refs ${bitmap.hashCode()} | Active Bitmaps ${activeBitmaps.value}" }
    }

    fun get(): ImageBitmap {
        return bitmap
    }

    fun free(){

        if(refs >= 0 && bitmap.usable()){
            refs--;
            if(refs <= 0){
                    bitmap.free()
                activeBitmaps.value--
            }
        }
        Napier.d { "$refs Refs ${bitmap.hashCode()} | Active Bitmaps ${activeBitmaps.value}" }
    }

    fun usable(): Boolean {
        return bitmap.usable()
    }
}

interface ImageRepository : KoinComponent {

    val cache: Cache<String, MiraBitmap>

    val coverRatios: HashMap<String, Float>

    val hashCache: HashMap<String, String>

    val client: HttpClient

    var deviceWidth: MutableState<Int>

    val bitmapJobsScope: CoroutineScope

    suspend fun loadHttpImage(
        tries: Int = 0,
        block: HttpRequestBuilder.() -> Unit,
    ): Pair<ByteReadChannel, Long>?

    suspend fun loadHttpImage(
        tries: Int = 0,
        builder: HttpRequestBuilder,
    ): Pair<ByteReadChannel, Long>?

    fun loadB64Image(url: String): ByteArray

    fun hashData(data: String): String
}


fun ImageBitmap.createMiraBitmap(): MiraBitmap {
    return MiraBitmap(bitmap = this)
}

class DefaultImageRepository : ImageRepository {

    override val bitmapJobsScope: CoroutineScope = CoroutineScope(Dispatchers.IO)

    override val cache = Cache<String, MiraBitmap>(
        150.0f * 1024,
        sizeOf = { it.get().sizeBytes().toFloat() / 1024.0f },
        onItemRemoved = { _, v ->
            v.free()
        },
    onItemAdded = { _, v ->
        v.use()
    },
    onFailedToAddItem = { _, v ->
        v.free()
    }) // Number of in-memory images at any given time 80MiB

    override val coverRatios: HashMap<String, Float> = HashMap()

    override val hashCache = HashMap<String, String>()

    override val client = HttpClient()

    override var deviceWidth: MutableState<Int> = mutableStateOf(0)

    override suspend fun loadHttpImage(
        tries: Int,
        block: HttpRequestBuilder.() -> Unit,

        ): Pair<ByteReadChannel, Long>? {
        return loadHttpImage(tries, HttpRequestBuilder().apply(block))
    }

    override suspend fun loadHttpImage(
        tries: Int,
        builder: HttpRequestBuilder
    ): Pair<ByteReadChannel, Long>? {
        try {
            val response = client.get(builder)
            return if (response.status == HttpStatusCode.OK
            ) {
                return Pair(response.bodyAsChannel(), response.contentLength() ?: 0)
            } else {
                null
            }
        } catch (_: CancellationException) {
            return null
        } catch (e: Exception) {
            Napier.e(
                "Exception while fetching http image ${e.message} ${builder.url}",
                tag = "Image Repository"
            )

            if (tries < 10) {
                return loadHttpImage(tries + 1, builder)
            }

            return null
        }
    }

    override fun loadB64Image(url: String): ByteArray {
        return url.substring(url.indexOf(",") + 1).decodeFromBase64()
    }

    override fun hashData(data: String): String {
        return hashCache[data] ?: run {
            val hashed = data.quickHash()
            hashCache[data] = hashed
            hashed
        }
    }

}

