package com.tarehimself.mira.common.ui

import FileBridge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import com.tarehimself.mira.common.quickHash
import com.tarehimself.mira.common.readAllInChunks
import com.tarehimself.mira.common.toChannel
import com.tarehimself.mira.common.toImageBitmap
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.data.RealmRepository
import io.github.aakira.napier.Napier
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface AsyncPainter<T : Painter> : KoinComponent {

    enum class EAsyncPainterStatus {
        LOADING,
        SUCCESS,
        FAIL
    }

    var painter: MutableState<T?>
    var status: MutableState<EAsyncPainterStatus>

    suspend fun retry() {

    }

    fun setStatus(status: EAsyncPainterStatus) {
        this.status.value = status
    }

    fun setPainter(painter: T?) {
        this.painter.value = painter
    }

    fun get(): T? {
        return this.painter.value
    }


}

open class AsyncBitmapPainter : AsyncPainter<BitmapPainter> {
    override var painter: MutableState<BitmapPainter?> = mutableStateOf(null)
    override var status: MutableState<AsyncPainter.EAsyncPainterStatus> = mutableStateOf(
        AsyncPainter.EAsyncPainterStatus.LOADING
    )

    var progress = mutableStateOf(0.0f)

    fun setProgress(progress: Float) {
        this.progress.value = progress
    }

    override fun setPainter(painter: BitmapPainter?) {
        super.setPainter(painter)
        if (painter != null) {
            setProgress(1.0f)
            setStatus(AsyncPainter.EAsyncPainterStatus.SUCCESS)
        } else {
            if (progress.value > 0.0f) {
                setStatus(AsyncPainter.EAsyncPainterStatus.FAIL)
            }
            setProgress(0.0f)
        }
    }

}

open class AsyncNetworkPainter : AsyncBitmapPainter() {
    override var painter: MutableState<BitmapPainter?> = mutableStateOf(null)

    val imageRepository: ImageRepository by inject()

    var chunkSize: Int = 1024

    open suspend fun load(
        url: String,
        filterQuality: FilterQuality = FilterQuality.High,
        block: HttpRequestBuilder.() -> Unit = {}
    ) {
        withContext(Dispatchers.IO) {
            val bytesChannel = imageRepository.loadHttpImage(url, block)
            val target = bytesChannel.first

            if (target != null) {


                val allBytes: ByteArray = if (bytesChannel.second.toInt() == 0) {
                    target.readAllInChunks(
                        minChunkSize = chunkSize,
                    )
                } else {
                    target.readAllInChunks(
                        bytesChannel.second,
                        minChunkSize = chunkSize,
                    ) { total, current ->
                        setProgress(current.toFloat() / total.toFloat())
                    }
                }

                allBytes.toImageBitmap()?.let { bitmap ->
                    imageRepository.cache.put(url, bitmap)

                    setPainter(BitmapPainter(bitmap, filterQuality = filterQuality))
                    return@withContext
                }
            }
            setStatus(AsyncPainter.EAsyncPainterStatus.FAIL)
        }

    }
}

@Composable
fun rememberNetworkImagePainter(
    url: String,
    filterQuality: FilterQuality = FilterQuality.High,
    chunkSize: Int = 1024,
    block: HttpRequestBuilder.() -> Unit = {}
): AsyncNetworkPainter {

    val painter = remember(url) {
        AsyncNetworkPainter().apply {
            this.chunkSize = chunkSize
            setPainter(
                when (val data = this.imageRepository.getCached(url)) {
                    is ImageBitmap -> {
                        BitmapPainter(data, filterQuality = filterQuality)
                    }

                    else -> {
                        null
                    }
                }
            )
        }
    }

    LaunchedEffect(url) {
        if (painter.painter.value == null) {
            painter.load(url, filterQuality, block)
        }
    }

    return painter
}

class AsyncMangaCoverPainter : AsyncNetworkPainter() {

    fun getCacheKey(url: String): String{
        return "$halfScreenWidth|$url"
    }

    var halfScreenWidth = 500

    override suspend fun load(
        url: String,
        filterQuality: FilterQuality,
        block: HttpRequestBuilder.() -> Unit
    ) {
        withContext(Dispatchers.IO){
            var wasFromDisk: Boolean

            val diskHash = imageRepository.hashData(url)
            val bytesChannel = run {
                val diskChannel = FileBridge.getCachedCover(diskHash)
                wasFromDisk = diskChannel.first != null

                if (!wasFromDisk) {
                    return@run imageRepository.loadHttpImage(url, block)
                }
                diskChannel
            }

            val target = bytesChannel.first

            if (target != null) {

                val allBytes: ByteArray = if (bytesChannel.second.toInt() == 0) {
                    target.readAllInChunks(
                        minChunkSize = chunkSize,
                    )
                } else {
                    target.readAllInChunks(
                        bytesChannel.second,
                        minChunkSize = chunkSize,
                    ) { total, current ->
                        setProgress(current.toFloat() / total.toFloat())
                    }
                }

                allBytes.toImageBitmap(halfScreenWidth)?.let { bitmap ->
                    imageRepository.cache.put(getCacheKey(url), bitmap)

                    if (!wasFromDisk) {
                        FileBridge.cacheCover(diskHash, allBytes.toChannel())
                    }

                    setPainter(BitmapPainter(bitmap, filterQuality = filterQuality))
                    return@withContext
                }
            }
            setStatus(AsyncPainter.EAsyncPainterStatus.FAIL)
        }
    }
}

