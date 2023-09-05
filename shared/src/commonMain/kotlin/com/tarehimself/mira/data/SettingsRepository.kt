package com.tarehimself.mira.data

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.russhwolf.settings.Settings
import com.tarehimself.mira.common.MutableCustomStateValue
import com.tarehimself.mira.common.mutableCustomStateValueOf
import org.koin.core.component.KoinComponent

interface SettingsRepository : KoinComponent {

    val settings: Settings

    data class SettingsKeys(
        val showNsfwSources: String = "settings.sources.nsfw",
        val sourcesApi: String = "settings.sources.api",
        val translatorEndpoint: String = "settings.translator.endpoint"
    )

    val keys: SettingsKeys

    val showNsfwSources: MutableCustomStateValue<Boolean>

    val sourcesApi: MutableCustomStateValue<String>

    val translatorEndpoint: MutableCustomStateValue<String>
}

class DefaultSettingsRepository : SettingsRepository {
    override val settings: Settings = Settings()

    override val keys: SettingsRepository.SettingsKeys = SettingsRepository.SettingsKeys()

    override val showNsfwSources: MutableCustomStateValue<Boolean> = mutableCustomStateValueOf(
        initial = settings.getBoolean(keys.showNsfwSources, false),
        onValueUpdated = {
            settings.putBoolean(keys.showNsfwSources, it)
        })

    override val sourcesApi: MutableCustomStateValue<String> = mutableCustomStateValueOf(
        initial = settings.getString(keys.sourcesApi, "https://manga.oyintare.dev/api/v1"),
        onValueUpdated = {
            settings.putString(keys.sourcesApi, it)
        })

    override val translatorEndpoint: MutableCustomStateValue<String> = mutableCustomStateValueOf(
        initial = settings.getString(keys.translatorEndpoint, ""),
        onValueUpdated = {
            settings.putString(keys.translatorEndpoint, it)
        })
}

fun <T> settingDelegate(
    key: String,
    factory: (key: String) -> T,
    updated: (key: String, value: T) -> Unit = { _, _ -> }
): MutableCustomStateValue<T> {
    return MutableCustomStateValue(initialValue = factory(key), onValueUpdated = {
        updated(key, it)
    })
}

@Composable
fun <T> rememberSetting(
    key: String,
    factory: (key: String) -> T,
    updated: (key: String, value: T) -> Unit = { _, _ -> }
): MutableCustomStateValue<T> {
    return remember(key) {
        settingDelegate(key = key, factory = factory, updated = updated)
    }
}


