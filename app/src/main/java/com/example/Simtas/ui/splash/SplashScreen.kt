package com.example.Simtas.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.CardBg
import com.example.Simtas.ui.theme.InactiveDot
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.OuterRingBorder
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.TextSubtitle
import com.example.Simtas.ui.theme.ThemeManager

private val SplashBorderColor: Color
    get() = if (ThemeManager.isDark()) Color(0xFF332B52) else Color(0xFFE9D5FF).copy(alpha = 0.8f)

/**
 * Splash screen UI component built with Jetpack Compose.
 * Features the clean Luxury Pastel Lavender design and observes state from [SplashViewModel].
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = remember { SplashViewModel() }
) {
    val uiState by viewModel.uiState.collectAsState()

    SplashScreenContent(
        currentPage = uiState.currentPage,
        totalPages = uiState.totalPages,
        modifier = modifier
    )
}

@Composable
fun SplashScreenContent(
    currentPage: Int = 0,
    totalPages: Int = 3,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = SplashBackground
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle ambient lavender glow in background
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .align(Alignment.Center)
            ) {
                val isDark = ThemeManager.isDark()
                val glowColor = if (isDark) Color(0xFF6C5CE7).copy(alpha = 0.14f) else Color(0xFFDDD6FE).copy(alpha = 0.50f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowColor, Color.Transparent),
                        center = center,
                        radius = size.width * 0.65f
                    ),
                    center = center,
                    radius = size.width * 0.65f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header Pill Label
                TopHeaderSection(
                    modifier = Modifier.padding(top = 36.dp)
                )

                // Center Artwork with concentric rings & floating sparkle badge
                CenterArtworkSection()

                // Text Titles Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "SmartNotes AI",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDarkPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Turn Lectures into Smart Notes.",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSubtitle,
                        textAlign = TextAlign.Center
                    )
                }

                // Bottom Section: Page Indicators & Footer Motto
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    PageIndicatorSection(
                        currentPage = currentPage,
                        totalPages = totalPages
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "LESS SCRIBBLING. MORE UNDERSTANDING.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.4.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Top "YOUR STUDY COMPANION" pill label with sparkle star.
 */
@Composable
private fun TopHeaderSection(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(InnerCircleBg)
            .border(1.dp, SplashBorderColor, RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Canvas(modifier = Modifier.size(14.dp)) {
                drawSparkleStar(
                    color = PurplePrimary,
                    center = Offset(size.width / 2, size.height / 2),
                    starRadius = size.width / 2.2f
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "YOUR STUDY COMPANION",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = PurplePrimary,
                letterSpacing = 1.2.sp
            )
        }
    }
}

/**
 * Central concentric rings + main purple icon box + floating sparkle badge.
 */
@Composable
private fun CenterArtworkSection() {
    val infiniteTransition = rememberInfiniteTransition(label = "SplashGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(280.dp)
    ) {
        // Outermost faint thin ring stroke
        Canvas(modifier = Modifier.size(270.dp)) {
            drawCircle(
                color = OuterRingBorder,
                radius = (size.minDimension / 2) * pulseScale,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        // Inner filled light purple circle
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(InnerCircleBg)
                .border(1.dp, SplashBorderColor, CircleShape)
        )

        // Main Purple Rounded Square Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(128.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(32.dp),
                    ambientColor = PurplePrimary.copy(alpha = 0.25f),
                    spotColor = PurplePrimary.copy(alpha = 0.35f)
                )
                .clip(RoundedCornerShape(32.dp))
                .background(
                    Brush.linearGradient(
                        listOf(PurplePrimary, Color(0xFF8B5CF6))
                    )
                )
        ) {
            NotebookPencilCanvas(modifier = Modifier.size(64.dp))
        }

        // Floating Sparkle Badge at Bottom-Right of the Main Purple Box
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(x = 46.dp, y = 46.dp)
                .size(46.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(14.dp),
                    ambientColor = PurplePrimary.copy(alpha = 0.15f),
                    spotColor = PurplePrimary.copy(alpha = 0.20f)
                )
                .clip(RoundedCornerShape(14.dp))
                .background(CardBg)
                .border(1.dp, SplashBorderColor, RoundedCornerShape(14.dp))
        ) {
            Canvas(modifier = Modifier.size(22.dp)) {
                drawSparkleStar(
                    color = PurplePrimary,
                    center = Offset(size.width / 2.2f, size.height / 1.8f),
                    starRadius = size.width / 2.8f
                )
                drawCircle(
                    color = PurplePrimary,
                    radius = 2.dp.toPx(),
                    center = Offset(size.width * 0.8f, size.height * 0.25f)
                )
            }
        }
    }
}

