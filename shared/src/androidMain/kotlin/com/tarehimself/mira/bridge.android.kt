import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Images
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.tarehimself.mira.common.ECacheType
import com.tarehimself.mira.common.EFilePaths
import com.tarehimself.mira.common.hash
import com.tarehimself.mira.data.RealmRepository
import io.github.aakira.napier.Napier
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.ByteReadChannel
import io.realm.kotlin.internal.intToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import kotlin.math.ceil


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

        actual suspend fun shareFile(filePath: String) {
            context?.let { ctx ->

                val uri = FileProvider.getUriForFile(
                    ctx,
                    "com.oyintare.mira.provider",
                    File(filePath)
                );

                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = ctx.contentResolver.getType(uri)
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }


                val shareIntent = Intent.createChooser(sendIntent, null)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                Napier.d { "Shared file with uri $uri" }
                ctx.startActivity(shareIntent)

            }
        }

        actual suspend fun saveImage(filePath: String) {
            context?.let { ctx ->

//                val uri = FileProvider.getUriForFile(
//                    ctx,
//                    "com.oyintare.mira.provider",
//                    File(filePath)
//                );

                val values = ContentValues()

                values.put(Images.Media.DATE_TAKEN, System.currentTimeMillis())
                values.put(Images.Media.MIME_TYPE, "image/jpeg")
//                values.put(MediaStore.MediaColumns.DATA, filePath)
                values.put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES + "/Mira"
                )

                ctx.contentResolver.insert(Images.Media.EXTERNAL_CONTENT_URI, values)?.let { uri ->
                    ctx.contentResolver.openOutputStream(uri)?.let { out ->
                        val file = File(filePath)
                        bufferedPipeTo(file.inputStream(), out)
                        Napier.d { "Saved Image" }
                    }
                }
            }
        }

    }
}




