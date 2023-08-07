package com.tarehimself.mira.common

import CacheBridge
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
import com.tarehimself.mira.data.ImageRepository
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
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

    suspend fun retry(){

    }

    fun setStatus(status: EAsyncPainterStatus){
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
    override var status: MutableState<AsyncPainter.EAsyncPainterStatus> = mutableStateOf(AsyncPainter.EAsyncPainterStatus.LOADING)

}

open class AsyncNetworkPainter : AsyncBitmapPainter() {
    override var painter: MutableState<BitmapPainter?> = mutableStateOf(null)

    val imageRepository: ImageRepository by inject()

    var chunkSize: Int = 1024

    var progress = mutableStateOf(0.0f)

    fun setProgress(progress: Float) {
        this.progress.value = progress
    }

    override fun setPainter(painter: BitmapPainter?) {
        super.setPainter(painter)
        if(painter != null){
            setProgress(1.0f)
            setStatus(AsyncPainter.EAsyncPainterStatus.SUCCESS)
        }
        else
        {
            if(progress.value > 0.0f){
                setStatus(AsyncPainter.EAsyncPainterStatus.FAIL)
            }
            setProgress(0.0f)
        }
    }

    open suspend fun load(
        url: String,
        filterQuality: FilterQuality = FilterQuality.High,
        block: HttpRequestBuilder.() -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val bytesChannel = imageRepository.loadHttpImage(url, block)
            if (bytesChannel.first != null && bytesChannel.second > 0) {

                val target = bytesChannel.first

                val allBytes: ByteArray = if(target is ByteReadChannel){
                    target.readAllInChunks(
                        bytesChannel.second,
                        chunkSize = chunkSize,
                    ) { total, current ->
                        setProgress(current.toFloat() / total.toFloat())
                    }
                } else {
                    target as ByteArray
                }


                val bitmap = allBytes.toImageBitmap()

                imageRepository.cache[url] = bitmap

                setPainter(BitmapPainter(bitmap, filterQuality = filterQuality))
                return@launch
            }
            setStatus(AsyncPainter.EAsyncPainterStatus.FAIL)
        }

    }
}

@Composable
fun networkImagePainter(
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
    override suspend fun load(
        url: String,
        filterQuality: FilterQuality,
        block: HttpRequestBuilder.() -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            var wasFromDisk = false

            val diskHash = imageRepository.hashData(url)
            val bytesChannel = run {
                val diskChannel = CacheBridge.getCachedCover(diskHash)
                wasFromDisk = diskChannel.first != null

                if (!wasFromDisk) {
                    return@run imageRepository.loadHttpImage(url, block)
                }
                diskChannel
            }

            if (bytesChannel.first != null && bytesChannel.second > 0) {
                val target = bytesChannel.first

                val allBytes: ByteArray = if(target is ByteReadChannel){
                    target.readAllInChunks(
                        bytesChannel.second,
                        chunkSize = chunkSize,
                    ) { total, current ->
                        setProgress(current.toFloat() / total.toFloat())
                    }
                } else {
                    target as ByteArray
                }

                val bitmap = allBytes.toImageBitmap()

                imageRepository.cache[url] = bitmap

                if (!wasFromDisk) {
                    CacheBridge.cacheCover(diskHash, allBytes)
                }

                setPainter(BitmapPainter(bitmap, filterQuality = filterQuality))
                return@launch
            }
            setStatus(AsyncPainter.EAsyncPainterStatus.FAIL)
        }
    }
}

@Composable
fun mangaCoverPainter(
    url: String,
    filterQuality: FilterQuality = FilterQuality.High,
    chunkSize: Int = 1024,
    block: HttpRequestBuilder.() -> Unit = {}
): AsyncMangaCoverPainter {

    val painter = remember(url) {
        AsyncMangaCoverPainter().apply {
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

@Composable
fun <T : AsyncPainter<*>> AsyncImage(
    painter: T,
    contentDescription: String,
    contentScale: ContentScale = ContentScale.None,
    crossFadeDuration: Int = 500,
    modifier: Modifier = Modifier,
    onLoading: (@Composable (painter: T) -> Unit) = {  },
    onFail: (@Composable (painter: T) -> Unit) = {  }
) {

    val asyncPainter: T = remember { painter }

    Crossfade(asyncPainter.status.value, animationSpec = tween(crossFadeDuration)) {
        when(it){
            AsyncPainter.EAsyncPainterStatus.LOADING -> {
                onLoading(asyncPainter)
            }
            AsyncPainter.EAsyncPainterStatus.SUCCESS -> {
                when(val resultingPainter = asyncPainter.get()){
                    null -> {

                    }
                    else ->
                    {
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