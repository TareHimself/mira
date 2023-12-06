package com.tarehimself.mira.storage

import io.ktor.utils.io.ByteReadChannel

actual class MediaStorage {
    actual companion object {
        actual suspend fun loadCachedCover(key: String): Pair<ByteReadChannel, Long>? {
            TODO("Not yet implemented")
        }

        actual suspend fun saveCachedCover(key: String): Boolean {
            TODO("Not yet implemented")
        }

        actual suspend fun loadBookmarkCover(
            sourceId: String,
            mangaId: String
        ): Pair<ByteReadChannel, Long>? {
            TODO("Not yet implemented")
        }

        actual suspend fun saveBookmarkCover(
            sourceId: String,
            mangaId: String
        ): Boolean {
            TODO("Not yet implemented")
        }

        actual suspend fun loadChapterPage(
            sourceId: String,
            mangaId: String,
            chapterId: String,
            pageIndex: Int
        ): Pair<ByteReadChannel, Long>? {
            TODO("Not yet implemented")
        }

        actual suspend fun saveChapterPage(
            sourceId: String,
            mangaId: String,
            chapterId: String,
            pageIndex: Int
        ): Boolean {
            TODO("Not yet implemented")
        }

        actual suspend fun markChapterDownloaded(
            sourceId: String,
            mangaId: String,
            chapterId: String
        ): Boolean {
            TODO("Not yet implemented")
        }

        actual suspend fun deleteChapter(
            sourceId: String,
            mangaId: String,
            chapterId: String
        ): Boolean {
            TODO("Not yet implemented")
        }

        actual suspend fun doesChapterExist(
            sourceId: String,
            mangaId: String,
            chapterId: String
        ): Boolean {
            TODO("Not yet implemented")
        }

        actual suspend fun deleteAllChapters(): Boolean {
            TODO("Not yet implemented")
        }

    }
}