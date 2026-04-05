package com.editor.app.ui.screens

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.editor.app.ui.theme.*
import com.editor.app.viewmodel.EditorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceoverSheet(
    viewModel: EditorViewModel,
    onDismiss: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Pulsing animation for record button
    val pulseAnim = rememberInfiniteTransition(label = "pulse")
    val scale by pulseAnim.animateFloat(
        initialValue = 1f,
        targetValue = if (state.isRecording) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Amplitude bar count — maps amplitude to 1–20
    val barCount = if (state.isRecording) {
        (state.recordingAmplitude / 1638).coerceIn(1, 20)
    } else 0

    ModalBottomSheet(
        onDismissRequest = {
            if (state.isRecording) viewModel.stopRecording()
            onDismiss()
        },
        containerColor = CardDark,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Voiceover",
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary
                )
                IconButton(onClick = {
                    if (state.isRecording) viewModel.stopRecording()
                    onDismiss()
                }) {
                    Icon(Icons.Default.Close, "Close", tint = TextSecondary)
                }
            }

            // Live waveform visualizer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(CardDarker, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.isRecording && barCount > 0) {
                    repeat(30) { i ->
                        val isActive = i < barCount
                        val barHeight = if (isActive) {
                            ((Math.sin(i * 0.5) * 0.5 + 0.5) * 48 + 8).dp
                        } else 8.dp

                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(barHeight)
                                .background(
                                    if (isActive) AccentOrange else AccentOrange.copy(0.15f),
                                    RoundedCornerShape(2.dp)
                                )
                        )
                        Spacer(Modifier.width(3.dp))
                    }
                } else {
                    Text(
                        if (state.isRecording) "Listening..." else "Tap record to start",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            // Recording duration timer
            Text(
                formatRecordingTime(state.recordingDurationMs),
                style = MaterialTheme.typography.headlineLarge,
                color = if (state.isRecording) AccentOrange else TextSecondary,
                fontWeight = FontWeight.Bold
            )

            // Record / Stop button
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .scale(scale)
                    .background(
                        if (state.isRecording) NeonPink else AccentOrange,
                        CircleShape
                    )
                    .border(3.dp, Color.White.copy(0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (state.isRecording) viewModel.stopRecording()
                        else viewModel.startRecording()
                    },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        if (state.isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (state.isRecording) "Stop" else "Record",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Text(
                if (state.isRecording) "Tap to stop recording" else "Tap mic to record",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            // Info row
            HorizontalDivider(color = Color(0xFF22223A))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Default.Info, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Text(
                    "Recording saves to Voiceover track at current position",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

fun formatRecordingTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    val tenths = (ms % 1000) / 100
    return "%02d:%02d.%d".format(min, sec, tenths)
}