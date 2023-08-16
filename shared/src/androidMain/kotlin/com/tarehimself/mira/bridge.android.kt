import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.PopupProperties
import io.github.aakira.napier.Napier
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

actual class ShareBridge {

    actual companion object {

        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null

        fun setContext(con: Context) {
            context = con
        }

        fun clearContext() {
            context = null
        }

        actual fun shareText(data: String) {
            context?.let { ctx ->
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, data)
                    type = "text/plain"

                }

                val shareIntent = Intent.createChooser(sendIntent, null)

                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                //or Intent.FLAG_ACTIVITY_CLEAR_TASK

                ctx.startActivity(shareIntent)
            }
        }
    }


}

actual class FileBridge {
    actual companion object {

        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null

        fun setContext(con: Context) {
            context = con
        }

        fun clearContext() {
            context = null
        }

        fun doesCacheDirExist() {

        }

        suspend fun File.write(channel: ByteReadChannel, minChunkSize: Int = 1024) {
            val stream = this.outputStream()

            var offset = 0
            do {

                val bufferSize = max(minChunkSize, channel.availableForRead)

                val buffer = ByteArray(bufferSize)

                val currentRead = channel.readAvailable(buffer, 0, bufferSize)
                offset += currentRead
                if (currentRead > 0) {
                    val copied = if (currentRead < bufferSize) {
                        buffer.copyOfRange(0, currentRead)
                    } else {
                        buffer
                    }

                    withContext(Dispatchers.IO) {
                        stream.write(copied)
                    }
                }
            } while (currentRead > 0)

            withContext(Dispatchers.IO) {
                stream.close()
            }
        }

        actual suspend fun cacheCover(key: String, channel: ByteReadChannel) {
            context?.cacheDir?.absolutePath?.let {
                val dir = File(it, "covers")

                if (!dir.exists()) {
                    dir.mkdirs()
                }

                File(dir, "$key.png").write(channel)
            }

        }

        actual fun getCachedCover(key: String): Pair<ByteReadChannel?, Long> {
            return context?.cacheDir?.absolutePath?.let {
                val dir = File(it, "covers")

                if (!dir.exists()) {
                    return Pair(null, 0)
                }

                val targetFile = File(dir, "$key.png")

                if (!targetFile.exists()) {
                    return Pair(null, 0)
                }


                return Pair(targetFile.readChannel(), targetFile.length())
            } ?: Pair(null, 0)
        }

        actual suspend fun saveChapterPage(
            uniqueId: String,
            chapter: String,
            page: Int,
            channel: ByteReadChannel
        ): Boolean {
            return context?.filesDir?.absolutePath?.let {
                val fileDir = File(File(File(it, "chapters"), uniqueId), chapter)

                if (!fileDir.exists()) {
                    if (!fileDir.mkdirs()) {
                        return false
                    }
                }

                Napier.d { "Saving to dir ${fileDir.absolutePath} ${fileDir.exists()}" }

                val targetFile = File(fileDir, page.toString())

                targetFile.write(channel)

                return true
            } ?: true
        }

        actual fun isChapterDownloaded(uniqueId: String, chapter: String): Boolean {
            return context?.filesDir?.absolutePath?.let {
                val fileDir = File(File(File(it, "chapters"), uniqueId), chapter)

                return fileDir.exists()
            } ?: true
        }

        actual fun deleteDownloadedChapter(uniqueId: String, chapter: String): Boolean {
            return context?.filesDir?.absolutePath?.let {
                val fileDir = File(File(File(it, "chapters"), uniqueId), chapter)

                if (!fileDir.exists()) {
                    return true
                }

                return fileDir.deleteRecursively()
            } ?: true
        }

        actual fun getDownloadedChapterPage(
            uniqueId: String,
            chapter: String,
            page: Int
        ): Pair<ByteReadChannel?, Long> {
            return context?.filesDir?.absolutePath?.let {
                val fileDir =
                    File(File(File(File(it, "chapters"), uniqueId), chapter), page.toString())

                if (!fileDir.exists()) {
                    Napier.d { "Chapter Page Does not exist ${fileDir.absolutePath}" }
                    return Pair(null, 0)
                }

                return Pair(fileDir.readChannel(), fileDir.length())
            } ?: Pair(null, 0)
        }

        actual fun getDownloadedChapterPagesNum(uniqueId: String, chapter: String): Int {
            return context?.filesDir?.absolutePath?.let {
                val fileDir = File(File(File(it, "chapters"), uniqueId), chapter)

                if (!fileDir.exists()) {
                    return -1
                }

                return fileDir.listFiles()?.size ?: -1
            } ?: -1
        }

    }
}

@Composable
actual fun DropdownMenu(
    expanded: Boolean,
    modifier: Modifier,
    onDismissRequest: () -> Unit,
    content: @Composable() (ColumnScope.() -> Unit)
) {
    androidx.compose.material3.DropdownMenu(
        expanded = expanded,
        modifier = modifier,
        content = content,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
actual fun DropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier,
    leadingIcon: @Composable() (() -> Unit)?,
    trailingIcon: @Composable() (() -> Unit)?,
) {
    androidx.compose.material3.DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon
    )
}