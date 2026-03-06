package com.emergency.finder.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SosButton() {

    val context = LocalContext.current

    FloatingActionButton(
        onClick = {
            context.startActivity(
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
            )
        },
        containerColor = Color.Red,
        contentColor = Color.White,
        shape = CircleShape
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {

            Icon(
                Icons.Default.Call,
                contentDescription = "SOS",
                tint = Color.White
            )

            Spacer(Modifier.width(8.dp))

            Text(
                "SOS",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}