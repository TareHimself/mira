package com.tarehimself.mira.data

import FileBridge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.tarehimself.mira.common.quickHash
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

suspend fun <T> withChapterDownload(
    uniqueId: String,
    chapterHash: String,
    onException: suspend (ex: Exception) -> Unit = { throw it },
    block: suspend () -> T
): T? {
    return try {
        block()
    } catch (ex: Exception) {
        FileBridge.deleteDownloadedChapter(uniqueId, chapterHash)
        onException(ex)
        null
    }
}

class ChapterDownloadFailed(message: String, val job: ChapterDownloadJob) :
    RuntimeException(message)

class ChapterDownloadJob(
    val sourceId: String,
    val mangaId: String,
    val chapterId: String,
    val jobName: String = "",
) {
    override fun hashCode(): Int {
        return "$sourceId$mangaId$chapterId".hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ChapterDownloadJob) {
            return super.equals(other)
        }

        return other.sourceId == sourceId && other.mangaId == mangaId && other.chapterId == chapterId
    }
}

interface ChapterDownloader : KoinComponent {
    val client: HttpClient

    val api: MangaApi

    val realmRepository: RealmRepository

    val isRunning: Boolean

    val currentJobProgress: MutableState<Float>

    val downloadQueue: LinkedHashSet<ChapterDownloadJob>

    val queueUpdates: MutableState<Int>
    fun processJobs()
    suspend fun downloadChapter(
        sourceId: String,
        mangaId: String,
        chapterId: String,
        jobName: String
    )


    fun isDownloaded(
        sourceId: String,
        mangaId: String,
        chapterId: String
    ): Boolean

    fun isDownloaded(job: ChapterDownloadJob): Boolean

    suspend fun updateProgress(progress: Float)

    suspend fun queueUpdated()

    fun getPagesDownloaded(sourceId: String,mangaId: String,chapterId: String): Int

    suspend fun deleteChapter(sourceId: String,mangaId: String,chapterId: String): Boolean

    suspend fun deleteAllChapters(): Boolean
}

class DefaultChapterDownloader() : ChapterDownloader {

    override val api: MangaApi by inject()

    override val realmRepository: RealmRepository by inject()

    override var client = HttpClient()

    override val downloadQueue: LinkedHashSet<ChapterDownloadJob> = LinkedHashSet()

    override val queueUpdates: MutableState<Int> = mutableStateOf(0)

    override val isRunning: Boolean = false

    override val currentJobProgress: MutableState<Float> = mutableStateOf(0.0f)

