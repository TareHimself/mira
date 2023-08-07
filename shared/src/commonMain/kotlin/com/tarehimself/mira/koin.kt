package com.tarehimself.mira

import com.tarehimself.mira.data.DefaultImageRepository
import com.tarehimself.mira.data.DefaultMangaApi
import com.tarehimself.mira.data.DefaultRealmRepository
import com.tarehimself.mira.data.ImageRepository
import com.tarehimself.mira.data.MangaApi
import com.tarehimself.mira.data.RealmRepository
import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(
            commonModule
        )
    }

val commonModule = module {
    single<MangaApi> {
        DefaultMangaApi()
    }

    single<RealmRepository> {
        DefaultRealmRepository()
    }

    single<ImageRepository> {
        DefaultImageRepository()
    }

    factory {
        
    }
}