package com.emergency.finder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.emergency.finder.screens.EmergencyApp
import com.emergency.finder.ui.theme.EmergencyFinderTheme
import com.google.android.libraries.places.api.Places

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Google Places API
        if (Places.isInitialized() == false) {
            Places.initialize(
                applicationContext,
                "AIzaSyAcN_S3IsFh0T_aAVgxPDeSwg-73LhEPPw"
            )
        }

        enableEdgeToEdge()

        setContent {
            EmergencyFinderTheme {
                EmergencyApp()
            }
        }
    }
}