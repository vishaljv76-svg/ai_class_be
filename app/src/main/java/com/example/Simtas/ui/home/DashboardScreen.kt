package com.example.Simtas.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Simtas.R
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.CardBg
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.PurpleSecondary
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.TextSubtitle
import com.example.Simtas.ui.theme.ThemeManager

// ============================================================================
// LUXURY PASTEL LAVENDER & VIBRANT PURPLE PALETTE
// ============================================================================
private val LuxuryVioletPrimary = Color(0xFF6C5CE7)
private val LuxuryPurpleDeep = Color(0xFF5B3DE8)
private val LuxuryElectricViolet = Color(0xFF8B5CF6)
private val LuxuryLilacGlow = Color(0xFFA78BFA)
private val LuxurySoftLavender = Color(0xFFEDE9FE)
private val BrightOrangeAccent = Color(0xFFFF7A00)

private val PastelLavenderBg: Color
    get() = Color.Transparent

private val CardBorderColor: Color
    get() = if (ThemeManager.isDark()) Color(0xFF332B52) else Color(0xFFEDE7F6)

private val DividerSubtleColor: Color
    get() = if (ThemeManager.isDark()) Color(0xFF2E2548) else Color(0xFFF1EDFC)

// Pastel Toolkit Card Fills
private val ToolkitFlashcardsBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF231A3A) else Color(0xFFF7F2FF)

private val ToolkitQuizBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF281938) else Color(0xFFFAF1FD)

private val ToolkitRecordBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF221A3A) else Color(0xFFF6F3FE)

private val ToolkitUploadBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF19233C) else Color(0xFFEFF6FF)

private val ToolkitNotesBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF1F1B38) else Color(0xFFF5F1FF)

private val ToolkitCopilotBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF1A223B) else Color(0xFFF0FDF4)

// ============================================================================
// ROOT DASHBOARD SCREEN (MVVM ARCHITECTURE)
// ============================================================================
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = remember { DashboardViewModel() },
    onNotificationClick: () -> Unit = {},
    onRecordLectureClick: () -> Unit = {},
    onUploadAudioClick: () -> Unit = {},
    onMyNotesClick: () -> Unit = {},
    onAiQuizClick: () -> Unit = {},
    onFlashcardsClick: () -> Unit = {},
    onViewAllNotesClick: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onTabSelect: (HomeBottomTab) -> Unit = {},
    onAiCopilotClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    DashboardScreenContent(
        uiState = uiState,
        modifier = modifier,
        onNotificationClick = onNotificationClick,
        onRecordLectureClick = {
            viewModel.onRecordLectureClicked()
            onRecordLectureClick()
        },
        onUploadAudioClick = {
            viewModel.onUploadAudioClicked()
            onUploadAudioClick()
        },
        onMyNotesClick = {
            viewModel.onMyNotesClicked()
            onMyNotesClick()
        },
        onAiQuizClick = {
            viewModel.onAiQuizClicked()
            onAiQuizClick()
        },
        onFlashcardsClick = onFlashcardsClick,
        onViewAllNotesClick = onViewAllNotesClick,
        onNoteClick = { noteId ->
            viewModel.onNoteItemClicked(noteId)
            onNoteClick(noteId)
        },
        onTabSelect = { tab ->
            viewModel.selectTab(tab)
            onTabSelect(tab)
        },
        onAiCopilotClick = onAiCopilotClick
    )
}

