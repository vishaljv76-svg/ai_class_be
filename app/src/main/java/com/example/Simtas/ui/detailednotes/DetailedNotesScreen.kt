package com.example.Simtas.ui.detailednotes

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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

val CalloutBoxBg = Color(0xFFF1EDFE)

/**
 * Detailed Notes ("Lecture notes") screen adhering to MVVM architecture.
 */
@Composable
fun DetailedNotesScreen(
    modifier: Modifier = Modifier,
    viewModel: DetailedNotesViewModel = remember { DetailedNotesViewModel() },
    onBackClick: () -> Unit = {},
    onDownloadPdfClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    DetailedNotesScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onBookmarkToggle = viewModel::toggleBookmark,
        onDownloadPdfClick = {
            viewModel.onDownloadPdfClicked()
            onDownloadPdfClick()
        }
    )
}

@Composable
fun DetailedNotesScreenContent(
    uiState: DetailedNotesUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onBookmarkToggle: () -> Unit = {},
    onDownloadPdfClick: () -> Unit = {}
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
                    isBookmarked = uiState.isBookmarked,
                    onBookmarkToggle = onBookmarkToggle
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Course & Tag Badges Row
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(InnerCircleBg)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = uiState.courseCode,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFEEF3FF))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = uiState.tagText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PurplePrimary
                        )
                    }
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
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Detailed Notes Card Container
                DetailedNotesContentCard(uiState = uiState)

                Spacer(modifier = Modifier.height(24.dp))

                // Download PDF CTA Button
                Button(
                    onClick = onDownloadPdfClick,
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
                            drawDownloadTrayIcon(Color.White)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Download PDF",
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
 * Top Navigation Bar with Back Arrow, Title, and Bookmark Icon
 */
@Composable
private fun TopNavigationBar(
    onBackClick: () -> Unit,
    isBookmarked: Boolean,
    onBookmarkToggle: () -> Unit
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
                text = "Lecture notes",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
        }

        IconButton(
            onClick = onBookmarkToggle,
            modifier = Modifier.size(36.dp)
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                drawBookmarkIcon(if (isBookmarked) PurplePrimary else TextDarkPrimary, isFilled = isBookmarked)
            }
        }
    }
}

/**
 * Detailed Notes Content Card
 */
@Composable
private fun DetailedNotesContentCard(uiState: DetailedNotesUiState) {
    val shape = RoundedCornerShape(24.dp)
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
            .padding(20.dp)
    ) {
        Column {
            // Section 1
            Text(
                text = uiState.section1Title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = uiState.section1Content,
                fontSize = 13.sp,
                color = TextDarkPrimary,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Section 2
            Text(
                text = uiState.section2Title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                uiState.section2Bullets.forEach { bullet ->
                    Text(
                        text = "• $bullet",
                        fontSize = 13.sp,
                        color = TextDarkPrimary,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Highlight Callout Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CalloutBoxBg)
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        text = uiState.calloutTitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = uiState.calloutContent,
                        fontSize = 12.sp,
                        color = TextDarkPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Section 3
            Text(
                text = uiState.section3Title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = uiState.section3Content1,
                fontSize = 13.sp,
                color = TextDarkPrimary,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = uiState.section3Content2,
                fontSize = 13.sp,
                color = TextDarkPrimary,
                lineHeight = 19.sp
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Excerpt Footer
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawChevronDown(TextMuted)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = uiState.footerText,
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

// --- Custom Canvas Graphics Helpers ---

private fun DrawScope.drawDownloadTrayIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val trayPath = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.65f)
        lineTo(size.width * 0.2f, size.height * 0.8f)
        lineTo(size.width * 0.8f, size.height * 0.8f)
        lineTo(size.width * 0.8f, size.height * 0.65f)
    }
    drawPath(path = trayPath, color = color, style = stroke)

    // Downward arrow
    drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.2f), end = Offset(size.width * 0.5f, size.height * 0.6f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val headPath = Path().apply {
        moveTo(size.width * 0.35f, size.height * 0.48f)
        lineTo(size.width * 0.5f, size.height * 0.6f)
        lineTo(size.width * 0.65f, size.height * 0.48f)
    }
    drawPath(path = headPath, color = color, style = stroke)
}

private fun DrawScope.drawBookmarkIcon(color: Color, isFilled: Boolean) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.25f, size.height * 0.18f)
        lineTo(size.width * 0.75f, size.height * 0.18f)
        lineTo(size.width * 0.75f, size.height * 0.82f)
        lineTo(size.width * 0.5f, size.height * 0.65f)
        lineTo(size.width * 0.25f, size.height * 0.82f)
        close()
    }
    if (isFilled) {
        drawPath(path = path, color = color)
    } else {
        drawPath(path = path, color = color, style = stroke)
    }
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
fun DetailedNotesScreenPreview() {
    AIBasedClassNotesAppTheme {
        DetailedNotesScreenContent(uiState = DetailedNotesUiState())
    }
}

