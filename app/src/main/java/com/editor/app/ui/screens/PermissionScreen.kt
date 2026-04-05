package com.editor.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.*
import com.editor.app.ui.theme.*

fun requiredPermissions(): List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.READ_MEDIA_VIDEO)
        add(Manifest.permission.READ_MEDIA_AUDIO)
        add(Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        add(Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    add(Manifest.permission.RECORD_AUDIO)
}

data class PermissionInfo(
    val permission: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

fun permissionInfoList(): List<PermissionInfo> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(PermissionInfo(
            Manifest.permission.READ_MEDIA_VIDEO,
            "Video Files",
            "Access videos from your gallery to import into projects",
            Icons.Default.VideoLibrary
        ))
        add(PermissionInfo(
            Manifest.permission.READ_MEDIA_AUDIO,
            "Audio Files",
            "Access music and audio files for your timeline",
            Icons.Default.AudioFile
        ))
        add(PermissionInfo(
            Manifest.permission.READ_MEDIA_IMAGES,
            "Images",
            "Access photos to use as overlays or thumbnails",
            Icons.Default.PhotoLibrary
        ))
    } else {
        add(PermissionInfo(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            "Storage",
            "Access media files stored on your device",
            Icons.Default.Storage
        ))
    }
    add(PermissionInfo(
        Manifest.permission.RECORD_AUDIO,
        "Microphone",
        "Record voiceovers directly inside the editor",
        Icons.Default.Mic
    ))
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(onPermissionsGranted: () -> Unit) {
    val permissions = requiredPermissions()
    val multiplePermissionsState = rememberMultiplePermissionsState(permissions)

    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (multiplePermissionsState.allPermissionsGranted) {
            onPermissionsGranted()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(NeonCyan.copy(0.1f), CircleShape)
                    .border(2.dp, NeonCyan.copy(0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.VideoCall,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(40.dp)
                )
            }

            // Title
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "VIBE EDIT",
                    style = MaterialTheme.typography.headlineLarge,
                    color = NeonCyan,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    "Grant permissions to get started",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }

            // Permission cards
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                permissionInfoList().forEach { info ->
                    val isGranted = multiplePermissionsState.permissions
                        .find { it.permission == info.permission }
                        ?.status?.isGranted ?: false
                    PermissionCard(info = info, isGranted = isGranted)
                }
            }

            // Grant button
            Button(
                onClick = { multiplePermissionsState.launchMultiplePermissionRequest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(14.dp),
                enabled = !multiplePermissionsState.allPermissionsGranted
            ) {
                Icon(Icons.Default.Security, null, tint = DeepBlack, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Grant Permissions",
                    color = DeepBlack,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Skip
            TextButton(onClick = onPermissionsGranted) {
                Text(
                    "Skip for now (limited features)",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun PermissionCard(info: PermissionInfo, isGranted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isGranted) NeonCyan.copy(0.05f) else CardDark,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (isGranted) NeonCyan.copy(0.3f) else Color(0xFF22223A),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    if (isGranted) NeonCyan.copy(0.15f) else Color(0xFF1A1A2A),
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                info.icon,
                contentDescription = null,
                tint = if (isGranted) NeonCyan else TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(info.title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Text(info.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }

        if (isGranted) {
            Icon(Icons.Default.CheckCircle, "Granted", tint = NeonCyan, modifier = Modifier.size(22.dp))
        } else {
            Icon(Icons.Default.RadioButtonUnchecked, "Not granted", tint = TextSecondary, modifier = Modifier.size(22.dp))
        }
    }
}