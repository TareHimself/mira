import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.ktor.utils.io.ByteReadChannel

expect class ShareBridge {
    companion object {
        fun shareText(data: String)
    }
}

expect class FileBridge {
    companion object {
        suspend fun cacheCover(key: String,channel: ByteReadChannel)

        fun getCachedCover(key: String): Pair<ByteReadChannel?,Long>

        suspend fun saveChapterPage(uniqueId: String, chapter: String,page: Int, channel: ByteReadChannel): Boolean

        fun isChapterDownloaded(uniqueId: String,chapter: String): Boolean

        fun deleteDownloadedChapter(uniqueId: String,chapter: String): Boolean

        fun getDownloadedChapterPage(uniqueId: String,chapter: String,page: Int): Pair<ByteReadChannel?,Long>

        fun getDownloadedChapterPagesNum(uniqueId: String,chapter: String): Int
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