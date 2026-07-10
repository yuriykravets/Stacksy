package com.partitionsoft.stacksy.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.stacksyDataStore by preferencesDataStore(name = "stacksy_preferences")

data class UserPreferences(
    val highScore: Int = 0,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
)

class PreferencesStore(context: Context) {
    private val dataStore = context.applicationContext.stacksyDataStore

    val preferences: Flow<UserPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { values ->
            UserPreferences(
                highScore = values[HIGH_SCORE] ?: 0,
                soundEnabled = values[SOUND_ENABLED] ?: true,
                vibrationEnabled = values[VIBRATION_ENABLED] ?: true,
            )
        }

    suspend fun setHighScore(score: Int) {
        dataStore.edit { values ->
            if (score > (values[HIGH_SCORE] ?: 0)) values[HIGH_SCORE] = score
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        dataStore.edit { it[VIBRATION_ENABLED] = enabled }
    }

    private companion object {
        val HIGH_SCORE = intPreferencesKey("high_score")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    }
}
