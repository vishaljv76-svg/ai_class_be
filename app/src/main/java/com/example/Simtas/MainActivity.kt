package com.example.Simtas

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Simtas.data.remote.SessionManager
import com.example.Simtas.ui.chat.AiChatScreen
import com.example.Simtas.ui.createnotes.CreateSmartNotesScreen
import com.example.Simtas.ui.detailednotes.DetailedNotesScreen
import com.example.Simtas.ui.detailednotes.DetailedNotesViewModel
import com.example.Simtas.ui.flashcards.FlashcardsScreen
import com.example.Simtas.ui.forgotpassword.ForgotPasswordScreen
import com.example.Simtas.ui.home.DashboardScreen
import com.example.Simtas.ui.home.HomeBottomTab
import com.example.Simtas.ui.login.LoginScreen
import com.example.Simtas.ui.mynotes.MyNotesScreen
import com.example.Simtas.ui.notes.GeneratedNotesScreen
import com.example.Simtas.ui.onboarding.StudentOnboardingScreen
import com.example.Simtas.ui.preview.LecturePreviewScreen
import com.example.Simtas.ui.profile.ProfileSettingsScreen
import com.example.Simtas.ui.qa.QaAssistantBottomSheet
import com.example.Simtas.ui.quiz.QuizGeneratorScreen
import com.example.Simtas.ui.quizquestion.QuizQuestionScreen
import com.example.Simtas.ui.quizresults.QuizResultsScreen
import com.example.Simtas.ui.record.RecordLectureScreen
import com.example.Simtas.ui.signup.SignupScreen
import com.example.Simtas.ui.splash.SplashScreen
import com.example.Simtas.ui.subjects.SubjectSelectionScreen
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.FontManager
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SmartNotesAppBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.ThemeManager
import com.example.Simtas.ui.transcript.TranscriptScreen
import com.example.Simtas.ui.upload.UploadLectureScreen
import com.example.Simtas.ui.welcome.WelcomeScreen
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

