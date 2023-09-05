package com.tarehimself.mira.common

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.goncalossilva.murmurhash.MurmurHash3
import io.ktor.utils.io.ByteReadChannel
import kotlin.math.max
import kotlin.math.min
import kotlin.reflect.KProperty

//expect fun shareString(data: String)
@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }


@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }

@Composable fun Modifier.useTopInsets(): Modifier{
    val insets = LocalWindowInsets.current
    val density = LocalDensity.current

    return this.padding(top = insets.getTop(density).pxToDp())
}

@Composable fun Modifier.useBottomInsets(): Modifier{
    val insets = LocalWindowInsets.current
    val density = LocalDensity.current

    return this.padding(bottom = insets.getBottom(density).pxToDp())
}
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
    return ByteReadChannel(this)
}

suspend fun ByteReadChannel.readAllInChunks(minChunkSize: Long = 1024): ByteArray {
    var offset = 0
    var result = ByteArray(0)
    do {

        val bufferSize = max(minChunkSize.toInt(),this.availableForRead)

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

suspend fun ByteReadChannel.readAllInChunks(total: Long, minChunkSize: Long = 1024, onDataRead: (total: Long, progress: Long) -> Unit = { _, _ -> }): ByteArray {
    var offset = 0
    val totalData = total.toInt()
    var result = ByteArray(0)
    do {
        val bufferSize = min(max(minChunkSize,this.availableForRead.toLong()),total).toInt()

        val buffer = ByteArray(bufferSize)

        val currentRead = this.readAvailable(buffer, 0, bufferSize)
        offset += currentRead
        onDataRead(totalData.toLong(),offset.toLong())
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



fun Modifier.borderRadius(radius: Dp) = this.clip(RoundedCornerShape(radius))


fun Modifier.borderRadius(
topStart: Dp = 0.dp,
topEnd: Dp = 0.dp,
bottomEnd: Dp = 0.dp,
bottomStart: Dp = 0.dp
) = this.clip(RoundedCornerShape(
    topStart = topStart,
    topEnd = topEnd,
    bottomEnd = bottomEnd,
    bottomStart = bottomStart
))


fun <T>mutableCustomStateValueOf(initial: T, onValueUpdated: (value: T) -> Unit = {}): MutableCustomStateValue<T> {
    return MutableCustomStateValue(initialValue = initial, onValueUpdated = onValueUpdated)
}

@Stable
class MutableCustomStateValue<T>(initialValue: T, val onValueUpdated: (value: T) -> Unit) {
    
    
    var value by mutableStateOf(initialValue)
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
        onValueUpdated(this.value)
    }
}

fun Color.brightness(percentage: Float): Color{
    return this.copy(red = this.red * percentage, green = this.green * percentage, blue = this.blue * percentage)
}

val LocalWindowInsets = staticCompositionLocalOf { WindowInsets(0.dp) }

enum class ECacheType {
    Images,
    Reader
}





