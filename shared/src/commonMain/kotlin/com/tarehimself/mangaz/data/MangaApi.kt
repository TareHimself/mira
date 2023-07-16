package com.tarehimself.mangaz.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.jvm.Volatile

@Serializable
abstract class MangaApiResponse<T>{
    abstract val data: T ?
    abstract val error: String?
}

@Serializable
data class SearchItems<T>(
    val items: List<T>,
    val next: String?
)

@Serializable
@Parcelize
data class MangaPreview (
    val id: String,
    val name: String,
    val cover: String,

): Parcelable

@Serializable
data class MangaExtras (
    var name: String,
    var value: String
)

@Serializable
data class MangaData (
    val id: String,
    val name: String,
    val cover: String,
    val status: String,
    val description: String,
    val tags: List<String>,
    val extras: List<MangaExtras>
)

@Serializable
data class MangaChapter (
    val id: String,
    val name: String,
    val key: Float,
    val released: String? = ""
)

@Serializable
@Parcelize
data class MangaSource (
    val id: String,
    val name: String,
) : Parcelable



@Serializable
class MangaSearchResponse(override val data: SearchItems<MangaPreview>?,
                          override val error: String?) : MangaApiResponse<SearchItems<MangaPreview>>()

@Serializable
class MangaInfoResponse(override val data: MangaData?,
                          override val error: String?) : MangaApiResponse<MangaData>()

@Serializable
class MangaChaptersResponse(override val data: List<MangaChapter>?,
                        override val error: String?) : MangaApiResponse<List<MangaChapter>>()

@Serializable
class MangaChapterResponse(override val data: List<String>?,
                            override val error: String?) : MangaApiResponse<List<String>>()

@Serializable
class MangaSourceResponse(override val data: List<MangaSource>?,
                           override val error: String?) : MangaApiResponse<List<MangaSource>>()

class MangaApi private constructor(){
    companion object {
        @Volatile
        private var instance: MangaApi? = null

        fun get() = instance ?: MangaApi().also { instance = it}


    }

    private val baseUrl = "http://10.0.0.165:8888"

    var client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }

    suspend fun getSources(): MangaSourceResponse {
        return client.get(baseUrl){
            url {
            }
        }.body()
    }

    suspend fun search(source: String,query: String,next: String = ""): MangaSearchResponse {
        return client.get("$baseUrl/$source"){
            url {
                parameters.append("query",query)
                parameters.append("page",next)
            }
        }.body()
    }

    suspend fun getManga(source: String, mangaId: String): MangaInfoResponse {
        return client.get("$baseUrl/$source/$mangaId"){
            url {
            }
        }.body()
    }

    suspend fun getChapters(source: String, mangaId: String): MangaChaptersResponse {
        return client.get("$baseUrl/$source/$mangaId/chapters"){
            url {
            }
        }.body()
    }

    suspend fun getChapter(source: String, mangaId: String, chapterId: String): MangaChapterResponse {
        return client.get("$baseUrl/$source/$mangaId/chapters/$chapterId"){
            url {
            }
        }.body()
    }
}



