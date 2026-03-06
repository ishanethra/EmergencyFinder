package com.emergency.finder.models

import android.content.ContentValues
import android.database.Cursor

data class EmergencyContact(
    val id: Long = 0,
    val name: String,
    val phoneNumber: String
) {
    companion object {
        const val TABLE_NAME = "emergency_contacts"
        const val COL_ID = "_id"
        const val COL_NAME = "contact_name"
        const val COL_PHONE = "phone_number"

        fun fromCursor(cursor: Cursor): EmergencyContact {
            return EmergencyContact(
                id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                phoneNumber = cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE))
            )
        }
    }

    fun toContentValues(): ContentValues {
        return ContentValues().apply {
            if (id != 0L) put(COL_ID, id)
            put(COL_NAME, name)
            put(COL_PHONE, phoneNumber)
        }
    }
}