actual class FileBridge {
    actual companion object {

        private val cacheMutexes = ECacheType.entries.map {
            Mutex()
        }

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


        actual suspend fun writeFile(
            key: String,
            channel: ByteReadChannel,
            type: EFilePaths,
            maxSize: Long
        ) = withContext(Dispatchers.IO) {
            getDirPath(type)?.let {
                val dir = File(it)

                when (dir.ensureDir()) {
                    true -> {
                        val fileToWrite = File(dir, key)
                        fileToWrite.write(channel)

                        Napier.d { "Cached item of size ${fileToWrite.length()} ${fileToWrite.absolutePath}" }

                        var cacheSize = dir.lengthRecursive()

                        if (maxSize != intToLong(0) && cacheSize > maxSize) {
                            dir.listFiles()?.let { files ->

                                files.sortBy { file -> file.lastModified() }

                                val mutableFiles = files.toMutableList()

                                @Suppress("KotlinConstantConditions")
                                while (cacheSize > maxSize) {
                                    val toRemove = mutableFiles.firstOrNull() ?: break
                                    val toRemoveSize = toRemove.length()
                                    if (toRemove.delete()) {
                                        cacheSize -= toRemoveSize
                                        mutableFiles.removeAt(0)
                                        return@withContext
                                    } else {
                                        break
                                    }
                                }
                            } ?: Unit
                        }
                    }

                    else -> {

                    }
                }
            } ?: Unit
        }

        actual suspend fun getFilePath(key: String, type: EFilePaths): String? =
            withContext(Dispatchers.IO) {
                getDirPath(type)?.let {
                    val dir = File(it)

                    val targetFile = File(dir, key)

                    when (targetFile.exists()) {
                        true -> {
//                            targetFile.copyTo(targetFile, true)
                            targetFile.absolutePath
                        }

                        else -> null
                    }
                }
            }

        actual suspend fun readFile(
            key: String,
            type: EFilePaths
        ): Pair<ByteReadChannel, Long>? = withContext(Dispatchers.IO) {
            getFilePath(key, type)?.let {
                val targetFile = File(it)

                if (!targetFile.exists()) {
                    null
                } else {
                    Pair(targetFile.readChannel(), targetFile.length())
                }
            }
        }

        actual suspend fun readFile(filePath: String): Pair<ByteReadChannel, Long>? =
            withContext(Dispatchers.IO) {
                val targetFile = File(filePath)

                if (!targetFile.exists()) {
                    null
                } else {
                    Pair(targetFile.readChannel(), targetFile.length())
                }
            }

        actual suspend fun clearFiles(type: EFilePaths) = withContext(Dispatchers.IO) {
            getDirPath(type)?.let {
                val dir = File(it)

                when (dir.exists()) {
                    true -> dir.delete()
                    else -> {}
                }

                Unit
            } ?: Unit
        }

        actual suspend fun getDirSize(type: EFilePaths) = withContext(Dispatchers.IO) {
            getDirPath(type)?.let {
                val dir = File(it)

                when (dir.exists()) {
                    true -> dir.lengthRecursive()
                    else -> null
                }
            }
        }

        actual fun isChapterDownloaded(mangaKeyHash: String, chapterIdHash: String): Boolean {
            return context?.filesDir?.absolutePath?.let {
                val fileDir = File(File(File(it, "chapters"), mangaKeyHash), chapterIdHash)

                return fileDir.exists()
            } ?: false
        }

        actual suspend fun getChapterPagePath(
            sourceId: String,
            mangaId: String,
            chapterId: String,
            pageIndex: Int
        ): String? = withContext(Dispatchers.IO) {
            context?.filesDir?.absolutePath?.let {
                val fileDir =
                    File(
                        File(
                            File(
                                File(it, "chapters"),
                                RealmRepository.getBookmarkKey(sourceId, mangaId).hash()
                            ), chapterId.hash()
                        ),
                        "${pageIndex.toString().padStart(5, '0')}.png"
                    )

                if (!fileDir.exists()) {
                    Napier.d { "Chapter Page Does not exist ${fileDir.absolutePath}" }
                    null
                } else {
                    fileDir.absolutePath
                }
            }
        }

        actual suspend fun saveChapterPage(
            sourceId: String,
            mangaId: String,
            chapterId: String,
            pageIndex: Int,
            channel: ByteReadChannel
        ): Boolean = withContext(Dispatchers.IO) {
            context?.filesDir?.absolutePath?.let {
                val fileDir =

                    File(
                        File(
                            File(it, "chapters"),
                            RealmRepository.getBookmarkKey(sourceId, mangaId).hash()
                        ), chapterId.hash()
                    )

                if (!fileDir.ensureDir()) {
                    false
                } else {
                    File(fileDir, "${pageIndex.toString().padStart(5, '0')}.png").write(channel)
                    true
                }


            } ?: false
        }

        actual suspend fun getChapterPageCount(
            sourceId: String,
            mangaId: String,
            chapterId: String
        ): Int = withContext(Dispatchers.IO) {
            context?.filesDir?.absolutePath?.let {
                val fileDir = File(
                    File(
                        File(it, "chapters"),
                        RealmRepository.getBookmarkKey(sourceId, mangaId).hash()
                    ), chapterId.hash()
                )

                if (!fileDir.exists()) {
                    -1
                } else {
                    fileDir.listFiles()?.size ?: -1
                }
            } ?: -1
        }

        actual suspend fun deleteChapter(
            sourceId: String,
            mangaId: String,
            chapterId: String
        ): Boolean = withContext(Dispatchers.IO) {
            context?.filesDir?.absolutePath?.let {
                val fileDir = File(
                    File(
                        File(it, "chapters"),
                        RealmRepository.getBookmarkKey(sourceId, mangaId).hash()
                    ), chapterId.hash()
                )
                Napier.d { "Deleting ${fileDir.absolutePath} TOTAL SIZE ${File(it).length()}" }
                fileDir.exists() && fileDir.deleteRecursively()
            } ?: false
        }

        actual suspend fun deleteAllChapters(): Boolean = withContext(Dispatchers.IO) {
            context?.filesDir?.absolutePath?.let {
                val fileDir = File(it, "chapters")
                Napier.d { "Deleting ${fileDir.absolutePath} TOTAL SIZE ${File(it).length()}" }
                fileDir.exists() && fileDir.deleteRecursively()
            } ?: false
        }

        actual fun getDirPath(type: EFilePaths): String? = context?.let {
            when (type) {
                EFilePaths.Bookmarks -> File(it.filesDir, "bookmarks").absolutePath
                EFilePaths.CoverCache -> File(it.cacheDir, "covers").absolutePath
                EFilePaths.SharedFiles -> File(it.filesDir, "shared").absolutePath
                EFilePaths.ReaderPageCache -> File(it.cacheDir, "reader").absolutePath
            }
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

fun calcSampleSize(width: Int, maxWidth: Int): Int = when (maxWidth >= 1) {
    true -> ceil(width.toFloat() / maxWidth.toFloat()).toInt()
    else -> 1
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(this)))
                .asImageBitmap()
        }

        BitmapFactory.decodeByteArray(this, 0, size)?.let { it.asImageBitmap() }
    } catch (e: Exception) {
        Napier.e("Error decoding image", e)
        null
    }
}

