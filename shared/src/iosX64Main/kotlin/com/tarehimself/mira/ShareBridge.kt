import io.ktor.utils.io.ByteReadChannel

actual class ShareBridge {
    actual companion object {
        actual fun shareText(data: String) {
        }
    }
}

actual class CacheBridge {
    actual companion object {
        actual fun cacheCover(key: String, data: ByteArray) {
        }

        actual fun getCachedCover(key: String): Pair<ByteReadChannel?, Long> {
            TODO("Not yet implemented")
        }

    }
}