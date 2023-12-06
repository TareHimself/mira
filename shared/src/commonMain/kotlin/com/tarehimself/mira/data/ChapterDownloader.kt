
package com.tarehimself.mira.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import com.tarehimself.mira.common.AsyncQueue
import com.tarehimself.mira.common.Cache
import com.tarehimself.mira.storage.MediaStorage
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

suspend fun <T> withChapterDownload(
    job: ChapterDownloadJob,
    onException: suspend (ex: Exception) -> Unit = { throw it },
    block: suspend () -> T
): T? {
    return try {
        block()
    } catch (ex: Exception) {
        MediaStorage.deleteChapter(sourceId = job.sourceId, mangaId = job.mangaId, chapterIndex = job.chapterIndex)
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
    val chapterIndex: Int,
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

    val currentJob: MutableState<ChapterDownloadJob?>
    val currentJobProgress: MutableState<Float>

    val downloadQueue: AsyncQueue<ChapterDownloadJob>

    val downloadedCache: Cache<String,Boolean>

    val queueUpdates: MutableState<Int>
    fun processJobs()
    suspend fun downloadChapter(
        sourceId: String,
        mangaId: String,
        chapterId: String,
        chapterIndex: Int,
        jobName: String
    )


    suspend fun isDownloaded(
        sourceId: String,
        mangaId: String,
        chapterIndex: Int
    ): Boolean

    suspend fun isDownloaded(job: ChapterDownloadJob): Boolean

    suspend fun updateProgress(progress: Float)

    suspend fun queueUpdated()

    suspend fun getPagesDownloaded(sourceId: String, mangaId: String, chapterIndex: Int): Int

    suspend fun deleteChapter(sourceId: String, mangaId: String, chapterIndex: Int): Boolean

    suspend fun deleteAllChapters(): Boolean
}

class DefaultChapterDownloader() : ChapterDownloader {

    override val api: MangaApi by inject()

    override val realmRepository: RealmRepository by inject()

    override var client = HttpClient()

    override val downloadedCache: Cache<String, Boolean> = Cache()

    override val downloadQueue: AsyncQueue<ChapterDownloadJob> = AsyncQueue()

    override val queueUpdates: MutableState<Int> = mutableStateOf(0)

    override val isRunning: Boolean = false

    override val currentJob: MutableState<ChapterDownloadJob?> = mutableStateOf(null)
    override val currentJobProgress: MutableState<Float> = mutableStateOf(0.0f)

    init {
        this.processJobs()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun processJobs() {

        CoroutineScope(newSingleThreadContext("Download Thread")).launch {

            while (true) {
                val job = downloadQueue.get();
                currentJob.value = job
                queueUpdated()
                Napier.d { "Downloading job ${job.jobName}" }
                val pages = api.getChapter(job.sourceId, job.mangaId, job.chapterId)

                if (pages.data == null) {
                    currentJob.value = null
                    updateProgress(0.0f)
                    Napier.d { "job ${job.jobName}, returned no api data" }
                    continue
                }

                withChapterDownload(job, onException = {
                    if (it is ChapterDownloadFailed) {
                        currentJob.value = null
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
                            throw ChapterDownloadFailed(
                                "Failed To Get Page ${response.status} ${response.request.url}",
                                job
                            )
                        }

                        val result = MediaStorage.saveChapterPage(sourceId = job.sourceId, mangaId = job.mangaId, chapterIndex = job.chapterIndex, pageIndex = idx, data = response.bodyAsChannel())

                        if (!result) {
                            throw ChapterDownloadFailed("Failed To Download Page", job)
                        }

                        updateProgress((idx + 1).toFloat() / pages.data.size)
                    }

                    MediaStorage.markChapterDownloaded(sourceId = job.sourceId, mangaId = job.mangaId, chapterIndex = job.chapterIndex)

                    currentJob.value = null
                    updateProgress(0.0f)
                    Napier.d { "Completed job ${job.jobName}" }
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
        chapterIndex: Int,
        jobName: String
    ) {

        val job = ChapterDownloadJob(
            sourceId = sourceId,
            mangaId = mangaId,
            chapterId = chapterId,
            chapterIndex = chapterIndex,
            jobName = jobName
        )

        if(downloadQueue.has(job)){
            return
        }

        downloadQueue.put(
            job
        )

        queueUpdated()
    }

    override suspend fun isDownloaded(sourceId: String, mangaId: String, chapterIndex: Int): Boolean {
        val result = MediaStorage.doesChapterExist(sourceId = sourceId,mangaId = mangaId,chapterIndex = chapterIndex)
        downloadedCache.put(sourceId+mangaId+chapterIndex,result)
        return result
    }

    override suspend fun isDownloaded(job: ChapterDownloadJob): Boolean {
        return isDownloaded(sourceId = job.sourceId,mangaId = job.mangaId,chapterIndex = job.chapterIndex)
    }

    override suspend fun getPagesDownloaded(
        sourceId: String,
        mangaId: String,
        chapterIndex: Int
    ): Int {
        return MediaStorage.getNumChapterPages(sourceId,mangaId,chapterIndex) ?: -1
    }

    override suspend fun deleteChapter(
        sourceId: String,
        mangaId: String,
        chapterIndex: Int
    ): Boolean {
        val result = MediaStorage.deleteChapter(sourceId = sourceId, mangaId = mangaId, chapterIndex = chapterIndex)
        queueUpdated()
        return result
    }

    override suspend fun deleteAllChapters(): Boolean {
        val result = MediaStorage.deleteAllChapters()
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
    chapterIndex: Int,
    chapterDownloader: ChapterDownloader = koinInject()
): MutableState<Pair<EChapterDownloadState, Float>> {


    val downloadState = remember(sourceId, mangaId, chapterId) {
        mutableStateOf(run {
            val job = ChapterDownloadJob(sourceId = sourceId, mangaId = mangaId, chapterId = chapterId, chapterIndex = chapterIndex)

            if(chapterDownloader.downloadQueue.has(job)){
                Pair(EChapterDownloadState.PENDING, 0.0f)
            }else if(chapterDownloader.downloadedCache[job.sourceId+job.mangaId+job.chapterIndex] == true){
                Pair(EChapterDownloadState.DOWNLOADED, 0.0f)
            }
            else{
                Pair(EChapterDownloadState.NONE, 0.0f)
            }
        })
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {

            downloadState.value = downloadState.value.copy(
                first = when (chapterDownloader.isDownloaded(sourceId, mangaId, chapterIndex)) {
                    true -> EChapterDownloadState.DOWNLOADED
                    else -> EChapterDownloadState.NONE
                }
            )

            Napier.d { "Set Download State ${downloadState.value.first} $sourceId $mangaId $chapterId" }

            val job = ChapterDownloadJob(
                sourceId = sourceId,
                mangaId = mangaId,
                chapterId = chapterId,
                chapterIndex = chapterIndex
            )

            val currentJob by chapterDownloader.currentJob

            snapshotFlow { chapterDownloader.queueUpdates.value }.collect {
                if (job == currentJob) { // Currently Being Downloaded
                    if (downloadState.value.first != EChapterDownloadState.DOWNLOADING) {
                        downloadState.value =
                            downloadState.value.copy(EChapterDownloadState.DOWNLOADING, 0.0f)
                    }
                    // Wait for the download to complete
                    launch {// So we can stop it later
                        snapshotFlow { chapterDownloader.currentJobProgress.value }.cancellable()
                            .collect { downloadProgress ->
                                downloadState.value = downloadState.value.copy(
                                    EChapterDownloadState.DOWNLOADING,
                                    downloadProgress
                                )

                                if (downloadProgress == 1.0f || job != currentJob) {

                                    downloadState.value = downloadState.value.copy(
                                        if (chapterDownloader.isDownloaded(
                                                sourceId,
                                                mangaId,
                                                chapterIndex
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
                } else if (chapterDownloader.downloadQueue.has(job)) { // Not being Downloaded But is in the queue
                    if (downloadState.value.first != EChapterDownloadState.PENDING) {
                        downloadState.value =
                            downloadState.value.copy(EChapterDownloadState.PENDING, 0.0f)
                    }
                } else {
                    val isDownloaded = chapterDownloader.isDownloaded(job)
                    if (downloadState.value.first != EChapterDownloadState.NONE && !chapterDownloader.downloadQueue.has(
                            job
                        ) && !isDownloaded
                    ) { // Not in the queue and not Downloaded
                        downloadState.value =
                            downloadState.value.copy(EChapterDownloadState.NONE, 0.0f)
                    } else { // Downloaded or not downloaded
                        downloadState.value =
                            downloadState.value.copy(
                                first = when (isDownloaded) {
                                    true -> EChapterDownloadState.DOWNLOADED
                                    else -> EChapterDownloadState.NONE
                                }, second = 0.0f
                            )
                    }
                }
            }
        }
    }

    return downloadState
}

@Composable
fun rememberChapterDownloadState(
    job: ChapterDownloadJob, chapterDownloader: ChapterDownloader = koinInject()
): MutableState<Pair<EChapterDownloadState, Float>> {
    return rememberChapterDownloadState(job.sourceId, job.mangaId, job.chapterId,job.chapterIndex, chapterDownloader)
}
