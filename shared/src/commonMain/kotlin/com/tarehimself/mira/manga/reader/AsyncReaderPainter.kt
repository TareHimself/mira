package com.tarehimself.mira.manga.reader

import bitmapFromFile
import com.tarehimself.mira.common.EFilePaths
import com.tarehimself.mira.common.readAllInChunks
import com.tarehimself.mira.common.toChannel
import com.tarehimself.mira.common.ui.AsyncBitmapPainter
import com.tarehimself.mira.common.ui.NetworkImageRequest
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.data.MangaChapter
import com.tarehimself.mira.data.createMiraBitmap
import com.tarehimself.mira.storage.MediaStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.koin.core.component.inject
import toBytes
import toImageBitmap

class AsyncReaderPainter(val sourceId: String, val mangaId: String, private val chapter: MangaChapter, val item: ReaderComponent.ReaderChapterItem<*>) : AsyncBitmapPainter() {
    override val abandonOnDispose: Boolean = false

    private val imageRepository: ImageRepository by inject()

    var chunkSize: Long = 16384

    suspend fun load() {
        withContext(Dispatchers.IO) {
            when(item){
                is ReaderComponent.NetworkChapterItem -> {
                    val request = NetworkImageRequest().fromMangaImage(item.data)

                    imageRepository.cache.getAsync(request.hashString)?.let {
                            miraBitmap ->
                        resource = miraBitmap
                        return@withContext
                    }

                    imageRepository.loadHttpImage(0, request.toKtorRequest())?.let {channel ->
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

                        allBytes.toImageBitmap(maxWidth = 0)?.let { bitmap ->
                            val miraBitmap = bitmap.createMiraBitmap()

                            imageRepository.cache.put(request.hashString, miraBitmap)
                            resource = miraBitmap

//                            saveToExternalCache(request.hashString, allBytes.toChannel())

                            return@withContext
                        }
                    }
                }
                is ReaderComponent.LocalChapterItem -> {
                    MediaStorage.getChapterPagePath(sourceId,mangaId,item.chapterIndex,item.pageIndex)?.let { pagePath ->
                        imageRepository.cache.getAsync(pagePath)?.let { miraBitmap ->
                            resource = miraBitmap
                            return@withContext
                        }
                        bitmapFromFile(pagePath)?.let { bitmap ->
                            val miraBitmap = bitmap.createMiraBitmap()
                            imageRepository.cache.put(pagePath, miraBitmap)
                            resource = miraBitmap
                            return@withContext
                        }
                    }
                }
                is ReaderComponent.TranslatedChapterItem<*> -> {
                    FileBridge.getFilePath(item.translatedFileName,
                        EFilePaths.ReaderPageCache)?.let { pagePath ->
                        imageRepository.cache.getAsync(pagePath)?.let { miraBitmap ->
                            resource = miraBitmap
                            return@withContext
                        }
                        bitmapFromFile(pagePath)?.let { bitmap ->
                            val miraBitmap = bitmap.createMiraBitmap()
                            imageRepository.cache.put(pagePath, miraBitmap)
                            resource = miraBitmap
                            return@withContext
                        }
                    }
                }
            }
            resource = null
        }

    }

    suspend fun saveTo(fileName: String, savePath: EFilePaths) = withContext(Dispatchers.IO){
        resource?.let {
            FileBridge.writeFile(fileName,it.get().toBytes().toChannel(),savePath)
            FileBridge.getFilePath(fileName,savePath)
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