@Composable
fun DashboardScreenContent(
    uiState: DashboardUiState,
    modifier: Modifier = Modifier,
    onNotificationClick: () -> Unit = {},
    onRecordLectureClick: () -> Unit = {},
    onUploadAudioClick: () -> Unit = {},
    onMyNotesClick: () -> Unit = {},
    onAiQuizClick: () -> Unit = {},
    onFlashcardsClick: () -> Unit = {},
    onViewAllNotesClick: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onTabSelect: (HomeBottomTab) -> Unit = {},
    onAiCopilotClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = PastelLavenderBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ambient Soft Lavender Background Glow behind the top header
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                val isDark = ThemeManager.isDark()
                val glowCenter = Offset(size.width * 0.85f, size.height * 0.15f)
                val glowRadius = size.width * 0.80f
                val glowColor = if (isDark) Color(0xFF6C5CE7).copy(alpha = 0.14f) else Color(0xFFDDD6FE).copy(alpha = 0.50f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = glowCenter,
                        radius = glowRadius
                    ),
                    center = glowCenter,
                    radius = glowRadius
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 84.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // 1. TOP HEADER: Circular student avatar, "Hi, [userName]!", notification & settings
                TopHeaderBar(
                    userName = uiState.userName,
                    userInitials = uiState.userInitials,
                    dateText = uiState.dateText,
                    onNotificationClick = onNotificationClick,
                    onProfileClick = { onTabSelect(HomeBottomTab.Profile) }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 2. FEATURED AI STUDY COPILOT BANNER: Purple gradient + 3D mascot duo + Ask AI button
                AiStudyCopilotBanner(
                    userName = uiState.userName,
                    onAiQuizClick = onAiQuizClick,
                    onRecordLectureClick = onRecordLectureClick,
                    onAiCopilotClick = onAiCopilotClick,
                    onFlashcardsClick = onFlashcardsClick
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 3. STUDY OVERVIEW: Large clean white card with real statistics (Total Notes, Subjects, Quizzes)
                StudyOverviewSection(
                    totalNotes = uiState.totalNotes,
                    subjectsCount = uiState.subjectsCount,
                    quizzesCount = uiState.quizzesCount
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4. STUDY TOOLKIT: Section Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp, 16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LuxuryVioletPrimary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Study Toolkit",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDarkPrimary
                        )
                    }

                    Text(
                        text = "All Tools",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LuxuryVioletPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. STUDY TOOLKIT: 2-Column Card Layout with 3D Mascot Illustrations
                ToolkitTwoColumnGrid(
                    onRecordLectureClick = onRecordLectureClick,
                    onUploadAudioClick = onUploadAudioClick,
                    onMyNotesClick = onMyNotesClick,
                    onAiQuizClick = onAiQuizClick,
                    onFlashcardsClick = onFlashcardsClick,
                    onAiCopilotClick = onAiCopilotClick
                )

                Spacer(modifier = Modifier.height(26.dp))

                // 6. CONTINUE LEARNING: Section Header with "View all ›"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp, 16.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(LuxuryElectricViolet)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue Learning",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDarkPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(InnerCircleBg)
                            .clickable { onViewAllNotesClick() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "View all ›",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LuxuryVioletPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 7. CONTINUE LEARNING: Real ViewModel Notes List or Empty State
                if (uiState.recentNotes.isEmpty()) {
                    EmptyRecentNotesCard(onRecordLectureClick = onRecordLectureClick)
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.recentNotes.forEach { note ->
                            RecentNoteCardItem(
                                note = note,
                                onClick = { onNoteClick(note.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // 8. FLOATING LUXURY BOTTOM NAVIGATION BAR
            HomeBottomNavigationBar(
                selectedTab = uiState.selectedTab,
                onTabSelect = onTabSelect,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ============================================================================
// 1. TOP HEADER BAR: Circular Avatar, Greeting & Action Buttons
// ============================================================================
@Composable
private fun TopHeaderBar(
    userName: String,
    userInitials: String,
    dateText: String,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val displayName = userName.trim().ifBlank {
        com.example.Simtas.data.remote.SessionManager.getUserName().trim()
    }
    val greeting = if (displayName.isNotBlank()) "Hi, $displayName!" else "Hi there!"
    val initials = userInitials.ifBlank {
        com.example.Simtas.data.remote.SessionManager.computeInitials(displayName).ifBlank { "AM" }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Left: Circular Student Avatar + User Greeting & Date
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Circular Student Avatar with Gradient Ring
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                LuxuryVioletPrimary,
                                LuxuryLilacGlow,
                                LuxuryElectricViolet,
                                LuxuryPurpleDeep,
                                LuxuryVioletPrimary
                            )
                        )
                    )
                    .padding(2.5.dp)
                    .clickable { onProfileClick() }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(CardBg)
                ) {
                    Text(
                        text = initials,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = LuxuryVioletPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = greeting,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ready to excel today?",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Right: Notification Bell & Profile Settings Action Buttons
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Notification Bell with Unread Dot
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CardBg)
                    .border(1.dp, CardBorderColor, CircleShape)
                    .clickable { onNotificationClick() }
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawBellIcon(TextDarkPrimary)
                }
                // Glowing Violet Unread Dot
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(CircleShape)
                        .background(LuxuryVioletPrimary)
                )
            }

            // Profile / Settings Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(CardBg)
                    .border(1.dp, CardBorderColor, CircleShape)
                    .clickable { onProfileClick() }
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawGearIcon(TextDarkPrimary)
                }
            }
        }
    }
}

// ============================================================================
// 2. FEATURED AI STUDY COPILOT BANNER (PURPLE GRADIENT & 3D MASCOT DUO)
// ============================================================================
@Composable
private fun AiStudyCopilotBanner(
    userName: String,
    onAiQuizClick: () -> Unit,
    onRecordLectureClick: () -> Unit,
    onAiCopilotClick: () -> Unit = {},
    onFlashcardsClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "CopilotTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val mascotBob by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotBob"
    )

    val shape = RoundedCornerShape(26.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = LuxuryPurpleDeep.copy(alpha = 0.30f),
                spotColor = LuxuryElectricViolet.copy(alpha = 0.45f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4A1A8F),
                        Color(0xFF6B2FB8),
                        Color(0xFF8845E0)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.45f),
                        Color.White.copy(alpha = 0.12f)
                    )
                ),
                shape
            )
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Main Top Content: Text on Left, Cute 3D Duo Mascot on Right
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Left Column: Copilot Badge, Title, Subtitle, Ask AI Button
                Column(modifier = Modifier.weight(1f)) {
                    // Pulsing AI Pill Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.20f))
                            .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
                            .clickable { onAiCopilotClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF55E6C1).copy(alpha = pulseScale))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AI STUDY COPILOT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Synthesize Your Lectures",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Instant structured notes, active recall flashcards, and quizzes.",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color.White.copy(alpha = 0.90f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary Action: Ask AI Button with Bright Orange Highlight
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(BrightOrangeAccent, Color(0xFFFF5252))
                                )
                            )
                            .clickable { onAiCopilotClick() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(13.dp)) {
                                drawSparkleStarIcon(Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Ask AI",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Right Column: Large, Clearly Visible 3D Cute Dog and Cat Mascot Duo
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .graphicsLayer { translationY = mascotBob },
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.mascot_duo_study),
                        contentDescription = "AI Study Mascots Studying Together",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Quick Action Chips Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // AI Quiz Chip
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { onAiQuizClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawSparkleStarIcon(Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI Quiz",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Flashcards Chip
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { onFlashcardsClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawSparkleStarIcon(Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Flashcards",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Record Chip
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(12.dp))
                        .clickable { onRecordLectureClick() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(12.dp)) {
                            drawMicrophoneIcon(Color.White)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Record",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. STUDY OVERVIEW SECTION (REAL VIEWMODEL DATA STATISTICS)
// ============================================================================
@Composable
private fun StudyOverviewSection(
    totalNotes: Int,
    subjectsCount: Int,
    quizzesCount: Int
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = LuxuryVioletPrimary.copy(alpha = 0.08f),
                spotColor = LuxuryVioletPrimary.copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(CardBg)
            .border(1.dp, CardBorderColor, shape)
            .padding(18.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Study Overview",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(InnerCircleBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Real-Time Stats",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = LuxuryVioletPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Metric 1: Total Notes
                LuxuryMetricItem(
                    number = String.format("%02d", totalNotes),
                    label = "Total Notes",
                    icon = { drawNoteIconSmall(LuxuryVioletPrimary) },
                    modifier = Modifier.weight(1f)
                )

                // Divider Line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(DividerSubtleColor)
                )

                // Metric 2: Subjects
                LuxuryMetricItem(
                    number = String.format("%02d", subjectsCount),
                    label = "Subjects",
                    icon = { drawSubjectsIconSmall(LuxuryElectricViolet) },
                    modifier = Modifier.weight(1f)
                )

                // Divider Line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(38.dp)
                        .background(DividerSubtleColor)
                )

                // Metric 3: Quizzes
                LuxuryMetricItem(
                    number = String.format("%02d", quizzesCount),
                    label = "Quizzes",
                    icon = { drawQuizIconSmall(PurpleSecondary) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun LuxuryMetricItem(
    number: String,
    label: String,
    icon: DrawScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(InnerCircleBg)
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                icon()
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = number,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDarkPrimary
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted
        )
    }
}

// ============================================================================
// 4. STUDY TOOLKIT: TWO-COLUMN GRID WITH ILLUSTRATED FEATURE CARDS
// ============================================================================
@Composable
private fun ToolkitTwoColumnGrid(
    onRecordLectureClick: () -> Unit,
    onUploadAudioClick: () -> Unit,
    onMyNotesClick: () -> Unit,
    onAiQuizClick: () -> Unit,
    onFlashcardsClick: () -> Unit,
    onAiCopilotClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // ROW 1: Record Lecture (Dog with headphones) & Upload Audio (Cat with books)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ToolkitIllustratedCard(
                title = "Record Lecture",
                subtitle = "Live audio & transcript",
                bgColor = ToolkitRecordBg,
                accentColor = LuxuryPurpleDeep,
                imageRes = R.drawable.mascot_dog_headphones,
                contentDescription = "Dog wearing headphones for lecture recording",
                modifier = Modifier.weight(1f),
                onClick = onRecordLectureClick
            )

            ToolkitIllustratedCard(
                title = "Upload Audio",
                subtitle = "Import lecture files",
                bgColor = ToolkitUploadBg,
                accentColor = Color(0xFF3B82F6),
                imageRes = R.drawable.mascot_cat_books,
                contentDescription = "Cat with books for audio upload",
                modifier = Modifier.weight(1f),
                onClick = onUploadAudioClick
            )
        }

        // ROW 2: My Notes (Cat reading) & AI Quiz (Dog with star/trophy)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ToolkitIllustratedCard(
                title = "My Notes",
                subtitle = "All subjects & topics",
                bgColor = ToolkitNotesBg,
                accentColor = LuxuryVioletPrimary,
                imageRes = R.drawable.mascot_cat_study,
                contentDescription = "Cat reading notes",
                modifier = Modifier.weight(1f),
                onClick = onMyNotesClick
            )

            ToolkitIllustratedCard(
                title = "AI Quiz",
                subtitle = "Practice & mock exams",
                bgColor = ToolkitQuizBg,
                accentColor = LuxuryElectricViolet,
                imageRes = R.drawable.mascot_dog_trophy,
                contentDescription = "Dog with star trophy for AI Quiz",
                modifier = Modifier.weight(1f),
                onClick = onAiQuizClick
            )
        }

        // ROW 3: AI Flashcards (Cat studying) & Ask AI (Dog with laptop)
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            ToolkitIllustratedCard(
                title = "AI Flashcards",
                subtitle = "Active recall decks",
                bgColor = ToolkitFlashcardsBg,
                accentColor = LuxuryPurpleDeep,
                imageRes = R.drawable.mascot_cat_study,
                contentDescription = "Cat studying flashcards",
                modifier = Modifier.weight(1f),
                onClick = onFlashcardsClick
            )

            ToolkitIllustratedCard(
                title = "Ask AI",
                subtitle = "Tutor & instant answers",
                bgColor = ToolkitCopilotBg,
                accentColor = Color(0xFF10B981),
                imageRes = R.drawable.mascot_dog_laptop,
                contentDescription = "Dog with laptop for Ask AI",
                modifier = Modifier.weight(1f),
                onClick = onAiCopilotClick
            )
        }
    }
}

