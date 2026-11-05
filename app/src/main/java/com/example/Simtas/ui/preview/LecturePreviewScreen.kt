package com.example.Simtas.ui.preview

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted

val PlayerCardBg = Color(0xFFF1EDFE)
val MicListeningRed = Color(0xFFDC2626)

/**
 * Screen 2: Lecture Preview with Real-Time Voice-to-Text and Manual Text Input.
 * Adheres strictly to MVVM architecture and the SmartNotes AI design language.
 */
@Composable
fun LecturePreviewScreen(
    modifier: Modifier = Modifier,
    audioPath: String? = null,
    viewModel: LecturePreviewViewModel = remember { LecturePreviewViewModel() },
    onBackClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onGenerateTranscriptClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(audioPath) {
        viewModel.loadAudio(audioPath, context)
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenDisposed()
        }
    }

    // Microphone permission launcher for voice input on Screen 2
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceInput(context)
        }
    }

    val handleMicClick = {
        if (uiState.isListening) {
            viewModel.stopVoiceInput(context)
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                viewModel.startVoiceInput(context)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LecturePreviewScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = {
            viewModel.onScreenDisposed()
            onBackClick()
        },
        onMoreOptionsClick = onMoreOptionsClick,
        onLectureTitleChange = viewModel::onLectureTitleChanged,
        onSubjectSelect = viewModel::onSubjectSelected,
        onPlayPauseToggle = viewModel::togglePlayPause,
        onRewind10s = viewModel::onRewind10s,
        onForward10s = viewModel::onForward10s,
        onSpeedClick = viewModel::onSpeedClicked,
        onMicClick = handleMicClick,
        onRecognizedTextChange = viewModel::onRecognizedTextChanged,
        onClearTextClick = viewModel::onClearTextClicked,
        onLanguageSelect = viewModel::onLanguageSelected,
        onTranscribeAudioClick = { viewModel.transcribeLoadedAudio(context) },
        onGenerateTranscriptClick = {
            if (viewModel.onGenerateTranscriptClicked()) {
                onGenerateTranscriptClick()
            }
        }
    )
}

