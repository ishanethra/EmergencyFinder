package com.emergency.finder.repository

import android.content.Context
import android.location.Location
import android.util.Log
import com.emergency.finder.database.DatabaseHelper
import com.emergency.finder.models.EmergencyService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import kotlin.math.*

class PlacesRepository(private val context: Context) {

    private val dbHelper = DatabaseHelper.getInstance(context)

    companion object {
        private const val TAG = "PlacesRepository"
        private const val API_KEY = "AIzaSyAcN_S3IsFh0T_aAVgxPDeSwg-73LhEPPw"
        private const val RADIUS = 40000
    }

    suspend fun fetchNearbyServices(location: Location): List<EmergencyService> =
        withContext(Dispatchers.IO) {
            val lat = location.latitude
            val lng = location.longitude
            
            try {
                val types = listOf("hospital", "police", "fire_station")
                val deferredResults = types.map { type ->
                    async { fetchByType(type, lat, lng) }
                }

                val allServices = deferredResults.awaitAll().flatten()

                if (allServices.isEmpty()) {
                    // FALLBACK: Try OpenStreetMap if Google returns nothing
                    return@withContext fetchFromOSM(lat, lng)
                }

                val sortedServices = allServices.sortedBy { it.distance }
                dbHelper.saveAll(sortedServices)
                sortedServices

            } catch (e: Exception) {
                Log.e(TAG, "Google API failed, trying OSM fallback", e)
                fetchFromOSM(lat, lng)
            }
        }

    /**
     * Alternative Way: OpenStreetMap (Overpass API)
     * No API Key required, completely free.
     */
    private suspend fun fetchFromOSM(lat: Double, lng: Double): List<EmergencyService> {
        return try {
            val query = """
                [out:json];
                (
                  node["amenity"="hospital"](around:5000,$lat,$lng);
                  node["amenity"="police"](around:5000,$lat,$lng);
                  node["amenity"="fire_station"](around:5000,$lat,$lng);
                );
                out body;
            """.trimIndent()

            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://overpass-api.de/api/interpreter?data=$encodedQuery"
            val response = URL(url).readText()
            val elements = JSONObject(response).getJSONArray("elements")
            
            val list = mutableListOf<EmergencyService>()
            for (i in 0 until elements.length()) {
                val item = elements.getJSONObject(i)
                val tags = item.optJSONObject("tags")
                val name = tags?.optString("name") ?: "Emergency Service"
                val type = when {
                    tags?.optString("amenity") == "hospital" -> EmergencyService.TYPE_HOSPITAL
                    tags?.optString("amenity") == "police" -> EmergencyService.TYPE_POLICE
                    else -> EmergencyService.TYPE_FIRE
                }

                val pLat = item.getDouble("lat")
                val pLng = item.getDouble("lon")

                list.add(EmergencyService(
                    name = name,
                    type = type,
                    address = tags?.optString("addr:street") ?: "Nearby Location",
                    latitude = pLat,
                    longitude = pLng,
                    distance = haversineKm(lat, lng, pLat, pLng)
                ))
            }
            list.sortedBy { it.distance }
        } catch (e: Exception) {
            Log.e(TAG, "OSM Fallback failed", e)
            defaultEmergencyContacts()
        }
    }

    private fun fetchByType(type: String, lat: Double, lng: Double): List<EmergencyService> {
        val list = mutableListOf<EmergencyService>()
        try {
            val urlString = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                    "location=$lat,$lng" +
                    "&radius=$RADIUS" +
                    "&type=$type" +
                    "&key=$API_KEY"

            val response = URL(urlString).readText()
            val json = JSONObject(response)
            
            if (json.has("error_message")) return emptyList()

            val results = json.getJSONArray("results")
            for (i in 0 until results.length()) {
                val place = results.getJSONObject(i)
                val geometry = place.getJSONObject("geometry").getJSONObject("location")
                val pLat = geometry.getDouble("lat")
                val pLng = geometry.getDouble("lng")

                list.add(EmergencyService(
                    name = place.getString("name"),
                    type = mapType(type),
                    address = place.optString("vicinity", "Address not available"),
                    latitude = pLat,
                    longitude = pLng,
                    distance = haversineKm(lat, lng, pLat, pLng),
                    placeId = place.getString("place_id")
                ))
            }
        } catch (e: Exception) { }
        return list
    }

    private fun mapType(type: String) = when (type) {
        "hospital" -> EmergencyService.TYPE_HOSPITAL
        "police" -> EmergencyService.TYPE_POLICE
        "fire_station" -> EmergencyService.TYPE_FIRE
        else -> EmergencyService.TYPE_HOSPITAL
    }

    private fun defaultEmergencyContacts() = listOf(
        EmergencyService(name = "Ambulance", type = EmergencyService.TYPE_HOSPITAL, address = "National", phone = "102"),
        EmergencyService(name = "Police", type = EmergencyService.TYPE_POLICE, address = "National", phone = "100"),
        EmergencyService(name = "Fire", type = EmergencyService.TYPE_FIRE, address = "National", phone = "101")
    )

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun hasCachedData(): Boolean = dbHelper.hasData()
    fun loadCachedServices(): List<EmergencyService> = dbHelper.getAllServices()
}