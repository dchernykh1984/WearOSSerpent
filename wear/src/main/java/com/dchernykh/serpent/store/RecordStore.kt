package com.dchernykh.serpent.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dchernykh.serpent.game.SpeedLevel
import com.dchernykh.serpent.game.normalizeScore
import kotlinx.coroutines.flow.first

/**
 * What survives closing the app: the difficulty last played, and one best score
 * per difficulty. Beating your record on Fast says something different from
 * beating it on Slow, so they are never pooled.
 *
 * An interface, because everything interesting about the game happens above it:
 * the view model is driven through this, so a JVM test can play whole games
 * against an in-memory implementation instead of an emulator.
 */
interface RecordStore {
    suspend fun readLevel(): SpeedLevel

    suspend fun writeLevel(level: SpeedLevel)

    suspend fun readBest(level: SpeedLevel): Int

    suspend fun writeBest(
        level: SpeedLevel,
        best: Int,
    )
}

private val Context.recordDataStore: DataStore<Preferences> by preferencesDataStore(name = "records")

private val LEVEL_KEY = stringPreferencesKey("speed_level")

private fun bestKey(level: SpeedLevel) = intPreferencesKey("best_${level.name}")

/**
 * The real store, on top of Preferences DataStore.
 *
 * Reads take the first value of the flow rather than collecting it: the game asks
 * for a record at two moments it chooses - opening the start screen and finishing
 * a game - and nothing else on the watch writes these keys, so there is no later
 * value to wait for.
 */
class DataStoreRecordStore(
    context: Context,
) : RecordStore {
    // The application context, not the activity's: a DataStore outlives any one
    // screen, and holding the activity here would leak it for the life of the app.
    private val dataStore = context.applicationContext.recordDataStore

    override suspend fun readLevel(): SpeedLevel = SpeedLevel.fromStoredName(dataStore.data.first()[LEVEL_KEY])

    override suspend fun writeLevel(level: SpeedLevel) {
        dataStore.edit { it[LEVEL_KEY] = level.name }
    }

    override suspend fun readBest(level: SpeedLevel): Int = normalizeScore(dataStore.data.first()[bestKey(level)])

    override suspend fun writeBest(
        level: SpeedLevel,
        best: Int,
    ) {
        dataStore.edit { it[bestKey(level)] = normalizeScore(best) }
    }
}
