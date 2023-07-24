package com.tarehimself.mira.data


import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.arkivanov.essenty.parcelable.IgnoredOnParcel
import com.arkivanov.essenty.parcelable.Parcelize
import com.tarehimself.mira.common.Cache
import com.tarehimself.mira.common.debug
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.notifications.InitialResults
import io.realm.kotlin.notifications.UpdatedResults
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.mongodb.kbson.ObjectId

@Parcelize
class StoredChapter : RealmObject, MangaChapter {
    @IgnoredOnParcel
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    @Index
    override var id: String = ""

    override var name: String = ""

    override var key: Float = 0.0f

    override var released: String? = ""
}

class StoredChapterReadInfo : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    var key: Float = 0.0f
    var progress: Int = 0
    var total: Int = 0
}

class StoredMangaExtras : RealmObject, MangaExtras{
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    override var name: String = ""
    override var value: String = ""
}

class StoredSources: RealmObject{
    @PrimaryKey
    var id: String = ""
    var name: String = ""
}

class StoredManga: RealmObject, MangaPreview, MangaData{


    @Index
    override var id: String = ""
    @Index
    var source: String = ""
    @Index
    override var name: String = ""
    override var cover: String = ""
    @Index
    override var status: String = ""
    override var description: String = ""

    override var tags: RealmList<String> = realmListOf()
    override var extras: RealmList<StoredMangaExtras> = realmListOf()
    var chapters: RealmList<StoredChapter> = realmListOf()
    var readInfo: RealmList<StoredChapterReadInfo> = realmListOf()
    @Index
    var addedAt: Long = 0

    @PrimaryKey
    var uniqueId: String = ""

}


interface RealmRepository : KoinComponent  {

    val api: MangaApi

    val libraryCache: Cache<String,StoredManga>

    val libraryUpdatedCallbacks: ArrayList<() -> Unit>

    val realm: Realm

    var activeScope: CoroutineScope ?
    fun stopActiveManaging()
    fun manageData()

    fun getLibrary(): Cache<String,StoredManga>

    fun getMangaKey(manga: StoredManga): String

    fun getMangaKey(sourceId:String,manga: ApiMangaPreview): String

    fun getMangaKey(sourceId:String,manga: MangaData): String

    fun getMangaKey(sourceId:String,manga: String): String

    fun has(sourceId:String,manga: ApiMangaPreview): Boolean

    fun has(sourceId:String,manga: MangaData): Boolean

    fun has(sourceId:String,manga: String): Boolean

    fun addToLibrary(sourceId: String, mangaData: MangaData, mangaChapters: List<MangaChapter>)

    fun addToLibrary(sourceId: String, mangaPreview: MangaPreview)

    fun updateChapters(sourceId: String,mangaId: String,mangaChapters: List<MangaChapter>)

    fun removeFromLibrary(sourceId: String,mangaId: String)

    fun subscribeOnLibraryUpdated(callback: () -> Unit): () -> Unit

}

class DefaultRealmRepository :  RealmRepository {

    override val api: MangaApi by inject<MangaApi>()

    override val libraryCache = Cache<String,StoredManga>()

    override val libraryUpdatedCallbacks: ArrayList<() -> Unit> = ArrayList()

    override val realm: Realm = Realm.open(RealmConfiguration.create(schema = setOf(StoredManga::class,StoredSources::class,StoredMangaExtras::class,StoredChapter::class,StoredChapterReadInfo::class)))

    override var activeScope: CoroutineScope ? = null

    override fun stopActiveManaging(){
        activeScope?.cancel()
    }
    override fun manageData(){
//        realm.writeBlocking {
//            val results = query<StoredManga>().find()
//            if(results.size > 0){
//                delete(results)
//            }
//
//        }
        stopActiveManaging()
        activeScope = CoroutineScope(Dispatchers.Main)
        activeScope?.launch {
            var lastData = listOf<StoredManga>()
            realm.query(StoredManga::class).asFlow().collect{changes ->
                when(changes){
                    is InitialResults<StoredManga> -> {
                        for(x in changes.list){
                            debug("Adding ${x.uniqueId} to the library")
                            libraryCache[x.uniqueId] = x
                        }
                        lastData = changes.list

                        for(callback in libraryUpdatedCallbacks){
                            callback()
                        }
                        debug("Library Updated")
                    }
                    is UpdatedResults<StoredManga> -> {
                        for(deletedIdx in changes.deletions){
                            val deleted = lastData[deletedIdx]
                            debug("Removing ${deleted.uniqueId} to the library")
                            libraryCache.remove(deleted.uniqueId)
                        }

                        for(insertedIdx in changes.insertions){
                            val inserted = changes.list[insertedIdx]
                            debug("Adding ${inserted.uniqueId} to the library")
                            libraryCache[inserted.uniqueId] = inserted

                        }

                        lastData = changes.list

                        debug("Library Updated")

                        for(callback in libraryUpdatedCallbacks){
                            callback()
                        }

                    }
                    else -> {}
                }
            }
        }
        debug("Managing data ${libraryCache.values.size}")
    }