@Composable
private fun ToolkitIllustratedCard(
    title: String,
    subtitle: String,
    bgColor: Color,
    accentColor: Color,
    imageRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = accentColor.copy(alpha = 0.08f),
                spotColor = accentColor.copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(bgColor)
            .border(1.dp, CardBorderColor, shape)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Illustrated Mascot Badge Container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                ) {
                    Image(
                        painter = painterResource(id = imageRes),
                        contentDescription = contentDescription,
                        modifier = Modifier.size(46.dp)
                    )
                }

                // Subtle chevron right indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(CardBg)
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawChevronRight(accentColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = TextSubtitle
            )
        }
    }
}

// ============================================================================
// 5. CONTINUE LEARNING SECTION (RECENT NOTES WITH REAL DATA)
// ============================================================================
@Composable
private fun RecentNoteCardItem(
    note: RecentNoteItem,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = LuxuryVioletPrimary.copy(alpha = 0.06f),
                spotColor = LuxuryVioletPrimary.copy(alpha = 0.09f)
            )
            .clip(shape)
            .background(CardBg)
            .border(1.dp, CardBorderColor, shape)
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(InnerCircleBg)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        when (note.iconType) {
                            RecentNoteIconType.Database -> drawDatabaseIcon(LuxuryVioletPrimary)
                            RecentNoteIconType.Math -> drawCalculatorIcon(LuxuryElectricViolet)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = note.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = TextDarkPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Subject Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(InnerCircleBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = note.subject,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = LuxuryVioletPrimary
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "· ${note.date} · ${note.duration}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(InnerCircleBg)
            ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    drawChevronRight(LuxuryVioletPrimary)
                }
            }
        }
    }
}

