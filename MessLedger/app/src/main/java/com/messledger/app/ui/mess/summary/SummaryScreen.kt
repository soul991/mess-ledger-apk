package com.messledger.app.ui.mess.summary

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.messledger.app.data.model.Expense
import com.messledger.app.data.model.Member
import com.messledger.app.ui.components.ErrorDialog
import com.messledger.app.ui.components.LoadingOverlay
import com.messledger.app.ui.theme.*
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SummaryScreen(
    messId: String,
    onBackClick: () -> Unit = {},
    viewModel: SummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
    ) {
        when (val state = uiState) {
            is SummaryUiState.Loading -> LoadingOverlay(isLoading = true)
            is SummaryUiState.Error -> ErrorDialog(message = state.message, onDismiss = onBackClick)
            is SummaryUiState.Success -> {
                DashboardContent(state = state)
            }
        }
    }
}

@Composable
private fun DashboardContent(
    state: SummaryUiState.Success
) {
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
                text = "Dashboard",
                style = TextStyle(
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = LedgerGreen
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            val memberWord = if (state.activeMemberCount == 1) "member" else "members"
            Text(
                text = buildAnnotatedString {
                    append("${state.messName} · ${state.activeMemberCount} $memberWord · ")
                    withStyle(SpanStyle(fontFamily = IbmPlexMonoFamily)) {
                        append("ID: ${state.messId}")
                    }
                },
                style = TextStyle(
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = InkSoft
                )
            )
        }

        // ── Cards Row (matches .cards-row, 2 columns) ─────────────────────
        // Card 1: Fund balance (conditional border: credit-green if >= 0, debit-red if < 0)
        // Card 2: This month's expenses (brass border)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Fund balance",
                value = formatINR(state.balance),
                sub = "All-time contributions minus fund spend",
                borderColor = if (state.balance >= 0) CreditGreen else DebitRed,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "This month's expenses",
                value = formatINR(state.thisMonthExpenses),
                sub = "Through ${formatDateShort(state.todayDateStr)}",
                borderColor = Brass,
                modifier = Modifier.weight(1f)
            )
        }

        // Card 3: Meal rate so far (brass border)
        // Card 4: Today's meals (brass border)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                label = "Meal rate so far",
                value = formatINR(state.mealRate),
                sub = "per meal, this month",
                borderColor = Brass,
                modifier = Modifier.weight(1f)
            )
            val guestMealText = if (state.guestsToday > 0) {
                " · ${state.guestsToday} guest meal${if (state.guestsToday == 1) "" else "s"}"
            } else ""
            StatCard(
                label = "Today's meals",
                value = "${state.lunchPresent} / ${state.dinnerPresent}",
                sub = "Lunch / Dinner present$guestMealText",
                borderColor = Brass,
                modifier = Modifier.weight(1f)
            )
        }

        // ── Section Card: Recent Expenses (matches .section-card) ─────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = PaperWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // <h3>Recent expenses</h3> -> literal <h3> uses ZillaSlabFamily (bold)
                Text(
                    text = "Recent expenses",
                    style = TextStyle(
                        fontFamily = ZillaSlabFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = LedgerGreen
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (state.recentExpenses.isEmpty()) {
                    Text(
                        text = "No expenses logged yet. Add one from the Expenses tab.",
                        style = TextStyle(
                            fontFamily = IbmPlexSansFamily,
                            fontSize = 14.sp,
                            color = InkSoft,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 8.dp)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        state.recentExpenses.forEachIndexed { index, expense ->
                            RecentExpenseRow(
                                expense = expense,
                                allMembers = state.allMembers,
                                isLast = index == state.recentExpenses.lastIndex
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    sub: String,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PaperWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(borderColor)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                Text(
                    text = label.uppercase(),
                    style = TextStyle(
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        letterSpacing = 0.5.sp,
                        color = InkSoft
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = value,
                    style = TextStyle(
                        fontFamily = IbmPlexMonoFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = Ink
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = sub,
                    style = TextStyle(
                        fontFamily = IbmPlexSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = InkSoft
                    )
                )
            }
        }
    }
}

@Composable
private fun RecentExpenseRow(
    expense: Expense,
    allMembers: List<Member>,
    isLast: Boolean
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
        // Date column (fixed width, mono)
        Text(
            text = formatDateShort(expense.date),
            style = TextStyle(
                fontFamily = IbmPlexMonoFamily,
                fontSize = 12.sp,
                color = InkSoft
            ),
            modifier = Modifier.width(62.dp)
        )

        // Main info: Category + Paid by {name} (name resolved against all members, including inactive)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = expense.category,
                style = TextStyle(
                    fontFamily = IbmPlexSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.5.sp,
                    color = Ink
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Paid by ${resolveMemberName(expense.paidBy, allMembers)}",
                style = TextStyle(
                    fontFamily = IbmPlexSansFamily,
                    fontSize = 12.sp,
                    color = InkSoft
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Amount (debit-red, mono)
        Text(
            text = formatINR(expense.amount),
            style = TextStyle(
                fontFamily = IbmPlexMonoFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                color = DebitRed,
                textAlign = TextAlign.End
            )
        )
    }
}

// ── Formatting and Lookup Helpers ──────────────────────────────────────────

private fun formatINR(amount: Double): String {
    val rounded = Math.round(amount * 100.0) / 100.0
    val symbols = DecimalFormatSymbols(Locale("en", "IN"))
    val df = DecimalFormat("#,##,##0.##", symbols)
    return "₹" + df.format(rounded)
}

private fun formatDateShort(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val date = parser.parse(dateStr)
        if (date != null) {
            val formatter = SimpleDateFormat("dd MMM", Locale.ENGLISH)
            formatter.format(date)
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}

private fun resolveMemberName(paidBy: String, allMembers: List<Member>): String {
    if (paidBy.equals("fund", ignoreCase = true)) return "Mess Fund"
    val member = allMembers.find { it.id == paidBy }
    return member?.name ?: if (paidBy.isNotBlank()) paidBy else "Unknown"
}
