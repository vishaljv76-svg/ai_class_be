package com.example.Simtas.ui.forgotpassword

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.CardBg
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.TextSubtitle
import com.example.Simtas.ui.theme.ThemeManager

private val ForgotBorderColor: Color
    get() = if (ThemeManager.isDark()) Color(0xFF332B52) else Color(0xFFE9D5FF).copy(alpha = 0.8f)

/**
 * Forgot Password / Reset Password screen adhering to MVVM architecture with clean Luxury Pastel Lavender design.
 */
@Composable
fun ForgotPasswordScreen(
    modifier: Modifier = Modifier,
    viewModel: ForgotPasswordViewModel = remember { ForgotPasswordViewModel() },
    onBackClick: () -> Unit = {},
    onBackToLoginClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    ForgotPasswordScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onEmailChange = viewModel::onEmailChanged,
        onResetPasswordClick = viewModel::onResetPasswordClicked,
        onBackToLoginClick = onBackToLoginClick
    )
}

@Composable
fun ForgotPasswordScreenContent(
    uiState: ForgotPasswordUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onResetPasswordClick: () -> Unit = {},
    onBackToLoginClick: () -> Unit = {}
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

                // Top Navigation Bar
                TopNavigationBar(onBackClick = onBackClick)

                Spacer(modifier = Modifier.height(24.dp))

                // Cute Key & Shield Security Badge Header
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PurplePrimary, Color(0xFF8B5CF6))
                            )
                        )
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = PurplePrimary.copy(alpha = 0.25f),
                            spotColor = PurplePrimary.copy(alpha = 0.35f)
                        )
                ) {
                    Canvas(modifier = Modifier.size(28.dp)) {
                        drawKeyIcon(Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title & Subtitle
                Text(
                    text = "Let's get you back in.",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter your student email and we'll send you a link to reset your password.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSubtitle,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Email Address Label & Input Box
                Text(
                    text = "Email address",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = onEmailChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg,
                        disabledContainerColor = CardBg,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = ForgotBorderColor,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    ),
                    leadingIcon = {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            drawMailIcon(PurplePrimary)
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Reset Email Sent Success Message Box
                if (uiState.isResetEmailSent) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "Reset instructions sent! " + uiState.infoMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Reset Password CTA Button
                Button(
                    onClick = onResetPasswordClick,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PurplePrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Canvas(modifier = Modifier.size(18.dp)) {
                                drawMailIcon(Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset Password",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Back to Login Link
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onBackToLoginClick() }
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        drawBackArrow(PurplePrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Back to Login",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Bottom Help Assistance Card
            HelpAssistanceCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
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

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "Reset password",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )
    }
}

/**
 * Bottom Help Assistance Card
 */
@Composable
private fun HelpAssistanceCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, ForgotBorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(InnerCircleBg)
            ) {
                Canvas(modifier = Modifier.size(16.dp)) {
                    drawSparkleStar(PurplePrimary, Offset(size.width / 2, size.height / 2), size.width / 2.5f)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Need help? Contact support@smartnotes.ai",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
        }
    }
}

// --- Custom Canvas Graphics Helpers ---

private fun DrawScope.drawBackArrow(color: Color) {
    val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    val path = Path().apply {
        moveTo(size.width * 0.6f, size.height * 0.25f)
        lineTo(size.width * 0.35f, size.height * 0.5f)
        lineTo(size.width * 0.6f, size.height * 0.75f)
    }
    drawPath(path = path, color = color, style = stroke)
}

private fun DrawScope.drawKeyIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(
        color = color,
        radius = size.width * 0.22f,
        center = Offset(size.width * 0.35f, size.height * 0.35f),
        style = stroke
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.5f, size.height * 0.5f),
        end = Offset(size.width * 0.8f, size.height * 0.8f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(size.width * 0.7f, size.height * 0.7f),
        end = Offset(size.width * 0.8f, size.height * 0.6f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawMailIcon(color: Color) {
    val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.15f, size.height * 0.25f),
        size = Size(size.width * 0.7f, size.height * 0.5f),
        cornerRadius = CornerRadius(3.dp.toPx()),
        style = stroke
    )
    val path = Path().apply {
        moveTo(size.width * 0.18f, size.height * 0.28f)
        lineTo(size.width * 0.5f, size.height * 0.52f)
        lineTo(size.width * 0.82f, size.height * 0.28f)
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
fun ForgotPasswordScreenPreview() {
    AIBasedClassNotesAppTheme {
        ForgotPasswordScreenContent(uiState = ForgotPasswordUiState())
    }
}
