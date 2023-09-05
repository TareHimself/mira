package com.tarehimself.mira.data


import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.benasher44.uuid.uuid4
import io.github.aakira.napier.Napier
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.ext.realmSetOf
import io.realm.kotlin.ext.toRealmList
import io.realm.kotlin.query.RealmQuery
import io.realm.kotlin.query.RealmSingleQuery
import io.realm.kotlin.query.Sort
import io.realm.kotlin.types.EmbeddedRealmObject
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.RealmSet
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class StoredChapter() : EmbeddedRealmObject, MangaChapter {


    constructor(id: String, name: String, released: String?) : this() {
        this.id = id
        this.name = name
        this.released = released
    }

    @Index
    override var id: String = ""

    @Index
    override var name: String = ""

    override var released: String? = null
}

class StoredMangaExtras() : EmbeddedRealmObject, MangaExtras {


    constructor(name: String, value: String) : this() {
        this.name = name
        this.value = value
    }

    override var name: String = ""
    override var value: String = ""
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

class StoredChapterReadInfo() : EmbeddedRealmObject {

    constructor(index: Int, progress: Int, total: Int) : this() {
        this.index = index
        this.progress = progress
        this.total = total
    }

    @Index
    var index: Int = 0
    var progress: Int = 0
    var total: Int = 0
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

class StoredMangaHeader() : EmbeddedRealmObject, MangaHeader {
    override var value: String = ""
    override var key: String = ""

    constructor(key: String, value: String) : this() {
        this.key = key
        this.value = value
    }
}

class StoredMangaImage() : EmbeddedRealmObject, MangaImage {
    override var src: String = ""
    override var headers: RealmList<StoredMangaHeader> = realmListOf()

    constructor(data: MangaImage) : this() {
        src = data.src
        headers.addAll(data.headers.map {
            StoredMangaHeader(key = it.key, value = it.value)
        })
    }
}

class StoredManga() : RealmObject, MangaPreview, MangaData {


    constructor(
        uniqueId: String,
        id: String,
        source: String,
        name: String,
        cover: StoredMangaImage,
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

    override var cover: StoredMangaImage? = StoredMangaImage()

    @Index
    var addedAt: Long = 0


    @Index
    override var status: String = ""
    override var description: String = ""

    override var tags: RealmList<String> = realmListOf()
    override var extras: RealmList<StoredMangaExtras> = realmListOf()

    var chapters: RealmList<StoredChapter> = realmListOf()

    var categories: RealmSet<String> = realmSetOf()

    fun updateFromApiData(data: MangaData) {
        name = data.name
        description = data.description
        cover = StoredMangaImage(data.cover!!)
        status = data.status
        extras = data.extras.map {
            StoredMangaExtras().apply {
                name = it.name
                value = it.value
            }
        }.toRealmList()
        tags = data.tags.toRealmList()
        share = data.share
    }
}


interface RealmRepository : KoinComponent {

    val api: MangaApi

    val realm: Realm

    val isUpdatingBookmarks: MutableState<Boolean>

    fun getBookmarkKey(manga: StoredManga): String

    fun getBookmarkKey(sourceId: String, manga: ApiMangaPreview): String

    fun getBookmarkKey(sourceId: String, manga: MangaData): String

    fun getBookmarkKey(sourceId: String, manga: String): String

    fun has(sourceId: String, manga: ApiMangaPreview): Boolean

    fun has(sourceId: String, manga: MangaData): Boolean

    fun has(sourceId: String, manga: String): Boolean

    fun getBookmarks(): RealmQuery<StoredManga>

    fun getBookmark(mangaKey: String): RealmSingleQuery<StoredManga>

    fun getChaptersRead(mangaKey: String): RealmSingleQuery<StoredChaptersRead>
    fun hasReadInfo(sourceId: String, manga: ApiMangaPreview): Boolean

    fun hasReadInfo(sourceId: String, manga: MangaData): Boolean

    fun hasReadInfo(sourceId: String, manga: String): Boolean

    suspend fun bookmark(sourceId: String, mangaData: MangaData, mangaChapters: List<MangaChapter>)

    suspend fun bookmark(sourceId: String, mangaPreview: MangaPreview)

    suspend fun updateChapters(sourceId: String, mangaId: String, mangaChapters: List<MangaChapter>)

    suspend fun removeBookmark(sourceId: String, mangaId: String)

    suspend fun removeBookmarks(uniqueIds: List<String>)

//    fun subscribeOnBookmarksUpdated(callback: () -> Unit): () -> Unit

    suspend fun markChapterAsRead(sourceId: String, mangaId: String, chapterIndex: Int)

