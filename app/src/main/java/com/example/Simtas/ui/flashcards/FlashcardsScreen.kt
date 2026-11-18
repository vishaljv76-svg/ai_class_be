package com.example.Simtas.ui.flashcards

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.Simtas.R
import com.example.Simtas.data.local.FlashcardDeck
import com.example.Simtas.data.local.FlashcardItem

// ============================================================================
// COLOURFUL THEME PALETTE (Purple/Violet Gradients + Bright Orange Accents + Crisp White)
// ============================================================================
private val DeepVioletBg1 = Color(0xFF2C1052)
private val DeepVioletBg2 = Color(0xFF4A1A7F)
private val DeepVioletBg3 = Color(0xFF672BA6)
private val DeepVioletBg4 = Color(0xFF7E38C4)

private val PurpleBgGradient = Brush.verticalGradient(
    colors = listOf(DeepVioletBg1, DeepVioletBg2, DeepVioletBg3, DeepVioletBg4)
)

// Bright Orange Primary Buttons & Important Highlights
private val BrightOrangePrimary = Color(0xFFFF6D00)
private val BrightOrangeAccent = Color(0xFFFF851A)
private val OrangeGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFF7A00), Color(0xFFFF5252))
)
private val OrangeWarmGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFFFF9100), Color(0xFFFF5E36))
)

// Clean White Rounded Cards with Soft Shadows
private val WhiteCardBg = Color(0xFFFFFFFF)
private val WhiteCardBorder = Color(0xFFEDE9FE)
private val WhiteCardSubtle = Color(0xFFFBF9FF)
private val WhiteCardShadowColor = Color(0x331E064A)

// Vibrant Accent Colors
private val CyberCyan = Color(0xFF00D2D3)
private val ElectricLilac = Color(0xFFA78BFA)
private val SunshineAmber = Color(0xFFFFB300)
private val MintGreenSuccess = Color(0xFF10B981)
private val CoralRoseWarning = Color(0xFFFF4757)

// Text Colors
private val TextDarkHeader = Color(0xFF1E1B2E)
private val TextDarkBody = Color(0xFF332D47)
private val TextDarkMuted = Color(0xFF726A8F)