@Composable
private fun EmptyRecentNotesCard(onRecordLectureClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(CardBg)
            .border(1.dp, CardBorderColor, shape)
            .clickable { onRecordLectureClick() }
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.mascot_cat_books),
                contentDescription = "Oliver the Study Cat",
                modifier = Modifier.size(68.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No notes created yet",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tap here to record your first lecture with AI",
                fontSize = 12.sp,
                color = TextSubtitle
            )
        }
    }
}

// ============================================================================
// 6. ELEGANT FLOATING BOTTOM NAVIGATION BAR
// ============================================================================
@Composable
private fun HomeBottomNavigationBar(
    selectedTab: HomeBottomTab,
    onTabSelect: (HomeBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = CardBg,
        shadowElevation = 16.dp,
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 16.dp)
        ) {
            // Tab 1: Home
            BottomTabItem(
                label = "Home",
                isSelected = selectedTab == HomeBottomTab.Home,
                onClick = { onTabSelect(HomeBottomTab.Home) },
                icon = { drawHomeIcon(if (selectedTab == HomeBottomTab.Home) LuxuryVioletPrimary else TextMuted) }
            )

            // Tab 2: My Notes
            BottomTabItem(
                label = "My Notes",
                isSelected = selectedTab == HomeBottomTab.Notes,
                onClick = { onTabSelect(HomeBottomTab.Notes) },
                icon = { drawNotesTabIcon(if (selectedTab == HomeBottomTab.Notes) LuxuryVioletPrimary else TextMuted) }
            )

            // Tab 3: Profile
            BottomTabItem(
                label = "Profile",
                isSelected = selectedTab == HomeBottomTab.Profile,
                onClick = { onTabSelect(HomeBottomTab.Profile) },
                icon = { drawProfileTabIcon(if (selectedTab == HomeBottomTab.Profile) LuxuryVioletPrimary else TextMuted) }
            )
        }
    }
}

