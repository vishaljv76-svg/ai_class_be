package com.example.Simtas.ui.record

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.TextSubtitle

val RecordingRed = Color(0xFFD32F2F)
val InactiveButtonBg = Color(0xFFF5F3FF)
val InactiveButtonText = Color(0xFF9B8EDB)

/**
 * Record Lecture Screen adhering to MVVM architecture.
 * Supports start/pause/resume/stop recording, audio permission handling,
 * and navigating to lecture preview with the recorded audio.
 */
@Composable
fun RecordLectureScreen(
    modifier: Modifier = Modifier,
    viewModel: RecordLectureViewModel = remember { RecordLectureViewModel() },
    onBackClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onStopRecordingClick: () -> Unit = {},
    onNextClick: (String) -> Unit = { onStopRecordingClick() }
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Safely release recorder if user navigates away
    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenDisposed()
        }
    }

    // Runtime microphone permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startRecording(context)
        } else {
            viewModel.onPermissionDenied()
        }
    }

    val handleStartRecording = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startRecording(context)
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    RecordLectureScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onMoreOptionsClick = onMoreOptionsClick,
        onStartClick = handleStartRecording,
        onPauseClick = viewModel::onPauseClicked,
        onResumeClick = viewModel::onResumeClicked,
        onStopClick = {
            viewModel.onStopClicked(context)
        },
        onNextClick = {
            val path = uiState.recordedFilePath ?: SessionManager.getLastRecordedAudioPath() ?: ""
            onNextClick(path)
        },
        onMicCircleClick = {
            when (uiState.status) {
                RecordingStatus.IDLE, RecordingStatus.STOPPED -> handleStartRecording()
                RecordingStatus.RECORDING -> viewModel.onPauseClicked()
                RecordingStatus.PAUSED -> viewModel.onResumeClicked()
            }
        }
    )
}