// ============================================================================
// ROOT COMPOSABLE
// ============================================================================
@Composable
fun FlashcardsScreen(
    modifier: Modifier = Modifier,
    viewModel: FlashcardsViewModel = remember { FlashcardsViewModel() },
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler {
        when (uiState.viewMode) {
            FlashcardViewMode.STUDY_SESSION, FlashcardViewMode.DECK_COMPLETED -> {
                viewModel.returnToCreation()
            }
            FlashcardViewMode.CREATION -> {
                onBackClick()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PurpleBgGradient)
    ) {
        // Ambient background glowing mesh
        AmbientStudyGlowOverlay()

        when (uiState.viewMode) {
            FlashcardViewMode.CREATION -> {
                FlashcardsCreationContent(
                    uiState = uiState,
                    onBackClick = onBackClick,
                    onTopicChange = viewModel::onTopicInputChanged,
                    onNumCardsChange = viewModel::onNumCardsChanged,
                    onGenerateClick = viewModel::generateFlashcards,
                    onDeckClick = viewModel::startDeck,
                    onDeleteDeck = viewModel::deleteDeck
                )
            }
            FlashcardViewMode.STUDY_SESSION -> {
                FlashcardStudyContent(
                    uiState = uiState,
                    onBackClick = viewModel::returnToCreation,
                    onFlipClick = viewModel::flipCard,
                    onMarkCard = viewModel::markCard,
                    onPreviousClick = viewModel::previousCard,
                    onNextClick = viewModel::nextCard
                )
            }
            FlashcardViewMode.DECK_COMPLETED -> {
                FlashcardCompletedContent(
                    uiState = uiState,
                    onStudyAgainClick = viewModel::restartDeck,
                    onReviewMissedClick = viewModel::reviewMissedOnly,
                    onBackToDecksClick = viewModel::returnToCreation
                )
            }
        }
    }
}

// ============================================================================
// 1. SCREEN A: FLASHCARD CREATION & SAVED DECKS (SHOWS BOTH MASCOTS TOGETHER)
// ============================================================================
@Composable
private fun FlashcardsCreationContent(
    uiState: FlashcardsUiState,
    onBackClick: () -> Unit,
    onTopicChange: (String) -> Unit,
    onNumCardsChange: (Int) -> Unit,
    onGenerateClick: () -> Unit,
    onDeckClick: (FlashcardDeck) -> Unit,
    onDeleteDeck: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(18.dp))

        // Top App Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                    .clickable { onBackClick() }
            ) {
                Text(
                    text = "←",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = "AI Smart Flashcards",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "Active Recall & Spaced Repetition",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Hero Duo Mascot Banner (Shows BOTH characters together: Cat with glasses & Dog with grad cap)
        HeroDuoMascotBannerCard()

        Spacer(modifier = Modifier.height(20.dp))

        // Main Input Card (Clean White Rounded Card with Soft Shadow)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = WhiteCardShadowColor,
                    spotColor = WhiteCardShadowColor
                )
                .clip(RoundedCornerShape(26.dp))
                .background(WhiteCardBg)
                .padding(22.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(BrightOrangePrimary.copy(alpha = 0.15f))
                    ) {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawSparkleStar(BrightOrangePrimary)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Subject or Topic",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkHeader
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter any Computer Science subject, concept, or exam topic.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = TextDarkMuted
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Topic Input TextField
                OutlinedTextField(
                    value = uiState.topicInput,
                    onValueChange = onTopicChange,
                    placeholder = {
                        Text(
                            text = "e.g., Computer Networks, Python OOP, Normalization",
                            fontSize = 14.sp,
                            color = TextDarkMuted.copy(alpha = 0.7f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrightOrangePrimary,
                        unfocusedBorderColor = WhiteCardBorder,
                        focusedTextColor = TextDarkHeader,
                        unfocusedTextColor = TextDarkHeader,
                        focusedContainerColor = WhiteCardSubtle,
                        unfocusedContainerColor = WhiteCardSubtle
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Suggestion Chips
                Text(
                    text = "Popular Suggestions:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                val suggestions = listOf(
                    "🌐 Computer Networks",
                    "🐍 Python OOP",
                    "🗄️ DBMS Normalization",
                    "⚙️ Operating Systems",
                    "🤖 Artificial Intelligence"
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    suggestions.take(3).forEach { item ->
                        val cleanTopic = item.substringAfter(" ").trim()
                        val isSelected = uiState.topicInput.equals(cleanTopic, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) BrightOrangePrimary.copy(alpha = 0.16f) else Color(0xFFF5F3FF)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) BrightOrangePrimary else Color(0xFFDDD6FE),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onTopicChange(cleanTopic) }
                                .padding(vertical = 8.dp, horizontal = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) BrightOrangePrimary else TextDarkHeader,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Number of Flashcards Selector
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Number of Flashcards",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkHeader
                    )

                    Text(
                        text = "${uiState.numCards} Cards Selected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrightOrangePrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val countOptions = listOf(5, 10, 15, 20)
                    for (count in countOptions) {
                        val isSelected = uiState.numCards == count
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (isSelected) OrangeWarmGradient else Brush.linearGradient(
                                        listOf(Color(0xFFF8F6FF), Color(0xFFEDE9FE))
                                    )
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else Color(0xFFDDD6FE),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { onNumCardsChange(count) }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$count",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else TextDarkHeader
                            )
                        }
                    }
                }

                if (!uiState.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CoralRoseWarning.copy(alpha = 0.12f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CoralRoseWarning
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // Bright Orange Primary Button: "Generate Flashcards"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = BrightOrangePrimary.copy(alpha = 0.45f),
                            spotColor = BrightOrangePrimary.copy(alpha = 0.6f)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(OrangeGradient)
                        .clickable(enabled = !uiState.isLoading) {
                            onGenerateClick()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Gemini is crafting flashcards...",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(20.dp)) {
                                drawSparkleStar(Color.White)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Generate Flashcards ✨",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Saved Decks Section
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp, 16.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(BrightOrangePrimary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Saved Flashcard Decks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${uiState.savedDecks.size} Decks",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (uiState.savedDecks.isEmpty()) {
            // Friendly Empty State Card with Both Mascots
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 6.dp, shape = RoundedCornerShape(22.dp), ambientColor = WhiteCardShadowColor)
                    .clip(RoundedCornerShape(22.dp))
                    .background(WhiteCardBg)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = R.drawable.mascot_duo_study),
                        contentDescription = "Oliver and Milo Study Buddies",
                        modifier = Modifier.size(90.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "No Decks Created Yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkHeader
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Generate your first deck above to begin studying with Oliver & Milo!",
                        fontSize = 13.sp,
                        color = TextDarkMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                for (deck in uiState.savedDecks) {
                    ColourfulSavedDeckItem(
                        deck = deck,
                        onOpen = { onDeckClick(deck) },
                        onDelete = { onDeleteDeck(deck.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}

/**
 * Hero Banner Card displaying BOTH study mascots together (Cat wearing glasses & Dog with grad cap)
 */
@Composable
private fun HeroDuoMascotBannerCard() {
    val infiniteTransition = rememberInfiniteTransition(label = "DuoMascotBob")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color.Black.copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF5B229D),
                        Color(0xFF7A36CA),
                        Color(0xFF934FEB)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.15f)
                    )
                ),
                RoundedCornerShape(26.dp)
            )
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Cute 3D Duo Mascot Asset (Cat with glasses + Dog with graduation cap)
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .graphicsLayer { translationY = bobOffset },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mascot_duo_study),
                    contentDescription = "Oliver the Cat and Milo the Grad Puppy",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrightOrangePrimary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "MEET YOUR STUDY BUDDIES 🐱🐶",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Study with Oliver & Milo!",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Oliver the Cat asks the questions, and Milo the Grad Puppy reveals the answers!",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.90f),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * Saved Deck Item in Clean White Card with Bright Orange "Study" Button
 */
@Composable
private fun ColourfulSavedDeckItem(
    deck: FlashcardDeck,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = WhiteCardShadowColor
            )
            .clip(RoundedCornerShape(20.dp))
            .background(WhiteCardBg)
            .border(1.dp, WhiteCardBorder, RoundedCornerShape(20.dp))
            .clickable { onOpen() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Vibrant Icon Capsule
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFF6D28D9))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📇", fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deck.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkHeader,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${deck.cards.size} Cards · ${deck.subject} · ${deck.createdAt}",
                    fontSize = 11.sp,
                    color = TextDarkMuted
                )

                if (deck.knownCount > 0 || deck.unknownCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✓ ${deck.knownCount} Known",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintGreenSuccess
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "✕ ${deck.unknownCount} Review",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralRoseWarning
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Bright Orange "Study" Action Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(OrangeWarmGradient)
                    .clickable { onOpen() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Study",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(28.dp)
            ) {
                Text(text = "✕", fontSize = 14.sp, color = TextDarkMuted)
            }
        }
    }
}

