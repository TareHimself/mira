import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.tarehimself.mira.common.ECacheType
import io.github.aakira.napier.Napier
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.ByteReadChannel
import io.realm.kotlin.internal.intToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer
import kotlin.math.ceil
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

        actual suspend fun shareImage(data: ByteArray) {
            context?.let { ctx ->
                Toast.makeText(ctx, "Under Construction", Toast.LENGTH_SHORT).show()
                return@let
                val sendIntent: Intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    setType("image/*")
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

suspend fun File.write(channel: ByteReadChannel, minChunkSize: Int = 1024) {
    val file = this

    withContext(Dispatchers.IO) {
        val stream = file.outputStream()

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

        stream.close()
    }
}

fun File.lengthRecursive(): Long {
    if (this.isFile) {
        return this.length()
    }

    if (this.isDirectory) {
        return this.listFiles()?.let {
            it.sumOf { file -> file.lengthRecursive() }
        } ?: 0
    }

    return 0
}

fun File.loadAsBitmap(maxWidth: Int = 0): ImageBitmap? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    val path = this.absolutePath

    return try {
        BitmapFactory.decodeFile(path, options)


        val sampleSize = ceil(
            options.outWidth.toFloat() / when (maxWidth) {
                0 -> options.outWidth.toFloat()
                else -> maxWidth.toFloat()
            }
        ).toInt()

        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        })?.asImageBitmap()
    } catch (e: Exception) {
        null
    }
}

