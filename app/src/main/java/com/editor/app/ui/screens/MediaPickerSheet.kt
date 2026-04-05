package com.editor.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.editor.app.ui.theme.*
import com.editor.app.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPickerSheet(
    viewModel: EditorViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    // ─── Three separate launchers ─────────────────────────────────────────────

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            // Grant persistent read permission
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            viewModel.importVideoClip(context, uri)
            onDismiss()
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Some audio URIs don't support persistable permissions — safe to ignore
            }
            viewModel.importAudioClip(context, uri)
            onDismiss()
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Safe to ignore
            }
            viewModel.importImageClip(context, uri)
            onDismiss()
        }
    }

    // ─── Bottom Sheet UI ──────────────────────────────────────────────────────

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Import Media",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        "Add clips to your timeline",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                }
            }

            HorizontalDivider(color = Color(0xFF22223A))

            // Video option
            ImportCard(
                label = "Video",
                description = "MP4, MOV, AVI — adds to Video track",
                icon = Icons.Default.VideoLibrary,
                color = NeonCyan,
                onClick = { videoPicker.launch("video/*") }
            )

            // Audio option
            ImportCard(
                label = "Audio",
                description = "MP3, WAV, AAC — adds to Music track",
                icon = Icons.Default.AudioFile,
                color = NeonPurple,
                onClick = { audioPicker.launch("audio/*") }
            )

            // Image option
            ImportCard(
                label = "Image",
                description = "JPG, PNG — added as 5-second clip",
                icon = Icons.Default.PhotoLibrary,
                color = NeonPink,
                onClick = { imagePicker.launch("image/*") }
            )

            HorizontalDivider(color = Color(0xFF22223A))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    "Clips are placed after the last clip on the track",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun ImportCard(
    label: String,
    description: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(0.05f), RoundedCornerShape(14.dp))
            .border(1.dp, color.copy(0.25f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(color.copy(0.12f), RoundedCornerShape(12.dp))
                .border(1.dp, color.copy(0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
    }
}