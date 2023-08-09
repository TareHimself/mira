package com.tarehimself.mira.data


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.arkivanov.essenty.parcelable.Parcelize
import com.benasher44.uuid.uuid4
import io.github.aakira.napier.Napier
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.ext.realmSetOf
import io.realm.kotlin.notifications.InitialResults
import io.realm.kotlin.notifications.UpdatedResults
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.RealmSet
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mongodb.kbson.ObjectId

@Parcelize
class StoredChapter() : RealmObject, MangaChapter {


    constructor(id: String, name: String, released: String?) : this() {
        this.id = id
        this.name = name
        this.released = released
    }

    @PrimaryKey
    @IgnoredOnParcel
    var uniqueId: ObjectId = ObjectId()

    @Index
    @IgnoredOnParcel
    override var id: String = ""

    @Index
    @IgnoredOnParcel
    override var name: String = ""

    override var released: String? = null
}

class StoredMangaExtras() : RealmObject, MangaExtras {


    constructor(name: String, value: String) : this() {
        this.name = name
        this.value = value
    }

    override var name: String = ""
    override var value: String = ""

    @PrimaryKey
    var uniqueId: ObjectId = ObjectId()

}

class StoredSources() : RealmObject {


    constructor(id: String, name: String) : this() {
        this.id = id
        this.name = name
    }

    @PrimaryKey
    var id: String = ""
    var name: String = ""

}

class StoredMangaCategory() : RealmObject {
    @PrimaryKey
    var id: String = ""

    @Index
    var name: String = ""

    var position: Int = 0

    constructor(name: String, position: Int) : this() {
        this.name = name
        this.id = uuid4().toString()
        this.position = position
    }
}
class StoredChapterReadInfo() : RealmObject {

    constructor(index: Int, progress: Int, total: Int) : this() {
        this.index = index
        this.progress = progress
        this.total = total
    }

    @Index
    var index: Int = 0
    var progress: Int = 0
    var total: Int = 0

    @PrimaryKey
    var uniqueId: ObjectId = ObjectId()
}
class StoredChaptersRead() : RealmObject {
    @PrimaryKey
    var uniqueId: String = ""

    @Index
    var mangaId: String = ""

    @Index
    var sourceId: String = ""

    var read: RealmSet<Int> = realmSetOf()
    var current: StoredChapterReadInfo? = null

    constructor(
        uniqueId: String,
        mangaId: String,
        sourceId: String
    ) : this() {
        this.uniqueId = uniqueId
        this.mangaId = mangaId
        this.sourceId = sourceId
    }
}
class StoredManga() : RealmObject, MangaPreview, MangaData {


    constructor(
        uniqueId: String,
        id: String,
        source: String,
        name: String,
        cover: String,
        addedAt: Long
    ) : this() {
        this.uniqueId = uniqueId
        this.id = id
        this.sourceId = source
        this.name = name
        this.cover = cover
        this.addedAt = addedAt
    }


    @PrimaryKey
    var uniqueId: String = ""

    @Index
    override var id: String = ""

    @Index
    var sourceId: String = ""

    override var share: String = ""

    @Index
    override var name: String = ""
    override var cover: String = ""

    @Index
    var addedAt: Long = 0


    @Index
    override var status: String = ""
    override var description: String = ""

    override var tags: RealmList<String> = realmListOf()
    override var extras: RealmList<StoredMangaExtras> = realmListOf()

    var chapters: RealmList<StoredChapter> = realmListOf()

    var categories: RealmSet<String> = realmSetOf()
}


interface RealmRepository : KoinComponent {

    val api: MangaApi

    val realm: Realm

    var activeScope: CoroutineScope?
    fun stopActiveManaging()
    fun manageData()

    fun getMangaKey(manga: StoredManga): String

    fun getMangaKey(sourceId: String, manga: ApiMangaPreview): String

    fun getMangaKey(sourceId: String, manga: MangaData): String

    fun getMangaKey(sourceId: String, manga: String): String

    fun has(sourceId: String, manga: ApiMangaPreview): Boolean

