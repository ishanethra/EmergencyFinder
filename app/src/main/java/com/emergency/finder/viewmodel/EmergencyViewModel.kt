package com.emergency.finder.viewmodel

import android.Manifest
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.emergency.finder.database.DatabaseHelper
import com.emergency.finder.models.EmergencyContact
import com.emergency.finder.models.EmergencyService
import com.emergency.finder.repository.PlacesRepository
import com.emergency.finder.utils.ShakeDetector
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

sealed class EmergencyUiState {
    object Idle         : EmergencyUiState()
    object Loading      : EmergencyUiState()
    object NoPermission : EmergencyUiState()
    object GpsOff       : EmergencyUiState()
    object Offline      : EmergencyUiState()
    data class Success(
        val services: List<EmergencyService>,
        val isFromCache: Boolean = false
    ) : EmergencyUiState()
    data class Error(val message: String) : EmergencyUiState()
}

class EmergencyViewModel(application: Application) : AndroidViewModel(application) {

    private val context     = application.applicationContext
    private val dbHelper    = DatabaseHelper.getInstance(context)
    private val repository  = PlacesRepository(context)
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)
    
    private var lastLocation: Location? = null
    var lastLatitude: Double = 0.0
    var lastLongitude: Double = 0.0

    private val _uiState  = MutableStateFlow<EmergencyUiState>(EmergencyUiState.Idle)
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    private val _isOnline = MutableStateFlow(checkNetworkStatus())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _contacts = MutableStateFlow<List<EmergencyContact>>(emptyList())
    val contacts: StateFlow<List<EmergencyContact>> = _contacts.asStateFlow()

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var sensorManager: SensorManager
    private var shakeDetector: ShakeDetector? = null

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.value = true
            loadServices()
        }
        override fun onLost(network: Network) {
            _isOnline.value = false
        }
    }

    init {
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerNetworkCallback(NetworkRequest.Builder().build(), networkCallback)
        
        setupShakeDetector()
        loadContacts()
        loadServices()
    }

    private fun setupShakeDetector() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer != null) {
            shakeDetector = ShakeDetector {
                triggerSos()
            }
            sensorManager.registerListener(shakeDetector, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onCleared() {
        super.onCleared()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        shakeDetector?.let { sensorManager.unregisterListener(it) }
    }

    fun loadContacts() {
        _contacts.value = dbHelper.getContacts()
    }

    fun addContact(name: String, phone: String) {
        dbHelper.addContact(EmergencyContact(name = name, phoneNumber = phone))
        loadContacts()
    }

    fun deleteContact(id: Long) {
        dbHelper.deleteContact(id)
        loadContacts()
    }

    fun triggerSos() {
        viewModelScope.launch {
            val location = getBestLocation()
            val mapsLink = if (location != null) {
                "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
            } else "Location unknown"

            val message = "I NEED HELP! My current location: $mapsLink"
            
            val savedContacts = dbHelper.getContacts()
            if (savedContacts.isEmpty()) {
                Log.w("SOS", "No emergency contacts saved!")
            }

            savedContacts.forEach { contact ->
                sendSms(contact.phoneNumber, message)
            }
        }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager: SmsManager = context.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("SOS", "SMS sent to $phoneNumber")
        } catch (e: Exception) {
            Log.e("SOS", "Failed to send SMS to $phoneNumber", e)
        }
    }

    fun loadServices() = viewModelScope.launch {
        if (!hasLocationPermission()) {
            _uiState.value = EmergencyUiState.NoPermission
            return@launch
        }

        if (repository.hasCachedData()) {
            _uiState.value = EmergencyUiState.Success(
                services    = repository.loadCachedServices(),
                isFromCache = true
            )
        } else {
            _uiState.value = EmergencyUiState.Loading
        }

        if (!_isOnline.value) {
            if (!repository.hasCachedData()) _uiState.value = EmergencyUiState.Offline
            return@launch
        }

        val location = withTimeoutOrNull(5_000L) { getBestLocation() }
        if (location == null) {
            if (_uiState.value !is EmergencyUiState.Success) {
                _uiState.value = EmergencyUiState.GpsOff
            }
            return@launch
        }

        lastLatitude = location.latitude
        lastLongitude = location.longitude

        try {
            if (shouldRefresh(location)) {
                lastLocation = location
                val fresh = repository.fetchNearbyServices(location)
                if (fresh.isNotEmpty()) {
                    _uiState.value = EmergencyUiState.Success(services = fresh, isFromCache = false)
                }
            }
        } catch (e: Exception) {
            Log.e("ViewModel", "Failed to fetch services", e)
        }
    }

    private suspend fun getBestLocation(): Location? {
        val last = getLastKnownLocation()
        if (last != null) return last
        return getFreshLocation()
    }

    private suspend fun getLastKnownLocation(): Location? = suspendCancellableCoroutine { cont ->
        if (!hasLocationPermission()) { cont.resume(null, null); return@suspendCancellableCoroutine }
        fusedClient.lastLocation.addOnSuccessListener { cont.resume(it, null) }.addOnFailureListener { cont.resume(null, null) }
    }

    private suspend fun getFreshLocation(): Location? = suspendCancellableCoroutine { cont ->
        if (!hasLocationPermission()) { cont.resume(null, null); return@suspendCancellableCoroutine }
        val cts = CancellationTokenSource()
        cont.invokeOnCancellation { cts.cancel() }
        fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
            .addOnSuccessListener { cont.resume(it, null) }
            .addOnFailureListener { cont.resume(null, null) }
    }

    private fun shouldRefresh(newLocation: Location): Boolean {
        val previous = lastLocation ?: return true
        return previous.distanceTo(newLocation) > 1000
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun checkNetworkStatus(): Boolean {
        val cm   = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}