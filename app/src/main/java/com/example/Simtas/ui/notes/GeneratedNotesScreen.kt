package com.example.Simtas.ui.notes

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
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

val SummaryCardBg = Color(0xFFF1EDFE)

/**
 * Generated Notes ("Your notes are ready") screen adhering to MVVM architecture.
 */
@Composable
fun GeneratedNotesScreen(
    modifier: Modifier = Modifier,
    viewModel: GeneratedNotesViewModel = remember { GeneratedNotesViewModel() },
    onBackClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onViewFullNotesClick: () -> Unit = {},
    onTakeQuizClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    GeneratedNotesScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onMoreOptionsClick = onMoreOptionsClick,
        onViewFullNotesClick = {
            viewModel.onViewFullNotesClicked()
            onViewFullNotesClick()
        },
        onTakeQuizClick = {
            viewModel.onTakeQuizClicked()
            onTakeQuizClick()
        },
        onSaveNotesClick = viewModel::onSaveNotesClicked
    )
}

@Composable
fun GeneratedNotesScreenContent(
    uiState: GeneratedNotesUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onViewFullNotesClick: () -> Unit = {},
    onTakeQuizClick: () -> Unit = {},
    onSaveNotesClick: () -> Unit = {}
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
                    text = uiState.metadataText,
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // AI Summary Highlight Card
                AiSummaryCard(summaryText = uiState.aiSummaryText)

                Spacer(modifier = Modifier.height(24.dp))

                // Key Takeaways Section
                Text(
                    text = "The key takeaways",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.keyTakeaways.forEach { takeaway ->
                        KeyTakeawayItemRow(text = takeaway)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Important Terms Section
                Text(
                    text = "Important terms",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Terms Tag Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.importantTerms.forEach { term ->
                        ImportantTermChip(text = term)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // View Full Notes Main CTA Button
                Button(
                    onClick = onViewFullNotesClick,
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
                            drawOpenBookIcon(Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "View Full Notes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Secondary Actions Row (Take Quiz & Save Notes)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Take Quiz Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(InnerCircleBg)
                            .clickable { onTakeQuizClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                drawBrainIcon(PurplePrimary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Take Quiz",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary
                            )
                        }
                    }

                    // Save Notes Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(InnerCircleBg)
                            .clickable { onSaveNotesClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                drawBookmarkIcon(PurplePrimary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (uiState.isSaved) "Saved" else "Save Notes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Footer Disclaimer
                Text(
                    text = "AI-assisted notes · Always verify with your course material",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
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
                text = "Your notes are ready",
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
 * AI Summary Highlight Card
 */
@Composable
private fun AiSummaryCard(summaryText: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SummaryCardBg)
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawSparkleStarIcon(PurplePrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI summary",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = summaryText,
                fontSize = 13.sp,
                color = TextDarkPrimary,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Key Takeaway Row Item
 */
@Composable
private fun KeyTakeawayItemRow(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Canvas(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        ) {
            drawCheckmarkCircle(PurplePrimary)
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = text,
            fontSize = 13.sp,
            color = TextDarkPrimary,
            lineHeight = 19.sp
        )
    }
}

/**
 * Important Term Tag Chip
 */
@Composable
private fun ImportantTermChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(InnerCircleBg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = PurplePrimary
        )
    }
}

// --- Custom Canvas Graphics Helpers ---

private fun DrawScope.drawOpenBookIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val leftPath = Path().apply {
        moveTo(size.width * 0.5f, size.height * 0.3f)
        quadraticTo(size.width * 0.3f, size.height * 0.22f, size.width * 0.15f, size.height * 0.26f)
        lineTo(size.width * 0.15f, size.height * 0.78f)
        quadraticTo(size.width * 0.3f, size.height * 0.74f, size.width * 0.5f, size.height * 0.82f)
        close()
    }
    drawPath(path = leftPath, color = color, style = stroke)

    val rightPath = Path().apply {
        moveTo(size.width * 0.5f, size.height * 0.3f)
        quadraticTo(size.width * 0.7f, size.height * 0.22f, size.width * 0.85f, size.height * 0.26f)
        lineTo(size.width * 0.85f, size.height * 0.78f)
        quadraticTo(size.width * 0.7f, size.height * 0.74f, size.width * 0.5f, size.height * 0.82f)
        close()
    }
    drawPath(path = rightPath, color = color, style = stroke)
}

private fun DrawScope.drawBrainIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = size.width * 0.28f, center = Offset(size.width * 0.36f, size.height * 0.5f), style = stroke)
    drawCircle(color = color, radius = size.width * 0.28f, center = Offset(size.width * 0.64f, size.height * 0.5f), style = stroke)
    drawLine(color = color, start = Offset(size.width * 0.36f, size.height * 0.5f), end = Offset(size.width * 0.64f, size.height * 0.5f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawBookmarkIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.25f, size.height * 0.18f)
        lineTo(size.width * 0.75f, size.height * 0.18f)
        lineTo(size.width * 0.75f, size.height * 0.82f)
        lineTo(size.width * 0.5f, size.height * 0.65f)
        lineTo(size.width * 0.25f, size.height * 0.82f)
        close()
    }
    drawPath(path = path, color = color, style = stroke)
}

private fun DrawScope.drawCheckmarkCircle(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = size.width * 0.42f, center = Offset(size.width / 2, size.height / 2), style = stroke)
    val checkPath = Path().apply {
        moveTo(size.width * 0.32f, size.height * 0.5f)
        lineTo(size.width * 0.46f, size.height * 0.64f)
        lineTo(size.width * 0.68f, size.height * 0.38f)
    }
    drawPath(path = checkPath, color = color, style = stroke)
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
fun GeneratedNotesScreenPreview() {
    AIBasedClassNotesAppTheme {
        GeneratedNotesScreenContent(uiState = GeneratedNotesUiState())
    }
}