    fun has(sourceId: String, manga: MangaData): Boolean

    fun has(sourceId: String, manga: String): Boolean

    fun getManga(mangaKey: String): RealmResults<StoredManga>

    fun getChaptersRead(mangaKey: String): RealmResults<StoredChaptersRead>
    fun hasReadInfo(sourceId: String, manga: ApiMangaPreview): Boolean

    fun hasReadInfo(sourceId: String, manga: MangaData): Boolean

    fun hasReadInfo(sourceId: String, manga: String): Boolean

    suspend fun bookmark(sourceId: String, mangaData: MangaData, mangaChapters: List<MangaChapter>)

    suspend fun bookmark(sourceId: String, mangaPreview: MangaPreview)

    suspend fun updateChapters(sourceId: String, mangaId: String, mangaChapters: List<MangaChapter>)

    suspend fun removeBookmark(sourceId: String, mangaId: String)

//    fun subscribeOnBookmarksUpdated(callback: () -> Unit): () -> Unit

    suspend fun markChapterAsRead(sourceId: String, mangaId: String, chapterIndex: Int)

    suspend fun updateMangaReadInfo(
        sourceId: String,
        mangaId: String,
        chapterIndex: Int,
        progress: Int,
        total: Int
    )

    suspend fun updateManga(
        sourceId: String,
        mangaId: String,
        update: (data: StoredManga) -> Unit
    )

    suspend fun createCategory(name: String)

}

class DefaultRealmRepository : RealmRepository {

    override val api: MangaApi by inject()

    override val realm: Realm = Realm.open(
        RealmConfiguration.Builder(
            schema = setOf(
                StoredManga::class,
                StoredSources::class,
                StoredMangaExtras::class,
                StoredChapter::class,
                StoredChapterReadInfo::class,
                StoredMangaCategory::class,
                StoredChaptersRead::class,
            )
        ).deleteRealmIfMigrationNeeded()
//            .migration(AutomaticSchemaMigration {
//            val oldRealm = it.oldRealm
//            val newRealm = it.newRealm
////
////            val oldStoredManga = oldRealm.query("StoredManga")
//
//            it.enumerate(className = "StoredManga"){
//                oldObject, newObject ->
//                newObject?.run {
//                    set(
//                        "chaptersRead",
//                        realmSetOf<Float>().toString()
//                    )
//                }
//            }
//
//        })
//            .schemaVersion(1)
            .build()
    )

    override var activeScope: CoroutineScope? = null

    override fun stopActiveManaging() {
        activeScope?.cancel()
    }

    override fun manageData() {
//        stopActiveManaging()
//        activeScope = CoroutineScope(Dispatchers.IO)
//        activeScope?.launch {
//            var lastData = listOf<StoredManga>()
//            realm.query(StoredManga::class).asFlow().collect { changes ->
//                when (changes) {
//                    is InitialResults<StoredManga> -> {
//                        for (x in changes.list) {
//                            bookmarks[x.uniqueId] = x
//                        }
//
//                        lastData = changes.list
//
//                        Napier.i(
//                            "Loaded ${bookmarks.keys.size} bookmarks",
//                            tag = "realm"
//                        )
//
//                        for (callback in ArrayList(bookmarksUpdatedCallbacks)) { // ArrayList is to avoid java.util.ConcurrentModificationException, not perfect but works
//                            callback()
//                        }
//                    }
//
//                    is UpdatedResults<StoredManga> -> {
//                        for (deletedIdx in changes.deletions) {
//                            val deleted = lastData[deletedIdx]
//                            Napier.i("Removing ${deleted.uniqueId} from bookmarks", tag = "realm")
//                            bookmarks.remove(deleted.uniqueId)
//                        }
//
//                        for (insertedIdx in changes.insertions) {
//                            val inserted = changes.list[insertedIdx]
//                            Napier.i("Adding ${inserted.uniqueId} to bookmarks", tag = "realm")
//                            bookmarks[inserted.uniqueId] = inserted
//
//                        }
//
//                        for (changedIdx in changes.changes) {
//                            val changed = changes.list[changedIdx]
//                            Napier.i("Updating ${changed.uniqueId} in bookmarks", tag = "realm")
//                            bookmarks[changed.uniqueId] = changed
//                        }
//
//                        lastData = changes.list
//
//                        Napier.i(
//                            "Bookmarks Updated calling ${bookmarksUpdatedCallbacks.size} callbacks",
//                            tag = "realm"
//                        )
//
//                        for (callback in ArrayList(bookmarksUpdatedCallbacks)) { // ArrayList is to avoid java.util.ConcurrentModificationException, not perfect but works
//                            callback()
//                        }
//                    }
//
//                    else -> {}
//                }
//            }
//        }
    }


