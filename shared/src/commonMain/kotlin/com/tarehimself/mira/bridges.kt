import io.ktor.utils.io.ByteReadChannel

expect class ShareBridge {
    companion object {
        fun shareText(data: String)
    }
}

expect class CacheBridge {
    companion object {
        fun cacheCover(key: String,data: ByteArray)

        fun getCachedCover(key: String): Pair<ByteReadChannel?,Long>
    }
}