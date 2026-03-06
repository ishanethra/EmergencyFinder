package com.emergency.finder.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import androidx.core.database.sqlite.transaction
import com.emergency.finder.models.EmergencyContact
import com.emergency.finder.models.EmergencyService

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "emergency_finder.db", null, 3) {

    companion object {
        private const val TAG = "DatabaseHelper"

        @Volatile
        private var INSTANCE: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: DatabaseHelper(context).also { INSTANCE = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${EmergencyService.TABLE_NAME} (
                ${EmergencyService.COL_ID}        INTEGER PRIMARY KEY AUTOINCREMENT,
                ${EmergencyService.COL_NAME}      TEXT NOT NULL,
                ${EmergencyService.COL_TYPE}      TEXT NOT NULL,
                ${EmergencyService.COL_ADDRESS}   TEXT,
                ${EmergencyService.COL_PHONE}     TEXT,
                ${EmergencyService.COL_LATITUDE}  REAL,
                ${EmergencyService.COL_LONGITUDE} REAL,
                ${EmergencyService.COL_DISTANCE}  REAL DEFAULT 0,
                ${EmergencyService.COL_PLACE_ID}  TEXT UNIQUE,
                timestamp INTEGER
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE ${EmergencyContact.TABLE_NAME} (
                ${EmergencyContact.COL_ID}    INTEGER PRIMARY KEY AUTOINCREMENT,
                ${EmergencyContact.COL_NAME}  TEXT NOT NULL,
                ${EmergencyContact.COL_PHONE} TEXT NOT NULL
            )
            """.trimIndent()
        )

        Log.d(TAG, "Database created")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS ${EmergencyContact.TABLE_NAME} (
                    ${EmergencyContact.COL_ID}    INTEGER PRIMARY KEY AUTOINCREMENT,
                    ${EmergencyContact.COL_NAME}  TEXT NOT NULL,
                    ${EmergencyContact.COL_PHONE} TEXT NOT NULL
                )
                """.trimIndent()
            )
        }
    }

    fun saveAll(services: List<EmergencyService>) {
        writableDatabase.transaction {
            services.forEach { s ->
                insertWithOnConflict(
                    EmergencyService.TABLE_NAME,
                    null,
                    s.toContentValues(),
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
        }
        cleanupOldEntries()
    }

    private fun cleanupOldEntries() {
        writableDatabase.execSQL(
            """
            DELETE FROM ${EmergencyService.TABLE_NAME}
            WHERE ${EmergencyService.COL_ID} NOT IN
            (
                SELECT ${EmergencyService.COL_ID}
                FROM ${EmergencyService.TABLE_NAME}
                ORDER BY ${EmergencyService.COL_ID} DESC
                LIMIT 300
            )
            """.trimIndent()
        )
    }

    fun getAllServices(): List<EmergencyService> {
        val list = mutableListOf<EmergencyService>()
        readableDatabase.query(
            EmergencyService.TABLE_NAME,
            null,
            null,
            null,
            null,
            null,
            "${EmergencyService.COL_DISTANCE} ASC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursor.toEmergencyService())
            }
        }
        return list
    }

    fun hasData(): Boolean {
        val cursor = readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${EmergencyService.TABLE_NAME}", null
        )
        return cursor.use {
            if (it.moveToFirst()) it.getLong(0) > 0 else false
        }
    }

    // Emergency Contact Methods
    fun addContact(contact: EmergencyContact): Long {
        return writableDatabase.insert(EmergencyContact.TABLE_NAME, null, contact.toContentValues())
    }

    fun getContacts(): List<EmergencyContact> {
        val list = mutableListOf<EmergencyContact>()
        readableDatabase.query(EmergencyContact.TABLE_NAME, null, null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(EmergencyContact.fromCursor(cursor))
            }
        }
        return list
    }
    
    fun deleteContact(id: Long) {
        writableDatabase.delete(EmergencyContact.TABLE_NAME, "${EmergencyContact.COL_ID} = ?", arrayOf(id.toString()))
    }

    private fun EmergencyService.toContentValues() = ContentValues().apply {
        put(EmergencyService.COL_NAME, name)
        put(EmergencyService.COL_TYPE, type)
        put(EmergencyService.COL_ADDRESS, address)
        put(EmergencyService.COL_PHONE, phone)
        put(EmergencyService.COL_LATITUDE, latitude)
        put(EmergencyService.COL_LONGITUDE, longitude)
        put(EmergencyService.COL_DISTANCE, distance)
        put(EmergencyService.COL_PLACE_ID, placeId)
        put("timestamp", System.currentTimeMillis())
    }

    private fun Cursor.toEmergencyService() = EmergencyService(
        id        = getLong(getColumnIndexOrThrow(EmergencyService.COL_ID)),
        name      = getString(getColumnIndexOrThrow(EmergencyService.COL_NAME)) ?: "",
        type      = getString(getColumnIndexOrThrow(EmergencyService.COL_TYPE)) ?: "",
        address   = getString(getColumnIndexOrThrow(EmergencyService.COL_ADDRESS)) ?: "",
        phone     = getString(getColumnIndexOrThrow(EmergencyService.COL_PHONE)) ?: "",
        latitude  = getDouble(getColumnIndexOrThrow(EmergencyService.COL_LATITUDE)),
        longitude = getDouble(getColumnIndexOrThrow(EmergencyService.COL_LONGITUDE)),
        distance  = getDouble(getColumnIndexOrThrow(EmergencyService.COL_DISTANCE)),
        placeId   = getString(getColumnIndexOrThrow(EmergencyService.COL_PLACE_ID)) ?: ""
    )
}