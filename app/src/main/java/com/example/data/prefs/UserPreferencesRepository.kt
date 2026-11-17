package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.SortOption
import com.example.data.model.ThemePreference
import com.example.data.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "archiva_prefs")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_OPTION = stringPreferencesKey("sort_option")
        val THEME_PREF = stringPreferencesKey("theme_pref")
    }

    val viewMode: Flow<ViewMode> = context.dataStore.data.map { prefs ->
        prefs[Keys.VIEW_MODE]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() } ?: ViewMode.LIST
    }

    val sortOption: Flow<SortOption> = context.dataStore.data.map { prefs ->
        prefs[Keys.SORT_OPTION]?.let { runCatching { SortOption.valueOf(it) }.getOrNull() } ?: SortOption.NAME_ASC
    }

    val themePreference: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        prefs[Keys.THEME_PREF]?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() } ?: ThemePreference.SYSTEM
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { it[Keys.VIEW_MODE] = mode.name }
    }

    suspend fun setSortOption(option: SortOption) {
        context.dataStore.edit { it[Keys.SORT_OPTION] = option.name }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME_PREF] = theme.name }
    }
}