    suspend fun updateBookmarkReadInfo(
        sourceId: String,
        mangaId: String,
        chapterIndex: Int,
        progress: Int,
        total: Int
    )

    suspend fun updateBookmark(
        sourceId: String,
        mangaId: String,
        update: (data: StoredManga) -> Unit
    )

    suspend fun createCategory(name: String)

    suspend fun deleteCategory(id: String)
    suspend fun updateCategoryPosition(id: String, position: Int)

    suspend fun updateCategoryName(id: String, name: String)
    fun getCategories(): RealmQuery<StoredMangaCategory>

    suspend fun updateBookmarksFromApi(ids: List<String> = listOf())

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
                StoredMangaImage::class,
                StoredMangaHeader::class
            )
        ).deleteRealmIfMigrationNeeded().build()
    )

    override val isUpdatingBookmarks: MutableState<Boolean> = mutableStateOf(false)


    override fun getBookmarkKey(manga: StoredManga): String {
        return "${manga.sourceId}|${manga.id}"
    }

    override fun getBookmarkKey(sourceId: String, manga: ApiMangaPreview): String {
        return "${sourceId}|${manga.id}"
    }

    override fun getBookmarkKey(sourceId: String, manga: MangaData): String {
        return "${sourceId}|${manga.id}"
    }

    override fun getBookmarkKey(sourceId: String, manga: String): String {
        return "${sourceId}|${manga}"
    }

