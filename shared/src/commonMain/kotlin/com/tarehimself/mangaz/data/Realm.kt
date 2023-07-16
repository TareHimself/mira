package com.tarehimself.mangaz.data


import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import io.realm.kotlin.ext.realmListOf
import io.realm.kotlin.query.RealmResults
import io.realm.kotlin.types.RealmList
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.Index
import io.realm.kotlin.types.annotations.PrimaryKey
import org.mongodb.kbson.ObjectId
import kotlin.jvm.Volatile


class StoredChapter : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    @Index
    var id: String = ""
    var name: String = ""
    var released: String? = ""
}

class StoredChapterReadInfo : RealmObject {
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    @Index
    var id: String = ""
    var progress: Int = 0
    var total: Int = 0
}

class StoredMangaExtras : RealmObject{
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    var name: String = ""
    var value: String = ""
}

class StoredSources: RealmObject{
    @PrimaryKey
    var id: String = ""
    var name: String = ""
}

class StoredManga: RealmObject{
    @PrimaryKey
    var _id: ObjectId = ObjectId()

    @Index
    var id: String = ""
    @Index
    var source: String = ""
    @Index
    var name: String = ""
    var cover: String = ""
    var status: String = ""
    var description: String = ""
    var tags: RealmList<String> = realmListOf()
    var extras: RealmList<StoredMangaExtras> = realmListOf()
    var chapters: RealmList<StoredChapter> = realmListOf()
    var readInfo: RealmList<StoredChapterReadInfo> = realmListOf()
    var addedAt: Int = 0
}


class RealmDatabase private constructor() {
    companion object {
        @Volatile
        private var instance: RealmDatabase? = null

        fun get() = instance ?: RealmDatabase().also { instance = it }
    }

    private val realm: Realm =
        Realm.open(RealmConfiguration.create(schema = setOf(StoredManga::class,StoredSources::class)))

    private fun getLibrary(): RealmResults<StoredManga> {
        return realm.query<StoredManga>().find()
    }
}