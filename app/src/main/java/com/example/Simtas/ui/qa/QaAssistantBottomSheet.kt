package com.example.Simtas.ui.qa

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.Simtas.ui.detailednotes.DetailedNotesUiState
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted

/**
 * Interactive SmartNotes AI Assistant Bottom Sheet.
 * Supports ChatGPT-style voice questioning (Sherpa-ONNX Whisper on-device)
 * and authenticated questions to the live Google Gemini backend (ai/chat.php).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QaAssistantBottomSheet(
    onDismissRequest: () -> Unit,
    transcriptText: String = "",
    detailedNotes: DetailedNotesUiState? = null,
    summaryText: String = "",
    keyTakeaways: List<String> = emptyList(),
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    viewModel: QaAssistantViewModel = remember { QaAssistantViewModel() }
) {
    var queryText by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Runtime microphone permission launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording(context)
        }
    }

    val handleMicClick = {
        if (uiState.isListening) {
            viewModel.stopVoiceRecording(context) { recognizedText ->
                val trimmed = recognizedText.trim()
                if (trimmed.isNotBlank()) {
                    queryText = if (queryText.isBlank()) trimmed else "$queryText $trimmed"
                }
            }
        } else {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (hasPermission) {
                viewModel.startVoiceRecording(context)
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (uiState.isListening) {
                viewModel.cancelVoiceRecording()
            }
            onDismissRequest()
        },
        sheetState = sheetState,
        containerColor = SplashBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
        ) {
            // Header with AI Status Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SmartNotes AI Assistant",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = if (uiState.conversationId != null) {
                            "Thread #${uiState.conversationId} · Live Google Gemini"
                        } else {
                            "Powered by Google Gemini (Online Assistant)"
                        },
                        fontSize = 12.sp,
                        color = PurplePrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFE8F0FE))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Gemini",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PurplePrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Voice Status & Language Selector Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        uiState.isListening -> "Listening... Speak now (tap stop when done)"
                        uiState.isTranscribingVoice -> "Transcribing speech (${uiState.voiceProgress}%)..."
                        else -> "Tap mic to speak · Review & edit before sending"
                    },
                    fontSize = 11.sp,
                    color = if (uiState.isListening) Color(0xFFDC2626) else TextMuted,
                    fontWeight = if (uiState.isListening) FontWeight.Bold else FontWeight.Normal
                )

                // Language toggle pill: English <-> Tamil
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEDE9FE))
                        .clickable {
                            val newLang = if (uiState.selectedLanguageCode == "en") "ta" else "en"
                            viewModel.setLanguage(newLang)
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (uiState.selectedLanguageCode == "ta") "Tamil (தமிழ்)" else "English",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = PurplePrimary
                    )
                }
            }

            // Input Row (OutlinedTextField + Mic Button + Ask Button)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = {
                        Text(
                            text = when {
                                uiState.isListening -> "Listening to your voice..."
                                uiState.isTranscribingVoice -> "Converting speech to text..."
                                else -> "Ask Gemini or speak question..."
                            },
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    enabled = !uiState.isLoading && !uiState.isTranscribingVoice,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Microphone Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                uiState.isListening -> Color(0xFFDC2626) // Red when recording
                                uiState.isTranscribingVoice -> Color(0xFFEDE9FE)
                                else -> Color(0xFFF3E8FF) // Subtle purple when idle
                            }
                        )
                        .clickable(enabled = !uiState.isLoading && !uiState.isTranscribingVoice) {
                            handleMicClick()
                        }
                ) {
                    if (uiState.isTranscribingVoice) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PurplePrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            if (uiState.isListening) {
                                drawSquareStopIcon(Color.White)
                            } else {
                                drawMicIcon(PurplePrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Ask Button
                Button(
                    onClick = {
                        val q = queryText.trim()
                        if (q.isNotBlank() && !uiState.isLoading && !uiState.isListening && !uiState.isTranscribingVoice) {
                            viewModel.askQuestion(
                                question = q,
                                subject = detailedNotes?.courseCode ?: "Database Systems",
                                lectureTitle = detailedNotes?.lectureTitle ?: "Lecture Notes",
                                transcriptText = transcriptText,
                                detailedNotes = detailedNotes,
                                summaryText = summaryText,
                                keyTakeaways = keyTakeaways
                            )
                            queryText = ""
                        }
                    },
                    enabled = !uiState.isLoading && !uiState.isListening && !uiState.isTranscribingVoice && queryText.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Ask", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Error Banner (if any)
            if (!uiState.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFDE8E8))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Notice: ${uiState.errorMessage}",
                        fontSize = 12.sp,
                        color = Color(0xFFC81E1E),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading indicator card while awaiting Gemini response
            if (uiState.isLoading) {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = PurplePrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SmartNotes AI is generating your answer...",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextDarkPrimary
                        )
                    }
                }
            }

            // Messages & Conversation History
            if (uiState.messages.isNotEmpty()) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(uiState.messages, key = { it.id }) { item ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Header label based on sender
                                    val (badgeText, badgeBg, badgeTextColor) = when (item.sender) {
                                        MessageSource.USER -> Triple(
                                            "You",
                                            Color(0xFFE8F0FE),
                                            PurplePrimary
                                        )
                                        MessageSource.ONLINE_GEMINI -> Triple(
                                            "Google Gemini",
                                            Color(0xFFE6F4EA),
                                            Color(0xFF137333)
                                        )
                                        MessageSource.OFFLINE_ENGINE -> Triple(
                                            "Offline Curriculum",
                                            Color(0xFFFEF3C7),
                                            Color(0xFFB45309)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = badgeText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeTextColor
                                        )
                                    }

                                    if (item.provider != null) {
                                        Text(
                                            text = item.provider,
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (item.sender == MessageSource.USER) {
                                    Text(
                                        text = item.text,
                                        fontSize = 13.sp,
                                        color = TextDarkPrimary,
                                        lineHeight = 19.sp
                                    )
                                } else {
                                    com.example.Simtas.ui.chat.MarkdownViewer(
                                        content = item.text
                                    )
                                }

                                if (item.explanation.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = item.explanation,
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Empty state guidance
                Text(
                    text = "Tip: Ask academic questions by typing or speaking in English or Tamil. Gemini generates grounded study answers in real-time.",
                    fontSize = 12.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

// --- Custom Canvas Vector Graphics for Microphone ---

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
        topLeft = Offset(size.width * 0.28f, size.height * 0.28f),
        size = Size(size.width * 0.44f, size.height * 0.44f),
        cornerRadius = CornerRadius(2.dp.toPx())
    )
}
