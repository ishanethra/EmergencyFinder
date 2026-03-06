package com.emergency.finder.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

/**
 * NetworkChangeReceiver
 *
 * Automatically triggered by Android when WiFi or mobile data
 * turns ON or OFF. It then sends a local broadcast so the
 * ViewModel can react (refresh data when back online, show
 * offline warning when disconnected).
 */
class NetworkChangeReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION          = "com.emergency.finder.CONNECTIVITY_CHANGED"
        const val EXTRA_CONNECTED = "is_connected"

        fun isConnected(context: Context): Boolean {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                    as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false)
                ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val connected = isConnected(context)
        Log.d("NetworkReceiver", "Network changed -> connected=$connected")

        // Re-broadcast locally so ViewModel can listen
        context.sendBroadcast(
            Intent(ACTION).putExtra(EXTRA_CONNECTED, connected)
        )
    }
}

/**
 * GpsStatusReceiver
 *
 * Triggered when the user turns GPS on or off in device settings.
 * Sends a local broadcast so the app can warn the user if GPS is off.
 */
class GpsStatusReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION        = "com.emergency.finder.GPS_STATUS_CHANGED"
        const val EXTRA_ENABLED = "gps_enabled"

        fun isGpsEnabled(context: Context): Boolean {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != LocationManager.PROVIDERS_CHANGED_ACTION) return
        val enabled = isGpsEnabled(context)
        Log.d("GpsReceiver", "GPS changed -> enabled=$enabled")

        context.sendBroadcast(
            Intent(ACTION).putExtra(EXTRA_ENABLED, enabled)
        )
    }
}