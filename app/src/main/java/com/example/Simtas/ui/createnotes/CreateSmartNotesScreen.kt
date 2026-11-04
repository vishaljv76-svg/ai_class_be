package com.example.Simtas.ui.createnotes

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
import com.example.Simtas.ui.home.HomeBottomTab
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.TextSubtitle

/**
 * Create Smart Notes screen adhering to MVVM architecture.
 */
@Composable
fun CreateSmartNotesScreen(
    modifier: Modifier = Modifier,
    viewModel: CreateSmartNotesViewModel = remember { CreateSmartNotesViewModel() },
    onBackClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.startNotesGeneration()
    }

    CreateSmartNotesScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onContinueClick = onContinueClick,
        onTabSelect = viewModel::selectTab
    )
}

@Composable
fun CreateSmartNotesScreenContent(
    uiState: CreateSmartNotesUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onContinueClick: () -> Unit = {},
    onTabSelect: (HomeBottomTab) -> Unit = {}
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = SplashBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 80.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Top Bar
                TopNavigationBar(onBackClick = onBackClick)

                Spacer(modifier = Modifier.height(24.dp))

                // Central Circular Progress Ring Artwork
                CircularProgressArtwork(progressPercent = uiState.progressPercent)

                Spacer(modifier = Modifier.height(24.dp))

                // Main Title & Subtitle
                Text(
                    text = uiState.mainTitle,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = uiState.subtitleText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSubtitle,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Active Generation Progress Card
                ActiveGenerationCard(
                    stepText = uiState.currentStepText,
                    progressPercent = uiState.progressPercent,
                    statusText = uiState.progressStatusText
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Included in your notes Section
                Text(
                    text = "Included in your notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Feature Items List
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.includedFeatures.forEach { feature ->
                        NoteFeatureCardItem(feature = feature)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // View Generated Notes Primary CTA Button
                Button(
                    onClick = onContinueClick,
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
                            text = "View Generated Notes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Bottom Navigation Bar
            HomeBottomNavigationBar(
                selectedTab = uiState.selectedTab,
                onTabSelect = onTabSelect,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
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
            text = "Create smart notes",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )
    }
}

/**
 * Central Circular Progress Artwork
 */
@Composable
private fun CircularProgressArtwork(progressPercent: Int) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(140.dp)
    ) {
        Canvas(modifier = Modifier.size(130.dp)) {
            val strokeWidth = 8.dp.toPx()
            // Outer background circle
            drawCircle(
                color = InnerCircleBg,
                radius = size.width / 2,
                style = Stroke(width = strokeWidth)
            )
            // Arc progress
            drawArc(
                color = PurplePrimary,
                startAngle = -90f,
                sweepAngle = 360f * (progressPercent / 100f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        // Inner Sparkle Box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(InnerCircleBg)
        ) {
            Canvas(modifier = Modifier.size(32.dp)) {
                drawSparkleStarIcon(PurplePrimary)
            }
        }
    }
}

/**
 * Active Generation Progress Card
 */
@Composable
private fun ActiveGenerationCard(
    stepText: String,
    progressPercent: Int,
    statusText: String
) {
    val shape = RoundedCornerShape(20.dp)
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
            .padding(16.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stepText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Text(
                    text = "$progressPercent%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(InnerCircleBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressPercent.toFloat() / 100f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusText,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

/**
 * Note Feature Item Card
 */
@Composable
private fun NoteFeatureCardItem(feature: NoteFeatureItem) {
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
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(InnerCircleBg)
                ) {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        when (feature.id) {
                            "1" -> drawSummaryLinesIcon(PurplePrimary)
                            "2" -> drawPadNotebookIcon(PurplePrimary)
                            "3" -> drawBulletListIcon(PurplePrimary)
                            else -> drawGlossaryAIcon(PurplePrimary)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = feature.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )

                    if (feature.subtitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = feature.subtitle,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            // Right Checkmark
            Canvas(modifier = Modifier.size(20.dp)) {
                drawCheckmarkCircle(PurplePrimary)
            }
        }
    }
}

/**
 * Bottom Navigation Bar
 */
@Composable
private fun HomeBottomNavigationBar(
    selectedTab: HomeBottomTab,
    onTabSelect: (HomeBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 12.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp)
        ) {
            // Tab 1: Home
            BottomTabItem(
                label = "Home",
                isSelected = selectedTab == HomeBottomTab.Home,
                onClick = { onTabSelect(HomeBottomTab.Home) },
                icon = { drawHomeIcon(if (selectedTab == HomeBottomTab.Home) PurplePrimary else TextMuted) }
            )

            // Tab 2: My Notes
            BottomTabItem(
                label = "My Notes",
                isSelected = selectedTab == HomeBottomTab.Notes,
                onClick = { onTabSelect(HomeBottomTab.Notes) },
                icon = { drawNotesTabIcon(if (selectedTab == HomeBottomTab.Notes) PurplePrimary else TextMuted) }
            )

            // Tab 3: Profile
            BottomTabItem(
                label = "Profile",
                isSelected = selectedTab == HomeBottomTab.Profile,
                onClick = { onTabSelect(HomeBottomTab.Profile) },
                icon = { drawProfileTabIcon(if (selectedTab == HomeBottomTab.Profile) PurplePrimary else TextMuted) }
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(modifier = Modifier.size(20.dp)) {
                icon()
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) PurplePrimary else TextMuted
            )
        }
    }
}

// --- Custom Canvas Graphics Helpers ---

private fun DrawScope.drawSummaryLinesIcon(color: Color) {
    val strokeWidth = 1.8.dp.toPx()
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.3f), end = Offset(size.width * 0.8f, size.height * 0.3f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.5f), end = Offset(size.width * 0.65f, size.height * 0.5f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.7f), end = Offset(size.width * 0.5f, size.height * 0.7f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawPadNotebookIcon(color: Color) {
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

private fun DrawScope.drawBulletListIcon(color: Color) {
    val strokeWidth = 1.8.dp.toPx()
    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width * 0.22f, size.height * 0.32f))
    drawLine(color = color, start = Offset(size.width * 0.38f, size.height * 0.32f), end = Offset(size.width * 0.82f, size.height * 0.32f), strokeWidth = strokeWidth, cap = StrokeCap.Round)

    drawCircle(color = color, radius = 2.dp.toPx(), center = Offset(size.width * 0.22f, size.height * 0.68f))
    drawLine(color = color, start = Offset(size.width * 0.38f, size.height * 0.68f), end = Offset(size.width * 0.82f, size.height * 0.68f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawGlossaryAIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.18f, size.height * 0.18f),
        size = Size(size.width * 0.64f, size.height * 0.64f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
    val aPath = Path().apply {
        moveTo(size.width * 0.35f, size.height * 0.68f)
        lineTo(size.width * 0.5f, size.height * 0.32f)
        lineTo(size.width * 0.65f, size.height * 0.68f)
    }
    drawPath(path = aPath, color = color, style = stroke)
    drawLine(color = color, start = Offset(size.width * 0.4f, size.height * 0.54f), end = Offset(size.width * 0.6f, size.height * 0.54f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
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
fun CreateSmartNotesScreenPreview() {
    AIBasedClassNotesAppTheme {
        CreateSmartNotesScreenContent(uiState = CreateSmartNotesUiState())
    }
}