@Composable
fun LecturePreviewScreenContent(
    uiState: LecturePreviewUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onLectureTitleChange: (String) -> Unit = {},
    onSubjectSelect: (String) -> Unit = {},
    onPlayPauseToggle: () -> Unit = {},
    onRewind10s: () -> Unit = {},
    onForward10s: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onMicClick: () -> Unit = {},
    onRecognizedTextChange: (String) -> Unit = {},
    onClearTextClick: () -> Unit = {},
    onLanguageSelect: (String) -> Unit = {},
    onTranscribeAudioClick: () -> Unit = {},
    onGenerateTranscriptClick: () -> Unit = {}
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
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Top Bar
                TopNavigationBar(
                    onBackClick = onBackClick,
                    onMoreOptionsClick = onMoreOptionsClick
                )

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFDC2626)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Subject Badge Pill
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(InnerCircleBg)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = uiState.subjectBadge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurplePrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Lecture Title
                Text(
                    text = uiState.lectureTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata Subtitle
                Text(
                    text = uiState.dateDurationCodeText,
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Audio Player Card Container (when audio exists)
                if (uiState.hasAudioFile) {
                    AudioPlayerCard(
                        statusText = uiState.statusText,
                        currentTimeText = uiState.currentTimeText,
                        totalTimeText = uiState.totalTimeText,
                        isPlaying = uiState.isPlaying,
                        speedText = uiState.playbackSpeedText,
                        playbackProgress = uiState.playbackProgress,
                        onPlayPauseToggle = onPlayPauseToggle,
                        onRewind10s = onRewind10s,
                        onForward10s = onForward10s,
                        onSpeedClick = onSpeedClick
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                }

                // --- ChatGPT-Style Voice & Text Input Section ---
                VoiceAndTextInputCard(
                    uiState = uiState,
                    onMicClick = onMicClick,
                    onRecognizedTextChange = onRecognizedTextChange,
                    onClearTextClick = onClearTextClick,
                    onLanguageSelect = onLanguageSelect,
                    onTranscribeAudioClick = onTranscribeAudioClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Lecture Title Field
                Text(
                    text = "Lecture title",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.lectureTitle,
                    onValueChange = onLectureTitleChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    ),
                    trailingIcon = {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawPencilEditIcon(TextMuted)
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Subject Selection Field
                Text(
                    text = "Subject",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                DropdownSelectField(
                    selectedText = uiState.selectedSubject,
                    options = uiState.subjectOptions,
                    onOptionSelected = onSubjectSelect
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Audio File Details Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        drawHeadphonesAudioIcon(PurplePrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = uiState.fileMetadataText,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Continue to Transcript CTA Button
                Button(
                    onClick = onGenerateTranscriptClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurplePrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawPadTranscriptIcon(Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Continue to Transcript",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * ChatGPT-Style Voice Input & Multiline Text Area Component
 */
@Composable
private fun VoiceAndTextInputCard(
    uiState: LecturePreviewUiState,
    onMicClick: () -> Unit,
    onRecognizedTextChange: (String) -> Unit,
    onClearTextClick: () -> Unit,
    onLanguageSelect: (String) -> Unit,
    onTranscribeAudioClick: () -> Unit
) {
    // Pulsing animation when microphone is actively listening
    val infiniteTransition = rememberInfiniteTransition(label = "micPulse")
    val micScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (uiState.isListening) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(PlayerCardBg)
            .padding(18.dp)
    ) {
        // Section Header Row: Title & Language Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        drawSparkleStarIcon(Color.White)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Speech & Text Input",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
            }

            // Language Selector Pills (English / Tamil)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageChip(
                    label = "EN",
                    isSelected = uiState.selectedLanguageCode == "en",
                    onClick = { onLanguageSelect("en") }
                )
                LanguageChip(
                    label = "தமிழ்",
                    isSelected = uiState.selectedLanguageCode == "ta",
                    onClick = { onLanguageSelect("ta") }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Voice Control Row (Microphone Button & Status)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated Mic Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .scale(micScale)
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (uiState.isListening) MicListeningRed else PurplePrimary)
                    .clickable { onMicClick() }
            ) {
                if (uiState.isTranscribing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        if (uiState.isListening) {
                            drawSquareStopIcon(Color.White)
                        } else {
                            drawMicIcon(Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        uiState.isListening -> "Listening... Speak clearly"
                        uiState.isTranscribing -> "Recognizing speech (${uiState.transcriptionProgress}%)..."
                        uiState.recognizedText.isNotBlank() -> "Speech recognized · Tap to add more"
                        else -> "Tap mic to speak · Real-time voice"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (uiState.isListening) MicListeningRed else TextDarkPrimary
                )
                Text(
                    text = when {
                        uiState.isListening -> "Tap stop button when finished speaking"
                        uiState.isTranscribing -> "Offline Sherpa-ONNX Whisper processing"
                        else -> "Offline Whisper Multilingual (${if (uiState.selectedLanguageCode == "ta") "Tamil" else "English"})"
                    },
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            // Quick Transcribe Action for Screen 1 Recording
            if (uiState.hasAudioFile && !uiState.isListening && !uiState.isTranscribing) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(InnerCircleBg)
                        .clickable { onTranscribeAudioClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (uiState.recognizedText.isBlank()) "Transcribe" else "Re-run",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Multiline Text Area Header (Label & Clear Button)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lecture Content & Questions",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )

            if (uiState.recognizedText.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onClearTextClick() }
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawTrashIcon(PurplePrimary)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Clear text",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurplePrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Multiline Editable Text Area
        OutlinedTextField(
            value = uiState.recognizedText,
            onValueChange = onRecognizedTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 260.dp),
            placeholder = {
                Text(
                    text = "Spoken words will appear here.\n\nYou can also type lecture notes, questions, or edit recognized text at any time...",
                    fontSize = 13.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                disabledContainerColor = Color.White,
                focusedBorderColor = PurplePrimary,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextDarkPrimary,
                unfocusedTextColor = TextDarkPrimary
            ),
            singleLine = false
        )
    }
}

@Composable
private fun LanguageChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) PurplePrimary else Color.White)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else TextDarkPrimary
        )
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
                text = "Lecture preview",
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
 * Audio Player Card Container Component
 */
@Composable
private fun AudioPlayerCard(
    statusText: String,
    currentTimeText: String,
    totalTimeText: String,
    isPlaying: Boolean,
    speedText: String,
    playbackProgress: Float = 0f,
    onPlayPauseToggle: () -> Unit,
    onRewind10s: () -> Unit,
    onForward10s: () -> Unit,
    onSpeedClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(PlayerCardBg)
            .padding(18.dp)
    ) {
        Column {
            // Status Pill Badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCompassClockIcon(PurplePrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurplePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Waveform Graphic
            AudioWaveformVisualizer(isPlaying = isPlaying, playbackProgress = playbackProgress)

            Spacer(modifier = Modifier.height(14.dp))

            // Timestamp Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = currentTimeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
                Text(
                    text = totalTimeText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Playback Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Rewind 10s Button
                IconButton(
                    onClick = onRewind10s,
                    modifier = Modifier.size(40.dp)
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawRewind10sIcon(TextDarkPrimary)
                    }
                }

                // Play / Pause Circular Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                        .clickable { onPlayPauseToggle() }
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        if (isPlaying) {
                            drawPauseIcon(Color.White)
                        } else {
                            drawPlayIcon(Color.White)
                        }
                    }
                }

                // Forward 10s Button
                IconButton(
                    onClick = onForward10s,
                    modifier = Modifier.size(40.dp)
                ) {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        drawForward10sIcon(TextDarkPrimary)
                    }
                }

                // Speed Selector Pill
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .clickable { onSpeedClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = speedText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                }
            }
        }
    }
}

/**
 * Audio Waveform Visualizer
 */
@Composable
private fun AudioWaveformVisualizer(isPlaying: Boolean, playbackProgress: Float = 0f) {
    val heights = listOf(
        0.3f, 0.45f, 0.6f, 0.75f, 0.9f, 0.65f, 0.85f, 1.0f,
        0.8f, 0.95f, 0.7f, 0.85f, 0.45f, 0.35f, 0.5f, 0.3f,
        0.4f, 0.25f, 0.35f, 0.2f
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
    ) {
        val strokeWidth = 3.5.dp.toPx()
        val step = size.width / (heights.size + 1)
        val activeCount = if (playbackProgress > 0f) {
            (heights.size * playbackProgress).toInt()
        } else if (isPlaying) {
            10
        } else {
            0
        }

        heights.forEachIndexed { index, h ->
            val x = step * (index + 1)
            val hPx = size.height * h
            val startY = (size.height - hPx) / 2
            val color = if (index <= activeCount) PurplePrimary else Color.White.copy(alpha = 0.8f)

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
 * Dropdown Selection Field
 */
@Composable
private fun DropdownSelectField(
    selectedText: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = selectedText,
                    fontSize = 14.sp,
                    color = TextDarkPrimary
                )

                Canvas(modifier = Modifier.size(14.dp)) {
                    drawChevronDown(TextMuted)
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color.White)
                .clip(RoundedCornerShape(12.dp))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            fontSize = 14.sp,
                            color = TextDarkPrimary
                        )
                    },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- Custom Canvas Graphics Helpers ---

private fun DrawScope.drawMicIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.35f, size.height * 0.15f),
        size = Size(size.width * 0.3f, size.height * 0.42f),
        cornerRadius = CornerRadius(size.width * 0.15f),
        style = stroke
    )
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(size.width * 0.22f, size.height * 0.28f),
        size = Size(size.width * 0.56f, size.height * 0.38f),
        style = stroke
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.5f, size.height * 0.66f),
        end = Offset(size.width * 0.5f, size.height * 0.82f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.32f, size.height * 0.82f),
        end = Offset(size.width * 0.68f, size.height * 0.82f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawSquareStopIcon(color: Color) {
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.3f, size.height * 0.3f),
        size = Size(size.width * 0.4f, size.height * 0.4f),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
}

private fun DrawScope.drawTrashIcon(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(
        color = color,
        start = Offset(size.width * 0.2f, size.height * 0.25f),
        end = Offset(size.width * 0.8f, size.height * 0.25f),
        strokeWidth = 1.5.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.28f, size.height * 0.25f),
        size = Size(size.width * 0.44f, size.height * 0.62f),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = stroke
    )
}

