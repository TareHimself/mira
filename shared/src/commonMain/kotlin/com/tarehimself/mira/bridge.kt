import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.tarehimself.mira.common.ECacheType
import io.ktor.utils.io.ByteReadChannel

expect class ShareBridge {
    companion object {
        fun shareText(data: String)

        suspend fun shareImage(data: ByteArray)
    }
}

expect class FileBridge {
    companion object {
        suspend fun cacheItem(key: String, channel: ByteReadChannel, type: ECacheType = ECacheType.Images, maxSize: Long = 0)

        suspend fun clearCache(type: ECacheType = ECacheType.Images)

        suspend fun getCacheSize(type: ECacheType = ECacheType.Images): Long?

        suspend fun getCachedItem(key: String,type: ECacheType = ECacheType.Images): Pair<ByteReadChannel, Long>?

        suspend fun getCachedItemPath(key: String,type: ECacheType = ECacheType.Images): String?

        suspend fun saveChapterPage(mangaKeyHash: String, chapterIdHash: String, page: Int, channel: ByteReadChannel): Boolean

        fun isChapterDownloaded(mangaKeyHash: String, chapterIdHash: String): Boolean

        fun deleteDownloadedChapter(mangaKeyHash: String, chapterIdHash: String): Boolean

        suspend fun getDownloadedChapterPage(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int): Pair<ByteReadChannel, Long>?

        suspend fun getDownloadedChapterPageAsBitmap(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int,
            maxWidth: Int): ImageBitmap?

        fun getDownloadedChapterPagesNum(mangaKeyHash: String, chapterIdHash: String): Int

        suspend fun deleteDownloadedChapters(): Boolean
    }
}


@Composable
expect fun DropdownMenu(expanded: Boolean, modifier: Modifier = Modifier,onDismissRequest: () -> Unit = {}, content: @Composable() (ColumnScope.() -> Unit))

@Composable
expect fun DropdownMenuItem(text: @Composable () -> Unit,
                             onClick: () -> Unit,
                             modifier: Modifier = Modifier,
                             leadingIcon: @Composable() (() -> Unit)? = null,
                             trailingIcon: @Composable() (() -> Unit)? = null)



expect fun ByteArray.toImageBitmap(): ImageBitmap?

expect fun ByteArray.toImageBitmap(maxWidth: Int): ImageBitmap?

expect fun ImageBitmap.sizeBytes(): Int

expect fun ImageBitmap.free()

expect fun ImageBitmap.toBytes(): ByteArray

expect fun ImageBitmap.usable(): Boolean

expect suspend fun bitmapFromCache(key: String, type: ECacheType = ECacheType.Images, maxWidth: Int = 0): ImageBitmap?



