package com.example.Simtas.ui.welcome

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Simtas.R
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.CardBg
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.TextSubtitle
import com.example.Simtas.ui.theme.ThemeManager

/**
 * Onboarding/Welcome screen featuring the approved SmartNotes AI hero illustration.
 * Preserves MVVM architecture, callbacks, and theme functionality.
 */
@Composable
fun WelcomeScreen(
    modifier: Modifier = Modifier,
    viewModel: WelcomeViewModel = remember { WelcomeViewModel() },
    onGetStartedClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
) {
    WelcomeScreenContent(
        modifier = modifier,
        onGetStartedClick = {
            viewModel.onGetStartedClicked()
            onGetStartedClick()
        },
        onLoginClick = {
            viewModel.onLoginClicked()
            onLoginClick()
        },
        onSignUpClick = {
            viewModel.onSignUpClicked()
            onSignUpClick()
        }
    )
}

@Composable
fun WelcomeScreenContent(
    modifier: Modifier = Modifier,
    onGetStartedClick: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {}
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
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // 1. Top Header Bar with App Logo Badge
                TopHeaderBar()

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Central Hero Illustration (Approved Cute Character & AI Robot)
                HeroIllustrationSection()

                Spacer(modifier = Modifier.height(20.dp))

                // 3. App Title & Approved Tagline
                Text(
                    text = "SmartNotes AI",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tagline Capsule Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(InnerCircleBg)
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Study Smarter • Learn Better • Achieve More",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "AI-powered lecture capture that transforms classroom audio into structured notes, flashcards & active quizzes.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSubtitle,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4. Feature Highlights Pills Row
                FeaturePillsRow()

                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. Bottom Action Section (Get Started & Login/Signup)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Get Started Button
                Button(
                    onClick = onGetStartedClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurplePrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
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
                            text = "Get Started",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Login Link
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have an account? ",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "Login",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary,
                        modifier = Modifier.clickable { onLoginClick() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sign Up Link
                Text(
                    text = "New here? Sign Up",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    modifier = Modifier.clickable { onSignUpClick() }
                )
            }
        }
    }
}

/**
 * Top App Logo Bar
 */
@Composable
private fun TopHeaderBar() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    Brush.linearGradient(
                        listOf(PurplePrimary, Color(0xFF8B5CF6))
                    )
                )
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                drawSparkleStar(Color.White, Offset(size.width / 2, size.height / 2), size.width / 2.2f)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = "SmartNotes AI",
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextDarkPrimary
        )
    }
}

/**
 * Central Hero Illustration Section
 * Displays the exact student and AI robot artwork without distortion or cropping.
 */
@Composable
private fun HeroIllustrationSection() {
    val isDark = ThemeManager.isDark()
    val borderColor = if (isDark) Color(0xFF332B52) else Color(0xFFE9D5FF).copy(alpha = 0.8f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(310.dp)
    ) {
        // Soft ambient glowing background card
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(295.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = PurplePrimary.copy(alpha = 0.12f),
                    spotColor = PurplePrimary.copy(alpha = 0.18f)
                )
                .clip(RoundedCornerShape(26.dp))
                .background(
                    if (isDark) Color(0xFF1B162C) else Color(0xFFFAF7FF)
                )
                .border(1.dp, borderColor, RoundedCornerShape(26.dp))
        )

        // Exact approved artwork loaded from drawable with ContentScale.Fit
        Image(
            painter = painterResource(id = R.drawable.hero_study_character),
            contentDescription = "SmartNotes AI Study Assistant with Robot",
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .height(290.dp)
                .padding(vertical = 4.dp),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * Clean minimal feature highlights row
 */
@Composable
private fun FeaturePillsRow() {
    val isDark = ThemeManager.isDark()
    val pillBg = if (isDark) Color(0xFF221A3A) else Color(0xFFF4F0FF)

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        FeatureMiniChip(
            icon = "🎙️",
            label = "Lecture Audio",
            bgColor = pillBg,
            modifier = Modifier.weight(1f)
        )
        FeatureMiniChip(
            icon = "✨",
            label = "Smart Summary",
            bgColor = pillBg,
            modifier = Modifier.weight(1f)
        )
        FeatureMiniChip(
            icon = "⚡",
            label = "Active Quiz",
            bgColor = pillBg,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FeatureMiniChip(
    icon: String,
    label: String,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .padding(vertical = 8.dp, horizontal = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = icon, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDarkPrimary
            )
        }
    }
}

// --- Custom Canvas Vector Helpers ---

private fun DrawScope.drawRightArrow(color: Color) {
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.5f)
        lineTo(size.width * 0.75f, size.height * 0.5f)
        moveTo(size.width * 0.5f, size.height * 0.25f)
        lineTo(size.width * 0.75f, size.height * 0.5f)
        lineTo(size.width * 0.5f, size.height * 0.75f)
    }
    drawPath(path = path, color = color, style = stroke)
}

private fun DrawScope.drawSparkleStar(color: Color, center: Offset, starRadius: Float) {
    val path = Path().apply {
        moveTo(center.x, center.y - starRadius)
        quadraticTo(center.x, center.y, center.x + starRadius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + starRadius)
        quadraticTo(center.x, center.y, center.x - starRadius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - starRadius)
        close()
    }
    drawPath(path = path, color = color)
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun WelcomeScreenPreview() {
    AIBasedClassNotesAppTheme {
        WelcomeScreenContent()
    }
}
