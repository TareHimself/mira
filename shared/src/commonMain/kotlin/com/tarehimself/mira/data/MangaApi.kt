package com.tarehimself.mira.data

import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent

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

interface MangaPreview {
    val id: String
    val name: String
    val cover: String
}

@Serializable
@Parcelize
data class ApiMangaPreview (
    override val id: String,
    override val name: String,
    override val cover: String,

): Parcelable, MangaPreview

interface MangaExtras {
    var name: String
    var value: String
}
@Serializable
data class ApiMangaExtras (
    override var name: String,
    override var value: String
)  : MangaExtras

interface MangaData {
    val id: String
    val name: String
    val cover: String
    val status: String
    val share: String
    val description: String
    val tags: List<String>
    val extras: List<MangaExtras>
}

@Serializable
data class ApiMangaData (
    override val id: String,
    override val name: String,
    override val cover: String,
    override val share: String,
    override val status: String,
    override val description: String,
    override val tags: List<String>,
    override val extras: List<ApiMangaExtras>
) : MangaData

interface MangaChapter : Parcelable {
    val id: String
    val name: String
    val released: String?
}
@Parcelize
@Serializable
data class ApiMangaChapter (
    override val id: String,
    override val name: String,
    override val released: String? = ""
) : MangaChapter

@Serializable
data class ApiMangaChapterPage (
    val headers: List<List<String>>,
    val src: String,
)

@Serializable
@Parcelize
data class MangaSource (
    val id: String,
    val name: String,
    val nsfw: Boolean,
) : Parcelable





@Serializable
class MangaSearchResponse(override val data: SearchItems<ApiMangaPreview>?,
                          override val error: String?) : MangaApiResponse<SearchItems<ApiMangaPreview>>()

@Serializable
class MangaInfoResponse(override val data: ApiMangaData?,
                        override val error: String?) : MangaApiResponse<ApiMangaData>()

@Serializable
class MangaChaptersResponse(override val data: List<ApiMangaChapter>?,
                        override val error: String?) : MangaApiResponse<List<ApiMangaChapter>>()

@Serializable
class MangaChapterResponse(override val data: List<ApiMangaChapterPage>?,
                            override val error: String?) : MangaApiResponse<List<ApiMangaChapterPage>>()

@Serializable
class MangaSourceResponse(override val data: List<MangaSource>?,
                           override val error: String?) : MangaApiResponse<List<MangaSource>>()

interface MangaApi: KoinComponent {
    val baseUrl: String

    val client: HttpClient
    suspend fun getSources(): MangaSourceResponse

    suspend fun search(source: String,query: String,next: String = ""): MangaSearchResponse

    suspend fun getManga(source: String, mangaId: String): MangaInfoResponse

    suspend fun getChapters(source: String, mangaId: String): MangaChaptersResponse

    suspend fun getChapter(source: String, mangaId: String, chapterId: String): MangaChapterResponse
}
class DefaultMangaApi : MangaApi {

    override val baseUrl = "https://manga.oyintare.dev/api/v1"

    override var client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }
    }



    override suspend fun getSources(): MangaSourceResponse {
        return try {
            client.get(baseUrl){
                url {
                }
            }.body()
        }catch (e: Exception){
            MangaSourceResponse(null,e.message)
        }
    }

    override suspend fun search(source: String,query: String,next: String): MangaSearchResponse {

        return try {
            client.get("$baseUrl/$source"){
                url {
                    parameters.append("query",query)
                    parameters.append("page",next)
                }
            }.body()
        } catch (e: Exception) {
            MangaSearchResponse(null,e.message)
        }
    }

    override suspend fun getManga(source: String, mangaId: String): MangaInfoResponse {
        return try {
            client.get("$baseUrl/$source/$mangaId"){
                url {
                }
            }.body()
        } catch (e: Exception) {
            return MangaInfoResponse(null,e.message)
        }
    }

    override suspend fun getChapters(source: String, mangaId: String): MangaChaptersResponse {
        return try {

            client.get("$baseUrl/$source/$mangaId/chapters") {
                url {
                }
            }.body()
        } catch (e: Exception) {
            return MangaChaptersResponse(null,e.message)
        }
    }

    override suspend fun getChapter(source: String, mangaId: String, chapterId: String): MangaChapterResponse {
        return try {

            client.get("$baseUrl/$source/$mangaId/chapters/$chapterId") {
                url {
                }
            }.body()
        } catch (e: Exception) {
            return MangaChapterResponse(null,e.message)
        }
    }
}



