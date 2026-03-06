package com.emergency.finder.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emergency.finder.models.EmergencyService
import com.emergency.finder.viewmodel.EmergencyUiState
import com.emergency.finder.viewmodel.EmergencyViewModel

// App colour palette
private val ColorRed    = Color(0xFFE53935)
private val ColorDarkRed= Color(0xFFB71C1C)
private val ColorOrange = Color(0xFFFF7043)
private val ColorBlue   = Color(0xFF1E88E5)
private val ColorCardBg = Color(0xFFFAFAFA)
private val ColorAppBg  = Color(0xFFF2F4F8)

// =============================================================================
//  ROOT COMPOSABLE — entry point called from MainActivity
// =============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyApp(vm: EmergencyViewModel = viewModel()) {

    val context  = LocalContext.current
    val uiState  by vm.uiState.collectAsState()
    val isOnline by vm.isOnline.collectAsState()

    // Launches the system permission dialog
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (granted) vm.loadServices()
    }

    Scaffold(
        containerColor = ColorAppBg,
        floatingActionButton = {
            SosButton()
        },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text       = "Emergency Finder",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 18.sp
                        )
                        Text(
                            text     = if (isOnline) "● Online  —  live results"
                            else          "● Offline  —  cached data",
                            fontSize = 11.sp,
                            color    = if (isOnline) Color(0xFF66BB6A) else Color(0xFFEF5350)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = ColorDarkRed,
                    titleContentColor = Color.White
                ),
                actions = {
                    // Quick SOS button in the toolbar
                    IconButton(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }) {
                        Icon(Icons.Default.Call, contentDescription = "SOS Call", tint = Color.White)
                    }
                    // Refresh button
                    IconButton(onClick = { vm.loadServices() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // Offline warning banner (shown below toolbar when no internet)
            if (!isOnline) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ColorOrange)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector         = Icons.Default.WifiOff,
                        contentDescription  = null,
                        tint                = Color.White,
                        modifier            = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = "No internet connection  —  showing cached data",
                        color    = Color.White,
                        fontSize = 13.sp
                    )
                }
            }

            // Animate between different UI states smoothly
            AnimatedContent(
                targetState  = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label        = "uiStateTransition"
            ) { state ->
                when (state) {

                    is EmergencyUiState.Idle -> {
                        // Nothing to show yet, blank screen
                    }

                    is EmergencyUiState.Loading -> {
                        LoadingScreen()
                    }

                    is EmergencyUiState.NoPermission -> {
                        InfoScreen(
                            icon    = "📍",
                            title   = "Location Permission Required",
                            message = "We need your location to find nearby hospitals, " +
                                    "police stations, and fire stations.",
                            btnText = "Grant Permission"
                        ) {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }

                    is EmergencyUiState.GpsOff -> {
                        InfoScreen(
                            icon    = "🛰️",
                            title   = "GPS is Turned Off",
                            message = "Please enable Location / GPS in your device settings " +
                                    "and tap Refresh.",
                            btnText = null
                        ) {}
                    }

                    is EmergencyUiState.Offline -> {
                        InfoScreen(
                            icon    = "📵",
                            title   = "No Internet & No Saved Data",
                            message = "You are offline and there is no previously saved data. " +
                                    "Calling national emergency number 112.",
                            btnText = "📞  Call 112  (SOS)"
                        ) {
                            context.startActivity(
                                Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }

                    is EmergencyUiState.Success -> {
                        ServiceListScreen(
                            services    = state.services,
                            isFromCache = state.isFromCache,
                            isOnline    = isOnline
                        )
                    }

                    is EmergencyUiState.Error -> {
                        InfoScreen(
                            icon    = "⚠️",
                            title   = "Something Went Wrong",
                            message = state.message,
                            btnText = "Try Again"
                        ) {
                            vm.loadServices()
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
//  SERVICE LIST SCREEN
// =============================================================================
@Composable
fun ServiceListScreen(
    services    : List<EmergencyService>,
    isFromCache : Boolean,
    isOnline    : Boolean
) {
    val context = LocalContext.current

    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        // Show a small note when data is from offline cache
        if (isFromCache) {
            item {
                Text(
                    text     = "⚠  Offline mode  —  showing last saved nearby services",
                    color    = ColorOrange,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        items(
            items = services,
            key   = { it.placeId.ifBlank { it.name } }
        ) { service ->

            ServiceCard(
                service = service,

                onCall = {

                    // If offline → call national emergency
                    if (!isOnline) {

                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                        )
                        return@ServiceCard
                    }

                    val number = service.safePhone()

                    // If hospital number exists → call hospital
                    if (number.isNotBlank()) {

                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                        )

                    } else {

                        // If number missing → search Justdial
                        val searchQuery = Uri.encode(service.name + " " + service.address)

                        val justDialUrl =
                            "https://www.justdial.com/search?q=$searchQuery"

                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(justDialUrl))
                        )
                    }
                },

                onMap = {

                    val uri = Uri.parse(
                        "google.navigation:q=${service.latitude},${service.longitude}"
                    )

                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    intent.setPackage("com.google.android.apps.maps")

                    context.startActivity(intent)
                },

                onShare = {
                    // Share location details via WhatsApp, SMS, etc.
                    val shareText =
                        "I need help!\n\n" +
                                "Nearest ${service.typeLabel()}:\n" +
                                "${service.name}\n" +
                                "${service.address}\n\n" +
                                "View on map:\n" +
                                "https://maps.google.com/?q=${service.latitude},${service.longitude}"

                    context.startActivity(
                        Intent.createChooser(
                            Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            },
                            "Send My Location"
                        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            )
        }
    }
}

// =============================================================================
//  SERVICE CARD
// =============================================================================
@Composable
fun ServiceCard(
    service : EmergencyService,
    onCall  : () -> Unit,
    onMap   : () -> Unit,
    onShare : () -> Unit
) {
    // Pick accent colour based on service type
    val accentColor = when (service.type) {
        EmergencyService.TYPE_HOSPITAL -> ColorRed
        EmergencyService.TYPE_POLICE   -> ColorBlue
        EmergencyService.TYPE_FIRE     -> ColorOrange
        else                           -> Color(0xFF43A047)
    }

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = ColorCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // ── Top row: icon + name + distance badge ──────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {

                // Coloured circle with emoji icon
                Box(
                    modifier         = Modifier
                        .size(48.dp)
                        .background(accentColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(service.typeIcon(), fontSize = 22.sp)
                }

                Spacer(Modifier.width(12.dp))

                // Name and type label
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = service.name,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 15.sp,
                        maxLines   = 2
                    )
                    Text(
                        text     = service.typeLabel(),
                        color    = accentColor,
                        fontSize = 12.sp
                    )
                }

                // Distance badge (e.g. "1.2 km")
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = accentColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text       = service.formattedDistance(),
                        color      = accentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 13.sp,
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(12.dp))

            // ── Address row ────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector        = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint               = Color.Gray,
                    modifier           = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text     = service.safeAddress(),
                    fontSize = 13.sp,
                    color    = Color.DarkGray
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Action buttons ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // 📞 Call button
                Button(
                    onClick        = onCall,
                    colors         = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape          = RoundedCornerShape(10.dp),
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Call, null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Call", fontSize = 13.sp)
                }

                // 📤 Share location button
                OutlinedButton(
                    onClick        = onShare,
                    shape          = RoundedCornerShape(10.dp),
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Share, null,
                        tint     = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Share", color = accentColor, fontSize = 13.sp)
                }

                // 📍 View on Map button (OpenStreetMap)
                OutlinedButton(
                    onClick        = onMap,
                    shape          = RoundedCornerShape(10.dp),
                    modifier       = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Icon(
                        Icons.Default.Map, null,
                        tint     = accentColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Map", color = accentColor, fontSize = 13.sp)
                }
            }
        }
    }
}

// =============================================================================
//  LOADING SCREEN
// =============================================================================
@Composable
fun LoadingScreen() {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ColorRed)
            Spacer(Modifier.height(16.dp))
            Text(
                text     = "Detecting your location...",
                color    = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

// =============================================================================
//  INFO / ERROR SCREEN  (permission denied, GPS off, offline, errors)
// =============================================================================
@Composable
fun InfoScreen(
    icon    : String,
    title   : String,
    message : String,
    btnText : String?,
    onClick : () -> Unit
) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(icon, fontSize = 60.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            text       = title,
            fontWeight = FontWeight.Bold,
            fontSize   = 18.sp,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text      = message,
            color     = Color.Gray,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center
        )
        if (btnText != null) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onClick,
                colors  = ButtonDefaults.buttonColors(containerColor = ColorRed),
                shape   = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text       = btnText,
                    color      = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}