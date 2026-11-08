package com.example.Simtas.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.CardBg
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.OuterRingBorder
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.PurpleSecondary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.TextSubtitle
import com.example.Simtas.ui.theme.ThemeManager

// --- Luxury Lavender AI Color Palette & Helpers ---
private val LuxuryVioletPrimary = Color(0xFF6C5CE7)
private val LuxuryPurpleDeep = Color(0xFF5B3DE8)
private val LuxuryElectricViolet = Color(0xFF8B5CF6)
private val LuxuryLilacGlow = Color(0xFFA78BFA)
private val LuxurySoftLavender = Color(0xFFEDE9FE)

private val CardBorderColor: Color
    get() = if (ThemeManager.isDark()) Color(0xFF332B52) else Color(0xFFE9D5FF).copy(alpha = 0.8f)

private val DividerSubtleColor: Color
    get() = if (ThemeManager.isDark()) Color(0xFF2E2548) else Color(0xFFF1EDFC)

private val ToolkitRecordBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF221A3A) else Color(0xFFF7F4FF)

private val ToolkitUploadBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF1B223D) else Color(0xFFEFF5FF)

private val ToolkitNotesBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF201B38) else Color(0xFFF5F1FF)

private val ToolkitQuizBg: Color
    get() = if (ThemeManager.isDark()) Color(0xFF261936) else Color(0xFFFAF2FE)

/**
 * Home Dashboard Screen adhering to MVVM architecture.
 * Preserves 100% of existing signatures, callbacks and real ViewModel data.
 */
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = remember { DashboardViewModel() },
    onNotificationClick: () -> Unit = {},
    onRecordLectureClick: () -> Unit = {},
    onUploadAudioClick: () -> Unit = {},
    onMyNotesClick: () -> Unit = {},
    onAiQuizClick: () -> Unit = {},
    onViewAllNotesClick: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onTabSelect: (HomeBottomTab) -> Unit = {}
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
        onViewAllNotesClick = onViewAllNotesClick,
        onNoteClick = { noteId ->
            viewModel.onNoteItemClicked(noteId)
            onNoteClick(noteId)
        },
        onTabSelect = { tab ->
            viewModel.selectTab(tab)
            onTabSelect(tab)
        }
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
    onViewAllNotesClick: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onTabSelect: (HomeBottomTab) -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = SplashBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Ambient Luxury Lavender Background Glow behind the header
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                val isDark = ThemeManager.isDark()
                val glowCenter = Offset(size.width * 0.85f, size.height * 0.15f)
                val glowRadius = size.width * 0.7f
                val glowColor = if (isDark) Color(0xFF6C5CE7).copy(alpha = 0.12f) else Color(0xFFDDD6FE).copy(alpha = 0.45f)
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
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 84.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Top Header Bar with Brand Badge & Bell Icon
                TopDashboardHeader(onNotificationClick = onNotificationClick)

                Spacer(modifier = Modifier.height(18.dp))

                // 2. User Greeting & Avatar with Luxury Violet Gradient Ring
                UserGreetingRow(
                    userName = uiState.userName,
                    dateText = uiState.dateText,
                    userInitials = uiState.userInitials
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Featured AI Study Assistant Luxury Card
                AiStudyAssistantBanner(
                    onAiQuizClick = onAiQuizClick,
                    onRecordLectureClick = onRecordLectureClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Study Overview Metrics Card (Real ViewModel Data Only)
                MetricsOverviewCard(
                    totalNotes = uiState.totalNotes,
                    subjectsCount = uiState.subjectsCount,
                    quizzesCount = uiState.quizzesCount
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 5. Section Header: Your Study Toolkit
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp, 16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(LuxuryVioletPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Your Study Toolkit",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 6. 2x2 Study Toolkit Grid
                ToolkitGridSection(
                    onRecordLectureClick = onRecordLectureClick,
                    onUploadAudioClick = onUploadAudioClick,
                    onMyNotesClick = onMyNotesClick,
                    onAiQuizClick = onAiQuizClick
                )

                Spacer(modifier = Modifier.height(26.dp))

                // 7. Recent Notes Section Header
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
                            text = "Pick up where you left off",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
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

                // 8. Recent Notes List (Real Data with Graceful Empty State)
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

            // 9. Floating Luxury Bottom Navigation Bar
            HomeBottomNavigationBar(
                selectedTab = uiState.selectedTab,
                onTabSelect = onTabSelect,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

/**
 * Top Dashboard Header with App Brand Badge & Notification Bell with unread indicator
 */
@Composable
private fun TopDashboardHeader(onNotificationClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(LuxuryPurpleDeep, LuxuryElectricViolet)
                        )
                    )
            ) {
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawSparkleStarIcon(Color.White)
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column {
                Text(
                    text = "SmartNotes AI",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary
                )
                Text(
                    text = "AI-Powered Learning Suite",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }

        // Notification Bell Icon with Subtle Outer Ring & Unread Dot
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
            // Glowing Unread Indicator Dot
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .clip(CircleShape)
                    .background(LuxuryVioletPrimary)
            )
        }
    }
}

/**
 * User Greeting Row with Dynamic Date Capsule and Luxury Avatar Ring
 */
@Composable
private fun UserGreetingRow(
    userName: String,
    dateText: String,
    userInitials: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            // Date Capsule Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(InnerCircleBg)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(16.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawSparkleStarIcon(LuxuryVioletPrimary)
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = dateText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSubtitle
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Hey $userName 👋",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextDarkPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Your intelligent learning workspace is ready.",
                fontSize = 12.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Luxury Avatar Badge with Gradient Ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
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
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(CardBg)
            ) {
                Text(
                    text = userInitials.ifEmpty { "AM" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = LuxuryVioletPrimary
                )
            }
        }
    }
}