// ============================================================================
// 2. SCREEN B: INTERACTIVE STUDY SESSION (CAT ON QUESTION, DOG ON ANSWER)
// ============================================================================
@Composable
private fun FlashcardStudyContent(
    uiState: FlashcardsUiState,
    onBackClick: () -> Unit,
    onFlipClick: () -> Unit,
    onMarkCard: (Boolean) -> Unit,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    val card = uiState.currentCard
    val density = LocalDensity.current.density

    // Smooth spring-based 3D Card Flip rotation
    val animatedRotation by animateFloatAsState(
        targetValue = if (uiState.isCardFlipped) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "3DCardFlip"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Spacer(modifier = Modifier.height(18.dp))

            // Study Top Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .clickable { onBackClick() }
                ) {
                    Text(
                        text = "←",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.activeDeck?.title ?: "Flashcards",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Card ${uiState.currentCardIndex + 1} of ${uiState.totalCards}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // Known / Don't Know Mini Counter Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.20f))
                        .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✓ ${uiState.reviewKnownCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6EE7B7)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "|",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "✕ ${uiState.reviewUnknownCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFCA5A5)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Vibrant Dual-Gradient Progress Bar (Bright Orange to Sunset Pink)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.20f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(uiState.progressFraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(OrangeGradient)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main 3D Flipping Flashcard (Clean White Rounded Card with Soft Shadow)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(350.dp)
                    .graphicsLayer {
                        rotationY = animatedRotation
                        cameraDistance = 16f * density
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = WhiteCardShadowColor,
                        spotColor = WhiteCardShadowColor
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(WhiteCardBg)
                    .border(
                        1.5.dp,
                        if (uiState.isCardFlipped) BrightOrangePrimary.copy(alpha = 0.5f) else WhiteCardBorder,
                        RoundedCornerShape(28.dp)
                    )
                    .clickable { onFlipClick() }
                    .padding(24.dp)
            ) {
                // If rotated past 90 degrees, flip contents so it's readable forward
                val isShowingBack = animatedRotation > 90f

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            if (isShowingBack) {
                                rotationY = 180f
                            }
                        }
                ) {
                    if (isShowingBack) {
                        // BACK OF CARD (ANSWER) - SHOWS DOG MASCOT
                        CardBackContent(
                            answerText = card?.back ?: "",
                            topicName = uiState.activeDeck?.topic ?: ""
                        )
                    } else {
                        // FRONT OF CARD (QUESTION) - SHOWS CAT MASCOT
                        CardFrontContent(
                            cardIndex = uiState.currentCardIndex + 1,
                            questionText = card?.front ?: "",
                            hintText = card?.hint ?: ""
                        )
                    }
                }
            }
        }

        // Bottom Action Controls: "Don't Know" and "Know" Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // "Don't Know" Button (Clean White Card with Coral Red Border & Icon)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .shadow(elevation = 6.dp, shape = RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.15f))
                        .clip(RoundedCornerShape(18.dp))
                        .background(WhiteCardBg)
                        .border(1.5.dp, CoralRoseWarning, RoundedCornerShape(18.dp))
                        .clickable { onMarkCard(false) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✕",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CoralRoseWarning
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Don't Know",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralRoseWarning
                        )
                    }
                }

                // "Know" Button (Vibrant Bright Orange Gradient Button!)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = BrightOrangePrimary.copy(alpha = 0.5f),
                            spotColor = BrightOrangePrimary.copy(alpha = 0.6f)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .background(OrangeGradient)
                        .clickable { onMarkCard(true) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "✓",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Know",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Navigation Row: Prev / Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "← Previous Card",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.currentCardIndex > 0) Color.White else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .clickable(enabled = uiState.currentCardIndex > 0) { onPreviousClick() }
                        .padding(8.dp)
                )

                Text(
                    text = "Next Card →",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (uiState.currentCardIndex < uiState.totalCards - 1) Color.White else Color.White.copy(alpha = 0.4f),
                    modifier = Modifier
                        .clickable(enabled = uiState.currentCardIndex < uiState.totalCards - 1) { onNextClick() }
                        .padding(8.dp)
                )
            }
        }
    }
}