    override fun getMangaKey(manga: StoredManga): String {
        return "${manga.sourceId}|${manga.id}"
    }

    override fun getMangaKey(sourceId: String, manga: ApiMangaPreview): String {
        return "${sourceId}|${manga.id}"
    }

    override fun getMangaKey(sourceId: String, manga: MangaData): String {
        return "${sourceId}|${manga.id}"
    }

    override fun getMangaKey(sourceId: String, manga: String): String {
        return "${sourceId}|${manga}"
    }

    override fun has(sourceId: String, manga: ApiMangaPreview): Boolean {
          return realm.query(StoredManga::class, query = "uniqueId == \$0",getMangaKey(sourceId,manga)).find().firstOrNull() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun has(sourceId: String, manga: MangaData): Boolean {
        return realm.query(StoredManga::class, query = "uniqueId == \$0",getMangaKey(sourceId,manga)).find().firstOrNull() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun has(sourceId: String, manga: String): Boolean {
        return realm.query(StoredManga::class, query = "uniqueId == \$0",getMangaKey(sourceId,manga)).find().firstOrNull() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun hasReadInfo(sourceId: String, manga: ApiMangaPreview): Boolean {
        return realm.query(StoredChaptersRead::class, query = "uniqueId == \$0",getMangaKey(sourceId,manga)).find().firstOrNull() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun hasReadInfo(sourceId: String, manga: MangaData): Boolean {
        return realm.query(StoredChaptersRead::class, query = "uniqueId == \$0",getMangaKey(sourceId,manga)).find().firstOrNull() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun hasReadInfo(sourceId: String, manga: String): Boolean {
        return realm.query(StoredChaptersRead::class, query = "uniqueId == \$0",getMangaKey(sourceId,manga)).find().firstOrNull() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun getManga(mangaKey: String): RealmResults<StoredManga> {
        return realm.query(StoredManga::class, query = "uniqueId == \$0",mangaKey).find()
    }

    override fun getChaptersRead(mangaKey: String): RealmResults<StoredChaptersRead> {
        return realm.query(StoredChaptersRead::class, query = "uniqueId == \$0",mangaKey).find()
    }
    override suspend fun bookmark(
        sourceId: String,
        mangaData: MangaData,
        mangaChapters: List<MangaChapter>
    ) {
        if (has(sourceId, mangaData.id)) {
            Napier.w("Ignoring ${mangaData.id}, It is already bookmarked", tag = "realm")
            return
        }

        realm.write {
            val uniqueId = getMangaKey(sourceId, mangaData.id)
            val newItem = StoredManga(
                uniqueId = uniqueId,
                source = sourceId,
                id = mangaData.id,
                name = mangaData.name,
                cover = mangaData.cover,
                addedAt = Clock.System.now().epochSeconds
            ).apply {


                status = mangaData.status
                description = mangaData.description
                tags.addAll(mangaData.tags)
                extras.addAll(mangaData.extras.map {
                    StoredMangaExtras(
                        name = it.name,
                        value = it.value
                    )
                })
                chapters.addAll(mangaChapters.map { chapter ->
                    StoredChapter(
                        id = chapter.id,
                        name = chapter.name, released = chapter.released
                    )
                })
                share = mangaData.share
            }

            copyToRealm(newItem)
        }
    }

    override suspend fun bookmark(sourceId: String, mangaPreview: MangaPreview) {
        if (has(sourceId, mangaPreview.id)) {
            Napier.w("Ignoring ${mangaPreview.id}, It is already bookmarked", tag = "realm")
            return
        }
        realm.write {
            val newItem = StoredManga(
                uniqueId = getMangaKey(sourceId, mangaPreview.id),
                source = sourceId,
                id = mangaPreview.id,
                name = mangaPreview.name,
                cover = mangaPreview.cover,
                addedAt = Clock.System.now().epochSeconds
            )

            copyToRealm(newItem)
        }
    }

    override suspend fun updateChapters(
        sourceId: String,
        mangaId: String,
        mangaChapters: List<MangaChapter>
    ) {

        if (!has(sourceId, mangaId)) {
            Napier.w("Cant update chapters, manga not bookmarked $sourceId $mangaId", tag = "realm")
            return
        }

        val uniqueId = getMangaKey(sourceId, mangaId)

        realm.write {

            query<StoredManga>("uniqueId == $0", uniqueId).find().firstOrNull()?.let {
                it.chapters = realmListOf()
                it.chapters.addAll(mangaChapters.map { chapter ->
                    StoredChapter(
                        id = chapter.id,
                        name = chapter.name, released = chapter.released
                    )
                })
            }
        }
    }

    override suspend fun updateManga(
        sourceId: String,
        mangaId: String,
        update: (data: StoredManga) -> Unit
    ) {

        if (!has(sourceId, mangaId)) {
            Napier.w("Cant update chapters, manga not bookmarked $sourceId $mangaId", tag = "realm")
            return
        }

        val uniqueId = getMangaKey(sourceId, mangaId)

        realm.write {
            val result = query<StoredManga>("uniqueId == $0", uniqueId).find().firstOrNull()
            if (result != null) {
                update(result)
            }
        }
    }

    override suspend fun removeBookmark(sourceId: String, mangaId: String) {
        val uniqueId = getMangaKey(sourceId, mangaId)
        realm.write {
            val results = query<StoredManga>("uniqueId == $0", uniqueId).find()
            if (results.size > 0) {
                delete(results.first())
            } else {
                Napier.w(
                    "Cant remove $mangaId, It is not in the library , key: $uniqueId",
                    tag = "realm"
                )
            }
        }
    }

//    override fun subscribeOnBookmarksUpdated(callback: () -> Unit): () -> Unit {
//        bookmarksUpdatedCallbacks.add(callback)
//        return {
//            bookmarksUpdatedCallbacks.remove(callback)
//        }
//    }

    override suspend fun updateMangaReadInfo(
        sourceId: String,
        mangaId: String,
        chapterIndex: Int,
        progress: Int,
        total: Int
    ) {
        realm.write {
            val uniqueId = getMangaKey(sourceId, mangaId)
            query<StoredChaptersRead>("uniqueId == $0", uniqueId).find().firstOrNull()?.let {
                when(val readInfo = it.current){
                    is StoredChapterReadInfo -> {
                        readInfo.index = chapterIndex
                        readInfo.progress = progress
                        readInfo.total = total
                    }
                    else -> {
                        it.current = StoredChapterReadInfo(
                            index = chapterIndex,
                            progress = progress,
                            total = total
                        )
                    }
                }
            } ?: run{
                copyToRealm(StoredChaptersRead(uniqueId,mangaId,sourceId).apply {
                    current = StoredChapterReadInfo(
                        index = chapterIndex,
                        progress = progress,
                        total = total
                    )
                })
            }
        }
    }

    override suspend fun markChapterAsRead(sourceId: String, mangaId: String, chapterIndex: Int) {

        val uniqueId = getMangaKey(sourceId, mangaId)

        realm.write {
            query<StoredChaptersRead>("uniqueId == $0", uniqueId).find().firstOrNull()?.let {
                if (it.read.add(chapterIndex)) {
                    Napier.i("Marked chapter $chapterIndex in manga $uniqueId as read", tag = "realm")
                }
            } ?: run {
                copyToRealm(StoredChaptersRead(uniqueId,mangaId,sourceId).apply {
                    read.add(chapterIndex)
                })
            }
        }
    }

    override suspend fun createCategory(name: String) {
        realm.write {
            copyToRealm(StoredMangaCategory(name,-1))
        }
    }

}


@Composable
fun <T> subscribeBookmarksUpdate(
    key: Any,
    getData: (realmRepository: RealmRepository) -> T,
    realmRepository: RealmRepository = koinInject()
): MutableState<T> {
    val data: MutableState<T> = remember { mutableStateOf(getData(realmRepository)) }

//    DisposableEffect(key) {
//        val unsubscribe = realmRepository.subscribeOnBookmarksUpdated {
//            data.value = getData(realmRepository)
//        }
//
//        onDispose {
//            unsubscribe()
//        }
//    }

    return data
}

@Composable
fun rememberReadInfo(sourceId: String,mangaId: String,realmRepository: RealmRepository = koinInject()): StoredChaptersRead? {

    val data = remember(sourceId,mangaId) { mutableStateOf<StoredChaptersRead?>(null) }

    LaunchedEffect(sourceId,mangaId){
        realmRepository.getChaptersRead(realmRepository.getMangaKey(sourceId,mangaId)).asFlow().collect{changes ->
            when (changes) {
                is InitialResults<StoredChaptersRead> -> {
                    data.value = changes.list.firstOrNull()

                    Napier.i(
                        "Updated Read Info For Manga",
                        tag = "realm"
                    )
                }

                is UpdatedResults<StoredChaptersRead> -> {
                    if(changes.deletions.isNotEmpty()){
                        data.value = null
                        Napier.i("Deleting Read Info For Manga", tag = "realm")
                    }

                    if(changes.insertions.isNotEmpty() ||changes.changes.isNotEmpty()){
                        data.value = changes.list.firstOrNull()
                        Napier.i("Updating Read Info For Manga", tag = "realm")
                    }
                }

                else -> {}
            }
        }
    }

    return data.value
}

@Composable
fun rememberMangaInfo(sourceId: String,mangaId: String,realmRepository: RealmRepository = koinInject()): StoredManga? {

    val data = remember(sourceId,mangaId) { mutableStateOf<StoredManga?>(null) }

    LaunchedEffect(sourceId,mangaId){
        realmRepository.getManga(realmRepository.getMangaKey(sourceId,mangaId)).asFlow().collect{changes ->
            when (changes) {
                is InitialResults<StoredManga> -> {
                    data.value = changes.list.firstOrNull()

                    Napier.i(
                        "Updated Info For Manga",
                        tag = "realm"
                    )
                }

                is UpdatedResults<StoredManga> -> {
                    if(changes.deletions.isNotEmpty()){
                        data.value = null
                        Napier.i("Deleting Info For Manga", tag = "realm")
                    }

                    if(changes.insertions.isNotEmpty() ||changes.changes.isNotEmpty()){
                        data.value = changes.list.firstOrNull()
                        Napier.i("Updating Info For Manga", tag = "realm")
                    }
                }

                else -> {}
            }
        }
    }

    return data.value
}

@Composable
fun rememberIsBookmarked(sourceId: String,mangaId: String,realmRepository: RealmRepository = koinInject()): Boolean {

    val data = remember(sourceId,mangaId) { mutableStateOf(realmRepository.has(sourceId,mangaId)) }

    LaunchedEffect(sourceId,mangaId){
        realmRepository.getManga(realmRepository.getMangaKey(sourceId,mangaId)).asFlow().collect{changes ->
            when (changes) {
                is InitialResults<StoredManga> -> {
                    data.value = changes.list.firstOrNull() != null
                }

                is UpdatedResults<StoredManga> -> {
                    if(changes.deletions.isNotEmpty()){
                        data.value = false
                    }

                    if(changes.insertions.isNotEmpty() ||changes.changes.isNotEmpty()){
                        data.value = true
                    }
                }

                else -> {}
            }
        }
    }

    return data.value
}