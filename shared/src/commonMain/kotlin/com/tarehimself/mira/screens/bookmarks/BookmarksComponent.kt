package com.tarehimself.mira.screens.bookmarks

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.arkivanov.essenty.parcelable.Parcelable
import com.arkivanov.essenty.parcelable.Parcelize
import com.arkivanov.essenty.statekeeper.consume
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.StoredManga
import io.github.aakira.napier.Napier
import io.realm.kotlin.query.Sort
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface BookmarksComponent : KoinComponent {

    val state: MutableValue<State>


    @Parcelize
    data class State(
        var selectedPage: Int,
    ) : Parcelable

    val onMangaSelected: (manga: StoredManga) -> Unit

    val realmDatabase: RealmRepository

    val context: ComponentContext

    fun updateSelectedPage(newSelected: Int)
}

class DefaultBookmarksComponent(
    componentContext: ComponentContext,
    override val onMangaSelected: (manga: StoredManga) -> Unit
) : BookmarksComponent, ComponentContext by componentContext {

    override val context: ComponentContext = componentContext

    override val realmDatabase: RealmRepository by inject()

    override val state: MutableValue<BookmarksComponent.State> = MutableValue(stateKeeper.consume(key = "BOOKMARKS") ?: BookmarksComponent.State(selectedPage = 0))


    init {
        stateKeeper.register(key = "BOOKMARKS") { state.value }
        lifecycle.subscribe(object : Lifecycle.Callbacks {
            override fun onCreate() {
                super.onCreate()

            }

            override fun onDestroy() {
                super.onDestroy()
            }
        })
    }

    override fun updateSelectedPage(newSelected: Int) {
        if(newSelected != state.value.selectedPage){
            Napier.d { "Updating selected page $newSelected" }
            state.update {
                it.copy(selectedPage = newSelected)
            }
        }
    }
}