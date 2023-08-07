package com.tarehimself.mira.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import io.ktor.utils.io.ByteReadChannel

//expect fun shareString(data: String)
@Composable
fun Dp.dpToPx() = with(LocalDensity.current) { this@dpToPx.toPx() }


@Composable
fun Int.pxToDp() = with(LocalDensity.current) { this@pxToDp.toDp() }


suspend fun ByteReadChannel.readAllInChunks(total: Long,chunkSize: Int = 1024, onDataRead: (total: Int,progress: Int) -> Unit = { _, _ -> }): ByteArray {

    var offset = 0
    val totalData = total.toInt()
    val byteArray = ByteArray(totalData)

    do {
        val currentRead = this.readAvailable(byteArray, offset, chunkSize.coerceIn(0,totalData))
        offset += currentRead
        onDataRead(totalData,offset)
    } while (currentRead > 0)

    return byteArray
}

