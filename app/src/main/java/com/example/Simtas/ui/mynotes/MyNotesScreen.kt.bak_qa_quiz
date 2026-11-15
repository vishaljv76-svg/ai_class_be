package com.example.Simtas.ui.mynotes

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
import com.example.Simtas.ui.home.RecentNoteIconType
import com.example.Simtas.ui.theme.AIBasedClassNotesAppTheme
import com.example.Simtas.ui.theme.InnerCircleBg
import com.example.Simtas.ui.theme.PurplePrimary
import com.example.Simtas.ui.theme.SplashBackground
import com.example.Simtas.ui.theme.TextDarkPrimary
import com.example.Simtas.ui.theme.TextMuted
import com.example.Simtas.ui.theme.TextSubtitle

val ChipUnselectedBg = Color.White
val TrashRed = Color(0xFFE53935)

/**
 * My Notes screen adhering to MVVM architecture.
 */
@Composable
fun MyNotesScreen(
    modifier: Modifier = Modifier,
    viewModel: MyNotesViewModel = remember { MyNotesViewModel() },
    onAddNoteClick: () -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onTabSelect: (HomeBottomTab) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    MyNotesScreenContent(
        uiState = uiState,
        modifier = modifier,
        onAddNoteClick = onAddNoteClick,
        onSearchQueryChange = viewModel::onSearchQueryChanged,
        onCategorySelect = viewModel::selectCategory,
        onNoteClick = onNoteClick,
        onDeleteNoteClick = viewModel::onDeleteNote,
        onTabSelect = { tab ->
            viewModel.selectTab(tab)
            onTabSelect(tab)
        }
    )
}

@Composable
fun MyNotesScreenContent(
    uiState: MyNotesUiState,
    modifier: Modifier = Modifier,
    onAddNoteClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onCategorySelect: (Int) -> Unit = {},
    onNoteClick: (String) -> Unit = {},
    onDeleteNoteClick: (String) -> Unit = {},
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
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Top Bar
                TopBarHeader(onAddNoteClick = onAddNoteClick)

                Spacer(modifier = Modifier.height(20.dp))

                // Headline & Stats Subtitle
                Text(
                    text = uiState.mainHeadline,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDarkPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = uiState.statsSubtitle,
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Search Bar Input
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = {
                        Text(
                            text = "Search your notes",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        disabledContainerColor = Color.White,
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = TextDarkPrimary,
                        unfocusedTextColor = TextDarkPrimary
                    ),
                    leadingIcon = {
                        Canvas(modifier = Modifier.size(18.dp)) {
                            drawSearchIcon(TextMuted)
                        }
                    },
                    trailingIcon = {
                        IconButton(onClick = {}) {
                            Canvas(modifier = Modifier.size(18.dp)) {
                                drawFilterSlidersIcon(PurplePrimary)
                            }
                        }
                    },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Filter Chips Row & Sort Indicator
                CategoryFilterChipsRow(
                    selectedCategoryIndex = uiState.selectedCategoryIndex,
                    onCategorySelect = onCategorySelect
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Month Section Header Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.monthSectionTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDarkPrimary
                    )

                    Text(
                        text = uiState.recentNotesCountText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurplePrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes List Items
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    uiState.notes.forEach { note ->
                        NoteCardItem(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onDeleteClick = { onDeleteNoteClick(note.id) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Backup Status Footer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        drawCloudCheckIcon(Color(0xFF388E3C))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = uiState.backupStatusText,
                        fontSize = 11.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
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
 * Top Navigation Bar with Title & Add Plus Icon
 */
@Composable
private fun TopBarHeader(onAddNoteClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "My notes",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = TextDarkPrimary
        )

        IconButton(
            onClick = onAddNoteClick,
            modifier = Modifier.size(36.dp)
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                drawPlusIcon(TextDarkPrimary)
            }
        }
    }
}

/**
 * Category Filter Chips Row
 */
@Composable
private fun CategoryFilterChipsRow(
    selectedCategoryIndex: Int,
    onCategorySelect: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val categories = listOf("All · 24", "CS · 16", "Math · 8")
            categories.forEachIndexed { index, label ->
                val isSelected = index == selectedCategoryIndex
                val bg = if (isSelected) InnerCircleBg else ChipUnselectedBg
                val textColor = if (isSelected) PurplePrimary else TextSubtitle

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(bg)
                        .clickable { onCategorySelect(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor
                    )
                }
            }
        }

        // Sort indicator
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Newest",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = PurplePrimary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Canvas(modifier = Modifier.size(12.dp)) {
                drawSortIcon(PurplePrimary)
            }
        }
    }
}

/**
 * Individual Note Card Item with Actions (Edit, Delete, PDF)
 */
@Composable
private fun NoteCardItem(
    note: NoteListItem,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .clip(shape)
            .background(Color.White)
            .clickable { onClick() }
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
                            when (note.iconType) {
                                RecentNoteIconType.Database -> drawDatabaseIcon(PurplePrimary)
                                RecentNoteIconType.Math -> drawCalculatorIcon(PurplePrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = note.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "${note.subject} · ${note.dateText}",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sub-row: Duration & Action Buttons (Pencil, Trash, PDF)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${note.durationText} · ${note.readTimeText}",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Edit pencil icon
                    Canvas(
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onClick() }
                    ) {
                        drawPencilEditIcon(PurplePrimary)
                    }

                    // Trash delete icon
                    Canvas(
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDeleteClick() }
                    ) {
                        drawTrashDeleteIcon(TextMuted)
                    }

                    // PDF download icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { /* Download PDF */ }
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawPdfDownloadIcon(PurplePrimary)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PDF",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurplePrimary
                        )
                    }
                }
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