/**
 * Front side of the flashcard (Question) - FEATURES CAT MASCOT (Oliver with glasses & purple book)
 */
@Composable
private fun CardFrontContent(
    cardIndex: Int,
    questionText: String,
    hintText: String
) {
    var showHint by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "CatBob")
    val catBob by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "catBob"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Badge Bar with Cat Mascot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cat Mascot Asset (Orange cartoon cat wearing round glasses & holding purple book)
                Image(
                    painter = painterResource(id = R.drawable.mascot_cat_study),
                    contentDescription = "Oliver the Study Cat",
                    modifier = Modifier
                        .size(46.dp)
                        .graphicsLayer { translationY = catBob }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFEDE9FE))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "QUESTION #$cardIndex",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF6D28D9)
                    )
                }
            }

            if (hintText.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (showHint) SunshineAmber.copy(alpha = 0.20f) else Color(0xFFF3F4F6))
                        .clickable { showHint = !showHint }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (showHint) "💡 Hint on" else "💡 Hint?",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showHint) Color(0xFFB45309) else TextDarkMuted
                    )
                }
            }
        }

        // Question Body
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = questionText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkHeader,
                textAlign = TextAlign.Center,
                lineHeight = 26.sp
            )

            AnimatedVisibility(visible = showHint && hintText.isNotBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7))
                            .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "💡 $hintText",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF92400E),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Tap to reveal prompt with Oliver
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(BrightOrangePrimary.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "🐾 Oliver says: Tap card to reveal answer",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BrightOrangePrimary
            )
        }
    }
}