@Composable
fun rememberMangaCoverPainter(
    url: String,
    filterQuality: FilterQuality = FilterQuality.High,
    chunkSize: Int = 1024,
    block: HttpRequestBuilder.() -> Unit = {}
): AsyncMangaCoverPainter {


    val halfScreenWidth = remember { 512 }

    val painter = remember(url) {
        AsyncMangaCoverPainter().apply {
            this.chunkSize = chunkSize
            this.halfScreenWidth = halfScreenWidth
            setPainter(
                when (val data = this.imageRepository.getCached(this.getCacheKey(url))) {
                    is ImageBitmap -> {
                        BitmapPainter(data, filterQuality = filterQuality)
                    }

                    else -> {
                        null
                    }
                }
            )
        }
    }

    LaunchedEffect(url) {
        if (painter.painter.value == null) {
            painter.load(url, filterQuality, block)
        }
    }

    return painter
}

class AsyncLocalPagePainter(
    val sourceId: String,
    mangaId: String,
    chapterId: String,
    private val pageIndex: Int,
    val chunkSize: Int = 1024
) : AsyncBitmapPainter() {

    val imageRepository: ImageRepository by inject()

    private val realmRepository: RealmRepository by inject()
    val uniqueId = realmRepository.getMangaKey(sourceId, mangaId).quickHash()
    val chapterIdHash = chapterId.quickHash()
    suspend fun load() {
        withContext(Dispatchers.IO) {

            val data = FileBridge.getDownloadedChapterPage(uniqueId, chapterIdHash, pageIndex)
            val target = data.first
            if (target != null && data.second > 0) {

                Napier.d { "Loading chapter" }
                val allBytes: ByteArray = target.readAllInChunks(
                    data.second,
                    minChunkSize = chunkSize
                ) { total, current ->
                    setProgress(current.toFloat() / total.toFloat())
                }

                allBytes.toImageBitmap()?.let {bitmap ->
                    imageRepository.cache.put(uniqueId + chapterIdHash + pageIndex, bitmap)

                    Napier.d { "Done Loading chapter" }

                    setPainter(BitmapPainter(bitmap, filterQuality = FilterQuality.High))
                    return@withContext
                }
            }
            setStatus(AsyncPainter.EAsyncPainterStatus.FAIL)
        }
    }
}

@Composable
fun rememberLocalPagePainter(
    sourceId: String,
    mangaId: String,
    chapterId: String,
    pageIndex: Int,
    chunkSize: Int = 1024
): AsyncLocalPagePainter {

    val painter = remember(sourceId, mangaId, chapterId, pageIndex, chunkSize) {
        AsyncLocalPagePainter(
            sourceId = sourceId,
            mangaId = mangaId,
            chapterId = chapterId,
            pageIndex = pageIndex,
            chunkSize = chunkSize
        ).apply {
            setPainter(
                when (val data =
                    this.imageRepository.getCached(uniqueId + chapterIdHash + pageIndex)) {
                    is ImageBitmap -> {
                        BitmapPainter(data, filterQuality = FilterQuality.High)
                    }

                    else -> {
                        null
                    }
                }
            )
        }
    }

    LaunchedEffect(painter) {
        if (painter.painter.value == null) {
            painter.load()
        }
    }

    return painter
}


@Composable
fun <T : AsyncPainter<*>> AsyncImage(
    painter: T,
    contentDescription: String,
    contentScale: ContentScale = ContentScale.None,
    crossFadeDuration: Int = 500,
    modifier: Modifier = Modifier,
    onLoading: (@Composable (painter: T) -> Unit) = { },
    onFail: (@Composable (painter: T) -> Unit) = { }
) {

    val asyncPainter: T = remember { painter }

    Crossfade(asyncPainter.status.value, animationSpec = tween(crossFadeDuration)) {
        when (it) {
            AsyncPainter.EAsyncPainterStatus.LOADING -> {
                onLoading(asyncPainter)
            }

            AsyncPainter.EAsyncPainterStatus.SUCCESS -> {
                when (val resultingPainter = asyncPainter.get()) {
                    null -> {

                    }

                    else -> {
                        Image(
                            painter = resultingPainter,
                            contentDescription = contentDescription,
                            modifier = modifier,
                            contentScale = contentScale
                        )
                    }
                }

            }

            AsyncPainter.EAsyncPainterStatus.FAIL -> {
                onFail(asyncPainter)
            }
        }
    }
}