/**
 * Dedicated AI Study Assistant Luxury Card with animated glowing badge and quick actions
 */
@Composable
private fun AiStudyAssistantBanner(
    onAiQuizClick: () -> Unit,
    onRecordLectureClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "AiPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val shape = RoundedCornerShape(24.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                ambientColor = LuxuryPurpleDeep.copy(alpha = 0.25f),
                spotColor = LuxuryElectricViolet.copy(alpha = 0.35f)
            )
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF4C30D4),
                        Color(0xFF6C5CE7),
                        Color(0xFF8B5CF6)
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
                        Color.White.copy(alpha = 0.10f)
                    )
                ),
                shape
            )
            .padding(20.dp)
    ) {
        // Subtle Background Decorative Arc in Canvas
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.05f),
                radius = size.width * 0.45f,
                center = Offset(size.width * 0.95f, size.height * 0.1f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.03f),
                radius = size.width * 0.7f,
                center = Offset(size.width * 0.95f, size.height * 0.1f)
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Pulsing AI Pill Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.20f))
                        .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(14.dp))
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

                // Small Sparkle Vector Indicator
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawSparkleStarIcon(Color.White.copy(alpha = 0.85f))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Synthesize Your Lectures",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Instant structured notes, active recall flashcards, and practice quizzes powered by AI.",
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color.White.copy(alpha = 0.90f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Primary Action Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable { onAiQuizClick() }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawSparkleStarIcon(LuxuryPurpleDeep)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Start AI Quiz",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = LuxuryPurpleDeep
                        )
                    }
                }

                // Secondary Action Button
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .clickable { onRecordLectureClick() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawMicrophoneIcon(Color.White)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Record",
                            fontSize = 12.sp,
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
 * Study Overview Metrics Card using real data from the ViewModel.
 */
@Composable
private fun MetricsOverviewCard(
    totalNotes: Int,
    subjectsCount: Int,
    quizzesCount: Int
) {
    val shape = RoundedCornerShape(22.dp)
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
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "STUDY OVERVIEW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = TextSubtitle
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(InnerCircleBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Synced",
                        fontSize = 10.sp,
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

                // Divider
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

                // Divider
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
                .size(34.dp)
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

/**
 * 2x2 Study Toolkit Grid Section with delicate multi-tone lavender fills
 */
@Composable
private fun ToolkitGridSection(
    onRecordLectureClick: () -> Unit,
    onUploadAudioClick: () -> Unit,
    onMyNotesClick: () -> Unit,
    onAiQuizClick: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LuxuryToolkitCard(
                title = "Record Lecture",
                subtitle = "Capture in real time",
                bgColor = ToolkitRecordBg,
                accentColor = LuxuryPurpleDeep,
                modifier = Modifier.weight(1f),
                onClick = onRecordLectureClick,
                icon = { drawMicrophoneIcon(LuxuryPurpleDeep) }
            )

            LuxuryToolkitCard(
                title = "Upload Audio",
                subtitle = "Import voice memos",
                bgColor = ToolkitUploadBg,
                accentColor = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f),
                onClick = onUploadAudioClick,
                icon = { drawUploadIcon(Color(0xFF3B82F6)) }
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            LuxuryToolkitCard(
                title = "My Notes",
                subtitle = "Sorted by subjects",
                bgColor = ToolkitNotesBg,
                accentColor = LuxuryVioletPrimary,
                modifier = Modifier.weight(1f),
                onClick = onMyNotesClick,
                icon = { drawNotePadIcon(LuxuryVioletPrimary) }
            )

            LuxuryToolkitCard(
                title = "AI Quiz",
                subtitle = "Active knowledge test",
                bgColor = ToolkitQuizBg,
                accentColor = LuxuryElectricViolet,
                modifier = Modifier.weight(1f),
                onClick = onAiQuizClick,
                icon = { drawSparkleStarIcon(LuxuryElectricViolet) }
            )
        }
    }
}

@Composable
private fun LuxuryToolkitCard(
    title: String,
    subtitle: String,
    bgColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: DrawScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = accentColor.copy(alpha = 0.06f),
                spotColor = accentColor.copy(alpha = 0.08f)
            )
            .clip(shape)
            .background(bgColor)
            .border(1.dp, CardBorderColor, shape)
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column(horizontalAlignment = Alignment.Start) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(InnerCircleBg)
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        icon()
                    }
                }

                // Subtle corner arrow indicator
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawChevronRight(TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = TextSubtitle
            )
        }
    }
}