actual fun ImageBitmap.sizeBytes(): Int {
    return this.asAndroidBitmap().byteCount
}


actual fun ByteArray.toImageBitmap(maxWidth: Int): ImageBitmap? {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {

            val header = ImageDecoder.OnHeaderDecodedListener { decoder, info, _ ->
                val size = info.size
                val sampleSize = calcSampleSize(size.width, maxWidth)

                decoder.setTargetSampleSize(sampleSize)
            }


            return ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(ByteBuffer.wrap(this)),
                header
            ).asImageBitmap()
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeByteArray(this, 0, size, options)

        val sampleSize = when (maxWidth >= 1) {
            true -> ceil(options.outWidth.toFloat() / maxWidth.toFloat()).toInt()
            else -> 1
        }

        return BitmapFactory.decodeByteArray(this, 0, size, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        })?.asImageBitmap()
    } catch (e: Exception) {
        Napier.e("Error decoding image", e)
        return null
    }
}

actual fun ImageBitmap.toBytes(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

actual suspend fun bitmapFromFile(
    key: String,
    type: EFilePaths,
    maxWidth: Int
): ImageBitmap? {
    return FileBridge.getFilePath(key, type)?.let {
        bitmapFromFile(filePath = it, maxWidth = maxWidth)
    }
}

actual suspend fun bitmapFromFile(
    filePath: String,
    maxWidth: Int
): ImageBitmap? {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val header = ImageDecoder.OnHeaderDecodedListener { decoder, info, _ ->
                val size = info.size
                val sampleSize = calcSampleSize(size.width, maxWidth)

                decoder.setTargetSampleSize(sampleSize)
            }


            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(filePath)), header)
                .asImageBitmap()
        }
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(filePath, options)

        val sampleSize = when (maxWidth >= 1) {
            true -> ceil(options.outWidth.toFloat() / maxWidth.toFloat()).toInt()
            else -> 1
        }

        return BitmapFactory.decodeFile(filePath, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        })?.asImageBitmap()
    } catch (e: Exception) {
        Napier.e("Error decoding image", e)
        return null
    }
}

actual fun ImageBitmap.free() {
    this.asAndroidBitmap().recycle()
}

actual fun ImageBitmap.usable(): Boolean {
    return !this.asAndroidBitmap().isRecycled
}

actual fun Modifier.imePadding(): Modifier = this.then(Modifier.imePadding())
actual class MiraFile actual constructor(path: String) {

    private val filePath = path
    actual fun absolutePath(): String {
        return asAndroidFile().absolutePath
    }

    actual fun exists(): Boolean {
        return asAndroidFile().exists()
    }

    actual suspend fun write(data: ByteReadChannel): Boolean = withContext(Dispatchers.IO) {
        asAndroidFile().write(data)
        exists()
    }

    actual suspend fun read(): Pair<ByteReadChannel, Long>? = withContext(Dispatchers.IO) {
        if (exists()) {
            Pair(asAndroidFile().readChannel(), asAndroidFile().length())
        } else {
            null
        }
    }

    actual operator fun MiraFile.plus(item: MiraFile): MiraFile {
        return MiraFile(filePath + File.pathSeparator + item.filePath)
    }

    actual operator fun MiraFile.plus(item: String): MiraFile {
        return MiraFile(filePath + File.pathSeparator + item)
    }

    actual override fun toString(): String = asAndroidFile().absolutePath


    fun asAndroidFile(): File = File(filePath)
}

fun File.asMiraFile(): MiraFile = MiraFile(absolutePath)