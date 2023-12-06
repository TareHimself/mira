package com.tarehimself.mira.storage

import android.annotation.SuppressLint
import android.content.Context
import com.tarehimself.mira.common.hash
import com.tarehimself.mira.common.padZeros
import ensureDir
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import write
import java.io.File

actual class MediaStorage {
    actual companion object {

        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null

        fun setContext(con: Context) {
            context = con
        }

        fun clearContext() {
            context = null
        }

        private const val coversCacheFolder = "covers"

        private const val zeroPadNum = 6

        private const val downloadCompletedFileName = "complete"

        private fun getCachePath(root: String) = context?.let {
            File(it.externalCacheDir, root)
        }

        private fun getMediaPath(root: String) = context?.let {
            File(it.getExternalFilesDir(null),root)
        }

        private fun getMangaPath(sourceId: String,
                                    mangaId: String) = getMediaPath(sourceId.hash())?.let {
            File(it,mangaId.hash())
        }

        private fun getChaptersPath(sourceId: String,
                                    mangaId: String) = getMangaPath(sourceId,mangaId)?.let {
                                        File(it,"chapters")
        }


        private fun getChapterPath(sourceId: String,
                                    mangaId: String,chapterIndex: Int) = getChaptersPath(sourceId,mangaId)?.let {
            File(it,chapterIndex.padZeros(zeroPadNum))
        }

        actual suspend fun loadCachedCover(key: String): Pair<ByteReadChannel, Long>? = withContext(Dispatchers.IO){
            getCachePath(coversCacheFolder)?.let {
                val file = File(it,key)

                when(file.exists()){
                    true -> Pair(file.readChannel(),file.length())
                    false -> null
                }
            }
        }

        actual suspend fun getCachedCoverPath(key: String): String? = withContext(Dispatchers.IO){
            getCachePath(coversCacheFolder)?.let {
                val file = File(it,key)

                when(file.exists()){
                    true -> file.absolutePath
                    false -> null
                }
            }
        }

        actual suspend fun saveCachedCover(key: String,data: ByteReadChannel): Boolean = withContext(Dispatchers.IO){
            getCachePath(coversCacheFolder)?.let {
                it.ensureDir()

                File(it,key).write(data)
                true
            } ?: false
        }

        actual suspend fun loadBookmarkCover(
            sourceId: String,
            mangaId: String
        ): Pair<ByteReadChannel, Long>? = withContext(Dispatchers.IO){
            getMediaPath(sourceId.hash())?.let {
                val file = File(File(it,mangaId.hash()),"cover.png")

                when(file.exists()){
                    true -> Pair(file.readChannel(),file.length())
                    false -> null
                }
            }
        }

        actual suspend fun getBookmarkCoverPath(
            sourceId: String,
            mangaId: String
        ): String? = withContext(Dispatchers.IO){
            getMediaPath(sourceId.hash())?.let {
                val file = File(File(it,mangaId.hash()),"cover.png")

                when(file.exists()){
                    true -> file.absolutePath
                    false -> null
                }
            }
        }

        actual suspend fun saveBookmarkCover(
            sourceId: String,
            mangaId: String,
            data: ByteReadChannel
        ): Boolean = withContext(Dispatchers.IO){
            getMediaPath(sourceId.hash())?.let {
                val dir = File(it,mangaId.hash())

                dir.ensureDir()

                File(dir,"cover.png").write(data)
                true
            } ?: false
        }

        actual suspend fun loadChapterPage(
            sourceId: String,
            mangaId: String,
            chapterIndex: Int,
            pageIndex: Int
        ): Pair<ByteReadChannel, Long>? = withContext(Dispatchers.IO){
            getChapterPath(sourceId,mangaId,chapterIndex)?.let {
                val file = File(it,"${pageIndex.padZeros(
                    zeroPadNum)}.png")

                when(file.exists()){
                    true -> Pair(file.readChannel(),file.length())
                    false -> null
                }
            }
        }

        actual suspend fun getChapterPagePath(
            sourceId: String,
            mangaId: String,
            chapterIndex: Int,
            pageIndex: Int
        ): String? = withContext(Dispatchers.IO){
            getChapterPath(sourceId,mangaId,chapterIndex)?.let {
                val file = File(it,"${pageIndex.padZeros(
                    zeroPadNum)}.png")

                when(file.exists()){
                    true -> file.absolutePath
                    false -> null
                }
            }
        }

        actual suspend fun saveChapterPage(
            sourceId: String,
            mangaId: String,
            chapterIndex: Int,
            pageIndex: Int,
            data: ByteReadChannel
        ): Boolean = withContext(Dispatchers.IO){
            getChapterPath(sourceId,mangaId,chapterIndex)?.let {
                it.ensureDir()

                File(it,"${pageIndex.padZeros(
                    zeroPadNum)}.png").write(data)
                true
            } ?: false
        }

        actual suspend fun markChapterDownloaded(
            sourceId: String,
            mangaId: String,
            chapterIndex: Int,
        ): Boolean = withContext(Dispatchers.IO){
            getChapterPath(sourceId,mangaId,chapterIndex)?.let {
                val file = File(it,
                    downloadCompletedFileName)

                file.createNewFile()
                true
            } ?: false
        }

        actual suspend fun deleteChapter(
            sourceId: String,
            mangaId: String,
            chapterIndex: Int
        ): Boolean = withContext(Dispatchers.IO){
            getChapterPath(sourceId,mangaId,chapterIndex)?.deleteRecursively() ?: false
        }

        actual suspend fun doesChapterExist(
            sourceId: String,
            mangaId: String,
            chapterIndex: Int
        ): Boolean = withContext(Dispatchers.IO){
            getChapterPath(sourceId,mangaId,chapterIndex)?.let {
                val file = File(it,downloadCompletedFileName)

                file.exists()
            } ?: false
        }

        actual suspend fun deleteAllChapters(): Boolean = withContext(Dispatchers.IO){
            getMediaPath("0")?.parentFile?.let {
                it.listFiles()?.toList()?.forEach { source ->
                    source.listFiles()?.toList()?.forEach { manga ->
                        val chaptersDir = File(manga,"chapters")

                        if(chaptersDir.exists()){
                            chaptersDir.deleteRecursively()
                        }
                    }
                }
                true
            } ?: false
        }

        actual suspend fun getNumChapterPages(
            sourceId: String,
            mangaId: String,
            chapterIndex: Int
        ): Int? = withContext(Dispatchers.IO){
            getChapterPath(sourceId,mangaId,chapterIndex)?.let {
                (it.listFiles()?.size ?: 0) - 1
            } ?: -1
        }
    }
}