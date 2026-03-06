package com.emergency.finder.providers

import android.content.ContentProvider
import android.content.ContentUris
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import com.emergency.finder.database.DatabaseHelper
import com.emergency.finder.models.EmergencyService

class EmergencyContentProvider : ContentProvider() {

    companion object {

        const val AUTHORITY = "com.emergency.finder.provider"
        const val BASE_PATH = "services"

        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/$BASE_PATH")

        private const val CODE_ALL = 1
        private const val CODE_BY_ID = 2
        private const val CODE_HOSPITAL = 3
        private const val CODE_POLICE = 4
        private const val CODE_FIRE = 5

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {

            addURI(AUTHORITY, BASE_PATH, CODE_ALL)
            addURI(AUTHORITY, "$BASE_PATH/#", CODE_BY_ID)

            addURI(AUTHORITY, "$BASE_PATH/hospital", CODE_HOSPITAL)
            addURI(AUTHORITY, "$BASE_PATH/police", CODE_POLICE)
            addURI(AUTHORITY, "$BASE_PATH/fire", CODE_FIRE)
        }
    }

    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(): Boolean {

        val ctx = context ?: return false

        dbHelper = DatabaseHelper.getInstance(ctx)

        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {

        val db = dbHelper.readableDatabase

        val defaultSort = "${EmergencyService.COL_DISTANCE} ASC"

        val cursor = when (uriMatcher.match(uri)) {

            CODE_ALL -> db.query(
                EmergencyService.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder ?: defaultSort
            )

            CODE_BY_ID -> db.query(
                EmergencyService.TABLE_NAME,
                projection,
                "${EmergencyService.COL_ID}=?",
                arrayOf(ContentUris.parseId(uri).toString()),
                null,
                null,
                sortOrder
            )

            CODE_HOSPITAL -> db.query(
                EmergencyService.TABLE_NAME,
                projection,
                "${EmergencyService.COL_TYPE}=?",
                arrayOf(EmergencyService.TYPE_HOSPITAL),
                null,
                null,
                sortOrder ?: defaultSort
            )

            CODE_POLICE -> db.query(
                EmergencyService.TABLE_NAME,
                projection,
                "${EmergencyService.COL_TYPE}=?",
                arrayOf(EmergencyService.TYPE_POLICE),
                null,
                null,
                sortOrder ?: defaultSort
            )

            CODE_FIRE -> db.query(
                EmergencyService.TABLE_NAME,
                projection,
                "${EmergencyService.COL_TYPE}=?",
                arrayOf(EmergencyService.TYPE_FIRE),
                null,
                null,
                sortOrder ?: defaultSort
            )

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        context?.contentResolver?.let {
            cursor?.setNotificationUri(it, uri)
        }

        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {

        if (uriMatcher.match(uri) != CODE_ALL) {
            throw IllegalArgumentException("Invalid URI for insert: $uri")
        }

        val db = dbHelper.writableDatabase

        val newId = db.insertWithOnConflict(
            EmergencyService.TABLE_NAME,
            null,
            values ?: ContentValues(),
            SQLiteDatabase.CONFLICT_REPLACE
        )

        return if (newId > 0) {

            context?.contentResolver?.notifyChange(uri, null)

            ContentUris.withAppendedId(CONTENT_URI, newId)

        } else null
    }

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {

        val db = dbHelper.writableDatabase

        val rows = when (uriMatcher.match(uri)) {

            CODE_ALL -> db.update(
                EmergencyService.TABLE_NAME,
                values,
                selection,
                selectionArgs
            )

            CODE_BY_ID -> db.update(
                EmergencyService.TABLE_NAME,
                values,
                "${EmergencyService.COL_ID}=?",
                arrayOf(ContentUris.parseId(uri).toString())
            )

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        if (rows > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }

        return rows
    }

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {

        val db = dbHelper.writableDatabase

        val rows = when (uriMatcher.match(uri)) {

            CODE_ALL -> db.delete(
                EmergencyService.TABLE_NAME,
                selection,
                selectionArgs
            )

            CODE_BY_ID -> db.delete(
                EmergencyService.TABLE_NAME,
                "${EmergencyService.COL_ID}=?",
                arrayOf(ContentUris.parseId(uri).toString())
            )

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }

        if (rows > 0) {
            context?.contentResolver?.notifyChange(uri, null)
        }

        return rows
    }

    override fun getType(uri: Uri): String {

        return when (uriMatcher.match(uri)) {

            CODE_ALL,
            CODE_HOSPITAL,
            CODE_POLICE,
            CODE_FIRE ->
                "vnd.android.cursor.dir/vnd.$AUTHORITY.$BASE_PATH"

            CODE_BY_ID ->
                "vnd.android.cursor.item/vnd.$AUTHORITY.$BASE_PATH"

            else ->
                throw IllegalArgumentException("Unknown URI: $uri")
        }
    }
}