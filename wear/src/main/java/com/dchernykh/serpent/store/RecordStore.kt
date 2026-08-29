package com.dchernykh.serpent.store

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dchernykh.serpent.game.SpeedLevel
import com.dchernykh.serpent.game.normalizeScore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException

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
 *
 * Storage that has gone wrong must not stop anyone playing. A DataStore whose file
 * is unreadable throws on every read and every write, and a game that would not
 * start because a preferences file was corrupt is far worse than one that forgets
 * a best score. So a failed read reads as nothing stored and a failed write is
 * dropped; the score for the game in front of you lives in the view model either
 * way, and only outlives the app when the disk lets it.
 */
class DataStoreRecordStore(
    context: Context,
) : RecordStore {
    // The application context, not the activity's: a DataStore outlives any one
    // screen, and holding the activity here would leak it for the life of the app.
    private val dataStore = context.applicationContext.recordDataStore

    private suspend fun read(): Preferences =
        dataStore.data
            .catch { cause ->
                // Only I/O. Anything else is a bug in this file rather than a
                // broken disk, and swallowing it would hide it.
                if (cause is IOException) emit(emptyPreferences()) else throw cause
            }.first()

    private suspend fun write(change: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit(change)
        } catch (_: IOException) {
            // Nothing to do and nothing worth saying: the game carries on.
        }
    }

    override suspend fun readLevel(): SpeedLevel = SpeedLevel.fromStoredName(read()[LEVEL_KEY])

    override suspend fun writeLevel(level: SpeedLevel) = write { it[LEVEL_KEY] = level.name }

    override suspend fun readBest(level: SpeedLevel): Int = normalizeScore(read()[bestKey(level)])

    override suspend fun writeBest(
        level: SpeedLevel,
        best: Int,
    ) = write { it[bestKey(level)] = normalizeScore(best) }
}