private fun DrawScope.drawSparkleStarIcon(color: Color) {
    val path = Path().apply {
        val cx = size.width / 2
        val cy = size.height / 2
        val r = size.width / 2.2f
        moveTo(cx, cy - r)
        quadraticTo(cx, cy, cx + r, cy)
        quadraticTo(cx, cy, cx, cy + r)
        quadraticTo(cx, cy, cx - r, cy)
        quadraticTo(cx, cy, cx, cy - r)
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawCompassClockIcon(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = size.width * 0.42f, center = Offset(size.width / 2, size.height / 2), style = stroke)
    drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.5f), end = Offset(size.width * 0.5f, size.height * 0.25f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.5f), end = Offset(size.width * 0.72f, size.height * 0.5f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawRewind10sIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawArc(color = color, startAngle = 40f, sweepAngle = 280f, useCenter = false, topLeft = Offset(size.width * 0.15f, size.height * 0.15f), size = Size(size.width * 0.7f, size.height * 0.7f), style = stroke)
}

private fun DrawScope.drawForward10sIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawArc(color = color, startAngle = -100f, sweepAngle = 280f, useCenter = false, topLeft = Offset(size.width * 0.15f, size.height * 0.15f), size = Size(size.width * 0.7f, size.height * 0.7f), style = stroke)
}

