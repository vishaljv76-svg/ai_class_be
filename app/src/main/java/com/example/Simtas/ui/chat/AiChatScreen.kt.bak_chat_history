package com.example.Simtas.ui.chat

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.Simtas.ui.qa.MessageSource
import com.example.Simtas.ui.qa.QaAssistantViewModel
import com.example.Simtas.ui.qa.QaMessageItem
import com.example.Simtas.ui.theme.CardBg
import com.example.Simtas.ui.theme.OuterRingBorder
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.ThemeManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * Full-screen ChatGPT-style AI Study Copilot for SmartNotes AI.
 * Powered by Google Gemini with on-device Sherpa-ONNX speech recognition,
 * multi-turn conversation context, audio upload transcription, and MySQL chat history.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    modifier: Modifier = Modifier,
    viewModel: QaAssistantViewModel = remember { QaAssistantViewModel() },
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var queryText by remember { mutableStateOf("") }
    var showHistorySheet by remember { mutableStateOf(false) }
    var isAttachingAudio by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Chronological order (oldest to newest, top to bottom)
    val displayMessages = remember(uiState.messages) {
        uiState.messages.sortedBy { it.timestamp }
    }

    // Auto-scroll to latest message on new message or loading change
    LaunchedEffect(displayMessages.size, uiState.isLoading) {
        if (displayMessages.isNotEmpty()) {
            listState.animateScrollToItem(displayMessages.size - 1)
        }
    }

    // Fetch conversation history list on screen entry
    LaunchedEffect(Unit) {
        viewModel.fetchConversations()
    }

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

    // Audio file picker launcher for lecture / audio transcription
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            isAttachingAudio = true
            coroutineScope.launch {
                try {
                    val tempFile = File(context.cacheDir, "attached_lecture_${System.currentTimeMillis()}.m4a")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(tempFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    viewModel.transcribeAudioFile(context, tempFile) { transcript ->
                        isAttachingAudio = false
                        val clean = transcript.trim()
                        if (clean.isNotBlank()) {
                            val promptPrefix = "Summarize and explain this lecture content:\n\n"
                            queryText = if (queryText.isBlank()) "$promptPrefix$clean" else "$queryText\n\n$clean"
                        }
                    }
                } catch (e: Exception) {
                    isAttachingAudio = false
                    Toast.makeText(context, "Could not read audio file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = SplashBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // --- 1. Top Bar with Branding & Navigation Actions ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            drawBackArrow(TextDarkPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "SmartNotes AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE8F0FE))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Gemini",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PurplePrimary
                                )
                            }
                        }
                        Text(
                            text = if (uiState.conversationTitle != null) {
                                uiState.conversationTitle!!
                            } else {
                                "ChatGPT-Style Academic Copilot"
                            },
                            fontSize = 11.sp,
                            color = TextMuted,
                            maxLines = 1
                        )
                    }
                }

                // Top Right Action Buttons: History & New Chat
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Chat History Drawer Toggle
                    IconButton(
                        onClick = {
                            viewModel.fetchConversations()
                            showHistorySheet = true
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            drawHistoryIcon(TextDarkPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Start New Chat Button
                    IconButton(
                        onClick = {
                            viewModel.startNewChat()
                            queryText = ""
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            drawPlusIcon(PurplePrimary)
                        }
                    }
                }
            }

            // --- 2. Main Conversation Area ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (displayMessages.isEmpty() && !uiState.isLoading) {
                    // Empty State / Welcome Screen with Quick Study Starters
                    EmptyChatWelcomeView(
                        onPromptSelected = { prompt ->
                            queryText = prompt
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(displayMessages, key = { it.id }) { item ->
                            ChatMessageCard(
                                message = item,
                                onCopyClick = { text ->
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("SmartNotes AI", text))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        if (uiState.isLoading) {
                            item(key = "loading_indicator") {
                                ThinkingIndicatorCard()
                            }
                        }

                        // Bottom spacing
                        item(key = "bottom_spacer") {
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }

            // --- 3. Error Banner (if any) ---
            if (!uiState.errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFDE8E8))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.errorMessage!!,
                            fontSize = 12.sp,
                            color = Color(0xFFC81E1E),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Dismiss",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC81E1E),
                            modifier = Modifier
                                .clickable { viewModel.clearError() }
                                .padding(start = 8.dp)
                        )
                    }
                }
            }

            // --- 4. Voice & Transcription Status Strip ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        uiState.isListening -> "Listening... Speak clearly (tap stop when done)"
                        uiState.isTranscribingVoice -> "Transcribing speech (${uiState.voiceProgress}%)..."
                        isAttachingAudio -> "Transcribing attached lecture audio..."
                        else -> "Ask any academic, coding, math, or writing question"
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

            // --- 5. ChatGPT-Style Bottom Input Bar ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attach Audio Button
                IconButton(
                    onClick = {
                        try {
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open audio picker: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !uiState.isLoading && !uiState.isListening && !uiState.isTranscribingVoice,
                    modifier = Modifier.size(42.dp)
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        drawAttachIcon(if (isAttachingAudio) PurplePrimary else TextMuted)
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Editable Multi-line Text Field
                OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    placeholder = {
                        Text(
                            text = when {
                                uiState.isListening -> "Listening to your voice..."
                                uiState.isTranscribingVoice -> "Converting speech to text..."
                                else -> "Message SmartNotes AI..."
                            },
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    },
                    maxLines = 5,
                    enabled = !uiState.isLoading && !uiState.isTranscribingVoice,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 130.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = OuterRingBorder,
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                // Microphone Button (Voice-to-Text)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                uiState.isListening -> Color(0xFFDC2626)
                                uiState.isTranscribingVoice -> Color(0xFFEDE9FE)
                                else -> Color(0xFFF3E8FF)
                            }
                        )
                        .clickable(enabled = !uiState.isLoading && !uiState.isTranscribingVoice) {
                            handleMicClick()
                        }
                ) {
                    if (uiState.isTranscribingVoice || isAttachingAudio) {
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

                // Send Button
                val canSend = queryText.isNotBlank() && !uiState.isLoading && !uiState.isListening && !uiState.isTranscribingVoice
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(if (canSend) PurplePrimary else Color(0xFFE2E8F0))
                        .clickable(enabled = canSend) {
                            val q = queryText.trim()
                            if (q.isNotBlank()) {
                                viewModel.askQuestion(
                                    question = q,
                                    subject = "General Education & Computer Science",
                                    lectureTitle = "SmartNotes AI Study Chat"
                                )
                                queryText = ""
                            }
                        }
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawSendIcon(if (canSend) Color.White else Color(0xFF94A3B8))
                        }
                    }
                }
            }
        }
    }

    // --- 6. Previous Conversations Modal Bottom Sheet ---
    if (showHistorySheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showHistorySheet = false },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chat History",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )

                    Button(
                        onClick = {
                            viewModel.startNewChat()
                            queryText = ""
                            showHistorySheet = false
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PurplePrimary)
                    ) {
                        Text("New Chat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.isLoadingConversations) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PurplePrimary)
                    }
                } else if (uiState.conversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No previous conversations found. Ask a question to start one!",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                    ) {
                        items(uiState.conversations, key = { it.id }) { conv ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CardBg),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.loadConversationHistory(conv.id)
                                        showHistorySheet = false
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = conv.displayTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextDarkPrimary,
                                            maxLines = 1
                                        )
                                        if (!conv.updated_at.isNullOrBlank()) {
                                            Text(
                                                text = "Updated: ${conv.updated_at}",
                                                fontSize = 11.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    if (conv.message_count != null && conv.message_count > 0) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFFEDE9FE))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "${conv.message_count} msgs",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PurplePrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Chat Message Bubble & Card Component ---

