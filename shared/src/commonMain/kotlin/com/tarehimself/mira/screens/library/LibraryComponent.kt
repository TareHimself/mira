package com.tarehimself.mira.ui.search

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.update
import com.arkivanov.essenty.lifecycle.Lifecycle
import com.tarehimself.mira.common.Cache
import com.tarehimself.mira.common.debug
import com.tarehimself.mira.data.DefaultRealmRepository
import com.tarehimself.mira.data.RealmRepository
import com.tarehimself.mira.data.StoredManga
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


interface LibraryComponent : KoinComponent  {
    val state: MutableValue<State>

    data class State(
        var library: List<StoredManga>
    )

    val onMangaSelected: (manga: StoredManga) -> Unit

    val onLibraryUpdated: () -> Unit

    val realmDatabase: RealmRepository

    fun loadLibrary()
}
class DefaultLibraryComponent (componentContext: ComponentContext,
                               override val onMangaSelected: (manga: StoredManga) -> Unit
) : LibraryComponent,ComponentContext by componentContext {

    override val realmDatabase: RealmRepository by inject<RealmRepository>()

    override val state: MutableValue<LibraryComponent.State> = MutableValue(LibraryComponent.State(library = realmDatabase.getLibrary().values.toList()))


    init {
        lifecycle.subscribe (object : Lifecycle.Callbacks {
            var unsubscribe: (() -> Unit) ? = null
            override fun onCreate() {
                super.onCreate()
                unsubscribe = realmDatabase.subscribeOnLibraryUpdated(onLibraryUpdated)
            }

            override fun onDestroy() {
                super.onDestroy()
                unsubscribe?.invoke()
            }
        })
    }

    override val onLibraryUpdated: () -> Unit = {
        state.update {
            it.library = realmDatabase.getLibrary().values.toList()
            it
        }
    }

    override fun loadLibrary() {
        onLibraryUpdated()
    }
}