private fun DrawScope.drawPlayIcon(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.3f, size.height * 0.2f)
        lineTo(size.width * 0.8f, size.height * 0.5f)
        lineTo(size.width * 0.3f, size.height * 0.8f)
        close()
    }
    drawPath(path = path, color = color)
}

private fun DrawScope.drawPauseIcon(color: Color) {
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.2f), end = Offset(size.width * 0.35f, size.height * 0.8f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.65f, size.height * 0.2f), end = Offset(size.width * 0.65f, size.height * 0.8f), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawPencilEditIcon(color: Color) {
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.8f), end = Offset(size.width * 0.8f, size.height * 0.2f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawHeadphonesAudioIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val arcPath = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.6f)
        quadraticTo(
            size.width * 0.2f, size.height * 0.2f,
            size.width * 0.5f, size.height * 0.2f
        )
        quadraticTo(
            size.width * 0.8f, size.height * 0.2f,
            size.width * 0.8f, size.height * 0.6f
        )
    }
    drawPath(path = arcPath, color = color, style = stroke)
    drawRoundRect(color = color, topLeft = Offset(size.width * 0.15f, size.height * 0.55f), size = Size(size.width * 0.15f, size.height * 0.3f), cornerRadius = CornerRadius(2.dp.toPx()))
    drawRoundRect(color = color, topLeft = Offset(size.width * 0.7f, size.height * 0.55f), size = Size(size.width * 0.15f, size.height * 0.3f), cornerRadius = CornerRadius(2.dp.toPx()))
}

private fun DrawScope.drawPadTranscriptIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.2f, size.height * 0.15f),
        size = Size(size.width * 0.6f, size.height * 0.7f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = stroke
    )
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.4f), end = Offset(size.width * 0.65f, size.height * 0.4f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.6f), end = Offset(size.width * 0.55f, size.height * 0.6f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawThreeDotsMenu(color: Color) {
    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width * 0.25f, size.height * 0.5f))
    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width * 0.5f, size.height * 0.5f))
    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width * 0.75f, size.height * 0.5f))
}

private fun DrawScope.drawChevronDown(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.35f)
        lineTo(size.width * 0.5f, size.height * 0.65f)
        lineTo(size.width * 0.8f, size.height * 0.35f)
    }
    drawPath(path = path, color = color, style = stroke)
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
fun LecturePreviewScreenPreview() {
    AIBasedClassNotesAppTheme {
        LecturePreviewScreenContent(uiState = LecturePreviewUiState())
    }
}
