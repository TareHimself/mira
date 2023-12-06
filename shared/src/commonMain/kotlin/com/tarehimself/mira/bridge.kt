import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import com.tarehimself.mira.common.EFilePaths
import io.ktor.utils.io.ByteReadChannel


expect class ShareBridge {
    companion object {
        fun shareText(data: String)

        suspend fun shareFile(filePath: String)

        suspend fun saveImage(filePath: String)
    }
}

expect class FileBridge {
    companion object {

//        suspend fun saveFile(path: String, channel: ByteReadChannel)
//        suspend fun getFile(path: String) : Pair<ByteReadChannel, Long>?
        suspend fun writeFile(key: String, channel: ByteReadChannel, type: EFilePaths, maxSize: Long = 0)

        suspend fun clearFiles(type: EFilePaths)

        suspend fun getDirSize(type: EFilePaths): Long?

        suspend fun readFile(key: String, type: EFilePaths): Pair<ByteReadChannel, Long>?

        suspend fun readFile(filePath: String): Pair<ByteReadChannel, Long>?

        suspend fun getFilePath(key: String, type: EFilePaths): String?

        fun getDirPath(type: EFilePaths): String?

        suspend fun saveChapterPage(sourceId: String,mangaId: String, chapterId: String, pageIndex: Int,channel: ByteReadChannel): Boolean

        fun isChapterDownloaded(mangaKeyHash: String, chapterIdHash: String): Boolean

        suspend fun getChapterPagePath(sourceId: String,mangaId: String, chapterId: String, pageIndex: Int): String?

        suspend fun getChapterPageCount(sourceId: String,mangaId: String, chapterId: String): Int

        suspend fun deleteChapter(sourceId: String,mangaId: String, chapterId: String): Boolean

        suspend fun deleteAllChapters(): Boolean
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

expect suspend fun bitmapFromFile(key: String, type: EFilePaths, maxWidth: Int = 0): ImageBitmap?

expect suspend fun bitmapFromFile(filePath: String, maxWidth: Int = 0): ImageBitmap?


expect fun Modifier.imePadding(): Modifier


expect class MiraFile(path: String){
    fun absolutePath(): String

    fun exists(): Boolean

    suspend fun write(data: ByteReadChannel): Boolean

    suspend fun read(): Pair<ByteReadChannel,Long>?

    operator fun MiraFile.plus(item: MiraFile): MiraFile

    operator fun MiraFile.plus(item: String): MiraFile

    override fun toString(): String
}



