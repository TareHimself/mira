import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.tarehimself.mira.common.ECacheType
import io.ktor.utils.io.ByteReadChannel

actual class ShareBridge {
    actual companion object {
        actual fun shareText(data: String) {
        }

        actual suspend fun shareImage(data: ByteArray) {
        }
    }
}

actual class FileBridge {
    actual companion object {
        actual suspend fun cacheItem(
            key: String,
            channel: ByteReadChannel,
            maxSize: Long
        ) {
            TODO("Not yet implemented")
        }

        actual suspend fun getCachedItem(key: String): Pair<ByteReadChannel, Long>? {
            TODO("Not yet implemented")
        }

        actual suspend fun saveChapterPage(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int,
            channel: ByteReadChannel
        ): Boolean {
            TODO("Not yet implemented")
        }

        actual fun isChapterDownloaded(
            mangaKeyHash: String,
            chapterIdHash: String
        ): Boolean {
            TODO("Not yet implemented")
        }

        actual fun deleteDownloadedChapter(
            mangaKeyHash: String,
            chapterIdHash: String
        ): Boolean {
            TODO("Not yet implemented")
        }

        actual fun getDownloadedChapterPage(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int
        ): Pair<ByteReadChannel, Long>? {
            TODO("Not yet implemented")
        }

        actual fun getDownloadedChapterPagesNum(
            mangaKeyHash: String,
            chapterIdHash: String
        ): Int {
            TODO("Not yet implemented")
        }

        actual fun deleteDownloadedChapters(): Boolean {
            TODO("Not yet implemented")
        }

        actual suspend fun getCachedItemPath(key: String): String? {
            TODO("Not yet implemented")
        }

        actual fun getDownloadedChapterPageAsBitmap(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int,
            maxWidth: Int
        ): ImageBitmap? {
            TODO("Not yet implemented")
        }

    }
}

@Composable
actual fun DropdownMenu(
    expanded: Boolean,
    modifier: Modifier,
    onDismissRequest: () -> Unit,
    content: @Composable() ColumnScope.() -> Unit
) {
    androidx.compose.material3.DropdownMenu(expanded = expanded, modifier = modifier, onDismissRequest = onDismissRequest, content = content)
}

@Composable
actual fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
    leadingIcon: @Composable() (() -> Unit)?,
    trailingIcon: @Composable() (() -> Unit)?
) {
    androidx.compose.material3.DropdownMenuItem(text = text, onClick = onClick, modifier = modifier, leadingIcon = leadingIcon, trailingIcon = trailingIcon)
}

actual fun ByteArray.toImageBitmap(maxWidth: Int): ImageBitmap? {
    TODO("Not yet implemented")
}

actual fun ImageBitmap.sizeBytes(): Int {
    TODO("Not yet implemented")
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    TODO("Not yet implemented")
}

actual fun ImageBitmap.toBytes(): ByteArray {
    TODO("Not yet implemented")
}

actual fun ImageBitmap.free() {
}

actual fun ImageBitmap.usable(): Boolean {
    TODO("Not yet implemented")
}

actual suspend fun bitmapFromCache(
    key: String,
    type: ECacheType,
    maxWidth: Int
): ImageBitmap? {
    TODO("Not yet implemented")
}