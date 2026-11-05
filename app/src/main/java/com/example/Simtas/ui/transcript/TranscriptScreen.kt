package com.example.Simtas.ui.transcript

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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.TextSubtitle

val MiniStripBg = Color(0xFFF1EDFE)

/**
 * Transcript Screen component adhering to MVVM architecture.
 * Supports real offline multilingual speech-to-text recognition via Whisper ONNX,
 * language selection (English, Tamil, and 90+ languages), model management, and editing.
 */
@Composable
fun TranscriptScreen(
    modifier: Modifier = Modifier,
    audioPath: String? = null,
    viewModel: TranscriptViewModel = remember { TranscriptViewModel() },
    onBackClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onGenerateNotesClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(audioPath) {
        viewModel.initContext(context, audioPath)
        if (viewModel.uiState.value.paragraphs.isEmpty() && !viewModel.uiState.value.isTranscribing) {
            viewModel.generateTranscript(context)
        }
    }

    TranscriptScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onMoreOptionsClick = onMoreOptionsClick,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onPlayPauseToggle = viewModel::togglePlayPause,
        onLanguageSelected = viewModel::onLanguageSelected,
        onDownloadModelClick = { viewModel.downloadOfflineModel(context) },
        onGenerateTranscriptClick = { viewModel.generateTranscript(context) },
        onEditTextClick = viewModel::onEditTextClicked,
        onEditableTextChange = viewModel::onEditableTextChanged,
        onSaveEditClick = viewModel::onSaveEditClicked,
        onCancelEditClick = viewModel::onCancelEditClicked,
        onSaveTranscriptClick = { viewModel.onSaveTranscriptClicked(context) },
        onGenerateNotesClick = {
            viewModel.onGenerateNotesClicked()
            onGenerateNotesClick()
        },
        onDismissError = viewModel::clearError
    )
}

