package com.oxymusic.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.oxymusic.app.model.AnimeTheme
import com.oxymusic.app.model.MascotPersonality
import com.oxymusic.app.model.Settings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "oxy_settings")

class SettingsRepository(@ApplicationContext private val context: Context) {
    private object Keys {
        val ADAPTIVE = booleanPreferencesKey("adaptive_colors")
        val ANIME = booleanPreferencesKey("anime_mode")
        val ANIME_THEME = intPreferencesKey("anime_theme")
        val INTENSITY = intPreferencesKey("anime_intensity")
        val KARAOKE = booleanPreferencesKey("karaoke_mode")
        val LOCK_LYRICS = booleanPreferencesKey("lock_lyrics")
        val CROSSFADE = floatPreferencesKey("crossfade")
        val SKIP_ERR = booleanPreferencesKey("skip_err")
        val CACHE_MB = intPreferencesKey("cache_mb")
        val MASCOT = booleanPreferencesKey("mascot")
        val PERSONALITY = intPreferencesKey("personality")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { p ->
        Settings(
            adaptiveColors = p[Keys.ADAPTIVE] ?: true,
            animeMode = p[Keys.ANIME] ?: false,
            animeTheme = AnimeTheme.entries.getOrElse(p[Keys.ANIME_THEME] ?: 0) { AnimeTheme.SAKURA },
            animeIntensity = p[Keys.INTENSITY] ?: 14,
            karaokeMode = p[Keys.KARAOKE] ?: true,
            lockScreenLyrics = p[Keys.LOCK_LYRICS] ?: true,
            crossfadeSeconds = p[Keys.CROSSFADE] ?: 0f,
            skipOnError = p[Keys.SKIP_ERR] ?: true,
            cacheSizeMb = p[Keys.CACHE_MB] ?: 500,
            mascotEnabled = p[Keys.MASCOT] ?: true,
            mascotPersonality = MascotPersonality.entries.getOrElse(p[Keys.PERSONALITY] ?: 0) { MascotPersonality.CUTE },
        )
    }

    suspend fun setAdaptive(v: Boolean) = context.dataStore.edit { it[Keys.ADAPTIVE] = v }
    suspend fun setAnimeMode(v: Boolean) = context.dataStore.edit { it[Keys.ANIME] = v }
    suspend fun setAnimeTheme(v: AnimeTheme) = context.dataStore.edit { it[Keys.ANIME_THEME] = v.ordinal }
    suspend fun setAnimeIntensity(v: Int) = context.dataStore.edit { it[Keys.INTENSITY] = v }
    suspend fun setKaraoke(v: Boolean) = context.dataStore.edit { it[Keys.KARAOKE] = v }
    suspend fun setLockLyrics(v: Boolean) = context.dataStore.edit { it[Keys.LOCK_LYRICS] = v }
    suspend fun setCrossfade(v: Float) = context.dataStore.edit { it[Keys.CROSSFADE] = v }
    suspend fun setSkipErr(v: Boolean) = context.dataStore.edit { it[Keys.SKIP_ERR] = v }
    suspend fun setCacheMb(v: Int) = context.dataStore.edit { it[Keys.CACHE_MB] = v }
    suspend fun setMascot(v: Boolean) = context.dataStore.edit { it[Keys.MASCOT] = v }
    suspend fun setPersonality(v: MascotPersonality) = context.dataStore.edit { it[Keys.PERSONALITY] = v.ordinal }
}

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides @Singleton
    fun provideSettingsRepository(@ApplicationContext ctx: Context) = SettingsRepository(ctx)
}