    init {
        this.processJobs()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun processJobs() {
        CoroutineScope(newSingleThreadContext("Download Thread")).launch {

            while (true) {
                if (downloadQueue.isNotEmpty()) {
                    val job = downloadQueue.first()
                    queueUpdated()

                    Napier.d { "Downloading job ${job.jobName}" }

                    val pages = api.getChapter(job.sourceId, job.mangaId, job.chapterId)

                    if (pages.data == null) {
                        downloadQueue.remove(job)
                        queueUpdated()
                        updateProgress(0.0f)
                        Napier.d { "job ${job.jobName}, returned no api data" }
                        continue
                    }

                    val uniqueId =
                        realmRepository.getBookmarkKey(job.sourceId, job.mangaId).quickHash()
                    val chapterHash = job.chapterId.quickHash()

                    withChapterDownload(uniqueId, chapterHash, onException = {
                        if (it is ChapterDownloadFailed) {
                            downloadQueue.remove(downloadQueue.first())
                            queueUpdated()
                            updateProgress(0.0f)
                            Napier.d { "Error while downloading job ${it.job.jobName}, ${it.message}" }
                        } else {
                            throw it
                        }
                    }) {

                        pages.data.forEachIndexed { idx, data ->
                            val response = client.get(data.src) {
                                data.headers.forEach {
                                    header(it.key, it.value)
                                }
                            }
                            if (response.status != HttpStatusCode.OK) {
                                throw ChapterDownloadFailed("Failed To Get Page", job)
                            }


                            if (!FileBridge.saveChapterPage(
                                    uniqueId,
                                    chapterHash,
                                    idx,
                                    response.bodyAsChannel()
                                )
                            ) {
                                throw ChapterDownloadFailed("Failed To Download Page", job)
                            }

                            updateProgress((idx + 1).toFloat() / pages.data.size)
                        }
                        downloadQueue.remove(job)
                        queueUpdated()
                        updateProgress(0.0f)
                        Napier.d { "Completed job ${job.jobName}" }
                    }
                } else {
                    delay(500)
                }
            }
        }
    }

    override suspend fun queueUpdated() {
        withContext(Dispatchers.Main) {
            queueUpdates.value += 1
        }
    }

    override suspend fun updateProgress(progress: Float) {
        withContext(Dispatchers.Main) {
            currentJobProgress.value = progress
        }
    }

    override suspend fun downloadChapter(
        sourceId: String,
        mangaId: String,
        chapterId: String,
        jobName: String
    ) {
        if(downloadQueue.add(
                ChapterDownloadJob(
                    sourceId = sourceId,
                    mangaId = mangaId,
                    chapterId = chapterId,
                    jobName = jobName
                )
            )){
            queueUpdated()
        }
    }

    override fun isDownloaded(sourceId: String, mangaId: String, chapterId: String): Boolean {
        return FileBridge.isChapterDownloaded(
            realmRepository.getBookmarkKey(sourceId, mangaId).quickHash(), chapterId.quickHash()
        )
    }

    override fun isDownloaded(job: ChapterDownloadJob): Boolean {
        return FileBridge.isChapterDownloaded(
            realmRepository.getBookmarkKey(job.sourceId, job.mangaId).quickHash(),
            job.chapterId.quickHash()
        )
    }

    override fun getPagesDownloaded(sourceId: String, mangaId: String, chapterId: String): Int {
        return FileBridge.getDownloadedChapterPagesNum(realmRepository.getBookmarkKey(sourceId, mangaId).quickHash(),chapterId.quickHash())
    }

    override suspend fun deleteChapter(sourceId: String, mangaId: String, chapterId: String): Boolean {
        val result = FileBridge.deleteDownloadedChapter(realmRepository.getBookmarkKey(sourceId, mangaId).quickHash(),chapterId.quickHash())
        queueUpdated()
        return result
    }

    override suspend fun deleteAllChapters(): Boolean {
        val result = FileBridge.deleteDownloadedChapters()
        queueUpdated()
        return result
    }
}

enum class EChapterDownloadState {
    NONE,
    PENDING,
    DOWNLOADING,
    DOWNLOADED
}

@Composable
fun rememberChapterDownloadState(
    sourceId: String,
    mangaId: String,
    chapterId: String,
    chapterDownloader: ChapterDownloader = koinInject()
): MutableState<Pair<EChapterDownloadState, Float>> {


    val downloadState = remember(sourceId, mangaId, chapterId) {
        mutableStateOf(Pair(EChapterDownloadState.NONE, 0.0f))
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO){
            downloadState.value = downloadState.value.copy(first = when(chapterDownloader.isDownloaded(sourceId,mangaId,chapterId)){
                true -> EChapterDownloadState.DOWNLOADED
                else -> EChapterDownloadState.NONE
            })
        }
        val job = ChapterDownloadJob(
            sourceId = sourceId,
            mangaId = mangaId,
            chapterId = chapterId
        )

        snapshotFlow { chapterDownloader.queueUpdates.value }.collect {
            val currentJob = chapterDownloader.downloadQueue.firstOrNull()
            if (job == currentJob) {

                if (downloadState.value.first != EChapterDownloadState.DOWNLOADING) {
                    downloadState.value =
                        downloadState.value.copy(EChapterDownloadState.DOWNLOADING, 0.0f)
                }
                launch {// So we can stop it later
                    snapshotFlow { chapterDownloader.currentJobProgress.value }.cancellable()
                        .collect { downloadProgress ->
                            if (chapterDownloader.isDownloaded(sourceId, mangaId, chapterId)) {
                                EChapterDownloadState.DOWNLOADED
                            } else {
                                EChapterDownloadState.NONE
                            }
                            downloadState.value = downloadState.value.copy(
                                EChapterDownloadState.DOWNLOADING,
                                downloadProgress
                            )

                            if (downloadProgress == 1.0f || currentJob != chapterDownloader.downloadQueue.firstOrNull()) {

                                downloadState.value = downloadState.value.copy(
                                    if (chapterDownloader.isDownloaded(
                                            sourceId,
                                            mangaId,
                                            chapterId
                                        )
                                    ) {
                                        EChapterDownloadState.DOWNLOADED
                                    } else {
                                        EChapterDownloadState.NONE
                                    }, 0.0f
                                )
                                cancel()
                                this.coroutineContext.job.cancel()
                            }
                        }
                }
            } else if (chapterDownloader.downloadQueue.contains(job) && downloadState.value.first != EChapterDownloadState.PENDING) {
                downloadState.value = downloadState.value.copy(EChapterDownloadState.PENDING, 0.0f)
            }else if(!chapterDownloader.isDownloaded(job) && downloadState.value.first == EChapterDownloadState.DOWNLOADED){
                downloadState.value = downloadState.value.copy(first = EChapterDownloadState.NONE, second = 0.0f)
            }
        }
    }

    return downloadState
}

@Composable
fun rememberChapterDownloadState(job: ChapterDownloadJob,chapterDownloader: ChapterDownloader = koinInject()
): MutableState<Pair<EChapterDownloadState, Float>> {
    return rememberChapterDownloadState(job.sourceId,job.mangaId,job.chapterId,chapterDownloader)
}
