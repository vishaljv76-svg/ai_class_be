package com.example.Simtas.ui.upload

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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

val UploadBoxBg = Color(0xFFF3EFFF)

/**
 * Upload Lecture screen adhering to MVVM architecture.
 */
@Composable
fun UploadLectureScreen(
    modifier: Modifier = Modifier,
    viewModel: UploadLectureViewModel = remember { UploadLectureViewModel() },
    onBackClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.onAudioFileSelected(context, uri)
        } else {
            viewModel.onPickerCancelled()
        }
    }

    UploadLectureScreenContent(
        uiState = uiState,
        modifier = modifier,
        onBackClick = onBackClick,
        onBrowseFilesClick = {
            try {
                audioPickerLauncher.launch(arrayOf("audio/*"))
            } catch (e: Exception) {
                viewModel.onPickerError("Unable to open file picker: ${e.localizedMessage ?: "Unknown error"}")
            }
        },
        onCancelUploadClick = viewModel::onCancelUploadClicked,
        onContinueClick = {
            viewModel.onContinueClicked()
            onContinueClick()
        }
    )
}

@Composable
fun UploadLectureScreenContent(
    uiState: UploadLectureUiState,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onBrowseFilesClick: () -> Unit = {},
    onCancelUploadClick: () -> Unit = {},
    onContinueClick: () -> Unit = {}
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
                    text = "Bring your lecture.",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Upload a recording. We'll find the ideas worth keeping.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSubtitle,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Dashed Dropzone Upload Card Box
                DashedUploadDropzoneCard(
                    onBrowseFilesClick = onBrowseFilesClick
                )

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = uiState.errorMessage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFDC2626),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Active Upload Progress Card (If file selected)
                if (uiState.isFileSelected) {
                    ActiveUploadProgressCard(
                        fileName = uiState.selectedFileName,
                        fileDetails = uiState.fileDurationAndSize,
                        progressPercent = uiState.uploadProgressPercent,
                        uploadedBytesText = uiState.uploadedBytesText,
                        onCancelClick = onCancelUploadClick
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Security & Privacy Disclaimer
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        drawShieldCheckIconSmall(PurplePrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Your audio is private and securely stored.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            // Bottom CTA Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                // Continue Button
                val isEnabled = uiState.isUploadComplete || uiState.uploadProgressPercent > 0
                val buttonBg = if (isEnabled) InnerCircleBg else InnerCircleBg.copy(alpha = 0.6f)
                val buttonTextColor = PurplePrimary

                Button(
                    onClick = onContinueClick,
                    enabled = isEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        disabledContainerColor = InnerCircleBg.copy(alpha = 0.6f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawRightArrow(buttonTextColor)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Continue",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = buttonTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Available when your upload is complete",
                    fontSize = 11.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
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
            text = "Upload lecture",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )
    }
}

/**
 * Dashed Upload Dropzone Card Box
 */
@Composable
private fun DashedUploadDropzoneCard(
    onBrowseFilesClick: () -> Unit
) {
    val strokeColor = PurplePrimary.copy(alpha = 0.4f)
    val shape = RoundedCornerShape(24.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(UploadBoxBg)
            .drawWithContent {
                drawContent()
                val strokeWidth = 1.5.dp.toPx()
                val pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                drawRoundRect(
                    color = strokeColor,
                    style = Stroke(width = strokeWidth, pathEffect = pathEffect),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
            .clickable { onBrowseFilesClick() }
            .padding(vertical = 28.dp, horizontal = 20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // White Cloud Icon Container Box
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(52.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color.Black.copy(alpha = 0.05f),
                        spotColor = Color.Black.copy(alpha = 0.08f)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
            ) {
                Canvas(modifier = Modifier.size(24.dp)) {
                    drawCloudUploadIcon(PurplePrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Choose an audio file",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary,
                modifier = Modifier.clickable { onBrowseFilesClick() }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "MP3, WAV, M4A · Up to 100 MB",
                fontSize = 12.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Browse Files White Pill Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { onBrowseFilesClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(modifier = Modifier.size(14.dp)) {
                        drawFolderIcon(PurplePrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Browse files",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                }
            }
        }
    }
}

/**
 * Active File Upload Progress Card
 */
@Composable
private fun ActiveUploadProgressCard(
    fileName: String,
    fileDetails: String,
    progressPercent: Int,
    uploadedBytesText: String,
    onCancelClick: () -> Unit
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
                            drawHeadphonesAudioIcon(PurplePrimary)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = fileName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = fileDetails,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                IconButton(
                    onClick = onCancelClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Canvas(modifier = Modifier.size(12.dp)) {
                        drawCloseCrossIcon(TextMuted)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Bar
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

            // Progress status text row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uploadedBytesText,
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Text(
                    text = "$progressPercent%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PurplePrimary
                )
            }
        }
    }
}

// --- Custom Canvas Graphics Helpers ---

private fun DrawScope.drawCloudUploadIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val cloudPath = Path().apply {
        moveTo(size.width * 0.25f, size.height * 0.65f)
        quadraticTo(
            size.width * 0.15f, size.height * 0.65f,
            size.width * 0.15f, size.height * 0.5f
        )
        quadraticTo(
            size.width * 0.15f, size.height * 0.35f,
            size.width * 0.32f, size.height * 0.32f
        )
        quadraticTo(
            size.width * 0.42f, size.height * 0.15f,
            size.width * 0.6f, size.height * 0.22f
        )
        quadraticTo(
            size.width * 0.8f, size.height * 0.22f,
            size.width * 0.82f, size.height * 0.45f
        )
        quadraticTo(
            size.width * 0.9f, size.height * 0.5f,
            size.width * 0.82f, size.height * 0.65f
        )
        close()
    }
    drawPath(path = cloudPath, color = color, style = stroke)

    // Upward arrow
    drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.65f), end = Offset(size.width * 0.5f, size.height * 0.38f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val arrowHead = Path().apply {
        moveTo(size.width * 0.38f, size.height * 0.48f)
        lineTo(size.width * 0.5f, size.height * 0.38f)
        lineTo(size.width * 0.62f, size.height * 0.48f)
    }
    drawPath(path = arrowHead, color = color, style = stroke)
}

private fun DrawScope.drawFolderIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val folderPath = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.25f)
        lineTo(size.width * 0.4f, size.height * 0.25f)
        lineTo(size.width * 0.5f, size.height * 0.38f)
        lineTo(size.width * 0.85f, size.height * 0.38f)
        lineTo(size.width * 0.85f, size.height * 0.8f)
        lineTo(size.width * 0.15f, size.height * 0.8f)
        close()
    }
    drawPath(path = folderPath, color = color, style = stroke)
}

private fun DrawScope.drawHeadphonesAudioIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val arcPath = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.6f)
        quadraticTo(
            size.width * 0.2f, size.height * 0.2f,
            size.width * 0.5f, size.height * 0.2f
        )
        quadraticTo(
            size.width * 0.8f, size.height * 0.2f,
            size.width * 0.8f, size.height * 0.6f
        )
    }
    drawPath(path = arcPath, color = color, style = stroke)
    drawRoundRect(color = color, topLeft = Offset(size.width * 0.15f, size.height * 0.55f), size = Size(size.width * 0.15f, size.height * 0.3f), cornerRadius = CornerRadius(2.dp.toPx()))
    drawRoundRect(color = color, topLeft = Offset(size.width * 0.7f, size.height * 0.55f), size = Size(size.width * 0.15f, size.height * 0.3f), cornerRadius = CornerRadius(2.dp.toPx()))
}

private fun DrawScope.drawCloseCrossIcon(color: Color) {
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.2f), end = Offset(size.width * 0.8f, size.height * 0.8f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.8f, size.height * 0.2f), end = Offset(size.width * 0.2f, size.height * 0.8f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawShieldCheckIconSmall(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    val shieldPath = Path().apply {
        moveTo(size.width * 0.5f, size.height * 0.1f)
        lineTo(size.width * 0.85f, size.height * 0.25f)
        lineTo(size.width * 0.85f, size.height * 0.55f)
        quadraticTo(
            size.width * 0.85f, size.height * 0.85f,
            size.width * 0.5f, size.height * 0.95f
        )
        quadraticTo(
            size.width * 0.15f, size.height * 0.85f,
            size.width * 0.15f, size.height * 0.55f
        )
        lineTo(size.width * 0.15f, size.height * 0.25f)
        close()
    }
    drawPath(path = shieldPath, color = color, style = stroke)

    val checkPath = Path().apply {
        moveTo(size.width * 0.35f, size.height * 0.52f)
        lineTo(size.width * 0.47f, size.height * 0.64f)
        lineTo(size.width * 0.65f, size.height * 0.42f)
    }
    drawPath(path = checkPath, color = color, style = stroke)
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
fun UploadLectureScreenPreview() {
    AIBasedClassNotesAppTheme {
        UploadLectureScreenContent(
            uiState = UploadLectureUiState(
                selectedFileName = "CS301_Normalization.m4a",
                fileDurationAndSize = "42:18 duration · 38.4 MB",
                uploadProgressPercent = 78,
                uploadedBytesText = "Uploading · 30.0 of 38.4 MB",
                isUploading = true,
                isFileSelected = true
            )
        )
    }
}