@Composable
fun RecordLectureScreenContent(
    uiState: RecordLectureUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onStartClick: () -> Unit = {},
    onPauseClick: () -> Unit = {},
    onResumeClick: () -> Unit = {},
    onStopClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onMicCircleClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = SplashBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Top Bar
                TopNavigationBar(
                    onBackClick = onBackClick,
                    onMoreOptionsClick = onMoreOptionsClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Course Code Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(InnerCircleBg)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = uiState.courseCode,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurplePrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Lecture Title
                Text(
                    text = uiState.lectureTitle,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Professor & Date Subtitle
                Text(
                    text = uiState.professorAndDate,
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Central Microphone Visual Halo Circle (Interactive Clickable)
                val isRecording = uiState.status == RecordingStatus.RECORDING
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val haloScale by infiniteTransition.animateFloat(
                    initialValue = 1.0f,
                    targetValue = if (isRecording) 1.08f else 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(900),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "haloScale"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(160.dp)
                        .scale(haloScale)
                        .clip(CircleShape)
                        .background(if (isRecording) InnerCircleBg.copy(alpha = 0.8f) else InnerCircleBg)
                        .clickable { onMicCircleClick() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(108.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) RecordingRed else PurplePrimary)
                    ) {
                        Canvas(modifier = Modifier.size(36.dp)) {
                            drawMicrophoneLargeIcon(Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Timer
                Text(
                    text = uiState.timerText,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Recording Status Dot + Text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val dotColor = when (uiState.status) {
                        RecordingStatus.RECORDING -> RecordingRed
                        RecordingStatus.PAUSED -> Color(0xFFF59E0B)
                        RecordingStatus.STOPPED -> Color(0xFF10B981)
                        RecordingStatus.IDLE -> TextMuted
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = uiState.statusText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = dotColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Live Audio Waveform Visualizer
                LiveAudioWaveformVisualizer(
                    isRecording = uiState.status == RecordingStatus.RECORDING
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = uiState.micConnectedText,
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                // Error message banner if any error occurred
                if (!uiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Recording Control Action Buttons Row
                RecordingControlButtonsRow(
                    status = uiState.status,
                    onStartClick = onStartClick,
                    onPauseClick = onPauseClick,
                    onResumeClick = onResumeClick,
                    onStopClick = onStopClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = when (uiState.status) {
                        RecordingStatus.IDLE -> "Tap Start or Microphone to begin recording"
                        RecordingStatus.RECORDING -> "Pause to pause timer · Stop finishes and saves audio"
                        RecordingStatus.PAUSED -> "Resume to continue recording · Stop saves audio"
                        RecordingStatus.STOPPED -> "Audio saved locally · Tap Next to review preview"
                    },
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Next Button (Enabled only after valid recording saved)
                val isNextEnabled = uiState.status == RecordingStatus.STOPPED &&
                        !uiState.recordedFilePath.isNullOrBlank() &&
                        uiState.fileSizeBytes > 0L
                val nextButtonBg = if (isNextEnabled) PurplePrimary else Color(0xFFD8D2EB)
                val nextButtonTextColor = if (isNextEnabled) Color.White else Color(0xFF8B80B6)

                Button(
                    onClick = onNextClick,
                    enabled = isNextEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = nextButtonBg,
                        disabledContainerColor = Color(0xFFE8E5F3)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (uiState.isUploading) "Uploading audio..." else "Next",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = nextButtonTextColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawRightArrow(nextButtonTextColor)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isNextEnabled) {
                        "Recorded audio ready (${uiState.formattedFileSize}) · Proceed to preview"
                    } else {
                        "Next is available after you stop and save a recording"
                    },
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Tip Card at Bottom
            TipInfoCard(
                text = uiState.tipText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

/**
 * Top Navigation Bar with Back Arrow, Title, and Overflow Menu
 */
@Composable
private fun TopNavigationBar(
    onBackClick: () -> Unit,
    onMoreOptionsClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(36.dp)
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawBackArrow(TextDarkPrimary)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "Record lecture",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
        }

        IconButton(
            onClick = onMoreOptionsClick,
            modifier = Modifier.size(36.dp)
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                drawThreeDotsMenu(TextDarkPrimary)
            }
        }
    }
}

/**
 * Live Audio Waveform Visualizer
 */
@Composable
private fun LiveAudioWaveformVisualizer(isRecording: Boolean) {
    val heights = listOf(
        0.3f, 0.45f, 0.6f, 0.75f, 0.9f, 0.65f, 0.85f, 1.0f,
        0.8f, 0.95f, 0.7f, 0.85f, 0.45f, 0.35f, 0.5f, 0.3f,
        0.4f, 0.25f, 0.35f, 0.2f
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .height(48.dp)
    ) {
        val strokeWidth = 3.5.dp.toPx()
        val step = size.width / (heights.size + 1)

        heights.forEachIndexed { index, h ->
            val x = step * (index + 1)
            val hPx = size.height * h
            val startY = (size.height - hPx) / 2
            val color = if (index < 12 && isRecording) PurplePrimary else InnerCircleBg

            drawLine(
                color = color,
                start = Offset(x, startY),
                end = Offset(x, startY + hPx),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Control Action Buttons Row
 * Dynamically presents Start Recording when IDLE/STOPPED,
 * and Pause / Resume / Stop when actively recording or paused.
 */
@Composable
private fun RecordingControlButtonsRow(
    status: RecordingStatus,
    onStartClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    onStopClick: () -> Unit
) {
    when (status) {
        RecordingStatus.IDLE, RecordingStatus.STOPPED -> {
            // Prominent Start Recording CTA
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(PurplePrimary)
                    .clickable { onStartClick() }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        drawMicrophoneIcon(Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (status == RecordingStatus.STOPPED) "Record Again" else "Start Recording",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        RecordingStatus.RECORDING, RecordingStatus.PAUSED -> {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pause / Resume Button
                if (status == RecordingStatus.RECORDING) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(InnerCircleBg)
                            .clickable { onPauseClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                drawPauseIcon(PurplePrimary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pause",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary
                            )
                        }
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(InnerCircleBg)
                            .clickable { onResumeClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(14.dp)) {
                                drawPlayIcon(PurplePrimary)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Resume",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary
                            )
                        }
                    }
                }

                // Stop Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PurplePrimary)
                        .clickable { onStopClick() }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawSquareStopIcon(Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Stop",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Bottom Tip Card
 */
@Composable
private fun TipInfoCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEEF3FF))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(20.dp)) {
                drawLightbulbIcon(PurplePrimary)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
                color = TextSubtitle,
                lineHeight = 18.sp
            )
        }
    }
}

// --- Custom Canvas Graphics Helpers ---

private fun DrawScope.drawMicrophoneLargeIcon(color: Color) {
    val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.35f, size.height * 0.15f),
        size = Size(size.width * 0.3f, size.height * 0.45f),
        cornerRadius = CornerRadius(6.dp.toPx()),
        style = stroke
    )
    val path = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.45f)
        quadraticTo(
            size.width * 0.2f, size.height * 0.75f,
            size.width * 0.5f, size.height * 0.75f
        )
        quadraticTo(
            size.width * 0.8f, size.height * 0.75f,
            size.width * 0.8f, size.height * 0.45f
        )
    }
    drawPath(path = path, color = color, style = stroke)
    drawLine(
        color = color,
        start = Offset(size.width * 0.5f, size.height * 0.75f),
        end = Offset(size.width * 0.5f, size.height * 0.9f),
        strokeWidth = 2.5.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawMicrophoneIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.35f, size.height * 0.15f),
        size = Size(size.width * 0.3f, size.height * 0.45f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
    val path = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.45f)
        quadraticTo(
            size.width * 0.2f, size.height * 0.75f,
            size.width * 0.5f, size.height * 0.75f
        )
        quadraticTo(
            size.width * 0.8f, size.height * 0.75f,
            size.width * 0.8f, size.height * 0.45f
        )
    }
    drawPath(path = path, color = color, style = stroke)
    drawLine(
        color = color,
        start = Offset(size.width * 0.5f, size.height * 0.75f),
        end = Offset(size.width * 0.5f, size.height * 0.9f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawPauseIcon(color: Color) {
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.2f), end = Offset(size.width * 0.35f, size.height * 0.8f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.65f, size.height * 0.2f), end = Offset(size.width * 0.65f, size.height * 0.8f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawPlayIcon(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.25f, size.height * 0.2f)
        lineTo(size.width * 0.85f, size.height * 0.5f)
        lineTo(size.width * 0.25f, size.height * 0.8f)
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawSquareStopIcon(color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.2f, size.height * 0.2f),
        size = Size(size.width * 0.6f, size.height * 0.6f),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
}

private fun DrawScope.drawRightArrow(color: Color) {
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(
        color = color,
        start = Offset(size.width * 0.15f, size.height * 0.5f),
        end = Offset(size.width * 0.85f, size.height * 0.5f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    val path = Path().apply {
        moveTo(size.width * 0.55f, size.height * 0.2f)
        lineTo(size.width * 0.85f, size.height * 0.5f)
        lineTo(size.width * 0.55f, size.height * 0.8f)
    }
    drawPath(path = path, color = color, style = stroke)
}

private fun DrawScope.drawLightbulbIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = size.width * 0.32f, center = Offset(size.width * 0.5f, size.height * 0.4f), style = stroke)
    drawLine(color = color, start = Offset(size.width * 0.38f, size.height * 0.75f), end = Offset(size.width * 0.62f, size.height * 0.75f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.42f, size.height * 0.88f), end = Offset(size.width * 0.58f, size.height * 0.88f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawThreeDotsMenu(color: Color) {
    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width * 0.25f, size.height * 0.5f))
    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width * 0.5f, size.height * 0.5f))
    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width * 0.75f, size.height * 0.5f))
}

private fun DrawScope.drawBackArrow(color: Color) {
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(
        color = color,
        start = Offset(size.width * 0.85f, size.height * 0.5f),
        end = Offset(size.width * 0.15f, size.height * 0.5f),
        strokeWidth = 2.dp.toPx(),
        cap = StrokeCap.Round
    )
    val path = Path().apply {
        moveTo(size.width * 0.45f, size.height * 0.2f)
        lineTo(size.width * 0.15f, size.height * 0.5f)
        lineTo(size.width * 0.45f, size.height * 0.8f)
    }
    drawPath(path = path, color = color, style = stroke)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RecordLectureScreenPreview() {
    AIBasedClassNotesAppTheme {
        RecordLectureScreenContent(uiState = RecordLectureUiState())
    }
}
