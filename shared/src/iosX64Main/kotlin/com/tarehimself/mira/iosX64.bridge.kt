import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.tarehimself.mira.common.EFilePaths
import io.ktor.utils.io.ByteReadChannel

actual class ShareBridge {
    actual companion object {
        actual fun shareText(data: String) {
        }

        actual suspend fun shareFile(filePath: String) {
        }


    }
}

actual class FileBridge {
    actual companion object {
        actual suspend fun writeFile(
            key: String,
            channel: ByteReadChannel,
            type: EFilePaths,
            maxSize: Long
        ) {
        }

        actual suspend fun clearFiles(type: EFilePaths) {
        }

        actual suspend fun getDirSize(type: EFilePaths): Long? {
            TODO("Not yet implemented")
        }

        actual suspend fun readFile(
            key: String,
            type: EFilePaths
        ): Pair<ByteReadChannel, Long>? {
            TODO("Not yet implemented")
        }

        actual suspend fun getFilePath(
            key: String,
            type: EFilePaths
        ): String? {
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

        actual suspend fun getDownloadedChapterPage(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int
        ): Pair<ByteReadChannel, Long>? {
            TODO("Not yet implemented")
        }

        actual suspend fun getDownloadedChapterPageAsBitmap(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int,
            maxWidth: Int
        ): ImageBitmap? {
            TODO("Not yet implemented")
        }

        actual fun getDownloadedChapterPagesNum(
            mangaKeyHash: String,
            chapterIdHash: String
        ): Int {
            TODO("Not yet implemented")
        }

        actual suspend fun deleteDownloadedChapters(): Boolean {
            TODO("Not yet implemented")
        }

        actual fun getDirPath(type: EFilePaths): String? {
            TODO("Not yet implemented")
        }

        actual suspend fun getDownloadedChapterPagePath(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int
        ): String? {
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

actual suspend fun bitmapFromFile(
    key: String,
    type: EFilePaths,
    maxWidth: Int
): ImageBitmap? {
    TODO("Not yet implemented")
}

actual fun Modifier.imePadding(): Modifier {
    TODO("Not yet implemented")
}

actual class MiraFile actual constructor(path: String)

actual operator fun MiraFile.plus(item: MiraFile): MiraFile {
    TODO("Not yet implemented")
}

actual operator fun MiraFile.plus(item: String): MiraFile {
    TODO("Not yet implemented")
}

actual suspend fun bitmapFromFile(
    filePath: String,
    maxWidth: Int
): ImageBitmap? {
    TODO("Not yet implemented")
}

