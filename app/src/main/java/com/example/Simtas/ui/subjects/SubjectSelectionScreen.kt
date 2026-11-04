package com.example.Simtas.ui.subjects

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.InactiveDot
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.TextSubtitle

val SelectedCardBg = Color(0xFFF3EFFF)

/**
 * Subject Selection ("Choose subjects") screen adhering to MVVM architecture.
 */
@Composable
fun SubjectSelectionScreen(
    modifier: Modifier = Modifier,
    viewModel: SubjectSelectionViewModel = remember { SubjectSelectionViewModel() },
    onBackClick: () -> Unit = {},
    onStartLearningClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    SubjectSelectionScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onSubjectToggle = viewModel::toggleSubjectSelection,
        onStartLearningClick = {
            viewModel.onStartLearningClicked()
            onStartLearningClick()
        }
    )
}

@Composable
fun SubjectSelectionScreenContent(
    uiState: SubjectSelectionUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onSubjectToggle: (String) -> Unit = {},
    onStartLearningClick: () -> Unit = {}
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

                Spacer(modifier = Modifier.height(20.dp))

                // Step Progress Indicator Bar (Step 2 of 2)
                StepProgressBar(
                    currentStep = uiState.currentStep,
                    totalSteps = uiState.totalSteps
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Title & Subtitle
                Text(
                    text = "What are you learning?",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Pick your subjects. We'll keep everything neatly organized.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSubtitle,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Primary CTA Button right below subtitle
                Button(
                    onClick = onStartLearningClick,
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
                            text = "Start Learning - ${uiState.selectedCount} subjects",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // List of Subject Cards
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.subjects.forEach { subject ->
                        SubjectCardItem(
                            subject = subject,
                            onToggle = { onSubjectToggle(subject.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

/**
 * Top Navigation Bar with Back Arrow and Title
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
            text = "Choose subjects",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )
    }
}

/**
 * Step Progress Indicator Bar (Step 2 of 2 - 100% full)
 */
@Composable
private fun StepProgressBar(
    currentStep: Int,
    totalSteps: Int
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "BUILD YOUR STUDY SPACE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PurplePrimary,
                letterSpacing = 1.1.sp
            )

            Text(
                text = "Step $currentStep of $totalSteps",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress bar background & full fill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(PurplePrimary)
        )
    }
}

/**
 * Individual Selectable Subject Card
 */
@Composable
private fun SubjectCardItem(
    subject: SubjectItem,
    onToggle: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val bgColor = if (subject.isSelected) SelectedCardBg else Color.White
    val borderModifier = if (subject.isSelected) {
        Modifier.border(1.5.dp, PurplePrimary.copy(alpha = 0.6f), shape)
    } else {
        Modifier.border(1.dp, Color.Transparent, shape)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .then(borderModifier)
            .clickable { onToggle() }
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
                // Left Icon Container
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(InnerCircleBg)
                ) {
                    Canvas(modifier = Modifier.size(22.dp)) {
                        when (subject.iconType) {
                            SubjectIconType.ComputerScience -> drawComputerIcon(PurplePrimary)
                            SubjectIconType.Mathematics -> drawCalculatorIcon(PurplePrimary)
                            SubjectIconType.Physics -> drawAtomIcon(PurplePrimary)
                            SubjectIconType.Chemistry -> drawFlaskIcon(PurplePrimary)
                            SubjectIconType.English -> drawBookIcon(PurplePrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = subject.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = subject.description,
                        fontSize = 12.sp,
                        color = TextSubtitle
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right Checkmark / Radio Indicator
            Canvas(modifier = Modifier.size(22.dp)) {
                if (subject.isSelected) {
                    drawCheckmarkCircle(PurplePrimary)
                } else {
                    drawEmptyRadioCircle(InactiveDot)
                }
            }
        }
    }
}

// --- Custom Canvas Graphics Helpers ---

private fun DrawScope.drawComputerIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    // Monitor screen
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.15f, size.height * 0.15f),
        size = Size(size.width * 0.7f, size.height * 0.5f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
    // Stand
    drawLine(
        color = color,
        start = Offset(size.width * 0.5f, size.height * 0.65f),
        end = Offset(size.width * 0.5f, size.height * 0.8f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
    // Base
    drawLine(
        color = color,
        start = Offset(size.width * 0.3f, size.height * 0.8f),
        end = Offset(size.width * 0.7f, size.height * 0.8f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawCalculatorIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    // Calculator body
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.2f, size.height * 0.12f),
        size = Size(size.width * 0.6f, size.height * 0.76f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
    // Display screen
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.3f, size.height * 0.22f),
        size = Size(size.width * 0.4f, size.height * 0.18f),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = stroke
    )
    // Buttons (4 dots)
    drawCircle(color = color, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.38f, size.height * 0.52f))
    drawCircle(color = color, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.62f, size.height * 0.52f))
    drawCircle(color = color, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.38f, size.height * 0.72f))
    drawCircle(color = color, radius = 1.8.dp.toPx(), center = Offset(size.width * 0.62f, size.height * 0.72f))
}

private fun DrawScope.drawAtomIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    // Center nucleus
    drawCircle(color = color, radius = size.width * 0.12f, center = Offset(size.width / 2, size.height / 2))
    // Outer orbit circle
    drawCircle(color = color, radius = size.width * 0.38f, center = Offset(size.width / 2, size.height / 2), style = stroke)
}

private fun DrawScope.drawFlaskIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.38f, size.height * 0.15f)
        lineTo(size.width * 0.62f, size.height * 0.15f)
        moveTo(size.width * 0.44f, size.height * 0.15f)
        lineTo(size.width * 0.44f, size.height * 0.4f)
        lineTo(size.width * 0.18f, size.height * 0.82f)
        quadraticTo(
            size.width * 0.18f, size.height * 0.88f,
            size.width * 0.5f, size.height * 0.88f
        )
        quadraticTo(
            size.width * 0.82f, size.height * 0.88f,
            size.width * 0.82f, size.height * 0.82f
        )
        lineTo(size.width * 0.56f, size.height * 0.4f)
        lineTo(size.width * 0.56f, size.height * 0.15f)
    }
    drawPath(path = path, color = color, style = stroke)
}

private fun DrawScope.drawBookIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    // Book left page
    val leftPath = Path().apply {
        moveTo(size.width * 0.5f, size.height * 0.3f)
        quadraticTo(
            size.width * 0.3f, size.height * 0.22f,
            size.width * 0.15f, size.height * 0.26f
        )
        lineTo(size.width * 0.15f, size.height * 0.78f)
        quadraticTo(
            size.width * 0.3f, size.height * 0.74f,
            size.width * 0.5f, size.height * 0.82f
        )
        close()
    }
    drawPath(path = leftPath, color = color, style = stroke)

    // Book right page
    val rightPath = Path().apply {
        moveTo(size.width * 0.5f, size.height * 0.3f)
        quadraticTo(
            size.width * 0.7f, size.height * 0.22f,
            size.width * 0.85f, size.height * 0.26f
        )
        lineTo(size.width * 0.85f, size.height * 0.78f)
        quadraticTo(
            size.width * 0.7f, size.height * 0.74f,
            size.width * 0.5f, size.height * 0.82f
        )
        close()
    }
    drawPath(path = rightPath, color = color, style = stroke)
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

private fun DrawScope.drawCheckmarkCircle(color: Color) {
    drawCircle(
        color = color,
        radius = size.width * 0.45f,
        center = Offset(size.width / 2, size.height / 2)
    )
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    val checkPath = Path().apply {
        moveTo(size.width * 0.3f, size.height * 0.5f)
        lineTo(size.width * 0.44f, size.height * 0.65f)
        lineTo(size.width * 0.7f, size.height * 0.38f)
    }
    drawPath(path = checkPath, color = Color.White, style = stroke)
}

private fun DrawScope.drawEmptyRadioCircle(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(
        color = color,
        radius = size.width * 0.42f,
        center = Offset(size.width / 2, size.height / 2),
        style = stroke
    )
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
fun SubjectSelectionScreenPreview() {
    AIBasedClassNotesAppTheme {
        SubjectSelectionScreenContent(uiState = SubjectSelectionUiState())
    }
}