@Composable
fun TranscriptScreenContent(
    uiState: TranscriptUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onPlayPauseToggle: () -> Unit = {},
    onLanguageSelected: (code: String, displayName: String) -> Unit = { _, _ -> },
    onDownloadModelClick: () -> Unit = {},
    onGenerateTranscriptClick: () -> Unit = {},
    onEditTextClick: () -> Unit = {},
    onEditableTextChange: (String) -> Unit = {},
    onSaveEditClick: () -> Unit = {},
    onCancelEditClick: () -> Unit = {},
    onSaveTranscriptClick: () -> Unit = {},
    onGenerateNotesClick: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    var showLanguageMenu by remember { mutableStateOf(false) }

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

                Spacer(modifier = Modifier.height(20.dp))

                // Lecture Title
                Text(
                    text = uiState.lectureTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Language Chip & Metadata Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(InnerCircleBg)
                                .clickable { showLanguageMenu = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.language,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PurplePrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "▾",
                                    fontSize = 12.sp,
                                    color = PurplePrimary
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            uiState.supportedLanguages.forEach { (code, name) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = name,
                                            fontWeight = if (code == uiState.selectedLanguageCode) FontWeight.Bold else FontWeight.Normal,
                                            color = if (code == uiState.selectedLanguageCode) PurplePrimary else TextDarkPrimary
                                        )
                                    },
                                    onClick = {
                                        showLanguageMenu = false
                                        onLanguageSelected(code, name)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = if (uiState.durationAndConfidence.isNotBlank()) uiState.durationAndConfidence else uiState.totalTimeText,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Offline Model Status Card (if not installed or downloading)
                if (!uiState.isModelInstalled) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(14.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Offline Model Required (~99 MB)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Download once using internet. After installation, multilingual transcription runs 100% offline with zero server calls.",
                                fontSize = 11.sp,
                                color = Color(0xFF78350F),
                                lineHeight = 16.sp
                            )

                            if (uiState.isDownloadingModel) {
                                Spacer(modifier = Modifier.height(10.dp))
                                LinearProgressIndicator(
                                    progress = { uiState.modelDownloadProgress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = PurplePrimary,
                                    trackColor = Color(0xFFFDE68A)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = uiState.modelDownloadStatusText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF92400E)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = onDownloadModelClick,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                                    modifier = Modifier.height(40.dp)
                                ) {
                                    Text(
                                        text = "Download Offline Model",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                } else {
                    // Model ready badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✓ Offline Whisper Engine Ready (English, Tamil, and 90+ Languages)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF059669)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Error Message Banner (if any)
                if (!uiState.errorMessage.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.errorMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = onDismissError,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Text(text = "✕", fontSize = 12.sp, color = Color(0xFFDC2626))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Generate Transcript Action Button
                if (uiState.paragraphs.isEmpty() || uiState.isTranscribing) {
                    Button(
                        onClick = onGenerateTranscriptClick,
                        enabled = !uiState.isTranscribing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary,
                            disabledContainerColor = Color(0xFFD8D2EB)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                drawSparkleStarIcon(Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.isTranscribing) "Transcribing on device..." else "Generate Transcript (Offline)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (uiState.isTranscribing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { uiState.transcriptionProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = PurplePrimary,
                            trackColor = InnerCircleBg
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = uiState.transcriptionStatusText,
                            fontSize = 11.sp,
                            color = PurplePrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }

                // Search & Filter Input Bar
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Search transcript",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
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
                    leadingIcon = {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawSearchIcon(TextMuted)
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = {}) {
                            Canvas(modifier = Modifier.size(18.dp)) {
                                drawFilterSlidersIcon(PurplePrimary)
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Mini Audio Playback Control Strip
                MiniAudioControlStrip(
                    currentTimeText = uiState.currentTimeText,
                    totalTimeText = uiState.totalTimeText,
                    isPlaying = uiState.isPlaying,
                    onPlayPauseToggle = onPlayPauseToggle
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Transcript Card Container (Read or Edit Mode)
                TranscriptCard(
                    paragraphs = uiState.paragraphs,
                    searchQuery = uiState.searchQuery,
                    excerptFooterText = uiState.excerptFooterText,
                    isEditing = uiState.isEditing,
                    editableText = uiState.editableText,
                    onEditTextClick = onEditTextClick,
                    onEditableTextChange = onEditableTextChange,
                    onSaveEditClick = onSaveEditClick,
                    onCancelEditClick = onCancelEditClick,
                    onGenerateTranscriptClick = onGenerateTranscriptClick
                )

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Bottom CTA Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Save Transcript & Generate Notes Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Save Transcript Button
                    Button(
                        onClick = onSaveTranscriptClick,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = InnerCircleBg
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = if (uiState.isSaved) "✓ Saved" else "Save Transcript",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }

                    // Generate Notes Button
                    val isGenerateNotesEnabled = uiState.paragraphs.isNotEmpty() && !uiState.isTranscribing
                    Button(
                        onClick = onGenerateNotesClick,
                        enabled = isGenerateNotesEnabled,
                        modifier = Modifier
                            .weight(1.2f)
                            .height(54.dp),
                        shape = RoundedCornerShape(27.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PurplePrimary,
                            disabledContainerColor = Color(0xFFD8D2EB)
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                drawSparkleStarIcon(Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Generate Notes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Review transcript text and technical terms before generating notes.",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Top Navigation Bar
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
                text = "Transcript",
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
 * Mini Audio Playback Control Strip
 */
@Composable
private fun MiniAudioControlStrip(
    currentTimeText: String,
    totalTimeText: String,
    isPlaying: Boolean,
    onPlayPauseToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MiniStripBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    onClick = onPlayPauseToggle,
                    modifier = Modifier.size(24.dp)
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        if (isPlaying) {
                            drawPauseIcon(PurplePrimary)
                        } else {
                            drawPlayIcon(PurplePrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "$currentTimeText / $totalTimeText",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary
                )
            }

            Canvas(modifier = Modifier.size(18.dp)) {
                drawSpeakerVolumeIcon(PurplePrimary)
            }
        }
    }
}

/**
 * Transcript Content Card Item (Supports normal reading, search query filtering, and inline text editing)
 */
@Composable
private fun TranscriptCard(
    paragraphs: List<TranscriptParagraph>,
    searchQuery: String,
    excerptFooterText: String,
    isEditing: Boolean,
    editableText: String,
    onEditTextClick: () -> Unit,
    onEditableTextChange: (String) -> Unit,
    onSaveEditClick: () -> Unit,
    onCancelEditClick: () -> Unit,
    onGenerateTranscriptClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(shape)
            .background(Color.White)
            .padding(18.dp)
    ) {
        Column {
            // Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Lecture transcript",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                if (paragraphs.isNotEmpty() && !isEditing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onEditTextClick() }
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawPencilEditIcon(PurplePrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit text",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isEditing) {
                // Editing Mode
                OutlinedTextField(
                    value = editableText,
                    onValueChange = onEditableTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = InnerCircleBg,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onCancelEditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF3F4F6)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Cancel", color = TextDarkPrimary, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onSaveEditClick,
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Save Changes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

            } else if (paragraphs.isEmpty()) {
                // Empty State
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No Transcript Yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Generate Transcript (Offline)' to transcribe your lecture recording directly on this device.",
                            fontSize = 12.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            } else {
                // Paragraphs List with Search Filtering
                val filteredParagraphs = if (searchQuery.isBlank()) {
                    paragraphs
                } else {
                    paragraphs.filter {
                        it.text.contains(searchQuery, ignoreCase = true) ||
                                it.speaker.contains(searchQuery, ignoreCase = true)
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    filteredParagraphs.forEach { paragraph ->
                        Column {
                            Text(
                                text = "${paragraph.timestamp} · ${paragraph.speaker}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            val textColor = if (paragraph.isHighlighted) PurplePrimary else TextDarkPrimary
                            Text(
                                text = paragraph.text,
                                fontSize = 13.sp,
                                fontWeight = if (paragraph.isHighlighted) FontWeight.Medium else FontWeight.Normal,
                                color = textColor,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                if (excerptFooterText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = excerptFooterText,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

// --- Custom Canvas Graphics Helpers ---

private fun DrawScope.drawSearchIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(
        color = color,
        radius = size.width * 0.32f,
        center = Offset(size.width * 0.4f, size.height * 0.4f),
        style = stroke
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.62f, size.height * 0.62f),
        end = Offset(size.width * 0.85f, size.height * 0.85f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawFilterSlidersIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.15f, size.height * 0.3f), end = Offset(size.width * 0.85f, size.height * 0.3f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.15f, size.height * 0.7f), end = Offset(size.width * 0.85f, size.height * 0.7f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.35f, size.height * 0.3f))
    drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.65f, size.height * 0.7f))
}

private fun DrawScope.drawPauseIcon(color: Color) {
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.2f), end = Offset(size.width * 0.35f, size.height * 0.8f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.65f, size.height * 0.2f), end = Offset(size.width * 0.65f, size.height * 0.8f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
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

private fun DrawScope.drawSpeakerVolumeIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val hornPath = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.35f)
        lineTo(size.width * 0.35f, size.height * 0.35f)
        lineTo(size.width * 0.55f, size.height * 0.2f)
        lineTo(size.width * 0.55f, size.height * 0.8f)
        lineTo(size.width * 0.35f, size.height * 0.65f)
        lineTo(size.width * 0.15f, size.height * 0.65f)
        close()
    }
    drawPath(path = hornPath, color = color)
    drawArc(color = color, startAngle = -45f, sweepAngle = 90f, useCenter = false, topLeft = Offset(size.width * 0.62f, size.height * 0.3f), size = Size(size.width * 0.25f, size.height * 0.4f), style = stroke)
}

private fun DrawScope.drawPencilEditIcon(color: Color) {
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.8f), end = Offset(size.width * 0.8f, size.height * 0.2f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
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
fun TranscriptScreenPreview() {
    AIBasedClassNotesAppTheme {
        TranscriptScreenContent(uiState = TranscriptUiState())
    }
}
