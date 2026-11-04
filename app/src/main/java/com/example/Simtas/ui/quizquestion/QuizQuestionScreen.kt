package com.example.Simtas.ui.quizquestion

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
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

val OptionSelectedBg = Color(0xFFF1EDFE)
val OptionBadgeGreyBg = Color(0xFFF0F0F5)

/**
 * Quiz Question ("Knowledge check") screen adhering to MVVM architecture.
 */
@Composable
fun QuizQuestionScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizQuestionViewModel = remember { QuizQuestionViewModel() },
    onBackClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onQuizFinish: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isQuizFinished) {
        if (uiState.isQuizFinished) {
            onQuizFinish()
        }
    }

    QuizQuestionScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onMoreOptionsClick = onMoreOptionsClick,
        onOptionSelect = viewModel::selectOption,
        onNextClick = {
            if (uiState.currentQuestionIndex >= uiState.totalQuestions) {
                viewModel.onNextClicked()
                onQuizFinish()
            } else {
                viewModel.onNextClicked()
            }
        }
    )
}

@Composable
fun QuizQuestionScreenContent(
    uiState: QuizQuestionUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {},
    onOptionSelect: (String) -> Unit = {},
    onNextClick: () -> Unit = {}
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

                // Progress & Mode Header Bar
                QuizProgressBar(
                    modeText = uiState.modeText,
                    currentQuestionIndex = uiState.currentQuestionIndex,
                    totalQuestions = uiState.totalQuestions,
                    topicText = uiState.topicText
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Question Title & Instruction
                Text(
                    text = uiState.questionText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary,
                    lineHeight = 28.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = uiState.instructionText,
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 4 Multiple Choice Option Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.options.forEach { option ->
                        QuizOptionCardItem(
                            option = option,
                            isSelected = uiState.selectedOptionKey == option.optionKey,
                            onSelect = { onOptionSelect(option.optionKey) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Bottom CTA Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Status Note Line
                Text(
                    text = uiState.statusNoteText,
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Next Button
                Button(
                    onClick = onNextClick,
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
                            drawRightArrow(Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Next",
                            fontSize = 16.sp,
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
                text = "Knowledge check",
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
 * Quiz Progress Bar
 */
@Composable
private fun QuizProgressBar(
    modeText: String,
    currentQuestionIndex: Int,
    totalQuestions: Int,
    topicText: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(InnerCircleBg)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = modeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PurplePrimary
                )
            }

            Text(
                text = "Question $currentQuestionIndex of $totalQuestions",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PurplePrimary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Progress bar background & fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(InnerCircleBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(currentQuestionIndex.toFloat() / totalQuestions.toFloat())
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(PurplePrimary)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = topicText,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

/**
 * Multiple Choice Option Card Item
 */
@Composable
private fun QuizOptionCardItem(
    option: QuizOptionItem,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val bgColor = if (isSelected) OptionSelectedBg else Color.White
    val borderModifier = if (isSelected) {
        Modifier.border(1.5.dp, PurplePrimary, shape)
    } else {
        Modifier.border(1.dp, Color.Transparent, shape)
    }

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
            .background(bgColor)
            .then(borderModifier)
            .clickable { onSelect() }
            .padding(16.dp)
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
                // Key Badge Circle (A, B, C, D)
                val badgeBg = if (isSelected) PurplePrimary else OptionBadgeGreyBg
                val badgeTextColor = if (isSelected) Color.White else TextDarkPrimary

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(badgeBg)
                ) {
                    Text(
                        text = option.optionKey,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = option.text,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = TextDarkPrimary,
                    lineHeight = 18.sp
                )
            }

            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Canvas(modifier = Modifier.size(20.dp)) {
                    drawCheckmarkCircle(PurplePrimary)
                }
            }
        }
    }
}

// --- Custom Canvas Graphics Helpers ---

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

private fun DrawScope.drawRightArrow(color: Color) {
    val strokeWidth = 2.dp.toPx()
    drawLine(
        color = color,
        start = Offset(size.width * 0.2f, size.height * 0.5f),
        end = Offset(size.width * 0.8f, size.height * 0.5f),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    val headPath = Path().apply {
        moveTo(size.width * 0.58f, size.height * 0.28f)
        lineTo(size.width * 0.82f, size.height * 0.5f)
        lineTo(size.width * 0.58f, size.height * 0.72f)
    }
    drawPath(
        path = headPath,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun QuizQuestionScreenPreview() {
    AIBasedClassNotesAppTheme {
        QuizQuestionScreenContent(uiState = QuizQuestionUiState())
    }
}
