import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.ktor.util.cio.toByteReadChannel
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.ceil

suspend fun bufferedPipeTo(a: InputStream, b: OutputStream, chunkSize: Int = 16384) {
    withContext(Dispatchers.IO) {
        val channel = a.toByteReadChannel()
        bufferedPipeTo(channel, b, chunkSize)
    }
}

suspend fun bufferedPipeTo(a: ByteReadChannel, b: OutputStream, chunkSize: Int = 16384) {
    withContext(Dispatchers.IO) {

        var offset = 0

        do {
            val buffer = ByteArray(chunkSize)

            val currentRead = a.readAvailable(buffer, 0, chunkSize)

            offset += currentRead

            if (currentRead > 0) {
                val copied = if (currentRead < chunkSize) {
                    buffer.copyOfRange(0, currentRead)
                } else {
                    buffer
                }

                withContext(Dispatchers.IO) {
                    b.write(copied)
                }
            }
        } while (currentRead > 0)

        b.close()
    }
}

suspend fun File.write(channel: ByteReadChannel, chunkSize: Int = 16384) {
    val file = this

    withContext(Dispatchers.IO) {
        bufferedPipeTo(channel, file.outputStream(), chunkSize)
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