@Composable
private fun BottomTabItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: DrawScope.() -> Unit
) {
    val pillBg = if (isSelected) InnerCircleBg else Color.Transparent
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(pillBg)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                icon()
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) LuxuryVioletPrimary else TextMuted
            )
        }
    }
}

// ============================================================================
// 7. CANVAS VECTOR GRAPHICS HELPERS
// ============================================================================
private fun DrawScope.drawBellIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.3f, size.height * 0.7f)
        lineTo(size.width * 0.2f, size.height * 0.7f)
        quadraticTo(
            size.width * 0.2f, size.height * 0.45f,
            size.width * 0.35f, size.height * 0.3f
        )
        lineTo(size.width * 0.35f, size.height * 0.2f)
        quadraticTo(
            size.width * 0.35f, size.height * 0.1f,
            size.width * 0.5f, size.height * 0.1f
        )
        quadraticTo(
            size.width * 0.65f, size.height * 0.1f,
            size.width * 0.65f, size.height * 0.2f
        )
        lineTo(size.width * 0.65f, size.height * 0.3f)
        quadraticTo(
            size.width * 0.8f, size.height * 0.45f,
            size.width * 0.8f, size.height * 0.7f
        )
        lineTo(size.width * 0.7f, size.height * 0.7f)
    }
    drawPath(path = path, color = color, style = stroke)
    drawArc(
        color = color,
        startAngle = 0f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(size.width * 0.4f, size.height * 0.72f),
        size = Size(size.width * 0.2f, size.height * 0.18f),
        style = stroke
    )
}