enum class AppScreen {
    Splash,
    Welcome,
    Login,
    Signup,
    ForgotPassword,
    StudentOnboarding,
    SubjectSelection,
    HomeDashboard,
    RecordLecture,
    UploadLecture,
    LecturePreview,
    Transcript,
    CreateSmartNotes,
    GeneratedNotes,
    DetailedNotes,
    QuizGenerator,
    QuizQuestion,
    QuizResults,
    MyNotes,
    ProfileSettings,
    AiChat,
    Flashcards
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)
        ThemeManager.init(this)
        FontManager.init(this)
        enableEdgeToEdge()
        setContent {
            val currentThemeMode by ThemeManager.themeMode.collectAsState()
            val currentFontStyle by FontManager.fontStyle.collectAsState()
            AIBasedClassNotesAppTheme(
                themeMode = currentThemeMode,
                fontStyle = currentFontStyle
            ) {
                SmartNotesAppBackground {
                    MainAppNavigation()
                }
            }
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainAppNavigation() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(AppScreen.Splash) }
    var selectedNoteId by remember { mutableStateOf("1") }
    var activeRecordedAudioPath by remember { mutableStateOf<String?>(null) }

    // Dynamic back navigation trackers
    var previousScreenForNotes by remember { mutableStateOf(AppScreen.HomeDashboard) }
    var previousScreenForQuiz by remember { mutableStateOf(AppScreen.HomeDashboard) }

    // Interactive Dialog states
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showTranscriptOptionsMenu by remember { mutableStateOf(false) }
    var showNotesOptionsMenu by remember { mutableStateOf(false) }
    var showDiscardRecordDialog by remember { mutableStateOf(false) }
    var showDiscardPreviewDialog by remember { mutableStateOf(false) }
    var showAiAssistantSheet by remember { mutableStateOf(false) }
    var pdfExportDialogResult by remember { mutableStateOf<com.example.Simtas.utils.ExportResult?>(null) }
    var pdfExportDialogTitle by remember { mutableStateOf("") }

    // Automatic session validation & persistent login check on app startup
    LaunchedEffect(currentScreen) {
        if (currentScreen == AppScreen.Splash) {
            delay(1500)
            if (SessionManager.isLoggedIn()) {
                val isValid = com.example.Simtas.data.repository.AuthRepository.validateSession(context)
                if (isValid) {
                    currentScreen = AppScreen.HomeDashboard
                } else {
                    currentScreen = AppScreen.Login
                }
            } else {
                currentScreen = AppScreen.Login
            }
        }
    }

    when (currentScreen) {
        AppScreen.Splash -> {
            SplashScreen()
        }
        AppScreen.Welcome -> {
            WelcomeScreen(
                onGetStartedClick = {
                    currentScreen = AppScreen.Signup
                },
                onLoginClick = {
                    currentScreen = AppScreen.Login
                },
                onSignUpClick = {
                    currentScreen = AppScreen.Signup
                }
            )
        }
        AppScreen.Login -> {
            LoginScreen(
                onBackClick = {
                    (context as? ComponentActivity)?.finish()
                },
                onForgotPasswordClick = {
                    currentScreen = AppScreen.ForgotPassword
                },
                onLoginSuccess = {
                    currentScreen = AppScreen.HomeDashboard
                },
                onSignUpClick = {
                    currentScreen = AppScreen.Signup
                }
            )
        }
        AppScreen.Signup -> {
            SignupScreen(
                onBackClick = {
                    currentScreen = AppScreen.Login
                },
                onLoginClick = {
                    currentScreen = AppScreen.Login
                },
                onSignupSuccess = {
                    currentScreen = AppScreen.StudentOnboarding
                }
            )
        }
        AppScreen.ForgotPassword -> {
            ForgotPasswordScreen(
                onBackClick = {
                    currentScreen = AppScreen.Login
                },
                onBackToLoginClick = {
                    currentScreen = AppScreen.Login
                }
            )
        }
        AppScreen.StudentOnboarding -> {
            StudentOnboardingScreen(
                onBackClick = {
                    currentScreen = AppScreen.Login
                },
                onChooseSubjectsClick = {
                    currentScreen = AppScreen.SubjectSelection
                },
                onContinueClick = {
                    currentScreen = AppScreen.SubjectSelection
                }
            )
        }
        AppScreen.SubjectSelection -> {
            SubjectSelectionScreen(
                onBackClick = {
                    currentScreen = AppScreen.StudentOnboarding
                },
                onStartLearningClick = {
                    currentScreen = AppScreen.HomeDashboard
                }
            )
        }
        AppScreen.HomeDashboard -> {
            DashboardScreen(
                onNotificationClick = {
                    showNotificationsDialog = true
                },
                onRecordLectureClick = {
                    currentScreen = AppScreen.RecordLecture
                },
                onUploadAudioClick = {
                    currentScreen = AppScreen.UploadLecture
                },
                onMyNotesClick = {
                    currentScreen = AppScreen.MyNotes
                },
                onAiQuizClick = {
                    previousScreenForQuiz = AppScreen.HomeDashboard
                    currentScreen = AppScreen.QuizGenerator
                },
                onFlashcardsClick = {
                    currentScreen = AppScreen.Flashcards
                },
                onViewAllNotesClick = {
                    currentScreen = AppScreen.MyNotes
                },
                onNoteClick = { noteId ->
                    previousScreenForNotes = AppScreen.HomeDashboard
                    selectedNoteId = noteId
                    currentScreen = AppScreen.DetailedNotes
                },
                onTabSelect = { tab ->
                    when (tab) {
                        HomeBottomTab.Home -> currentScreen = AppScreen.HomeDashboard
                        HomeBottomTab.Notes -> currentScreen = AppScreen.MyNotes
                        HomeBottomTab.Profile -> currentScreen = AppScreen.ProfileSettings
                    }
                },
                onAiCopilotClick = {
                    currentScreen = AppScreen.AiChat
                }
            )
        }
        AppScreen.RecordLecture -> {
            RecordLectureScreen(
                onBackClick = {
                    currentScreen = AppScreen.HomeDashboard
                },
                onMoreOptionsClick = {
                    showDiscardRecordDialog = true
                },
                onStopRecordingClick = {
                    activeRecordedAudioPath = SessionManager.getLastRecordedAudioPath()
                    currentScreen = AppScreen.LecturePreview
                },
                onNextClick = { path ->
                    activeRecordedAudioPath = path.ifBlank { SessionManager.getLastRecordedAudioPath() }
                    currentScreen = AppScreen.LecturePreview
                }
            )
        }
        AppScreen.UploadLecture -> {
            UploadLectureScreen(
                onBackClick = {
                    currentScreen = AppScreen.HomeDashboard
                },
                onContinueClick = {
                    activeRecordedAudioPath = SessionManager.getLastRecordedAudioPath()
                    currentScreen = AppScreen.LecturePreview
                }
            )
        }
        AppScreen.LecturePreview -> {
            LecturePreviewScreen(
                audioPath = activeRecordedAudioPath ?: SessionManager.getLastRecordedAudioPath(),
                onBackClick = {
                    currentScreen = AppScreen.HomeDashboard
                },
                onMoreOptionsClick = {
                    showDiscardPreviewDialog = true
                },
                onGenerateTranscriptClick = {
                    currentScreen = AppScreen.Transcript
                }
            )
        }
        AppScreen.Transcript -> {
            TranscriptScreen(
                audioPath = activeRecordedAudioPath ?: SessionManager.getLastRecordedAudioPath(),
                onBackClick = {
                    currentScreen = AppScreen.LecturePreview
                },
                onMoreOptionsClick = {
                    showTranscriptOptionsMenu = true
                },
                onGenerateNotesClick = {
                    currentScreen = AppScreen.CreateSmartNotes
                }
            )
        }
        AppScreen.CreateSmartNotes -> {
            CreateSmartNotesScreen(
                onBackClick = {
                    currentScreen = AppScreen.Transcript
                },
                onContinueClick = {
                    currentScreen = AppScreen.GeneratedNotes
                }
            )
        }
        AppScreen.GeneratedNotes -> {
            GeneratedNotesScreen(
                onBackClick = {
                    currentScreen = AppScreen.HomeDashboard
                },
                onMoreOptionsClick = {
                    showNotesOptionsMenu = true
                },
                onViewFullNotesClick = {
                    previousScreenForNotes = AppScreen.GeneratedNotes
                    currentScreen = AppScreen.DetailedNotes
                },
                onTakeQuizClick = {
                    previousScreenForQuiz = AppScreen.GeneratedNotes
                    currentScreen = AppScreen.QuizGenerator
                }
            )
        }
        AppScreen.DetailedNotes -> {
            val detailedViewModel: DetailedNotesViewModel = remember(selectedNoteId) { DetailedNotesViewModel() }
            val detailedUiState by detailedViewModel.uiState.collectAsState()
            LaunchedEffect(selectedNoteId) {
                detailedViewModel.loadNote(selectedNoteId)
            }
            DetailedNotesScreen(
                viewModel = detailedViewModel,
                onBackClick = {
                    currentScreen = previousScreenForNotes
                },
                onDownloadPdfClick = {
                    val pdfData = com.example.Simtas.utils.PdfExportManager.createPdfDataFromDetailedNotes(detailedUiState)
                    val result = com.example.Simtas.utils.PdfExportManager.exportNoteToPdf(context, pdfData)
                    if (result.isSuccess) {
                        pdfExportDialogTitle = detailedUiState.lectureTitle
                        pdfExportDialogResult = result.getOrNull()
                    } else {
                        android.widget.Toast.makeText(context, "Could not export PDF: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        AppScreen.QuizGenerator -> {
            QuizGeneratorScreen(
                onBackClick = {
                    currentScreen = previousScreenForQuiz
                },
                onGenerateQuizClick = {
                    currentScreen = AppScreen.QuizQuestion
                }
            )
        }
        AppScreen.QuizQuestion -> {
            QuizQuestionScreen(
                onBackClick = {
                    currentScreen = AppScreen.QuizGenerator
                },
                onQuizFinish = {
                    currentScreen = AppScreen.QuizResults
                }
            )
        }
        AppScreen.QuizResults -> {
            QuizResultsScreen(
                onBackClick = {
                    currentScreen = AppScreen.QuizGenerator
                },
                onRetryQuizClick = {
                    currentScreen = AppScreen.QuizQuestion
                },
                onBackToNotesClick = {
                    currentScreen = AppScreen.MyNotes
                }
            )
        }
        AppScreen.MyNotes -> {
            MyNotesScreen(
                onAddNoteClick = {
                    currentScreen = AppScreen.RecordLecture
                },
                onNoteClick = { noteId ->
                    previousScreenForNotes = AppScreen.MyNotes
                    selectedNoteId = noteId
                    currentScreen = AppScreen.DetailedNotes
                },
                onTabSelect = { tab ->
                    when (tab) {
                        HomeBottomTab.Home -> currentScreen = AppScreen.HomeDashboard
                        HomeBottomTab.Notes -> currentScreen = AppScreen.MyNotes
                        HomeBottomTab.Profile -> currentScreen = AppScreen.ProfileSettings
                    }
                }
            )
        }
        AppScreen.ProfileSettings -> {
            ProfileSettingsScreen(
                onAccountPasswordClick = {
                    showAccountDialog = true
                },
                onPrivacyStorageClick = {
                    showPrivacyDialog = true
                },
                onLogoutClick = {
                    coroutineScope.launch {
                        com.example.Simtas.data.repository.AuthRepository.logout(context)
                        currentScreen = AppScreen.Login
                    }
                },
                onTabSelect = { tab ->
                    when (tab) {
                        HomeBottomTab.Home -> currentScreen = AppScreen.HomeDashboard
                        HomeBottomTab.Notes -> currentScreen = AppScreen.MyNotes
                        HomeBottomTab.Profile -> currentScreen = AppScreen.ProfileSettings
                    }
                }
            )
        }
        AppScreen.AiChat -> {
            AiChatScreen(
                onBackClick = {
                    currentScreen = AppScreen.HomeDashboard
                }
            )
        }
        AppScreen.Flashcards -> {
            FlashcardsScreen(
                onBackClick = {
                    currentScreen = AppScreen.HomeDashboard
                }
            )
        }
    }

    // --- Back Navigation Handlers ---
    // Prevent returning to Login using the Back button immediately after successful login or while on Dashboard
    BackHandler(enabled = currentScreen == AppScreen.HomeDashboard) {
        (context as? ComponentActivity)?.finish()
    }

    BackHandler(enabled = currentScreen == AppScreen.Login) {
        (context as? ComponentActivity)?.finish()
    }

    BackHandler(enabled = currentScreen == AppScreen.Signup || currentScreen == AppScreen.ForgotPassword || currentScreen == AppScreen.StudentOnboarding) {
        currentScreen = AppScreen.Login
    }

    BackHandler(enabled = currentScreen == AppScreen.SubjectSelection) {
        currentScreen = AppScreen.StudentOnboarding
    }

    BackHandler(enabled = currentScreen == AppScreen.RecordLecture || currentScreen == AppScreen.UploadLecture || currentScreen == AppScreen.GeneratedNotes || currentScreen == AppScreen.MyNotes || currentScreen == AppScreen.ProfileSettings || currentScreen == AppScreen.AiChat || currentScreen == AppScreen.Flashcards) {
        currentScreen = AppScreen.HomeDashboard
    }

    BackHandler(enabled = currentScreen == AppScreen.LecturePreview) {
        currentScreen = AppScreen.HomeDashboard
    }

    BackHandler(enabled = currentScreen == AppScreen.Transcript) {
        currentScreen = AppScreen.LecturePreview
    }

    BackHandler(enabled = currentScreen == AppScreen.CreateSmartNotes) {
        currentScreen = AppScreen.Transcript
    }

    BackHandler(enabled = currentScreen == AppScreen.DetailedNotes) {
        currentScreen = previousScreenForNotes
    }

    BackHandler(enabled = currentScreen == AppScreen.QuizGenerator) {
        currentScreen = previousScreenForQuiz
    }

    BackHandler(enabled = currentScreen == AppScreen.QuizQuestion || currentScreen == AppScreen.QuizResults) {
        currentScreen = AppScreen.QuizGenerator
    }

    // --- Interactive Action Dialogs & Sheets ---

    // 0. SmartNotes AI Assistant Bottom Sheet
    if (showAiAssistantSheet) {
        QaAssistantBottomSheet(
            onDismissRequest = { showAiAssistantSheet = false }
        )
    }

    // 1. Dashboard Notification Alerts Dialog
    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Text(
                    text = "Notifications & Study Alerts",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDarkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    NotificationAlertItem(
                        title = "Lecture Notes Ready",
                        message = "AI summary and key takeaways for Database Normalization are ready.",
                        time = "10m ago"
                    )
                    NotificationAlertItem(
                        title = "Daily Study Streak",
                        message = "Review AI & Machine Learning concepts to keep your 4-day streak alive.",
                        time = "2h ago"
                    )
                    NotificationAlertItem(
                        title = "Practice Quiz Available",
                        message = "Take a 5-question AI quiz to test your memory on recent lectures.",
                        time = "Yesterday"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showNotificationsDialog = false
                        Toast.makeText(context, "All notifications marked as read", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Mark All as Read", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text("Close", color = TextDarkPrimary)
                }
            }
        )
    }

    // 2. Account & Password Information Dialog
    if (showAccountDialog) {
        val userEmail = SessionManager.getUserEmail() ?: "student@university.edu"
        AlertDialog(
            onDismissRequest = { showAccountDialog = false },
            title = {
                Text(
                    text = "Account & Security",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDarkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Account Details",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = PurplePrimary
                    )
                    Text("• Email: $userEmail", fontSize = 13.sp, color = TextDarkPrimary)
                    Text("• Role: University Student", fontSize = 13.sp, color = TextDarkPrimary)
                    Text("• Session: Active & Authenticated", fontSize = 13.sp, color = TextDarkPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Password Security",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = PurplePrimary
                    )
                    Text(
                        text = "Your password is encrypted and securely stored. To reset your password, please use the Forgot Password link on the login screen or contact your university administrator.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAccountDialog = false }) {
                    Text("Got it", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 3. Privacy & Audio Storage Dialog
    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(
                    text = "Privacy & Audio Storage",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = TextDarkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Academic Privacy Assurance",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = PurplePrimary
                    )
                    Text("• All lecture audio and notes are strictly private to your account.", fontSize = 12.sp, color = TextDarkPrimary)
                    Text("• Audio files are processed securely in local app sandboxes.", fontSize = 12.sp, color = TextDarkPrimary)
                    Text("• Class contents are never shared with third parties or external AI training.", fontSize = 12.sp, color = TextDarkPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Local Storage & Cache",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = PurplePrimary
                    )
                    Text("• Temporary recording buffers are automatically cleaned up after note creation.", fontSize = 12.sp, color = TextMuted)
                    Text("• You can delete any saved note anytime from the My Notes screen.", fontSize = 12.sp, color = TextMuted)
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Understand", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // 4. Transcript Options Menu Dialog
    if (showTranscriptOptionsMenu) {
        AlertDialog(
            onDismissRequest = { showTranscriptOptionsMenu = false },
            title = { Text("Transcript Options", fontWeight = FontWeight.Bold, color = TextDarkPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Lecture Transcript", "Lecture Transcript: Normalization organizes relations to minimize repeated data and prevent anomalies. 1NF, 2NF, 3NF.")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Transcript copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showTranscriptOptionsMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy Transcript to Clipboard", color = PurplePrimary, modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Lecture Transcript from SmartNotes AI:\nNormalization organizes relations to minimize repeated data and protect data integrity.")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Transcript via"))
                            showTranscriptOptionsMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share Transcript...", color = PurplePrimary, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTranscriptOptionsMenu = false }) {
                    Text("Close", color = TextDarkPrimary)
                }
            }
        )
    }

    // 5. Notes Options Menu Dialog
    if (showNotesOptionsMenu) {
        AlertDialog(
            onDismissRequest = { showNotesOptionsMenu = false },
            title = { Text("Notes Options", fontWeight = FontWeight.Bold, color = TextDarkPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "SmartNotes AI Summary:\nDatabase Normalization — Organizing relations to minimize duplicate data and anomalies.")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Note Summary via"))
                            showNotesOptionsMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share Note Summary...", color = PurplePrimary, modifier = Modifier.fillMaxWidth())
                    }
                    TextButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Note Summary", "Database Normalization: Organizing relations to minimize repeated data. 1NF, 2NF, 3NF.")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Summary copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showNotesOptionsMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Copy Summary to Clipboard", color = PurplePrimary, modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotesOptionsMenu = false }) {
                    Text("Close", color = TextDarkPrimary)
                }
            }
        )
    }

    // 6. Discard Recording Confirmation Dialog
    if (showDiscardRecordDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardRecordDialog = false },
            title = { Text("Discard Recording?", fontWeight = FontWeight.Bold, color = TextDarkPrimary) },
            text = { Text("Are you sure you want to discard this recording? Current lecture audio will be cancelled.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardRecordDialog = false
                        currentScreen = AppScreen.HomeDashboard
                        Toast.makeText(context, "Recording discarded", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Discard", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardRecordDialog = false }) {
                    Text("Keep Recording", color = TextDarkPrimary)
                }
            }
        )
    }

    // 7. Discard Lecture Preview Confirmation Dialog
    if (showDiscardPreviewDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardPreviewDialog = false },
            title = { Text("Discard Lecture?", fontWeight = FontWeight.Bold, color = TextDarkPrimary) },
            text = { Text("Are you sure you want to discard this lecture without generating transcript?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardPreviewDialog = false
                        currentScreen = AppScreen.HomeDashboard
                        Toast.makeText(context, "Lecture discarded", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Discard", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardPreviewDialog = false }) {
                    Text("Keep", color = TextDarkPrimary)
                }
            }
        )
    }

    // 8. PDF Export Result Dialog (Open / Share / Dismiss)
    if (pdfExportDialogResult != null) {
        val result = pdfExportDialogResult!!
        AlertDialog(
            onDismissRequest = { pdfExportDialogResult = null },
            title = {
                Text(
                    text = "PDF Saved to Downloads",
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = pdfExportDialogTitle.ifBlank { "Lecture Notes" },
                        fontWeight = FontWeight.SemiBold,
                        color = TextDarkPrimary
                    )
                    Text(
                        text = "File: ${result.fileName}\nLocation: Downloads/SmartNotes_AI\nPages: ${result.pageCount}",
                        color = TextMuted
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        com.example.Simtas.utils.PdfExportManager.openPdf(context, result.uri)
                        pdfExportDialogResult = null
                    }
                ) {
                    Text("Open PDF", color = PurplePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            com.example.Simtas.utils.PdfExportManager.sharePdf(context, result.uri, pdfExportDialogTitle)
                            pdfExportDialogResult = null
                        }
                    ) {
                        Text("Share", color = PurplePrimary, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { pdfExportDialogResult = null }) {
                        Text("Dismiss", color = TextDarkPrimary)
                    }
                }
            }
        )
    }
}

