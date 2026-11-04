package com.example.Simtas.ui.login

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
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

private val LoginBorderColor: Color
    get() = if (ThemeManager.isDark()) Color(0xFF332B52) else Color(0xFFE9D5FF).copy(alpha = 0.8f)

/**
 * Login Screen adhering to MVVM architecture with clean Luxury Pastel Lavender design.
 */
@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = remember { LoginViewModel() },
    onBackClick: () -> Unit = {},
    onSignUpClick: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    onLoginSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LoginScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onEmailChange = viewModel::onEmailChanged,
        onPasswordChange = viewModel::onPasswordChanged,
        onTogglePasswordVisibility = viewModel::togglePasswordVisibility,
        onForgotPasswordClick = {
            viewModel.onForgotPasswordClicked()
            onForgotPasswordClick()
        },
        onLoginClick = {
            viewModel.onLoginClicked { onLoginSuccess() }
        },
        onSignUpClick = onSignUpClick
    )
}

@Composable
fun LoginScreenContent(
    uiState: LoginUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onEmailChange: (String) -> Unit = {},
    onPasswordChange: (String) -> Unit = {},
    onTogglePasswordVisibility: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
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
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Top Navigation Bar
                TopNavigationBar(onBackClick = onBackClick)

                Spacer(modifier = Modifier.height(24.dp))

                // Cute Educational Badge Header
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(58.dp)
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
                    Canvas(modifier = Modifier.size(32.dp)) {
                        drawNotebookPencil(Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Title & Subtitle
                Text(
                    text = "Welcome back, learner.",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Your ideas and study notes are right where you left them.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSubtitle
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Field 1: Email Address
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
                        unfocusedBorderColor = LoginBorderColor,
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

                Spacer(modifier = Modifier.height(18.dp))

                // Field 2: Password
                Text(
                    text = "Password",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = CardBg,
                        unfocusedContainerColor = CardBg,
                        disabledContainerColor = CardBg,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = LoginBorderColor,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    ),
                    leadingIcon = {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            drawLockIcon(PurplePrimary)
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = onTogglePasswordVisibility) {
                            Canvas(modifier = Modifier.size(20.dp)) {
                                drawEyeIcon(TextMuted, isOff = !uiState.isPasswordVisible)
                            }
                        }
                    },
                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Forgot Password Link
                Text(
                    text = "Forgot password?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onForgotPasswordClick() }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Error Message Display
                if (!uiState.errorMessage.isNullOrEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFD32F2F),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Login CTA Button
                Button(
                    onClick = onLoginClick,
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
                                drawRightArrow(Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Login",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Sign Up Redirect
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Don't have an account? ",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                    Text(
                        text = "Sign Up",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary,
                        modifier = Modifier.clickable { onSignUpClick() }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }

            // Bottom Security Info Card
            SecurityInfoCard(
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
            text = "Login",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )
    }
}

/**
 * Bottom Security Info Card
 */
@Composable
private fun SecurityInfoCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, LoginBorderColor, RoundedCornerShape(16.dp))
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
                    drawLockIcon(PurplePrimary)
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "End-to-end encrypted • Student session protected",
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

private fun DrawScope.drawNotebookPencil(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.2f, size.height * 0.15f),
        size = Size(size.width * 0.6f, size.height * 0.7f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.38f), end = Offset(size.width * 0.65f, size.height * 0.38f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.35f, size.height * 0.55f), end = Offset(size.width * 0.55f, size.height * 0.55f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
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

private fun DrawScope.drawLockIcon(color: Color) {
    val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.2f, size.height * 0.42f),
        size = Size(size.width * 0.6f, size.height * 0.46f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(size.width * 0.32f, size.height * 0.16f),
        size = Size(size.width * 0.36f, size.height * 0.38f),
        style = stroke
    )
}

private fun DrawScope.drawEyeIcon(color: Color, isOff: Boolean) {
    val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
    val eyePath = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.5f)
        quadraticTo(
            size.width * 0.5f, size.height * 0.2f,
            size.width * 0.85f, size.height * 0.5f
        )
        quadraticTo(
            size.width * 0.5f, size.height * 0.8f,
            size.width * 0.15f, size.height * 0.5f
        )
    }
    drawPath(path = eyePath, color = color, style = stroke)
    drawCircle(color = color, radius = size.width * 0.14f, center = Offset(size.width * 0.5f, size.height * 0.5f))

    if (isOff) {
        drawLine(
            color = color,
            start = Offset(size.width * 0.2f, size.height * 0.8f),
            end = Offset(size.width * 0.8f, size.height * 0.2f),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    AIBasedClassNotesAppTheme {
        LoginScreenContent(uiState = LoginUiState())
    }
}