@Composable
private fun ChatMessageCard(
    message: QaMessageItem,
    onCopyClick: (String) -> Unit
) {
    if (message.sender == MessageSource.USER) {
        // User Message: Right-Aligned Bubble
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
                    .background(PurplePrimary)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = Color.White,
                    lineHeight = 20.sp
                )
            }
        }
    } else {
        // Assistant Message (Google Gemini or Offline Fallback)
        val isOffline = message.sender == MessageSource.OFFLINE_ENGINE
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, OuterRingBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header badge row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isOffline) Color(0xFFFEF3C7) else Color(0xFFE6F4EA))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isOffline) "Offline Curriculum" else "Google Gemini",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isOffline) Color(0xFFB45309) else Color(0xFF137333)
                            )
                        }
                    }

                    // Copy action button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { onCopyClick(message.text) }
                            .padding(4.dp)
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawCopyIcon(PurplePrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Copy",
                            fontSize = 11.sp,
                            color = PurplePrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Rich Markdown & Visual Diagrams Rendering
                MarkdownViewer(
                    content = message.text,
                    onCopyText = onCopyClick
                )

                if (message.explanation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message.explanation,
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

// --- Thinking Indicator Card ---

@Composable
private fun ThinkingIndicatorCard() {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth(0.9f)
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
                color = TextDarkPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// --- Empty State Welcome View (ChatGPT-Style Quick Starters) ---

@Composable
private fun EmptyChatWelcomeView(
    onPromptSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE9FE)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                drawSparkleStarIcon(PurplePrimary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "How can I help you study today?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Ask questions across computer science, math, formulas, code, essay writing, or general knowledge.",
            fontSize = 12.sp,
            color = TextMuted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 4 ChatGPT-Style Quick Prompt Cards
        val promptStarters = listOf(
            "Explain BCNF Normalization with a simple real-world example.",
            "Write a Python algorithm to find duplicates in an array.",
            "Derive and explain Bayes' Theorem and its role in AI.",
            "Create a structured 7-day study plan for exams."
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            promptStarters.forEach { prompt ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, OuterRingBorder, RoundedCornerShape(12.dp))
                        .clickable { onPromptSelected(prompt) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = prompt,
                        fontSize = 12.sp,
                        color = TextDarkPrimary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// --- Custom Canvas Vector Graphics for Chat Screen ---

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

private fun DrawScope.drawPlusIcon(color: Color) {
    val strokeWidth = 2.dp.toPx()
    drawLine(
        color = color,
        start = Offset(size.width * 0.5f, size.height * 0.2f),
        end = Offset(size.width * 0.5f, size.height * 0.8f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.2f, size.height * 0.5f),
        end = Offset(size.width * 0.8f, size.height * 0.5f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawHistoryIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(
        color = color,
        radius = size.width * 0.38f,
        center = Offset(size.width / 2, size.height / 2),
        style = stroke
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.5f, size.height * 0.5f),
        end = Offset(size.width * 0.5f, size.height * 0.28f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.5f, size.height * 0.5f),
        end = Offset(size.width * 0.68f, size.height * 0.5f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawAttachIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.35f, size.height * 0.7f)
        lineTo(size.width * 0.65f, size.height * 0.4f)
        quadraticTo(size.width * 0.8f, size.height * 0.25f, size.width * 0.65f, size.height * 0.2f)
        quadraticTo(size.width * 0.5f, size.height * 0.25f, size.width * 0.3f, size.height * 0.45f)
        lineTo(size.width * 0.25f, size.height * 0.65f)
        quadraticTo(size.width * 0.3f, size.height * 0.85f, size.width * 0.5f, size.height * 0.85f)
        lineTo(size.width * 0.7f, size.height * 0.65f)
    }
    drawPath(path = path, color = color, style = stroke)
}

private fun DrawScope.drawSendIcon(color: Color) {
    val path = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.5f)
        lineTo(size.width * 0.5f, size.height * 0.15f)
        lineTo(size.width * 0.85f, size.height * 0.5f)
        moveTo(size.width * 0.5f, size.height * 0.15f)
        lineTo(size.width * 0.5f, size.height * 0.85f)
    }
    drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
}

private fun DrawScope.drawCopyIcon(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.35f, size.height * 0.15f),
        size = Size(size.width * 0.55f, size.height * 0.6f),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = stroke
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.15f, size.height * 0.35f),
        size = Size(size.width * 0.55f, size.height * 0.55f),
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