/**
 * Recent Note Item Card
 */
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

/**
 * Empty Recent Notes fallback card
 */
@Composable
private fun EmptyRecentNotesCard(onRecordLectureClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
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
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(InnerCircleBg)
            ) {
                Canvas(modifier = Modifier.size(22.dp)) {
                    drawNotePadIcon(LuxuryVioletPrimary)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No notes created yet",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Tap here to record your first lecture",
                fontSize = 11.sp,
                color = TextSubtitle
            )
        }
    }
}

/**
 * Floating Luxury Bottom Navigation Bar
 */
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

// --- Custom Canvas Graphics Helpers ---

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

private fun DrawScope.drawUploadIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.55f)
        lineTo(size.width * 0.2f, size.height * 0.8f)
        lineTo(size.width * 0.8f, size.height * 0.8f)
        lineTo(size.width * 0.8f, size.height * 0.55f)
    }
    drawPath(path = path, color = color, style = stroke)
    drawLine(
        color = color,
        start = Offset(size.width * 0.5f, size.height * 0.65f),
        end = Offset(size.width * 0.5f, size.height * 0.2f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
    val headPath = Path().apply {
        moveTo(size.width * 0.35f, size.height * 0.35f)
        lineTo(size.width * 0.5f, size.height * 0.2f)
        lineTo(size.width * 0.65f, size.height * 0.35f)
    }
    drawPath(path = headPath, color = color, style = stroke)
}

private fun DrawScope.drawNotePadIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.2f, size.height * 0.15f),
        size = Size(size.width * 0.6f, size.height * 0.7f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.38f), end = Offset(size.width * 0.65f, size.height * 0.38f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.58f), end = Offset(size.width * 0.55f, size.height * 0.58f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
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
