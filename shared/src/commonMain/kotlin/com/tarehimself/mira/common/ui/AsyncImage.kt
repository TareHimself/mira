package com.tarehimself.mira.common.ui

import FileBridge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import bitmapFromCache
import com.tarehimself.mira.common.quickHash
import com.tarehimself.mira.common.readAllInChunks
import com.tarehimself.mira.common.toChannel
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.data.MangaImage
import com.tarehimself.mira.data.MiraBitmap
import com.tarehimself.mira.data.createMiraBitmap
import io.github.aakira.napier.Napier
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.Url
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import sizeBytes
import toImageBitmap
import kotlin.math.roundToInt

class NetworkImageRequest {
    private var url: String = ""
    private var headers: ArrayList<Pair<String, String>> = arrayListOf()

    val hashString: String
        get() = "${url.hashCode()}${headers.joinToString { "[${it.first}|${it.second}]" }}".quickHash()

    private fun addHeader(key: String, value: String) {
        headers.add(Pair(key, value))
    }

//    fun updateUrl(url: String) {
//        this.url = url
//    }

    fun fromMangaImage(image: MangaImage) {
        this.url = image.src
        image.headers.forEach {
            addHeader(it.key, it.value)
        }
    }

    fun toKtorRequest(): HttpRequestBuilder {
        val self = this
        return HttpRequestBuilder().apply {
            url(Url(self.url))
            self.headers.forEach {
                header(it.first, it.second)
            }
        }
    }

    override fun hashCode(): Int {
        return hashString.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as NetworkImageRequest

        return this.hashCode() == other.hashCode()
    }
}

abstract class AsyncPainter : KoinComponent, Painter() {

    enum class EAsyncPainterStatus {
        LOADING,
        SUCCESS,
        FAIL
    }

    val status: MutableState<EAsyncPainterStatus> = mutableStateOf(EAsyncPainterStatus.LOADING)

    fun setStatus(status: EAsyncPainterStatus) {
        this.status.value = status
    }

    open fun beforePaint(): AsyncPainter? {
        return null
    }

    open fun onDisposed() {

    }
}

open class AsyncBitmapPainter : AsyncPainter() {
    var resource: MiraBitmap? = null
        set(value) {
            if (value != null && value.get().height > 0 && value.get().width > 0) {
                field = value
                srcSize = IntSize(value.get().width, value.get().height)
                value.use()
                setProgress(1.0f)
                setStatus(EAsyncPainterStatus.SUCCESS)
            } else {
                field = null
                setProgress(0.0f)
                setStatus(EAsyncPainterStatus.FAIL)
            }
        }

    var filterQuality: FilterQuality = FilterQuality.High
    var alpha: Float = 1.0f
    private var colorFilter: ColorFilter? = null

    open val abandonOnDispose = true

    private var srcSize: IntSize = IntSize.Zero
    override val intrinsicSize: Size
        get() = srcSize.toSize()

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        resource?.let { image ->

            if(!image.usable() || size.minDimension == 0.0f){
                onDisposed()
                return
            }

            drawImage(
                image.get(),
                IntOffset(0, 0),
                srcSize,
                dstSize = IntSize(
                    this@onDraw.size.width.roundToInt(),
                    this@onDraw.size.height.roundToInt()
                ),
                alpha = alpha,
                colorFilter = colorFilter,
                filterQuality = filterQuality
            )
        }
    }

    var progress = mutableStateOf(0.0f)

    fun setProgress(progress: Float) {
        this.progress.value = progress
    }

    override fun beforePaint(): AsyncPainter? {
        return this
//        return resource?.let {
//            if(!it.usable()){
//                onDisposed()
//                null
//            }
//            else
//            {
//                srcSize = IntSize(it.get().width, it.get().height)
//                this
//            }
//
//        }
    }

    override fun onDisposed() {
        super.onDisposed()
        if (abandonOnDispose) {
            resource?.let {
                resource = null
                it.free()
            }
        }
    }
}