@Composable
private fun NotificationAlertItem(
    title: String,
    message: String,
    time: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkPrimary
            )
            Text(
                text = time,
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = message,
            fontSize = 12.sp,
            color = TextMuted,
            lineHeight = 16.sp
        )
    }
}

/**
 * Real Native PDF Generation using Android SDK's [PdfDocument] via [com.example.Simtas.utils.PdfExportManager].
 * Formats notes, writes to the Downloads directory, and notifies user.
 */
private fun generateAndSavePdf(
    context: Context,
    noteTitle: String,
    noteSubject: String,
    noteSummary: String,
    keyTakeaways: List<String>
) {
    try {
        val pdfData = com.example.Simtas.utils.NotePdfData(
            title = noteTitle.ifBlank { "Lecture Notes" },
            subject = noteSubject.ifBlank { "General" },
            dateText = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()),
            section1Title = "Overview",
            section1Content = noteSummary,
            section2Title = "Key Takeaways",
            section2Bullets = keyTakeaways
        )
        val result = com.example.Simtas.utils.PdfExportManager.exportNoteToPdf(context, pdfData)
        if (result.isSuccess) {
            val exportResult = result.getOrNull()
            Toast.makeText(context, "PDF saved to Downloads:\n${exportResult?.fileName}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "Could not save PDF: ${result.exceptionOrNull()?.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Could not save PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}