private fun DrawScope.drawPlusIcon(color: Color) {
    val strokeWidth = 2.dp.toPx()
    drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.2f), end = Offset(size.width * 0.5f, size.height * 0.8f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.5f), end = Offset(size.width * 0.8f, size.height * 0.5f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawSearchIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = size.width * 0.32f, center = Offset(size.width * 0.4f, size.height * 0.4f), style = stroke)
    drawLine(color = color, start = Offset(size.width * 0.62f, size.height * 0.62f), end = Offset(size.width * 0.85f, size.height * 0.85f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawFilterSlidersIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.15f, size.height * 0.3f), end = Offset(size.width * 0.85f, size.height * 0.3f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.15f, size.height * 0.7f), end = Offset(size.width * 0.85f, size.height * 0.7f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.35f, size.height * 0.3f))
    drawCircle(color = color, radius = 2.5.dp.toPx(), center = Offset(size.width * 0.65f, size.height * 0.7f))
}

private fun DrawScope.drawSortIcon(color: Color) {
    val strokeWidth = 1.8.dp.toPx()
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.25f), end = Offset(size.width * 0.8f, size.height * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.5f), end = Offset(size.width * 0.6f, size.height * 0.5f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.75f), end = Offset(size.width * 0.4f, size.height * 0.75f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
}

private fun DrawScope.drawDatabaseIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawArc(color = color, startAngle = 0f, sweepAngle = 360f, useCenter = false, topLeft = Offset(size.width * 0.2f, size.height * 0.15f), size = Size(size.width * 0.6f, size.height * 0.22f), style = stroke)
    drawArc(color = color, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(size.width * 0.2f, size.height * 0.38f), size = Size(size.width * 0.6f, size.height * 0.22f), style = stroke)
    drawArc(color = color, startAngle = 0f, sweepAngle = 180f, useCenter = false, topLeft = Offset(size.width * 0.2f, size.height * 0.62f), size = Size(size.width * 0.6f, size.height * 0.22f), style = stroke)
}

private fun DrawScope.drawCalculatorIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.2f, size.height * 0.15f),
        size = Size(size.width * 0.6f, size.height * 0.7f),
        cornerRadius = CornerRadius(4.dp.toPx()),
        style = stroke
    )
    drawRoundRect(
        color = color,
        topLeft = Offset(size.width * 0.3f, size.height * 0.25f),
        size = Size(size.width * 0.4f, size.height * 0.18f),
        cornerRadius = CornerRadius(2.dp.toPx()),
        style = stroke
    )
}

private fun DrawScope.drawPencilEditIcon(color: Color) {
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.8f), end = Offset(size.width * 0.8f, size.height * 0.2f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawTrashDeleteIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawLine(color = color, start = Offset(size.width * 0.2f, size.height * 0.3f), end = Offset(size.width * 0.8f, size.height * 0.3f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    drawRoundRect(color = color, topLeft = Offset(size.width * 0.28f, size.height * 0.3f), size = Size(size.width * 0.44f, size.height * 0.55f), cornerRadius = CornerRadius(2.dp.toPx()), style = stroke)
}

private fun DrawScope.drawPdfDownloadIcon(color: Color) {
    val stroke = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
    val trayPath = Path().apply {
        moveTo(size.width * 0.2f, size.height * 0.65f)
        lineTo(size.width * 0.2f, size.height * 0.8f)
        lineTo(size.width * 0.8f, size.height * 0.8f)
        lineTo(size.width * 0.8f, size.height * 0.65f)
    }
    drawPath(path = trayPath, color = color, style = stroke)
    drawLine(color = color, start = Offset(size.width * 0.5f, size.height * 0.2f), end = Offset(size.width * 0.5f, size.height * 0.6f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
}

private fun DrawScope.drawCloudCheckIcon(color: Color) {
    val stroke = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    val cloudPath = Path().apply {
        moveTo(size.width * 0.25f, size.height * 0.65f)
        quadraticTo(size.width * 0.15f, size.height * 0.65f, size.width * 0.15f, size.height * 0.5f)
        quadraticTo(size.width * 0.15f, size.height * 0.35f, size.width * 0.32f, size.height * 0.32f)
        quadraticTo(size.width * 0.42f, size.height * 0.15f, size.width * 0.6f, size.height * 0.22f)
        quadraticTo(size.width * 0.8f, size.height * 0.22f, size.width * 0.82f, size.height * 0.45f)
        quadraticTo(size.width * 0.9f, size.height * 0.5f, size.width * 0.82f, size.height * 0.65f)
        close()
    }
    drawPath(path = cloudPath, color = color, style = stroke)
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MyNotesScreenPreview() {
    AIBasedClassNotesAppTheme {
        MyNotesScreenContent(uiState = MyNotesUiState())
    }
}