/**
 * Back side of the flashcard (Answer & Explanation) - FEATURES DOG MASCOT (Milo with graduation cap)
 */
@Composable
private fun CardBackContent(
    answerText: String,
    topicName: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "DogBob")
    val dogBob by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dogBob"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Badge Bar with Dog Mascot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Dog Mascot Asset (Golden puppy wearing graduation cap)
                Image(
                    painter = painterResource(id = R.drawable.mascot_dog_grad),
                    contentDescription = "Milo the Grad Puppy",
                    modifier = Modifier
                        .size(48.dp)
                        .graphicsLayer { translationY = dogBob }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MintGreenSuccess.copy(alpha = 0.18f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "ANSWER & EXPLANATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MintGreenSuccess
                    )
                }
            }

            if (topicName.isNotBlank()) {
                Text(
                    text = topicName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDarkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Answer Body
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = answerText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = TextDarkBody,
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        }

        // Tap to flip back prompt with Milo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFEDE9FE))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "🎓 Milo says: Tap card to flip back",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6D28D9)
            )
        }
    }
}

// ============================================================================
// 3. SCREEN C: DECK COMPLETED SUMMARY (DOG MASCOT WITH GRADUATION CAP)
// ============================================================================
@Composable
private fun FlashcardCompletedContent(
    uiState: FlashcardsUiState,
    onStudyAgainClick: () -> Unit,
    onReviewMissedClick: () -> Unit,
    onBackToDecksClick: () -> Unit
) {
    val total = uiState.totalCards
    val known = uiState.reviewKnownCount
    val missed = uiState.reviewUnknownCount
    val mastery = if (total > 0) ((known.toFloat() / total.toFloat()) * 100).toInt() else 0

    val infiniteTransition = rememberInfiniteTransition(label = "GradDogCelebration")
    val celebrationScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrationScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Celebratory Dog Mascot with Graduation Cap & Confetti
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCelebrationSparkles()
            }

            // Milo the Grad Puppy Asset in Celebration
            Image(
                painter = painterResource(id = R.drawable.mascot_dog_grad),
                contentDescription = "Milo the Grad Puppy Celebrating",
                modifier = Modifier
                    .size(118.dp)
                    .graphicsLayer {
                        scaleX = celebrationScale
                        scaleY = celebrationScale
                    }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Deck Completed! 🎓🎉",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(4.dp))

        val praiseText = when {
            mastery == 100 -> "Flawless Mastery! Milo says you're an academic superstar! 🌟"
            mastery >= 70 -> "Great Work! You and Milo have conquered this topic! 🚀"
            else -> "Good Effort! Milo says spaced repetition turns mistakes into mastery! 💪"
        }

        Text(
            text = praiseText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Clean White Mastery Card with Soft Shadows
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(26.dp),
                    ambientColor = WhiteCardShadowColor
                )
                .clip(RoundedCornerShape(26.dp))
                .background(WhiteCardBg)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MASTERY SCORE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = TextDarkMuted
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$mastery%",
                    fontSize = 44.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (mastery >= 70) MintGreenSuccess else BrightOrangePrimary
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Stats 3-Column Breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$known",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MintGreenSuccess
                        )
                        Text(
                            text = "Known",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDarkMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .width(1.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$missed",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = CoralRoseWarning
                        )
                        Text(
                            text = "Need Review",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDarkMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .width(1.dp)
                            .background(Color(0xFFE5E7EB))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$total",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDarkHeader
                        )
                        Text(
                            text = "Total Cards",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDarkMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Primary Action: Bright Orange "Study Again" Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(18.dp),
                    ambientColor = BrightOrangePrimary.copy(alpha = 0.5f),
                    spotColor = BrightOrangePrimary.copy(alpha = 0.6f)
                )
                .clip(RoundedCornerShape(18.dp))
                .background(OrangeGradient)
                .clickable { onStudyAgainClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Study Again (All Cards)",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        // Secondary Action: Review Missed Cards (if any)
        if (missed > 0) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(18.dp), ambientColor = Color.Black.copy(alpha = 0.1f))
                    .clip(RoundedCornerShape(18.dp))
                    .background(WhiteCardBg)
                    .border(1.5.dp, CoralRoseWarning, RoundedCornerShape(18.dp))
                    .clickable { onReviewMissedClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Review Missed Cards ($missed)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralRoseWarning
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tertiary Action: Back to Decks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.20f))
                .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                .clickable { onBackToDecksClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Back to Flashcard Decks",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

// ============================================================================
// CANVAS VECTOR DRAWINGS (Sparkles & Ambient Glows)
// ============================================================================

/**
 * Ambient background floating glowing orbs and sparkle stars
 */
@Composable
private fun AmbientStudyGlowOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        // Glowing violet radial mesh in top right
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF9333EA).copy(alpha = 0.28f), Color.Transparent),
                center = Offset(size.width * 0.9f, size.height * 0.12f),
                radius = size.width * 0.6f
            ),
            center = Offset(size.width * 0.9f, size.height * 0.12f),
            radius = size.width * 0.6f
        )

        // Glowing bright orange accent aura in bottom left
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(BrightOrangePrimary.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.1f, size.height * 0.85f),
                radius = size.width * 0.55f
            ),
            center = Offset(size.width * 0.1f, size.height * 0.85f),
            radius = size.width * 0.55f
        )
    }
}