fun File.ensureDir(): Boolean {
    if (!this.exists()) {
        return this.mkdirs()
    }

    return true
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


        actual suspend fun cacheItem(
            key: String,
            channel: ByteReadChannel,
            type: ECacheType,
            maxSize: Long
        ) = withContext(Dispatchers.IO) {
            context?.cacheDir?.absolutePath?.let {
                val dir = File(it, type.ordinal.toString())

                when (dir.ensureDir()) {
                    true -> {
                        cacheMutexes[type.ordinal].withLock {
                            val fileToWrite = File(dir, key)
                            fileToWrite.write(channel)

                            Napier.d { "Cached item of size ${fileToWrite.length()} ${key}" }

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
                    }

                    else -> {

                    }
                }
            } ?: Unit
        }

        actual suspend fun getCachedItemPath(key: String, type: ECacheType): String? =
            withContext(Dispatchers.IO) {
                context?.cacheDir?.absolutePath?.let {
                    val dir = File(it, type.ordinal.toString())

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

        actual suspend fun getCachedItem(
            key: String,
            type: ECacheType
        ): Pair<ByteReadChannel, Long>? = withContext(Dispatchers.IO) {
            getCachedItemPath(key)?.let {
                val targetFile = File(it)

                if (!targetFile.exists()) {
                    null
                } else {
                    Pair(targetFile.readChannel(), targetFile.length())
                }
            }
        }

        actual suspend fun clearCache(type: ECacheType) = withContext(Dispatchers.IO) {
            context?.cacheDir?.absolutePath?.let {
                val dir = File(it, type.ordinal.toString())

                when (dir.exists()) {
                    true -> dir.delete()
                    else -> {}
                }

                Unit
            } ?: Unit
        }

        actual suspend fun getCacheSize(type: ECacheType) = withContext(Dispatchers.IO) {
            context?.cacheDir?.absolutePath?.let {
                val dir = File(it, type.ordinal.toString())

                when (dir.exists()) {
                    true -> dir.lengthRecursive()
                    else -> null
                }
            }
        }


        actual suspend fun saveChapterPage(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int,
            channel: ByteReadChannel
        ): Boolean = withContext(Dispatchers.IO) {
            context?.filesDir?.absolutePath?.let {
                val fileDir = File(File(File(it, "chapters"), mangaKeyHash), chapterIdHash)

                if (!fileDir.ensureDir()) {
                    Napier.d { "Failed to create dir ${fileDir.absolutePath} " }
                    false
                } else {


                    val targetFile = File(fileDir, page.toString().padStart(5,'0'))

                    Napier.d { "Saving to ${fileDir.absolutePath}" }

                    targetFile.write(channel)

                    true
                }
            } ?: false
        }

        actual fun isChapterDownloaded(mangaKeyHash: String, chapterIdHash: String): Boolean {
            return context?.filesDir?.absolutePath?.let {
                val fileDir = File(File(File(it, "chapters"), mangaKeyHash), chapterIdHash)

                return fileDir.exists()
            } ?: false
        }

        actual fun deleteDownloadedChapter(mangaKeyHash: String, chapterIdHash: String): Boolean {
            return context?.filesDir?.absolutePath?.let {
                val fileDir = File(File(File(it, "chapters"), mangaKeyHash), chapterIdHash)

                if (!fileDir.exists()) {
                    return true
                }

                return fileDir.deleteRecursively()
            } ?: false
        }

        actual suspend fun getDownloadedChapterPage(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int
        ): Pair<ByteReadChannel, Long>? = withContext(Dispatchers.IO) {
            context?.filesDir?.absolutePath?.let {
                val fileDir =
                    File(File(File(File(it, "chapters"), mangaKeyHash), chapterIdHash), page.toString().padStart(5,'0'))

                if (!fileDir.exists()) {
                    Napier.d { "Chapter Page Does not exist ${fileDir.absolutePath}" }
                    null
                } else {
                    Pair(fileDir.readChannel(), fileDir.length())
                }
            }
        }

        actual fun getDownloadedChapterPagesNum(mangaKeyHash: String, chapterIdHash: String): Int =
            context?.filesDir?.absolutePath?.let {
                val fileDir = File(File(File(it, "chapters"), mangaKeyHash), chapterIdHash)

                if (!fileDir.exists()) {
                    -1
                } else {
                    fileDir.listFiles()?.size ?: -1
                }
            } ?: -1

        actual suspend fun deleteDownloadedChapters(): Boolean = withContext(Dispatchers.IO) {
            context?.filesDir?.absolutePath?.let {
                val fileDir = File(it, "chapters")
                Napier.d { "Deleting ${fileDir.absolutePath} TOTAL SIZE ${File(it).length()}" }
                fileDir.exists() && fileDir.deleteRecursively()
            } ?: false
        }

        actual suspend fun getDownloadedChapterPageAsBitmap(
            mangaKeyHash: String,
            chapterIdHash: String,
            page: Int,
            maxWidth: Int
        ): ImageBitmap? = withContext(Dispatchers.IO) {
            context?.filesDir?.absolutePath?.let {
                val fileDir =
                    File(File(File(File(it, "chapters"), mangaKeyHash), chapterIdHash), page.toString().padStart(5,'0'))

                if (!fileDir.exists()) {
                    Napier.d { "Chapter Page Does not exist ${fileDir.absolutePath}" }
                    null
                } else {
                    fileDir.loadAsBitmap(maxWidth)
                }
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

fun calcSampleSize(width: Int,maxWidth: Int): Int = when(maxWidth >= 1){
    true -> ceil(width.toFloat() / maxWidth.toFloat()).toInt()
    else -> 1
}

actual fun ByteArray.toImageBitmap(): ImageBitmap? {

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
    {
        return try {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(this))).asImageBitmap()
        } catch (e: Exception){
            Napier.e("Error decoding image",e)
            null
        }
    }

    return BitmapFactory.decodeByteArray(this, 0, size)?.let { it.asImageBitmap() }
}

actual fun ImageBitmap.sizeBytes(): Int {
    return this.asAndroidBitmap().byteCount
}


actual fun ByteArray.toImageBitmap(maxWidth: Int): ImageBitmap? {

    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
    {
        try {
            val header = ImageDecoder.OnHeaderDecodedListener { decoder, info, _ ->
                val size = info.size
                val sampleSize = calcSampleSize(size.width,maxWidth)

                decoder.setTargetSampleSize(sampleSize)
            }


            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(this)), header).asImageBitmap()
        }
        catch (e: Exception){
            Napier.e("Error decoding image",e)
            return null
        }
    }

    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }


    BitmapFactory.decodeByteArray(this, 0, size, options)

    val sampleSize = when(maxWidth >= 1){
        true -> ceil(options.outWidth.toFloat() / maxWidth.toFloat()).toInt()
        else -> 1
    }

    return BitmapFactory.decodeByteArray(this, 0, size, BitmapFactory.Options().apply {
        inSampleSize = sampleSize
    })?.asImageBitmap()

}

actual fun ImageBitmap.toBytes(): ByteArray {
    val stream = ByteArrayOutputStream()
    this.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
}

actual suspend fun bitmapFromCache(
    key: String,
    type: ECacheType,
    maxWidth: Int
): ImageBitmap? {


    return FileBridge.getCachedItemPath(key,type)?.let {

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        {
            try {
                val header = ImageDecoder.OnHeaderDecodedListener { decoder, info, _ ->
                    val size = info.size
                    val sampleSize = calcSampleSize(size.width,maxWidth)

                    decoder.setTargetSampleSize(sampleSize)
                }


                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(File(it)), header).asImageBitmap()
            }
            catch (e: Exception){
                Napier.e("Error decoding image",e)
                return null
            }
        }

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        BitmapFactory.decodeFile(it, options)

        val sampleSize = when(maxWidth >= 1){
            true -> ceil(options.outWidth.toFloat() / maxWidth.toFloat()).toInt()
            else -> 1
        }

        return BitmapFactory.decodeFile(it, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        })?.asImageBitmap()
    }

}

actual fun ImageBitmap.free() {
    this.asAndroidBitmap().recycle()
}

actual fun ImageBitmap.usable(): Boolean {
    return !this.asAndroidBitmap().isRecycled
}
