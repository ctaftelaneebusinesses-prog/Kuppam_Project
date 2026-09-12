package com.onetowncity.app.cache

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Plain SQLite (framework-provided, zero new dependencies — matching this
 * codebase's existing hand-rolled-over-library convention for HTTP/JSON/PKCE)
 * rather than Room: the cache only ever needs one generic shape — a raw API
 * response body keyed by request, with a few metadata columns for
 * city-scoped and type-scoped eviction. One table covers all cacheable
 * content types (business/property/project/events/scholarships/lost&found/
 * places-to-visit/student-services/tuition-centers/categories) without
 * needing a typed entity+DAO per type.
 */
internal class OfflineCacheDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DB_NAME,
    null,
    DB_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_CACHE (
                $COL_KEY TEXT PRIMARY KEY,
                $COL_CITY_SLUG TEXT,
                $COL_ENTITY_TYPE TEXT NOT NULL,
                $COL_RESPONSE_JSON TEXT NOT NULL,
                $COL_CACHED_AT INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_cache_city ON $TABLE_CACHE ($COL_CITY_SLUG)")
        db.execSQL("CREATE INDEX idx_cache_cached_at ON $TABLE_CACHE ($COL_CACHED_AT)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Cache-only data — safe to drop and recreate on schema changes rather
        // than writing migrations for content that's disposable by design.
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHE")
        onCreate(db)
    }

    companion object {
        private const val DB_NAME = "one_town_city_offline_cache.db"
        private const val DB_VERSION = 1
        const val TABLE_CACHE = "cached_responses"
        const val COL_KEY = "cache_key"
        const val COL_CITY_SLUG = "city_slug"
        const val COL_ENTITY_TYPE = "entity_type"
        const val COL_RESPONSE_JSON = "response_json"
        const val COL_CACHED_AT = "cached_at"
    }
}