/**
 * Custom Canvas drawing for Notebook + Lines + Binding loops + Pencil.
 */
@Composable
private fun NotebookPencilCanvas(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.5.dp.toPx()
        val white = Color.White

        val padLeft = size.width * 0.24f
        val padTop = size.height * 0.18f
        val padWidth = size.width * 0.52f
        val padHeight = size.height * 0.64f
        val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

        drawRoundRect(
            color = white,
            topLeft = Offset(padLeft, padTop),
            size = Size(padWidth, padHeight),
            cornerRadius = cornerRadius,
            style = Stroke(width = strokeWidth)
        )

        val bindingStartX = padLeft - (2.dp.toPx())
        val bindingLength = 5.dp.toPx()
        val startY = padTop + (padHeight * 0.2f)
        val stepY = padHeight * 0.18f

        for (i in 0 until 4) {
            val currentY = startY + (i * stepY)
            drawLine(
                color = white,
                start = Offset(bindingStartX, currentY),
                end = Offset(bindingStartX + bindingLength, currentY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }

        val lineStartX = padLeft + (padWidth * 0.22f)
        val lineEndX = padLeft + (padWidth * 0.78f)
        val line1Y = padTop + (padHeight * 0.35f)
        val line2Y = padTop + (padHeight * 0.52f)
        val line3Y = padTop + (padHeight * 0.69f)

        drawLine(
            color = white,
            start = Offset(lineStartX, line1Y),
            end = Offset(lineEndX, line1Y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = white,
            start = Offset(lineStartX, line2Y),
            end = Offset(lineEndX, line2Y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        drawLine(
            color = white,
            start = Offset(lineStartX, line3Y),
            end = Offset(lineStartX + (padWidth * 0.35f), line3Y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        val pStartX = size.width * 0.54f
        val pStartY = size.height * 0.46f
        val pEndX = size.width * 0.76f
        val pEndY = size.height * 0.24f

        drawLine(
            color = white,
            start = Offset(pStartX, pStartY),
            end = Offset(pEndX, pEndY),
            strokeWidth = strokeWidth * 1.3f,
            cap = StrokeCap.Round
        )

        val tipPath = Path().apply {
            moveTo(pStartX - 2.dp.toPx(), pStartY + 2.dp.toPx())
            lineTo(pStartX + 3.dp.toPx(), pStartY + 6.dp.toPx())
            lineTo(pStartX - 6.dp.toPx(), pStartY - 3.dp.toPx())
            close()
        }
        drawPath(
            path = tipPath,
            color = white
        )
    }
}

private fun DrawScope.drawSparkleStar(
    color: Color,
    center: Offset,
    starRadius: Float
) {
    val path = Path().apply {
        val cx = center.x
        val cy = center.y
        val r = starRadius

        moveTo(cx, cy - r)
        quadraticTo(cx, cy, cx + r, cy)
        quadraticTo(cx, cy, cx, cy + r)
        quadraticTo(cx, cy, cx - r, cy)
        quadraticTo(cx, cy, cx, cy - r)
        close()
    }
    drawPath(path = path, color = color)
}

@Composable
private fun PageIndicatorSection(
    currentPage: Int,
    totalPages: Int,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        for (i in 0 until totalPages) {
            val isActive = i == currentPage
            if (isActive) {
                Box(
                    modifier = Modifier
                        .width(24.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(InactiveDot)
                )
            }

            if (i < totalPages - 1) {
                Spacer(modifier = Modifier.width(6.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    AIBasedClassNotesAppTheme {
        SplashScreenContent()
    }
}
