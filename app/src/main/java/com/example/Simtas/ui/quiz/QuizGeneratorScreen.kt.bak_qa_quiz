package com.example.Simtas.ui.quiz

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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

val QuizCardBg = Color(0xFFEEF3FF)
val SegmentSelectedBg = Color(0xFFEFEBFD)

/**
 * AI Quiz Generator screen adhering to MVVM architecture.
 */
@Composable
fun QuizGeneratorScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizGeneratorViewModel = remember { QuizGeneratorViewModel() },
    onBackClick: () -> Unit = {},
    onGenerateQuizClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    QuizGeneratorScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onSubjectSelect = viewModel::onSubjectSelected,
        onQuestionsCountSelect = viewModel::onNumberOfQuestionsSelected,
        onQuestionTypeSelect = viewModel::onQuestionTypeSelected,
        onDifficultySelect = viewModel::onDifficultySelected,
        onGenerateQuizClick = {
            viewModel.onGenerateQuizClicked()
            onGenerateQuizClick()
        }
    )
}

@Composable
fun QuizGeneratorScreenContent(
    uiState: QuizGeneratorUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSubjectSelect: (String) -> Unit = {},
    onQuestionsCountSelect: (Int) -> Unit = {},
    onQuestionTypeSelect: (QuestionType) -> Unit = {},
    onDifficultySelect: (DifficultyLevel) -> Unit = {},
    onGenerateQuizClick: () -> Unit = {}
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
                TopNavigationBar(onBackClick = onBackClick)

                Spacer(modifier = Modifier.height(24.dp))

                // Headline & Subtitle
                Text(
                    text = "Turn knowing into\nunderstanding.",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A little practice makes a big difference.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSubtitle
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Based On Your Lecture Card
                BasedOnLectureCard(
                    lectureTitle = uiState.lectureTitle,
                    lectureMetadata = uiState.lectureMetadata
                )

                Spacer(modifier = Modifier.height(20.dp))

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

                Spacer(modifier = Modifier.height(18.dp))

                // Number of questions Selector
                Text(
                    text = "Number of questions",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(5, 10, 15).forEach { count ->
                        SegmentPillItem(
                            text = count.toString(),
                            isSelected = uiState.numberOfQuestions == count,
                            onClick = { onQuestionsCountSelect(count) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Question type Selector
                Text(
                    text = "Question type",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        QuestionType.MCQ to "MCQ",
                        QuestionType.TRUE_FALSE to "True / False",
                        QuestionType.MIXED to "Mixed"
                    ).forEach { (type, label) ->
                        SegmentPillItem(
                            text = label,
                            isSelected = uiState.questionType == type,
                            onClick = { onQuestionTypeSelect(type) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Difficulty Selector
                Text(
                    text = "Difficulty",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        DifficultyLevel.EASY to "Easy",
                        DifficultyLevel.MODERATE to "Moderate",
                        DifficultyLevel.HARD to "Hard"
                    ).forEach { (level, label) ->
                        SegmentPillItem(
                            text = label,
                            isSelected = uiState.difficulty == level,
                            onClick = { onDifficultySelect(level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Estimated Time Summary Line
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        drawClockIcon(TextMuted)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.estimatedTimeText,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Generate Quiz CTA Button
                Button(
                    onClick = onGenerateQuizClick,
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
                            drawSparkleStarIcon(Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Generate Quiz",
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
 * Top Navigation Bar
 */
@Composable
private fun TopNavigationBar(onBackClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
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
            text = "AI quiz generator",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )
    }
}

/**
 * Based on your lecture Card
 */
@Composable
private fun BasedOnLectureCard(
    lectureTitle: String,
    lectureMetadata: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(QuizCardBg)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    drawNotePadIconSmall(PurplePrimary)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BASED ON YOUR LECTURE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    letterSpacing = 1.1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = lectureTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = lectureMetadata,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

/**
 * Segment Pill Selector Item
 */
@Composable
private fun SegmentPillItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    val bgColor = if (isSelected) SegmentSelectedBg else Color.White
    val borderModifier = if (isSelected) {
        Modifier.border(1.5.dp, PurplePrimary, shape)
    } else {
        Modifier.border(1.dp, Color.Transparent, shape)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .height(46.dp)
            .clip(shape)
            .background(bgColor)
            .then(borderModifier)
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) PurplePrimary else TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Dropdown Select Field
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

private fun DrawScope.drawNotePadIconSmall(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.2f, size.height * 0.15f),
        size = Size(size.width * 0.6f, size.height * 0.7f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = stroke
    )
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.4f), end = Offset(size.width * 0.65f, size.height * 0.4f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawClockIcon(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = size.width * 0.42f, center = Offset(size.width / 2, size.height / 2), style = stroke)
    drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.5f), end = Offset(size.width * 0.5f, size.height * 0.25f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.5f), end = Offset(size.width * 0.72f, size.height * 0.5f), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
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
fun QuizGeneratorScreenPreview() {
    AIBasedClassNotesAppTheme {
        QuizGeneratorScreenContent(uiState = QuizGeneratorUiState())
    }
}