    override fun getLibrary(): Cache<String,StoredManga> {
        return libraryCache
    }


    override fun getMangaKey(manga: StoredManga): String {
        return "${manga.source}|${manga.id}"
    }

    override fun getMangaKey(sourceId:String,manga: ApiMangaPreview): String {
        return "${sourceId}|${manga.id}"
    }

    override fun getMangaKey(sourceId:String,manga: MangaData): String {
        return "${sourceId}|${manga.id}"
    }

    override fun getMangaKey(sourceId:String,manga: String): String {
        return "${sourceId}|${manga}"
    }

    override fun has(sourceId:String,manga: ApiMangaPreview): Boolean {
        return libraryCache.contains(getMangaKey(sourceId,manga))
    }

    override fun has(sourceId:String,manga: MangaData): Boolean {
        return libraryCache.contains(getMangaKey(sourceId,manga))
    }

    override fun has(sourceId:String,manga: String): Boolean {
        return libraryCache.contains(getMangaKey(sourceId,manga))
    }

    override fun addToLibrary(sourceId: String, mangaData: MangaData, mangaChapters: List<MangaChapter>) {
        if(has(sourceId,mangaData.id)){
            debug("Ignoring ${mangaData.id}, It is already in the library")
            return
        }

        realm.writeBlocking {
            val newItem = StoredManga().apply {
                uniqueId = getMangaKey(sourceId,mangaData.id)
                source = sourceId
                id = mangaData.id
                name = mangaData.name
                cover = mangaData.cover
                status = mangaData.status
                description = mangaData.description
                tags.addAll(mangaData.tags)
                extras.addAll(mangaData.extras.map {
                    StoredMangaExtras().apply {
                        name = it.name
                        value = it.value
                    }
                })
                chapters.addAll(mangaChapters.map {
                    StoredChapter().apply {
                        id = it.id
                        name = it.name

                    }
                })
                addedAt = Clock.System.now().epochSeconds
            }

            copyToRealm(newItem)
            debug("Added ${mangaData.name} To Library")
        }
    }

    override fun addToLibrary(sourceId: String, mangaPreview: MangaPreview) {
        if(has(sourceId,mangaPreview.id)){
            debug("Ignoring ${mangaPreview.id}, It is already in the library")
            return
        }
        realm.writeBlocking {
            val newItem = StoredManga().apply {
                uniqueId = getMangaKey(sourceId,mangaPreview.id)
                source = sourceId
                id = mangaPreview.id
                name = mangaPreview.name
                cover = mangaPreview.cover
                addedAt = Clock.System.now().epochSeconds
            }

            copyToRealm(newItem)
            debug("Added ${mangaPreview.name} To Library")
        }
    }
    override fun updateChapters(sourceId: String,mangaId: String,mangaChapters: List<MangaChapter>){

        if(!has(sourceId,mangaId)){
            return
        }

        val uniqueId = getMangaKey(sourceId,mangaId)

        realm.writeBlocking {
            val result = query<StoredManga>("id == $0",uniqueId).first().find()
            if (result != null) {
                result.chapters = realmListOf()
                result.chapters.addAll(mangaChapters.map {
                    StoredChapter().apply {
                        id = it.id
                        name = it.name

                    }
                })
            }


        }
    }

    override fun removeFromLibrary(sourceId: String,mangaId: String){
        val uniqueId = getMangaKey(sourceId,mangaId)
        realm.writeBlocking {
            val results = query<StoredManga>("uniqueId == $0",uniqueId).find()
            if(results.size > 0){
                delete(results.first())
            }
            else
            {
                debug("Cant remove $mangaId, It is not in the library , key: $uniqueId")
            }

        }
    }

    override fun subscribeOnLibraryUpdated(callback: () -> Unit): () -> Unit{
        libraryUpdatedCallbacks.add(callback)
        return {
            libraryUpdatedCallbacks.remove(callback)
        }
    }

}

@Composable
fun <T>subscribeLibraryUpdate(getData: (realmRepository: RealmRepository) -> T,realmRepository: RealmRepository = koinInject()): T {
    val data: MutableState<T> = remember { mutableStateOf(getData(realmRepository)) }

    DisposableEffect(Unit) {
        val unsubscribe = realmRepository.subscribeOnLibraryUpdated{
            data.value = getData(realmRepository)
        }

        onDispose {
            unsubscribe()
        }
    }

    return data.value
}