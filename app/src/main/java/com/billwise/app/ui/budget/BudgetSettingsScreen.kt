package com.billwise.app.ui.budget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.billwise.app.ui.viewmodel.BudgetViewModel
import com.billwise.app.ui.viewmodel.InsightViewModel

@Composable
fun BudgetSettingsScreen(
    budgetViewModel: BudgetViewModel,
    insightViewModel: InsightViewModel
) {
    val budget        by budgetViewModel.budget.collectAsState()
    val totalSpent    by insightViewModel.totalSpent.collectAsState()
    val budgetProgress by insightViewModel.budgetProgress.collectAsState()

    var limitInput by remember(budget) {
        mutableStateOf(budget?.monthlyLimit?.let { "%.0f".format(it) } ?: "")
    }
    var saved by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            "Budget",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // ── Current Budget Card ────────────────────────────────────────
        if (budget != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF065F46), Color(0xFF064E3B))
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Monthly Budget",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f))
                                Text("₹${String.format("%,.0f", budget!!.monthlyLimit)}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White)
                            }
                            Icon(Icons.Default.Savings,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp))
                        }

                        val remaining = budget!!.monthlyLimit - totalSpent
                        Text(
                            if (remaining >= 0)
                                "₹${String.format("%,.2f", remaining)} remaining"
                            else
                                "₹${String.format("%,.2f", -remaining)} over budget!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (remaining >= 0) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                        )

                        // Progress bar
                        val prog = budgetProgress.coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(prog)
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        when {
                                            prog >= 1f -> Color(0xFFF87171)
                                            prog >= 0.8f -> Color(0xFFFBBF24)
                                            else -> Color(0xFF34D399)
                                        }
                                    )
                            )
                        }
                        Text("${(prog * 100).toInt()}% used",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // ── Set / Update Budget ────────────────────────────────────────
        Text(
            if (budget == null) "Set Your Monthly Budget" else "Update Budget",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        OutlinedTextField(
            value = limitInput,
            onValueChange = { limitInput = it; saved = false },
            label = { Text("Monthly Limit (₹)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            prefix = { Text("₹ ") }
        )

        Button(
            onClick = {
                budgetViewModel.saveBudget(limitInput)
                saved = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = limitInput.isNotBlank()
        ) {
            Text("Save Budget", fontWeight = FontWeight.SemiBold)
        }

        if (saved) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                )
            ) {
                Text(
                    "✅ Budget saved successfully!",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
