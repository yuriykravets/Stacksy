package com.partitionsoft.stacksy.core.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.partitionsoft.stacksy.collection.domain.REWARDED_SET_RUNS
import com.partitionsoft.stacksy.collection.domain.SnackSet
import com.partitionsoft.stacksy.collection.domain.canPlaySnackSet
import com.partitionsoft.stacksy.collection.domain.remainingUsesAfterStarting
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.stacksyDataStore by preferencesDataStore(name = "stacksy_preferences")

data class UserPreferences(
    val highScore: Int = 0,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val selectedSnackSet: SnackSet = SnackSet.Classic,
    val snackSetUses: Map<SnackSet, Int> = emptyMap(),
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
                selectedSnackSet = SnackSet.fromStorageId(values[SELECTED_SNACK_SET]),
                snackSetUses = SnackSet.entries
                    .filterNot(SnackSet::isFree)
                    .associateWith { values[usesKey(it)] ?: 0 },
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

    suspend fun selectSnackSet(snackSet: SnackSet) {
        dataStore.edit { values ->
            val remainingUses = values[usesKey(snackSet)] ?: 0
            if (canPlaySnackSet(snackSet, remainingUses)) {
                values[SELECTED_SNACK_SET] = snackSet.storageId
            }
        }
    }

    suspend fun unlockSnackSet(snackSet: SnackSet) {
        if (snackSet.isFree) return
        dataStore.edit { values ->
            values[usesKey(snackSet)] = REWARDED_SET_RUNS
            values[SELECTED_SNACK_SET] = snackSet.storageId
        }
    }

    suspend fun consumeSelectedSnackSetRun(): SnackSet {
        var playableSet = SnackSet.Classic
        dataStore.edit { values ->
            val selected = SnackSet.fromStorageId(values[SELECTED_SNACK_SET])
            val remainingUses = values[usesKey(selected)] ?: 0
            playableSet = selected.takeIf { canPlaySnackSet(it, remainingUses) }
                ?: SnackSet.Classic

            if (playableSet != selected) {
                values[SELECTED_SNACK_SET] = SnackSet.Classic.storageId
            } else if (!selected.isFree) {
                values[usesKey(selected)] =
                    remainingUsesAfterStarting(selected, remainingUses)
            }
        }
        return playableSet
    }

    private companion object {
        val HIGH_SCORE = intPreferencesKey("high_score")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val SELECTED_SNACK_SET = stringPreferencesKey("selected_snack_set")

        fun usesKey(snackSet: SnackSet) =
            intPreferencesKey("snack_set_${snackSet.storageId}_remaining_uses")
    }
}