/**
 * Draws celebratory floating sparkles and confetti around the dog mascot
 */
private fun DrawScope.drawCelebrationSparkles() {
    val cx = size.width / 2f
    val cy = size.height / 2f

    val confettiColors = listOf(
        BrightOrangePrimary,
        CyberCyan,
        SunshineAmber,
        MintGreenSuccess,
        ElectricLilac,
        Color.White
    )

    val confettiOffsets = listOf(
        Offset(cx - 48.dp.toPx(), cy - 45.dp.toPx()),
        Offset(cx + 46.dp.toPx(), cy - 48.dp.toPx()),
        Offset(cx - 54.dp.toPx(), cy - 10.dp.toPx()),
        Offset(cx + 56.dp.toPx(), cy - 8.dp.toPx()),
        Offset(cx - 42.dp.toPx(), cy + 34.dp.toPx()),
        Offset(cx + 44.dp.toPx(), cy + 36.dp.toPx())
    )

    for (i in confettiOffsets.indices) {
        drawCircle(
            color = confettiColors[i % confettiColors.size],
            radius = 3.5.dp.toPx(),
            center = confettiOffsets[i]
        )
    }
}

/**
 * Draws a 4-point sparkle star
 */
private fun DrawScope.drawSparkleStar(color: Color) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val r = size.minDimension / 2f

    val path = Path().apply {
        moveTo(cx, cy - r)
        quadraticTo(cx, cy, cx + r, cy)
        quadraticTo(cx, cy, cx, cy + r)
        quadraticTo(cx, cy, cx - r, cy)
        quadraticTo(cx, cy, cx, cy - r)
        close()
    }
    drawPath(path = path, color = color, style = Fill)
}