@Composable
fun rememberBitmapPainter(
    bitmap: MiraBitmap?,
    filterQuality: FilterQuality = FilterQuality.High
): AsyncBitmapPainter {

    val painter = remember(bitmap) {
        AsyncBitmapPainter().apply {
            this.filterQuality = filterQuality
        }
    }

    DisposableEffect(painter) {
        onDispose {
            painter.onDisposed()
        }
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            painter.resource = when (bitmap) {
                is MiraBitmap -> {
                    bitmap
                }

                else -> {
                    null
                }
            }
        }
    }

    return painter
}

open class AsyncNetworkPainter : AsyncBitmapPainter() {

    override val abandonOnDispose: Boolean = false

    private val imageRepository: ImageRepository by inject()

    var chunkSize: Long = 1024

    var maxWidth: Int = 0

    var loadFromExternalCache: suspend (key: String) -> ImageBitmap? = { null }

    var saveToExternalCache: suspend (key: String, value: ByteReadChannel) -> Unit = { _, _ -> }

    lateinit var request: NetworkImageRequest

    private val memoryCacheKey: String
        get() = "${maxWidth}${request.hashString}".quickHash()

    open suspend fun load() {
        withContext(Dispatchers.IO) {

            imageRepository.cache.getAsync(memoryCacheKey)?.let {
                resource = it
                return@withContext
            }

            loadFromExternalCache(request.hashString)?.let {
                val miraBitmap = it.createMiraBitmap()
                imageRepository.cache.put(key = memoryCacheKey, value = miraBitmap)
                resource = miraBitmap
                return@withContext
            }

            val channel = imageRepository.loadHttpImage(0, request.toKtorRequest())


            if (channel != null) {
                val allBytes: ByteArray = if (channel.second.toInt() == 0) {
                    channel.first.readAllInChunks(
                        minChunkSize = chunkSize,
                    )
                } else {
                    channel.first.readAllInChunks(
                        channel.second,
                        minChunkSize = chunkSize,
                    ) { total, current ->
                        setProgress(current.toFloat() / total.toFloat())
                    }
                }

                allBytes.toImageBitmap(maxWidth = maxWidth)?.let { bitmap ->
                    val miraBitmap = bitmap.createMiraBitmap()

                    imageRepository.cache.put(memoryCacheKey, miraBitmap)
                    resource = miraBitmap

                    saveToExternalCache(request.hashString, allBytes.toChannel())

                    return@withContext
                }
            }
            resource = null
        }

    }

    override fun onDisposed() {
        super.onDisposed()

        resource?.let { bitmap ->
            resource = null
            bitmap.free()
        }
    }
}

@Composable
fun rememberNetworkPainter(
    filterQuality: FilterQuality = FilterQuality.High,
    chunkSize: Long = 2 * 1024 * 1024,
    maxWidth: Int = koinInject<ImageRepository>().deviceWidth.value,
    loadFromExternalCache: suspend (key: String) -> ImageBitmap? = { key ->
        withContext(
            Dispatchers.IO
        ) {
            bitmapFromCache(key, maxWidth = maxWidth)
        }
    },
    saveToExternalCache: suspend (key: String, value: ByteReadChannel) -> Unit = { key, value ->
        withContext(
            Dispatchers.IO
        ) { FileBridge.cacheItem(key, value, maxSize = 500 * 1024 * 1024) }
    },//700 * 1024 * 1024
    block: NetworkImageRequest.() -> Unit
): AsyncNetworkPainter {

    val request = NetworkImageRequest().apply(block)

    val painter = remember(request.hashCode()) {
        AsyncNetworkPainter().apply {
            this.chunkSize = chunkSize
            this.maxWidth = maxWidth
            this.loadFromExternalCache = loadFromExternalCache
            this.saveToExternalCache = saveToExternalCache
            this.filterQuality = filterQuality
            this.request = request
        }
    }

    DisposableEffect(painter) {
        onDispose {
            painter.onDisposed()
        }
    }

    LaunchedEffect(request.hashCode()) {
        withContext(Dispatchers.IO) {
            painter.load()
        }
    }

    return painter
}