    override fun has(sourceId: String, manga: ApiMangaPreview): Boolean {
        return realm.query(
            StoredManga::class,
            query = "uniqueId == \$0",
            getBookmarkKey(sourceId, manga)
        ).first().find() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun has(sourceId: String, manga: MangaData): Boolean {
        return realm.query(
            StoredManga::class,
            query = "uniqueId == \$0",
            getBookmarkKey(sourceId, manga)
        ).first().find() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun has(sourceId: String, manga: String): Boolean {
        return realm.query(
            StoredManga::class,
            query = "uniqueId == \$0",
            getBookmarkKey(sourceId, manga)
        ).first().find() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun hasReadInfo(sourceId: String, manga: ApiMangaPreview): Boolean {
        return realm.query(
            StoredChaptersRead::class,
            query = "uniqueId == \$0",
            getBookmarkKey(sourceId, manga)
        ).first().find() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun hasReadInfo(sourceId: String, manga: MangaData): Boolean {
        return realm.query(
            StoredChaptersRead::class,
            query = "uniqueId == \$0",
            getBookmarkKey(sourceId, manga)
        ).first().find() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun hasReadInfo(sourceId: String, manga: String): Boolean {
        return realm.query(
            StoredChaptersRead::class,
            query = "uniqueId == \$0",
            getBookmarkKey(sourceId, manga)
        ).first().find() != null
//        return bookmarks.contains(getMangaKey(sourceId, manga))
    }

    override fun getBookmarks(): RealmQuery<StoredManga> {
        return realm.query(StoredManga::class)
    }

    override fun getBookmark(mangaKey: String): RealmSingleQuery<StoredManga> {
        return realm.query(StoredManga::class, query = "uniqueId == \$0", mangaKey).first()
    }

    override fun getChaptersRead(mangaKey: String): RealmSingleQuery<StoredChaptersRead> {
        return realm.query(StoredChaptersRead::class, query = "uniqueId == \$0", mangaKey).first()
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
            val uniqueId = getBookmarkKey(sourceId, mangaData.id)
            val newItem = StoredManga(
                uniqueId = uniqueId,
                source = sourceId,
                id = mangaData.id,
                name = mangaData.name,
                cover = StoredMangaImage(mangaData.cover!!),
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
                uniqueId = getBookmarkKey(sourceId, mangaPreview.id),
                source = sourceId,
                id = mangaPreview.id,
                name = mangaPreview.name,
                cover = StoredMangaImage(mangaPreview.cover!!),
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

        val uniqueId = getBookmarkKey(sourceId, mangaId)

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

    override suspend fun updateBookmark(
        sourceId: String,
        mangaId: String,
        update: (data: StoredManga) -> Unit
    ) {

        if (!has(sourceId, mangaId)) {
            Napier.w("Cant update chapters, manga not bookmarked $sourceId $mangaId", tag = "realm")
            return
        }

        val uniqueId = getBookmarkKey(sourceId, mangaId)

        realm.write {
            val result = query<StoredManga>("uniqueId == $0", uniqueId).find().firstOrNull()?.let(update)
        }
    }

    override suspend fun removeBookmark(sourceId: String, mangaId: String) {
        val uniqueId = getBookmarkKey(sourceId, mangaId)
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

    override suspend fun removeBookmarks(uniqueIds: List<String>) {

        realm.write {
            query<StoredManga>().find().filter {  uniqueIds.contains(it.uniqueId)  }.forEach {
                delete(it)
            }
        }
    }

//    override fun subscribeOnBookmarksUpdated(callback: () -> Unit): () -> Unit {
//        bookmarksUpdatedCallbacks.add(callback)
//        return {
//            bookmarksUpdatedCallbacks.remove(callback)
//        }
//    }

    override suspend fun updateBookmarkReadInfo(
        sourceId: String,
        mangaId: String,
        chapterIndex: Int,
        progress: Int,
        total: Int
    ) {
        realm.write {
            val uniqueId = getBookmarkKey(sourceId, mangaId)
            Napier.d { "Progress $progress Total $total" }
            query<StoredChaptersRead>("uniqueId == $0", uniqueId).find().firstOrNull()?.let {
                when (val readInfo = it.current) {
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
            } ?: run {
                copyToRealm(StoredChaptersRead(uniqueId, mangaId, sourceId).apply {
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

        val uniqueId = getBookmarkKey(sourceId, mangaId)

        realm.write {
            query<StoredChaptersRead>("uniqueId == $0", uniqueId).find().firstOrNull()?.let {
                if (it.read.add(chapterIndex)) {
                    Napier.i(
                        "Marked chapter $chapterIndex in manga $uniqueId as read",
                        tag = "realm"
                    )
                }

                if(it.current?.index == chapterIndex){
                    it.current = null
                }
            } ?: run {
                copyToRealm(StoredChaptersRead(uniqueId, mangaId, sourceId).apply {
                    read.add(chapterIndex)
                })
            }
        }
    }

    override suspend fun createCategory(name: String) {
        realm.write {
            query(StoredMangaCategory::class).sort("position", Sort.DESCENDING).find()
                .firstOrNull()?.let {
                    copyToRealm(StoredMangaCategory(name, it.position + 1))
                } ?: run {
                copyToRealm(StoredMangaCategory(name, 0))
            }
        }
    }

    override suspend fun deleteCategory(id: String) {
        realm.write {
            query(StoredMangaCategory::class, "id = $0", id).find().firstOrNull()?.let {
                query(StoredManga::class, "$0 IN categories", id).find()
                    .forEach { manga -> manga.categories.remove(id) }
                delete(it)
                query(StoredMangaCategory::class).sort("position", Sort.ASCENDING).find()
                    .forEachIndexed { idx, cat -> cat.position = idx }
            }
        }
    }

    override suspend fun updateCategoryPosition(id: String, position: Int) {
        Napier.d { "Updating Position of $id To $position" }
        if (position < 0) {
            return
        }

        realm.write {
            val results = query(StoredMangaCategory::class).sort("position", Sort.ASCENDING).find()
            if (results.isEmpty()) {
                return@write
            } else {
                results.find { it.id == id }?.let { item ->
                    val isMovingUp = position < item.position
                    val beforeItem = results.filter {
                        (if (isMovingUp) {
                            it.position < position
                        } else {
                            it.position <= position
                        }) &&
                                it.id != item.id
                    }
                    val afterItem = results.filter {
                        (if (isMovingUp) {
                            it.position >= position
                        } else {
                            it.position > position
                        }) && it.id != item.id
                    }
                    val newItems = arrayListOf<StoredMangaCategory>()
                    newItems.addAll(beforeItem)
                    newItems.add(item)
                    newItems.addAll(afterItem)
                    Napier.d { "SORTING CATEGORIES ${beforeItem.map { it.name }}  ${results.map { it.name }}  ${results.map { it.position }}" }
                    newItems.forEachIndexed { idx, it -> it.position = idx }
                }
            }
        }
    }

    override suspend fun updateCategoryName(id: String, name: String) {
        realm.write {
            query(StoredMangaCategory::class, "id = $0", id).find().firstOrNull()?.let { item ->
                item.name = name
            }
        }
    }

    override fun getCategories(): RealmQuery<StoredMangaCategory> {
        return realm.query(StoredMangaCategory::class).sort("position", Sort.ASCENDING)
    }

    override suspend fun updateBookmarksFromApi(ids: List<String>) {
        when (ids.isEmpty()) {
            true -> getBookmarks().asFlow().first().list
            else -> getBookmarks().asFlow().first().list.filter { ids.contains(it.uniqueId) }
                .sortedBy { ids.indexOf(it.uniqueId) }
        }.forEach {
            api.getChapters(it.sourceId, it.id).data?.let { chapters ->
                updateChapters(it.sourceId, it.id, chapters)
                Napier.d { "Updated ${it.name}, ${chapters.size} Chapters" }
            }
        }
    }
}


@Composable
fun rememberReadInfo(
    sourceId: String,
    mangaId: String,
    lazy: Boolean = true,
    realmRepository: RealmRepository = koinInject()
): MutableState<StoredChaptersRead?> {

    val data = remember(sourceId, mangaId) {
        mutableStateOf(
            when (lazy) {
                true -> null
                else -> {
                    realmRepository.getChaptersRead(
                        realmRepository.getBookmarkKey(
                            sourceId,
                            mangaId
                        )
                    ).find()
                }
            }
        )
    }

    LaunchedEffect(sourceId, mangaId) {
        realmRepository.getChaptersRead(realmRepository.getBookmarkKey(sourceId, mangaId)).asFlow()
            .collect { changes ->
                data.value = changes.obj
            }
    }

    return data
}

@Composable
fun rememberBookmarkInfo(
    sourceId: String,
    mangaId: String,
    lazy:Boolean = true,
    realmRepository: RealmRepository = koinInject()
): StoredManga? {

    val data = remember(sourceId, mangaId) { mutableStateOf(when(lazy){
        true -> null
        else -> realmRepository.getBookmark(realmRepository.getBookmarkKey(sourceId, mangaId)).find()
    }) }

    LaunchedEffect(sourceId, mangaId) {

        realmRepository.getBookmark(realmRepository.getBookmarkKey(sourceId, mangaId)).asFlow()
            .collect { changes ->
                data.value = changes.obj
            }
    }

    return data.value
}

@Composable
fun rememberBookmarks(
    realmRepository: RealmRepository = koinInject()
): MutableState<List<StoredManga>> {

    val data = remember { mutableStateOf<List<StoredManga>>(listOf()) }

    LaunchedEffect(Unit) {
//        realmRepository.realm.write {
//            query(StoredMangaCategory::class,"name = $0","").first().find()?.let {
//                Napier.d { "Found ${it.name} ${it.id}" }
//                        val data = query(StoredManga::class,"$0 IN categories",it.id).find()
//
//                Napier.d { "Deleting ${data.size} manga" }
//                delete(data)
//            }
//        }
        realmRepository.realm.query(StoredManga::class)
            .sort("addedAt", Sort.DESCENDING).asFlow()
            .collect { changes ->
                data.value = changes.list
            }
    }

    return data
}

@Composable
fun rememberIsBookmarked(
    sourceId: String,
    mangaId: String,
    lazy: Boolean = true,
    realmRepository: RealmRepository = koinInject()
): MutableState<Boolean> {

    val data =
        remember(sourceId, mangaId) {
            mutableStateOf(
                when (lazy) {
                    true -> false
                    else -> realmRepository.has(sourceId, mangaId)
                }
            )
        }


    LaunchedEffect(sourceId, mangaId) {
        realmRepository.getBookmark(realmRepository.getBookmarkKey(sourceId, mangaId)).asFlow()
            .collect { changes ->
                data.value = changes.obj != null
            }
    }

    return data
}

@Composable
fun rememberCategories(
    lazy: Boolean = true,
    realmRepository: RealmRepository = koinInject()
): MutableState<List<StoredMangaCategory>> {

    val data = remember {
        mutableStateOf(
            when (lazy) {
                true -> listOf()
                else -> realmRepository.getCategories().find().toList()
            }
        )
    }

    LaunchedEffect(Unit) {
        realmRepository.getCategories().asFlow()
            .collect { changes ->
                data.value = changes.list
            }
    }

    return data
}



@Composable
fun rememberBookmarkCategories(
    sourceId: String,
    mangaId: String,
    lazy: Boolean = true,
    realmRepository: RealmRepository = koinInject()
): MutableState<List<Pair<StoredMangaCategory, Boolean>>> {

    val mangaData = rememberBookmarkInfo(sourceId, mangaId,lazy)

    val data = remember(sourceId,mangaId,mangaData) {
        mutableStateOf(when(mangaData == null){
            true -> listOf()
            else -> when (lazy) {
                true -> listOf()
                else -> {
                    realmRepository.getCategories().find().map {
                        Pair(it, mangaData.categories.contains(it.id))
                    }
                }
            }
        })
    }

    LaunchedEffect(sourceId,mangaId,mangaData) {
        mangaData?.let {
            realmRepository.getCategories().asFlow()
                .collect { changes ->
                    data.value = changes.list.map {
                        Pair(it, mangaData.categories.contains(it.id))
                    }
                }
        }
    }

    return data
}