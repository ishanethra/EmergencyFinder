package com.emergency.finder.models

data class EmergencyService(
    val id: Long = 0,
    val name: String = "",
    val type: String = "",
    val address: String = "",
    val phone: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distance: Double = 0.0,
    val placeId: String = ""
) {

    companion object {

        const val TABLE_NAME = "emergency_services"

        const val COL_ID = "_id"
        const val COL_NAME = "name"
        const val COL_TYPE = "type"
        const val COL_ADDRESS = "address"
        const val COL_PHONE = "phone"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
        const val COL_DISTANCE = "distance"
        const val COL_PLACE_ID = "place_id"

        const val TYPE_HOSPITAL = "hospital"
        const val TYPE_POLICE = "police"
        const val TYPE_FIRE = "fire"
    }

    // Distance formatting for UI card
    fun formattedDistance(): String {

        return if (distance < 1.0) {
            "${(distance * 1000).toInt()} m"
        } else {
            "%.1f km".format(distance)
        }
    }

    // Emoji icon for card UI
    fun typeIcon(): String {
        return when (type) {
            TYPE_HOSPITAL -> "🏥"
            TYPE_POLICE -> "🚔"
            TYPE_FIRE -> "🚒"
            else -> "📍"
        }
    }

    // Label used in UI
    fun typeLabel(): String {
        return when (type) {
            TYPE_HOSPITAL -> "Hospital"
            TYPE_POLICE -> "Police Station"
            TYPE_FIRE -> "Fire Station"
            else -> "Emergency Service"
        }
    }

    // Ensure address is never blank
    fun safeAddress(): String {

        return if (address.isBlank()) {
            "Location near you"
        } else {
            address
        }
    }

    // Emergency phone fallback logic
    fun safePhone(): String {

        // If phone exists, clean it
        if (phone.isNotBlank()) {

            val cleaned = phone.replace("[^0-9+]".toRegex(), "")

            if (cleaned.isNotBlank()) {
                return cleaned
            }
        }

        // If phone not available, use emergency fallback
        return when (type) {

            TYPE_POLICE -> "100"
            TYPE_FIRE -> "101"
            TYPE_HOSPITAL -> "102"

            else -> "112"
        }
    }
}