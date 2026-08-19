package com.messledger.app.ui.mess.meals

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messledger.app.data.model.Member
import com.messledger.app.ui.components.ErrorDialog
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MealsScreen(
    messId: String,
    onNavigateBack: () -> Unit = {},
    viewModel: MealsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.toastMessage.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        when (val state = uiState) {
            is MealsUiState.Loading -> LoadingOverlay(isLoading = true)
            is MealsUiState.Error -> ErrorDialog(message = state.message, onDismiss = onNavigateBack)
            is MealsUiState.Success -> {
                MealsContent(
                    state = state,
                    onPreviousDay = { viewModel.previousDay() },
                    onNextDay = { viewModel.nextDay() },
                    onSelectDate = { viewModel.setSelectedDate(it) },
                    onToggleMeal = { memberId, slot, currentAbsent ->
                        viewModel.toggleMeal(memberId, slot, currentAbsent)
                    }
                )
            }
        }
    }
}

@Composable
private fun MealsContent(
    state: MealsUiState.Success,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onSelectDate: (String) -> Unit,
    onToggleMeal: (memberId: String, slot: String, currentAbsent: Boolean) -> Unit
) {
    val context = LocalContext.current

    // Native DatePicker dialog clamped to today's date
    val calendar = Calendar.getInstance()
    val parts = state.selectedDate.split("-")
    val selYear = parts.getOrNull(0)?.toIntOrNull() ?: calendar.get(Calendar.YEAR)
    val selMonth = parts.getOrNull(1)?.toIntOrNull() ?: (calendar.get(Calendar.MONTH) + 1)
    val selDay = parts.getOrNull(2)?.toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)

    val datePickerDialog = remember(state.selectedDate) {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val newDate = String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                onSelectDate(newDate)
            },
            selYear,
            selMonth - 1,
            selDay
        ).apply {
            datePicker.maxDate = System.currentTimeMillis()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Panel Header (matches .panel-header) ─────────────────────────
        // .panel-title is a <div> in index.html -> uses IbmPlexSansFamily (bold), NOT Zilla Slab
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Meals",
                style = TextStyle(
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = LedgerGreen
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Everyone is counted present by default — mark only who's absent.",
                style = TextStyle(
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = InkSoft
                )
            )
        }

        // ── Section Card (matches .section-card) ─────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = PaperWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Date Field Label
                Text(
                    text = "DATE",
                    style = TextStyle(
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.5.sp,
                        color = InkSoft
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Date Picker Input Control
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Paper)
                        .border(1.5.dp, PaperLine, RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Day Button
                    IconButton(
                        onClick = onPreviousDay,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Day",
                            tint = LedgerGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Date clickable center area
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { datePickerDialog.show() }
                            .padding(vertical = 6.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = LedgerGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formatDateDisplay(state.selectedDate, state.isToday),
                            style = TextStyle(
                                fontFamily = IbmPlexMonoFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.5.sp,
                                color = Ink
                            )
                        )
                    }

                    // Next Day Button (disabled if today)
                    IconButton(
                        onClick = onNextDay,
                        enabled = !state.isToday,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Day",
                            tint = if (!state.isToday) LedgerGreen else InkSoft.copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // ── Member Meal Rows (matches .list-wrap / .meal-member-row) ──
                if (state.members.isEmpty()) {
                    Text(
                        text = "Add members from Settings first.",
                        style = TextStyle(
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 14.sp,
                            color = InkSoft,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 30.dp, horizontal = 10.dp)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        state.members.forEachIndexed { index, memberRow ->
                            MealMemberRowItem(
                                item = memberRow,
                                isLast = index == state.members.lastIndex,
                                onToggleLunch = {
                                    onToggleMeal(memberRow.member.id, "lunch", memberRow.lunchAbsent)
                                },
                                onToggleDinner = {
                                    onToggleMeal(memberRow.member.id, "dinner", memberRow.dinnerAbsent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MealMemberRowItem(
    item: MealMemberUiModel,
    isLast: Boolean,
    onToggleLunch: () -> Unit,
    onToggleDinner: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                if (!isLast) {
                    drawLine(
                        color = PaperLine,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }
            }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Avatar + Name (truncated with ellipsis if long)
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Member Avatar (28x28 circle, Zilla Slab bold initial)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(LedgerGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = memberInitial(item.member.name),
                    style = TextStyle(
                        fontFamily = ZillaSlabFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = PaperWhite
                    )
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = item.member.name,
                style = TextStyle(
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = Ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Right: Lunch & Dinner toggle buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lunch Column
            MealSlotColumn(
                label = "Lunch",
                isAbsent = item.lunchAbsent,
                onToggle = onToggleLunch
            )

            // Dinner Column
            MealSlotColumn(
                label = "Dinner",
                isAbsent = item.dinnerAbsent,
                onToggle = onToggleDinner
            )
        }
    }
}

@Composable
private fun MealSlotColumn(
    label: String,
    isAbsent: Boolean,
    onToggle: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // .meal-pill-label
        Text(
            text = label.uppercase(),
            style = TextStyle(
                fontFamily = IbmPlexSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 10.sp,
                letterSpacing = 0.4.sp,
                color = InkSoft,
                textAlign = TextAlign.Center
            )
        )
        Spacer(modifier = Modifier.height(4.dp))

        // .meal-pill (.present / .absent)
        val bgColor = if (isAbsent) DebitRedBg else CreditGreenBg
        val contentColor = if (isAbsent) DebitRed else CreditGreen
        val text = if (isAbsent) "ABSENT" else "PRESENT"

        Box(
            modifier = Modifier
                .widthIn(min = 70.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(bgColor)
                .border(1.5.dp, contentColor, RoundedCornerShape(20.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = TextStyle(
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.4.sp,
                    color = contentColor,
                    textAlign = TextAlign.Center
                )
            )
        }
    }
}

// ── Formatting and Helper Functions ────────────────────────────────────────

private fun memberInitial(name: String): String {
    return name.trim().take(1).uppercase()
}

private fun formatDateDisplay(dateStr: String, isToday: Boolean): String {
    if (dateStr.isBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val date = parser.parse(dateStr)
        if (date != null) {
            val formatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.ENGLISH)
            val formatted = formatter.format(date)
            if (isToday) "Today ($formatted)" else formatted
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}
