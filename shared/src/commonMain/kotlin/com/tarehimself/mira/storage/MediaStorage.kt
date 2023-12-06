package com.tarehimself.mira.storage

import io.ktor.utils.io.ByteReadChannel

expect class MediaStorage {
    companion object {
        suspend fun loadCachedCover(key: String): Pair<ByteReadChannel, Long>?

        suspend fun getCachedCoverPath(key: String): String?

        suspend fun saveCachedCover(key: String,data: ByteReadChannel): Boolean

        suspend fun loadBookmarkCover(sourceId: String, mangaId: String): Pair<ByteReadChannel, Long>?

        suspend fun getBookmarkCoverPath(sourceId: String, mangaId: String): String?

        suspend fun saveBookmarkCover(sourceId: String, mangaId: String,data: ByteReadChannel): Boolean

        suspend fun loadChapterPage(sourceId: String, mangaId: String, chapterIndex: Int,pageIndex: Int): Pair<ByteReadChannel, Long>?

        suspend fun getChapterPagePath(sourceId: String, mangaId: String, chapterIndex: Int,pageIndex: Int): String?

        suspend fun getNumChapterPages(sourceId: String, mangaId: String, chapterIndex: Int): Int?

        suspend fun saveChapterPage(sourceId: String, mangaId: String, chapterIndex: Int,pageIndex: Int,data: ByteReadChannel): Boolean

        suspend fun markChapterDownloaded(sourceId: String, mangaId: String, chapterIndex: Int): Boolean

        suspend fun deleteChapter(sourceId: String, mangaId: String, chapterIndex: Int): Boolean

        suspend fun doesChapterExist(sourceId: String, mangaId: String, chapterIndex: Int): Boolean

        suspend fun deleteAllChapters(): Boolean
    }
}