@Composable
fun rememberCoverPreviewPainter(
    filterQuality: FilterQuality = FilterQuality.High,
    chunkSize: Long = 1024,
    block: NetworkImageRequest.() -> Unit
): AsyncNetworkPainter {


    return rememberNetworkPainter(
        filterQuality = filterQuality,
        chunkSize = chunkSize,
        maxWidth = koinInject<ImageRepository>().deviceWidth.value / 2,
        block = block
    )
}

@Composable
fun rememberCoverPainter(
    filterQuality: FilterQuality = FilterQuality.High,
    chunkSize: Long = 1024,
    block: NetworkImageRequest.() -> Unit
): AsyncNetworkPainter {

    return rememberNetworkPainter(
        filterQuality = filterQuality,
        chunkSize = chunkSize,
        maxWidth = koinInject<ImageRepository>().deviceWidth.value,
        block = block
    )
}

class AsyncCustomPainter(val loader: suspend () -> ImageBitmap?,cacheKeyFunction: () -> String?) : AsyncBitmapPainter() {

    override val abandonOnDispose: Boolean = false

    private val imageRepository: ImageRepository by inject()


    private val cacheKey = cacheKeyFunction()

    suspend fun load() {
        withContext(Dispatchers.IO) {

            cacheKey?.let { key ->
                imageRepository.cache.getAsync(key)?.let {
                    resource = it
                    return@withContext
                }
            }

            loader()?.let { bitmap ->
                val miraBitmap = bitmap.createMiraBitmap()


                cacheKey?.let {
                    imageRepository.cache.put(it,miraBitmap)
                }

                resource = miraBitmap

                Napier.d { "Bitmap with size ${miraBitmap.get().width} ${miraBitmap.get().height} ${miraBitmap.get().sizeBytes() / 1024}" }
                return@withContext
            }

            resource = null
        }
    }

    override fun onDisposed() {
        super.onDisposed()
        resource?.let { bitmap ->
            resource = null
            bitmap.free()
        }
    }
}

@Composable
fun rememberCustomPainter(
    loader: suspend () -> ImageBitmap?,
    cacheKeyFunction: () -> String? = { null },
    loaderKeyFunction: () -> String? = { null }
): AsyncCustomPainter {

    val painter = remember(loaderKeyFunction()) {
        AsyncCustomPainter(
            loader = loader,
            cacheKeyFunction = cacheKeyFunction,
        )
    }

    LaunchedEffect(painter) {
        withContext(Dispatchers.IO) {
            painter.load()
        }
    }

    DisposableEffect(painter) {
        onDispose {
            Napier.d { "Local Painter Disposed" }
            painter.onDisposed()
        }
    }

    return painter
}


@Composable
fun <T : AsyncPainter> AsyncImage(
    asyncPainter: T,
    contentDescription: String,
    contentScale: ContentScale = ContentScale.None,
    crossFadeDuration: Int = 500,
    modifier: Modifier = Modifier,
    onLoading: (@Composable (painter: T) -> Unit) = { },
    onFail: (@Composable (painter: T) -> Unit) = { }
) {


    val status by remember(asyncPainter) { asyncPainter.status }

    Crossfade(status, animationSpec = tween(crossFadeDuration)) {
        when (it) {
            AsyncPainter.EAsyncPainterStatus.LOADING -> {
                onLoading(asyncPainter)
            }

            AsyncPainter.EAsyncPainterStatus.SUCCESS -> {
                Image(
                    painter = asyncPainter,
                    contentDescription = contentDescription,
                    modifier = modifier,
                    contentScale = contentScale
                )
            }

            AsyncPainter.EAsyncPainterStatus.FAIL -> {
                onFail(asyncPainter)
            }
        }
    }
}