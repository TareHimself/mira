package com.tarehimself.mira.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.goncalossilva.murmurhash.MurmurHash3
import io.ktor.utils.io.ByteReadChannel
import androidx.compose.ui.graphics.ImageBitmap
import io.github.aakira.napier.Napier
import kotlin.math.max

//expect fun shareString(data: String)
@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }


@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }


//suspend fun ByteReadChannel.readAllInChunks(total: Long,chunkSize: Int = 1024, onDataRead: (total: Int,progress: Int) -> Unit = { _, _ -> }): ByteArray {
//
//    var offset = 0
//    val totalData = total.toInt()
//    val byteArray = ByteArray(totalData)
//
//    do {
//        val currentRead = this.readAvailable(byteArray, offset, chunkSize.coerceIn(0,totalData))
//        offset += currentRead
//        onDataRead(totalData,offset)
//    } while (currentRead > 0)
//
//    return byteArray


fun ByteArray.concat(other: ByteArray): ByteArray{
    return byteArrayOf(*this,*other)
}

fun ByteArray.toChannel(): ByteReadChannel {
    return ByteReadChannel(this.copyOf())
}

suspend fun ByteReadChannel.readAllInChunks(minChunkSize: Int = 1024): ByteArray {
    var offset = 0
    var result = ByteArray(0)
    do {

        val bufferSize = max(minChunkSize,this.availableForRead)

        val buffer = ByteArray(bufferSize)

        val currentRead = this.readAvailable(buffer, 0, bufferSize)
        offset += currentRead
        if(currentRead > 0){
            val copied = if(currentRead < bufferSize){
                buffer.copyOfRange(0,currentRead)
            }
            else {
                buffer
            }

            result = result.concat(copied)
        }
    } while (currentRead > 0)

    return result
}

suspend fun ByteReadChannel.readAllInChunks(total: Long, minChunkSize: Int = 1024, onDataRead: (total: Int, progress: Int) -> Unit = { _, _ -> }): ByteArray {
    var offset = 0
    val totalData = total.toInt()
    var result = ByteArray(0)
    do {
        val bufferSize = max(minChunkSize,this.availableForRead)
        val buffer = ByteArray(bufferSize)

        val currentRead = this.readAvailable(buffer, 0, bufferSize)
        offset += currentRead
        onDataRead(totalData,offset)
        if(currentRead > 0){
            val copied = if(currentRead < bufferSize){
                buffer.copyOfRange(0,currentRead)
            }
            else {
                buffer
            }

            result = result.concat(copied)
        }
    } while (currentRead > 0)

    return result
}

fun String.quickHash(): String {
    return "${MurmurHash3().hash32x86(this.encodeToByteArray())}"
}

expect fun ByteArray.toImageBitmap(): ImageBitmap ?

expect fun ByteArray.toImageBitmap(maxWidth: Int): ImageBitmap ?

expect fun ImageBitmap.sizeBytes(): Int



