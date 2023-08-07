import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import io.ktor.util.cio.readChannel
import io.ktor.utils.io.ByteReadChannel
import java.io.File

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
            context?.let {ctx ->
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

actual class CacheBridge {
    actual companion object {

        @SuppressLint("StaticFieldLeak")
        private var context: Context? = null

        fun setContext(con: Context) {
            context = con
        }

        fun clearContext() {
            context = null
        }

        fun doesCacheDirExist(){

        }

        actual fun cacheCover(key: String, data: ByteArray) {
            context?.cacheDir?.absolutePath?.let {
                val dir = File(it,"covers")

                if(!dir.exists()){
                    dir.mkdirs()
                }

                File(dir,"$key.png").writeBytes(data)
            }

        }

        actual fun getCachedCover(key: String): Pair<ByteReadChannel?, Long> {
            return context?.cacheDir?.absolutePath?.let {
                val dir = File(it,"covers")

                if(!dir.exists()){
                    return Pair(null,0)
                }

                val targetFile = File(dir,"$key.png")

                if(!targetFile.exists())
                {
                    return  Pair(null,0)
                }


                return Pair(targetFile.readChannel(), targetFile.length())
            } ?: Pair(null,0)
        }

    }
}