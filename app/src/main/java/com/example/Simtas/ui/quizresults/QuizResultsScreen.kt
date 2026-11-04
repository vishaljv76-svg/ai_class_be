package com.example.Simtas.ui.quizresults

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

val ScoreHeroCardBg = Color(0xFFF1EDFE)
val GreenCorrectBg = Color(0xFFE8F5E9)
val GreenCorrectText = Color(0xFF2E7D32)
val RedIncorrectBg = Color(0xFFFFEBEE)
val RedIncorrectText = Color(0xFFC62828)

/**
 * Quiz Results screen adhering to MVVM architecture.
 */
@Composable
fun QuizResultsScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizResultsViewModel = remember { QuizResultsViewModel() },
    onBackClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onRetryQuizClick: () -> Unit = {},
    onBackToNotesClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    QuizResultsScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onShareClick = onShareClick,
        onRetryQuizClick = {
            viewModel.onRetryQuizClicked()
            onRetryQuizClick()
        },
        onBackToNotesClick = {
            viewModel.onBackToNotesClicked()
            onBackToNotesClick()
        }
    )
}

@Composable
fun QuizResultsScreenContent(
    uiState: QuizResultsUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onRetryQuizClick: () -> Unit = {},
    onBackToNotesClick: () -> Unit = {},
    onViewAllAnswersClick: () -> Unit = {}
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
                    onShareClick = onShareClick
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Score Hero Card
                ScoreHeroCard(uiState = uiState)

                Spacer(modifier = Modifier.height(20.dp))

                // Performance Feedback Summary
                Text(
                    text = uiState.feedbackHeader,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = uiState.feedbackText,
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Review Your Answers Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Review your answers",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )

                    Text(
                        text = "View all ${uiState.totalQuestions}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary,
                        modifier = Modifier.clickable { onViewAllAnswersClick() }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Review Items List
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.reviewItems.forEach { review ->
                        AnswerReviewCardItem(review = review)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Buttons Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Retry Quiz Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(PurplePrimary)
                            .clickable { onRetryQuizClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                drawRefreshIcon(Color.White)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Retry Quiz",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Back to Notes Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(InnerCircleBg)
                            .clickable { onBackToNotesClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(16.dp)) {
                                drawOpenBookIcon(PurplePrimary)
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Back to Notes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PurplePrimary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Top Navigation Bar with Back Arrow, Title, and Share Icon
 */
@Composable
private fun TopNavigationBar(
    onBackClick: () -> Unit,
    onShareClick: () -> Unit
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
                text = "Quiz results",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
        }

        IconButton(
            onClick = onShareClick,
            modifier = Modifier.size(36.dp)
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                drawShareIcon(TextDarkPrimary)
            }
        }
    }
}

/**
 * Score Hero Card
 */
@Composable
private fun ScoreHeroCard(uiState: QuizResultsUiState) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ScoreHeroCardBg)
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Ribbon Medal Icon
            Canvas(modifier = Modifier.size(28.dp)) {
                drawRibbonMedalIcon(PurplePrimary)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Headline Title
            Text(
                text = uiState.titleMessage,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Score Percentage & Fraction
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${uiState.scorePercentage}%",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${uiState.correctCount} / ${uiState.totalQuestions} correct",
                    fontSize = 14.sp,
                    color = TextSubtitle,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = uiState.quizTopicMetadata,
                fontSize = 11.sp,
                color = TextMuted,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Correct & Incorrect Badges Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Correct Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GreenCorrectBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✓ ${uiState.correctCount} correct",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenCorrectText
                    )
                }

                // Incorrect Badge
                val incorrectCount = uiState.totalQuestions - uiState.correctCount
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(RedIncorrectBg)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "✕ $incorrectCount incorrect",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedIncorrectText
                    )
                }
            }
        }
    }
}

/**
 * Answer Review Card Item
 */
@Composable
private fun AnswerReviewCardItem(review: AnswerReviewItem) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.03f),
                spotColor = Color.Black.copy(alpha = 0.05f)
            )
            .clip(shape)
            .background(Color.White)
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(18.dp)) {
                    if (review.isCorrect) {
                        drawCheckmarkCircleGreen(GreenCorrectText)
                    } else {
                        drawCrossCircleRed(RedIncorrectText)
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "${review.questionNumber} · ${review.questionTopic}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
            }

            if (!review.isCorrect && review.userAnswerText != null && review.correctAnswerText != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Your answer: ${review.userAnswerText} · Correct answer: ${review.correctAnswerText}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedIncorrectText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = review.explanationText,
                fontSize = 12.sp,
                color = TextMuted,
                lineHeight = 17.sp
            )
        }
    }
}

// --- Custom Canvas Graphics Helpers ---

private fun DrawScope.drawRibbonMedalIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(
        color = color,
        radius = size.width * 0.32f,
        center = Offset(size.width * 0.5f, size.height * 0.38f),
        style = stroke
    )
    val ribbonPath = Path().apply {
        moveTo(size.width * 0.38f, size.height * 0.65f)
        lineTo(size.width * 0.3f, size.height * 0.95f)
        lineTo(size.width * 0.5f, size.height * 0.82f)
        lineTo(size.width * 0.7f, size.height * 0.95f)
        lineTo(size.width * 0.62f, size.height * 0.65f)
    }
    drawPath(path = ribbonPath, color = color, style = stroke)
}

private fun DrawScope.drawCheckmarkCircleGreen(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = size.width * 0.42f, center = Offset(size.width / 2, size.height / 2), style = stroke)
    val checkPath = Path().apply {
        moveTo(size.width * 0.32f, size.height * 0.5f)
        lineTo(size.width * 0.46f, size.height * 0.64f)
        lineTo(size.width * 0.68f, size.height * 0.38f)
    }
    drawPath(path = checkPath, color = color, style = stroke)
}

private fun DrawScope.drawCrossCircleRed(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = size.width * 0.42f, center = Offset(size.width / 2, size.height / 2), style = stroke)
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.35f), end = Offset(size.width * 0.65f, size.height * 0.65f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.65f, size.height * 0.35f), end = Offset(size.width * 0.35f, size.height * 0.65f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawRefreshIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawArc(color = color, startAngle = 40f, sweepAngle = 280f, useCenter = false, topLeft = Offset(size.width * 0.15f, size.height * 0.15f), size = Size(size.width * 0.7f, size.height * 0.7f), style = stroke)
    val arrowHead = Path().apply {
        moveTo(size.width * 0.65f, size.height * 0.12f)
        lineTo(size.width * 0.85f, size.height * 0.25f)
        lineTo(size.width * 0.72f, size.height * 0.42f)
    }
    drawPath(path = arrowHead, color = color)
}

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

private fun DrawScope.drawShareIcon(color: Color) {
    drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.75f, size.height * 0.25f))
    drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.25f, size.height * 0.5f))
    drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.75f, size.height * 0.75f))
    drawLine(color = color, start = Offset(size.width * 0.28f, size.height * 0.48f), end = Offset(size.width * 0.72f, size.height * 0.27f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.28f, size.height * 0.52f), end = Offset(size.width * 0.72f, size.height * 0.73f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
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
fun QuizResultsScreenPreview() {
    AIBasedClassNotesAppTheme {
        QuizResultsScreenContent(uiState = QuizResultsUiState())
    }
}
