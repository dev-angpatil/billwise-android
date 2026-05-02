package com.billwise.app.ui.insights

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billwise.app.ui.viewmodel.InsightViewModel
import java.util.Calendar
import kotlin.math.abs

private val CHART_COLORS = listOf(
    Color(0xFF6366F1), Color(0xFF10B981), Color(0xFFF59E0B),
    Color(0xFFEF4444), Color(0xFF3B82F6), Color(0xFF8B5CF6),
    Color(0xFFF97316), Color(0xFF14B8A6), Color(0xFFD946EF),
    Color(0xFF64748B)
)

private enum class ChartType { BAR, LINE, DONUT }

@Composable
fun InsightsScreen(viewModel: InsightViewModel) {
    val insights          by viewModel.insights.collectAsState()
    val dailySpend        by viewModel.dailySpend.collectAsState()
    val categoryBreakdown by viewModel.categoryBreakdown.collectAsState()
    val selectedMonth     by viewModel.selectedMonth.collectAsState()
    val selectedYear      by viewModel.selectedYear.collectAsState()
    val avgDailySpend     by viewModel.averageDailySpend.collectAsState()
    val dailyBudget       by viewModel.dailyBudget.collectAsState()
    
    // New metrics
    val topMerchants      by viewModel.topMerchants.collectAsState()
    val weeklyComparison  by viewModel.weeklyComparison.collectAsState()
    val incomeVsExpense   by viewModel.incomeVsExpenseTrend.collectAsState()
    val aiAdvice          by viewModel.aiAdvice.collectAsState()

    var selectedChartType by remember { mutableStateOf(ChartType.BAR) }
    var selectedCategoryIndex by remember { mutableStateOf(-1) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // ── Header + Month Nav ─────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Finance Insights", style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Default.ChevronLeft, "Previous Month")
                    }
                    val monthNames = arrayOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                    Text("${monthNames.getOrElse(selectedMonth - 1) { "" }} $selectedYear",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Default.ChevronRight, "Next Month")
                    }
                }
            }
        }

        // ── Weekly Comparison Card ──────────────────────────────────────
        item {
            WeeklyComparisonCard(weeklyComparison)
        }

        // ── Burn Rate Section ──────────────────────────────────────────
        item {
            BurnRateCard(avgDailySpend, dailyBudget)
        }

        // ── Chart Type Selector ───────────────────────────────────────
        item {
            ChartSelector(selectedChartType) { selectedChartType = it }
        }

        // ── Main Data Visualization ───────────────────────────────────
        item {
            ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(16.dp)) {
                    when (selectedChartType) {
                        ChartType.DONUT -> {
                            val sorted = categoryBreakdown.entries.sortedByDescending { it.value }
                            val total = sorted.sumOf { it.value }
                            if (total > 0) {
                                DonutChart(
                                    entries = sorted,
                                    total = total,
                                    selectedIndex = selectedCategoryIndex,
                                    onSliceClick = { idx -> selectedCategoryIndex = if (selectedCategoryIndex == idx) -1 else idx }
                                )
                            } else EmptyChartState("No category data")
                        }
                        ChartType.BAR -> {
                            val entries = dailySpend.entries.sortedBy { it.key }.takeLast(14)
                            if (entries.isNotEmpty()) {
                                Text("Daily Spending (Last 14 Days)", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(12.dp))
                                DailyBarChart(entries, (entries.maxOfOrNull { it.value } ?: 1.0))
                            } else EmptyChartState("No daily data")
                        }
                        ChartType.LINE -> {
                            if (incomeVsExpense.isNotEmpty()) {
                                Text("Income vs Expense Trend", style = MaterialTheme.typography.labelMedium)
                                Spacer(Modifier.height(12.dp))
                                IncomeExpenseLineChart(incomeVsExpense)
                            } else EmptyChartState("No trend data")
                        }
                    }
                }
            }
        }

        // ── Top Merchants Section ──────────────────────────────────────
        if (topMerchants.isNotEmpty()) {
            item {
                Text("Top Spending Merchants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            item {
                TopMerchantsList(topMerchants)
            }
        }

        // ── AI Financial Coach ──────────────────────────────────────────
        item {
            Text("AI Financial Coach", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("🤖", fontSize = 28.sp)
                        Text("Personalized Analysis", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    
                    if (aiAdvice != null) {
                        Text(
                            aiAdvice!!,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Text(
                            "Get deep insights into your spending habits and actionable advice from our AI coach.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.generateAiAdvice() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = CircleShape
                    ) {
                        Text(if (aiAdvice == null) "Ask AI Coach" else "Refresh Advice")
                    }
                }
            }
        }

        // ── AI Insights (Rules Based) ───────────────────────────────────
        if (insights.isNotEmpty()) {
            item {
                Text("Smart Observations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(insights) { insight ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(insight.take(2), fontSize = 20.sp)
                        Text(insight.drop(2).trim(), style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyComparisonCard(comparison: Pair<Double, Double>) {
    val (thisWeek, lastWeek) = comparison
    val diff = thisWeek - lastWeek
    val pct = if (lastWeek > 0) (abs(diff) / lastWeek * 100).toInt() else 0
    val isIncrease = diff > 0
    val trendColor = if (isIncrease) Color(0xFFEF4444) else Color(0xFF10B981)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("This Week", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Text("₹${String.format("%,.0f", thisWeek)}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isIncrease) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = trendColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("$pct%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = trendColor)
                }
                Text(if (isIncrease) "More than last week" else "Less than last week", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun BurnRateCard(avg: Double, limit: Double) {
    ElevatedCard(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Daily Burn Rate", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Limit: ₹${String.format("%.0f", limit)}", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(12.dp))
            val progress = if (limit > 0) (avg / limit).toFloat().coerceIn(0f, 1f) else 0f
            val color = if (avg > limit && limit > 0) Color(0xFFEF4444) else Color(0xFF6366F1)
            
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (avg > limit && limit > 0) "⚠️ Spending ₹${String.format("%.0f", avg - limit)} above daily limit"
                else "✅ ₹${String.format("%.0f", limit - avg)} under daily limit",
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun ChartSelector(selected: ChartType, onSelect: (ChartType) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).padding(4.dp)
    ) {
        listOf(ChartType.BAR to "Daily", ChartType.LINE to "Trend", ChartType.DONUT to "Split").forEach { (type, label) ->
            val isSel = selected == type
            Box(
                Modifier.weight(1f).clip(CircleShape).background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(type) }.padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(label, color = if (isSel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TopMerchantsList(merchants: List<Pair<String, Double>>) {
    val maxAmt = merchants.maxOfOrNull { it.second } ?: 1.0
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        merchants.forEachIndexed { idx, (name, amt) ->
            val color = CHART_COLORS[idx % CHART_COLORS.size]
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text("₹${String.format("%,.0f", amt)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(color.copy(alpha = 0.1f))) {
                    val progress by animateFloatAsState((amt / maxAmt).toFloat(), tween(800, delayMillis = idx * 100))
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().clip(CircleShape).background(color))
                }
            }
        }
    }
}

@Composable
private fun IncomeExpenseLineChart(data: List<Triple<String, Double, Double>>) {
    val maxAmt = data.maxOf { maxOf(it.second, it.third) }.takeIf { it > 0 } ?: 1.0
    Canvas(Modifier.fillMaxWidth().height(160.dp)) {
        val stepX = size.width / (data.size - 1)
        
        // Draw Income Line (Green)
        val incomePts = data.mapIndexed { idx, (_, inc, _) -> Offset(idx * stepX, size.height - (inc / maxAmt * size.height).toFloat()) }
        drawPath(createSmoothPath(incomePts), Color(0xFF10B981), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        
        // Draw Expense Line (Indigo)
        val expensePts = data.mapIndexed { idx, (_, _, exp) -> Offset(idx * stepX, size.height - (exp / maxAmt * size.height).toFloat()) }
        drawPath(createSmoothPath(expensePts), Color(0xFF6366F1), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        data.forEach { (name, _, _) -> Text(name, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
    }
}

private fun createSmoothPath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        val controlPoint1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
        val controlPoint2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
        path.cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p2.x, p2.y)
    }
    return path
}

@Composable
private fun EmptyChartState(msg: String) {
    Box(Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
        Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
    }
}

// ── Reuse existing Donut and Bar logic but refined ────────────────────────────
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun DonutChart(entries: List<Map.Entry<String, Double>>, total: Double, selectedIndex: Int, onSliceClick: (Int) -> Unit) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(entries) { animProgress.animateTo(1f, tween(1000, easing = EaseInOutCubic)) }

    Column {
        Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(170.dp)) {
                val strokeW = 40.dp.toPx()
                var startAngle = -90f
                entries.forEachIndexed { idx, (_, amt) ->
                    val sweep = ((amt / total) * 360f * animProgress.value).toFloat()
                    val color = CHART_COLORS[idx % CHART_COLORS.size]
                    val isSelected = idx == selectedIndex
                    
                    drawArc(
                        color = color,
                        startAngle = startAngle + 1f,
                        sweepAngle = (sweep - 2f).coerceAtLeast(0.1f),
                        useCenter = false,
                        style = Stroke(width = if (isSelected) strokeW * 1.2f else strokeW, cap = StrokeCap.Round),
                        size = if (isSelected) size * 1.05f else size,
                        topLeft = if (isSelected) Offset(-size.width * 0.025f, -size.height * 0.025f) else Offset.Zero
                    )
                    startAngle += sweep
                }
            }
            if (selectedIndex >= 0) {
                val sel = entries[selectedIndex]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(sel.value / total * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                    Text(sel.key, style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("₹${String.format("%.0f", total)}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Total Spent", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        
        FlowRow(
            Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            entries.forEachIndexed { idx, (cat, _) ->
                val color = CHART_COLORS[idx % CHART_COLORS.size]
                val isSelected = idx == selectedIndex
                Surface(
                    onClick = { onSliceClick(idx) },
                    shape = CircleShape,
                    color = if (isSelected) color else color.copy(alpha = 0.1f),
                    border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (isSelected) Color.White else color))
                        Spacer(Modifier.width(6.dp))
                        Text(cat, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else color)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBarChart(entries: List<Map.Entry<Int, Double>>, maxAmt: Double) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(entries) { animProgress.animateTo(1f, tween(800)) }

    Canvas(Modifier.fillMaxWidth().height(150.dp)) {
        val barW = size.width / (entries.size * 2f)
        val gap = barW
        entries.forEachIndexed { idx, (_, amt) ->
            val barH = (amt / maxAmt * size.height * animProgress.value).toFloat()
            val x = idx * (barW + gap) + gap / 2
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(Color(0xFF6366F1), Color(0xFF818CF8).copy(alpha = 0.6f))),
                topLeft = Offset(x, size.height - barH),
                size = Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f)
            )
        }
    }
}