private fun DrawScope.drawGearIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(
        color = color,
        radius = size.width * 0.22f,
        center = Offset(size.width * 0.5f, size.height * 0.5f),
        style = stroke
    )
    val numTeeth = 6
    val cx = size.width * 0.5f
    val cy = size.height * 0.5f
    val rInner = size.width * 0.30f
    val rOuter = size.width * 0.44f
    for (i in 0 until numTeeth) {
        val angle = (i * (2 * Math.PI / numTeeth)).toFloat()
        val cosA = Math.cos(angle.toDouble()).toFloat()
        val sinA = Math.sin(angle.toDouble()).toFloat()
        drawLine(
            color = color,
            start = Offset(cx + rInner * cosA, cy + rInner * sinA),
            end = Offset(cx + rOuter * cosA, cy + rOuter * sinA),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawNoteIconSmall(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.2f, size.height * 0.15f),
        size = Size(size.width * 0.6f, size.height * 0.7f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = stroke
    )
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.4f), end = Offset(size.width * 0.65f, size.height * 0.4f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.6f), end = Offset(size.width * 0.55f, size.height * 0.6f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawSubjectsIconSmall(color: Color) {
    drawLine(color = color, start = Offset(size.width * 0.25f, size.height * 0.8f), end = Offset(size.width * 0.25f, size.height * 0.4f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.8f), end = Offset(size.width * 0.5f, size.height * 0.2f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.75f, size.height * 0.8f), end = Offset(size.width * 0.75f, size.height * 0.5f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawQuizIconSmall(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = size.width * 0.35f, center = Offset(size.width / 2, size.height / 2), style = stroke)
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

private fun DrawScope.drawDatabaseIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawArc(color = color, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(size.width * 0.2f, size.height * 0.15f), size = Size(size.width * 0.6f, size.height * 0.22f), style = stroke)
    drawArc(color = color, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(size.width * 0.2f, size.height * 0.38f), size = Size(size.width * 0.6f, size.height * 0.22f), style = stroke)
    drawArc(color = color, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(size.width * 0.2f, size.height * 0.62f), size = Size(size.width * 0.6f, size.height * 0.22f), style = stroke)
}

private fun DrawScope.drawCalculatorIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.2f, size.height * 0.15f),
        size = Size(size.width * 0.6f, size.height * 0.7f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.3f, size.height * 0.25f),
        size = Size(size.width * 0.4f, size.height * 0.18f),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = stroke
    )
}

private fun DrawScope.drawChevronRight(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.35f, size.height * 0.2f)
        lineTo(size.width * 0.65f, size.height * 0.5f)
        lineTo(size.width * 0.35f, size.height * 0.8f)
    }
    drawPath(path = path, color = color, style = stroke)
}

private fun DrawScope.drawHomeIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.45f)
        lineTo(size.width * 0.5f, size.height * 0.15f)
        lineTo(size.width * 0.85f, size.height * 0.45f)
        lineTo(size.width * 0.85f, size.height * 0.85f)
        lineTo(size.width * 0.15f, size.height * 0.85f)
        close()
    }
    drawPath(path = path, color = color, style = stroke)
}

private fun DrawScope.drawNotesTabIcon(color: Color) {
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

private fun DrawScope.drawProfileTabIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(
        color = color,
        radius = size.width * 0.22f,
        center = Offset(size.width * 0.5f, size.height * 0.32f),
        style = stroke
    )
    val bodyPath = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.82f)
        quadraticTo(
            size.width * 0.2f, size.height * 0.62f,
            size.width * 0.5f, size.height * 0.62f
        )
        quadraticTo(
            size.width * 0.8f, size.height * 0.62f,
            size.width * 0.8f, size.height * 0.82f
        )
    }
    drawPath(path = bodyPath, color = color, style = stroke)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview() {
    AIBasedClassNotesAppTheme {
        DashboardScreenContent(uiState = DashboardUiState())
    }
}
