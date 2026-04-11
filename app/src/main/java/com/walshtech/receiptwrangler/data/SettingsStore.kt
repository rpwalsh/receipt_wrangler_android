/*
 * Copyright (c) 2026 Ryan P. Walsh
 * All rights reserved.
 * This software is proprietary and confidential.
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * This is NOT open source software.
 */

package com.walshtech.receiptwrangler.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "receipt_wrangler_settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

class SettingsStore(private val context: Context) {
    private val laneKey = stringPreferencesKey("lane")
    private val projectNameKey = stringPreferencesKey("project_name")
    private val reportNameKey = stringPreferencesKey("report_name")
    private val tutorialSeenKey = booleanPreferencesKey("tutorial_seen")
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val laneFlow: Flow<UserLane> = context.settingsDataStore.data.map { prefs ->
        parseUserLane(prefs[laneKey].orEmpty())
    }

    val reportNameFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        prefs[reportNameKey].orEmpty()
    }

    val projectNameFlow: Flow<String> = context.settingsDataStore.data.map { prefs ->
        val lane = parseUserLane(prefs[laneKey].orEmpty())
        prefs[projectNameKey].orEmpty().ifBlank { defaultProjectName(lane) }
    }

    val tutorialSeenFlow: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[tutorialSeenKey] ?: false
    }

    val themeModeFlow: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        enumValueOrDefault(prefs[themeModeKey].orEmpty(), ThemeMode.SYSTEM)
    }

    suspend fun setLane(lane: UserLane) {
        context.settingsDataStore.edit { it[laneKey] = lane.name }
    }

    suspend fun setReportName(name: String) {
        context.settingsDataStore.edit { it[reportNameKey] = name }
    }

    suspend fun setProjectName(name: String) {
        context.settingsDataStore.edit { prefs ->
            val lane = parseUserLane(prefs[laneKey].orEmpty())
            prefs[projectNameKey] = name.ifBlank { defaultProjectName(lane) }
        }
    }

    suspend fun setTutorialSeen(seen: Boolean) {
        context.settingsDataStore.edit { it[tutorialSeenKey] = seen }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeModeKey] = mode.name }
    }
}
