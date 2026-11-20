package com.example.cherrycharm.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cherrycharm_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Small on-device preferences: theme, onboarding, recent searches, recently viewed. */
@Singleton
class PreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val ONBOARDED = booleanPreferencesKey("onboarding_done")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
        val RECENTLY_VIEWED = stringPreferencesKey("recently_viewed")
    }

    private val prefs: Flow<Preferences> = context.dataStore.data.catch { emit(emptyPreferences()) }

    val themeMode: Flow<ThemeMode> = prefs.map { p ->
        runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }

    val onboardingDone: Flow<Boolean> = prefs.map { it[Keys.ONBOARDED] ?: false }

    val recentSearches: Flow<List<String>> = prefs.map { it[Keys.RECENT_SEARCHES].splitList() }

    val recentlyViewed: Flow<List<String>> = prefs.map { it[Keys.RECENTLY_VIEWED].splitList() }

    suspend fun isOnboardingDone(): Boolean = onboardingDone.first()

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { it[Keys.ONBOARDED] = true }
    }

    suspend fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        context.dataStore.edit { p ->
            val list = p[Keys.RECENT_SEARCHES].splitList().filterNot { it.equals(q, true) }
            p[Keys.RECENT_SEARCHES] = (listOf(q) + list).take(10).joinToString(SEP)
        }
    }

    suspend fun removeRecentSearch(query: String) {
        context.dataStore.edit { p ->
            p[Keys.RECENT_SEARCHES] = p[Keys.RECENT_SEARCHES].splitList().filterNot { it == query }.joinToString(SEP)
        }
    }

    suspend fun clearRecentSearches() {
        context.dataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
    }

    suspend fun addRecentlyViewed(productId: String) {
        context.dataStore.edit { p ->
            val list = p[Keys.RECENTLY_VIEWED].splitList().filterNot { it == productId }
            p[Keys.RECENTLY_VIEWED] = (listOf(productId) + list).take(15).joinToString(SEP)
        }
    }

    private fun String?.splitList(): List<String> =
        if (isNullOrBlank()) emptyList() else split(SEP).filter { it.isNotBlank() }

    private companion object { const val SEP = "\u001F" }
}
