package com.onetowncity.app.cache

import android.content.ContentValues
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class CachedEntry(val json: String, val cachedAtMillis: Long)

/**
 * The generic read/write/evict API over OfflineCacheDatabase. All calls run
 * on Dispatchers.IO — SQLite access never touches the main thread (offline-
 * first spec's "avoid blocking the main thread").
 *
 * init() follows the same pattern as SessionManager.init/NetworkMonitor.init
 * — called once from MainActivity.onCreate, storing an application Context
 * so call sites throughout the app (starting with httpJson) don't need a
 * Context threaded through every fetch function's signature.
 */
internal object OfflineCache {
    private var initialized = false
    private lateinit var db: OfflineCacheDatabase

    /** Hard cap on stored entries — oldest evicted first once exceeded, so a long-running install can't grow the cache without bound. */
    private const val MAX_CACHE_ENTRIES = 600

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        db = OfflineCacheDatabase(context)
    }

    suspend fun read(key: String): CachedEntry? = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext null
        db.readableDatabase.query(
            OfflineCacheDatabase.TABLE_CACHE,
            arrayOf(OfflineCacheDatabase.COL_RESPONSE_JSON, OfflineCacheDatabase.COL_CACHED_AT),
            "${OfflineCacheDatabase.COL_KEY} = ?",
            arrayOf(key),
            null,
            null,
            null,
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                CachedEntry(
                    json = cursor.getString(0),
                    cachedAtMillis = cursor.getLong(1),
                )
            } else {
                null
            }
        }
    }

    suspend fun write(key: String, citySlug: String?, entityType: String, json: String) = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext
        val values = ContentValues().apply {
            put(OfflineCacheDatabase.COL_KEY, key)
            put(OfflineCacheDatabase.COL_CITY_SLUG, citySlug)
            put(OfflineCacheDatabase.COL_ENTITY_TYPE, entityType)
            put(OfflineCacheDatabase.COL_RESPONSE_JSON, json)
            put(OfflineCacheDatabase.COL_CACHED_AT, System.currentTimeMillis())
        }
        db.writableDatabase.insertWithOnConflict(
            OfflineCacheDatabase.TABLE_CACHE,
            null,
            values,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE,
        )
        enforceStorageLimit()
    }

    /** Explicit invalidation for one city — e.g. a manual "force refresh" or account logout, not called on every read. */
    suspend fun clearCity(citySlug: String) = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext
        db.writableDatabase.delete(
            OfflineCacheDatabase.TABLE_CACHE,
            "${OfflineCacheDatabase.COL_CITY_SLUG} = ?",
            arrayOf(citySlug),
        )
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        if (!initialized) return@withContext
        db.writableDatabase.delete(OfflineCacheDatabase.TABLE_CACHE, null, null)
    }

    private fun enforceStorageLimit() {
        val database = db.writableDatabase
        val count = database.compileStatement("SELECT COUNT(*) FROM ${OfflineCacheDatabase.TABLE_CACHE}").use {
            it.simpleQueryForLong()
        }
        if (count <= MAX_CACHE_ENTRIES) return
        val excess = count - MAX_CACHE_ENTRIES
        database.execSQL(
            """
            DELETE FROM ${OfflineCacheDatabase.TABLE_CACHE}
            WHERE ${OfflineCacheDatabase.COL_KEY} IN (
                SELECT ${OfflineCacheDatabase.COL_KEY} FROM ${OfflineCacheDatabase.TABLE_CACHE}
                ORDER BY ${OfflineCacheDatabase.COL_CACHED_AT} ASC
                LIMIT $excess
            )
            """.trimIndent(),
        )
    }
}
