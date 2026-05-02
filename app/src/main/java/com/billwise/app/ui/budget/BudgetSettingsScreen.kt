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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetSettingsScreen(
    budgetViewModel: BudgetViewModel,
    insightViewModel: InsightViewModel
) {
    val budgets       by budgetViewModel.budgets.collectAsState()
    val categorySpent by insightViewModel.categoryBreakdown.collectAsState()

    val categories = listOf("Total", "Food & Dining", "Transport & Travel", "Groceries", "Shopping", "Subscriptions & Entertainment", "Health & Medical", "Utilities & Bills", "Investment", "Education")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    
    val currentBudget = budgets.find { it.category == selectedCategory }
    val currentSpent = if (selectedCategory == "Total") {
        insightViewModel.totalSpent.collectAsState().value
    } else {
        categorySpent[selectedCategory] ?: 0.0
    }

    var limitInput by remember(selectedCategory, currentBudget) {
        mutableStateOf(currentBudget?.monthlyLimit?.let { "%.0f".format(it) } ?: "")
    }
    var saved by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            "Budget Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // ── Category Selector ────────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                expanded = false
                                saved = false
                            }
                        )
                    }
                }
            }
        }

        // ── Current Category Status ────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            val bgBrush = if (currentBudget != null && currentSpent > currentBudget.monthlyLimit) {
                Brush.linearGradient(colors = listOf(Color(0xFF7F1D1D), Color(0xFF450A0A)))
            } else {
                Brush.linearGradient(colors = listOf(Color(0xFF065F46), Color(0xFF064E3B)))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgBrush)
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("$selectedCategory Budget",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.7f))
                            Text("₹${if (currentBudget != null) String.format("%,.0f", currentBudget.monthlyLimit) else "N/A"}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White)
                        }
                        Icon(Icons.Default.Savings,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(40.dp))
                    }

                    Text(
                        "Spent so far: ₹${String.format("%,.2f", currentSpent)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    if (currentBudget != null) {
                        val prog = (currentSpent / currentBudget.monthlyLimit).toFloat().coerceIn(0f, 1.2f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp))
                                .background(Color.White.copy(alpha = 0.15f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth((prog / 1.2f).coerceIn(0f, 1f))
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                                    .background(
                                        when {
                                            prog >= 1f -> Color(0xFFF87171)
                                            prog >= 0.75f -> Color(0xFFFBBF24)
                                            else -> Color(0xFF34D399)
                                        }
                                    )
                            )
                        }
                        Text("${(prog * 100).toInt()}% of limit used",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f))
                    }
                }
            }
        }

        // ── Set / Update Budget ────────────────────────────────────────
        Text(
            if (currentBudget == null) "Set $selectedCategory Limit" else "Update $selectedCategory Limit",
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
            prefix = { Text("₹ ") },
            supportingText = {
                if (currentBudget == null && selectedCategory != "Total") {
                    Text("Suggestion: ₹2,000 (Based on spending habits)")
                }
            }
        )

        Button(
            onClick = {
                budgetViewModel.saveBudget(selectedCategory, limitInput)
                saved = true
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = limitInput.isNotBlank()
        ) {
            Text("Save $selectedCategory Budget", fontWeight = FontWeight.SemiBold)
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
                    "✅ $selectedCategory budget saved successfully!",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
