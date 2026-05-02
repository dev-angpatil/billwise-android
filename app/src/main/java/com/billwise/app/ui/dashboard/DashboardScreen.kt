package com.billwise.app.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billwise.app.ui.theme.BrandCoral80
import com.billwise.app.ui.theme.BrandEmerald80
import com.billwise.app.ui.theme.BrandIndigo80
import com.billwise.app.ui.viewmodel.InsightViewModel
import java.text.SimpleDateFormat
import java.util.*

private val MONTH_NAMES = listOf(
    "Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"
)

private val CATEGORY_COLORS = listOf(
    Color(0xFF818CF8), Color(0xFF34D399), Color(0xFFFBBF24),
    Color(0xFFF87171), Color(0xFF60A5FA), Color(0xFFA78BFA)
)

@Composable
fun DashboardScreen(viewModel: InsightViewModel) {
    val totalSpent        by viewModel.totalSpent.collectAsState()
    val totalIncome       by viewModel.totalIncome.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val budgetProgress    by viewModel.budgetProgress.collectAsState()
    val budget            by viewModel.budget.collectAsState()
    val selectedMonth     by viewModel.selectedMonth.collectAsState()
    val selectedYear      by viewModel.selectedYear.collectAsState()
    val avgDailySpend     by viewModel.averageDailySpend.collectAsState()
    val dailyBudget       by viewModel.dailyBudget.collectAsState()
    val insights          by viewModel.insights.collectAsState()
    val overLimitCategories by viewModel.overLimitCategories.collectAsState()

    val animatedProgress by animateFloatAsState(
        targetValue = budgetProgress,
        animationSpec = tween(durationMillis = 800),
        label = "budgetProgress"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // ── Header + Month Navigator ──────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Dashboard",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        "${MONTH_NAMES[selectedMonth - 1]} $selectedYear",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Next month",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // ── Hero Card: Spend vs Budget ────────────────────────────────────
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF312E81), Color(0xFF1E1B4B))
                            )
                        )
                ) {
                    // Abstract decorative shapes
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawCircle(
                            color = Color(0xFF6366F1).copy(alpha = 0.2f),
                            radius = size.width * 0.4f,
                            center = Offset(size.width, 0f)
                        )
                        drawCircle(
                            color = Color(0xFFEC4899).copy(alpha = 0.15f),
                            radius = size.width * 0.25f,
                            center = Offset(0f, size.height)
                        )
                    }
                    
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            "Total Spent This Month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            "₹${String.format("%,.2f", totalSpent)}",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        if (budget != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "of ₹${String.format("%,.0f", budget!!.monthlyLimit)} budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                val pct = (animatedProgress * 100).toInt()
                                Text(
                                    "$pct%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = when {
                                        animatedProgress >= 1f -> Color(0xFFF87171)
                                        animatedProgress >= 0.8f -> Color(0xFFFBBF24)
                                        else -> Color(0xFF34D399)
                                    }
                                )
                            }
                            // Progress bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(animatedProgress)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            when {
                                                animatedProgress >= 1f -> Color(0xFFF87171)
                                                animatedProgress >= 0.8f -> Color(0xFFFBBF24)
                                                else -> Color(0xFF34D399)
                                            }
                                        )
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column {
                                Text("Income", style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f))
                                Text("₹${String.format("%,.2f", totalIncome)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Daily Burn", style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.6f))
                                Text("₹${String.format("%.0f", avgDailySpend)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (avgDailySpend > dailyBudget && dailyBudget > 0) Color(0xFFF87171) else Color(0xFF34D399),
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // ── Over-Limit Banners ────────────────────────────────────────────
        items(overLimitCategories) { category ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("⚠️", fontSize = 20.sp)
                    Text(
                        "$category limit exceeded!",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // ── Subscriptions & Alerts ────────────────────────────────────────
        val fixedCostsInsight = insights.find { it.contains("Fixed Costs") }
        if (fixedCostsInsight != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("🏷️", fontSize = 24.sp)
                        Column {
                            Text("Fixed Costs Detected", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(fixedCostsInsight.replace("🏷️", "").trim(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // ── Category Breakdown ────────────────────────────────────────────
        if (categoryBreakdown.isNotEmpty()) {
            item {
                Text(
                    "Category Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val sortedEntries = categoryBreakdown.entries
                            .sortedByDescending { it.value }
                        val maxVal = sortedEntries.firstOrNull()?.value?.takeIf { it > 0.0 } ?: 1.0

                        sortedEntries.forEachIndexed { idx, entry ->
                            val color = CATEGORY_COLORS[idx % CATEGORY_COLORS.size]
                            val barFraction = (entry.value / maxVal).toFloat()
                            val animatedBar by animateFloatAsState(
                                targetValue = barFraction,
                                animationSpec = tween(600, delayMillis = idx * 80),
                                label = "bar$idx"
                            )

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(entry.key,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Text("₹${String.format("%,.2f", entry.value)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = color)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color.copy(alpha = 0.15f))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(animatedBar)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(color)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No spending data for this month yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

