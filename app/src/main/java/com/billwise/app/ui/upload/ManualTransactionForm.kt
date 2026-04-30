package com.billwise.app.ui.upload

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import com.billwise.app.ui.viewmodel.BillViewModel
import java.util.UUID

private val CATEGORIES = listOf("Food", "Transport", "Shopping", "Subscriptions", "Others", "Uncategorized")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualTransactionForm(
    viewModel: BillViewModel,
    onSuccess: () -> Unit
) {
    var amount      by remember { mutableStateOf("") }
    var merchant    by remember { mutableStateOf("") }
    var category    by remember { mutableStateOf("Others") }
    var isDebit     by remember { mutableStateOf(true) }
    var errorMsg    by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Amount
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it; errorMsg = null },
            label = { Text("Amount (₹)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true,
            isError = errorMsg != null,
            leadingIcon = { Icon(Icons.Default.AttachMoney, contentDescription = null) }
        )

        // Merchant
        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it; errorMsg = null },
            label = { Text("Merchant / Description") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // DEBIT / CREDIT toggle
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = isDebit,
                onClick = { isDebit = true },
                label = { Text("Debit (−)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.error,
                    selectedLabelColor = MaterialTheme.colorScheme.onError
                )
            )
            FilterChip(
                selected = !isDebit,
                onClick = { isDebit = false },
                label = { Text("Credit (+)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondary,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        }

        // Category dropdown
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it }
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false }
            ) {
                CATEGORIES.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat) },
                        onClick = { category = cat; categoryExpanded = false }
                    )
                }
            }
        }

        if (errorMsg != null) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = {
                val amountVal = amount.toDoubleOrNull()
                if (amountVal == null || amountVal <= 0) {
                    errorMsg = "Please enter a valid amount."
                    return@Button
                }
                if (merchant.isBlank()) {
                    errorMsg = "Please enter a merchant name."
                    return@Button
                }
                viewModel.addManualTransaction(
                    amount = amountVal,
                    merchant = merchant.trim(),
                    category = category,
                    type = if (isDebit) TransactionType.DEBIT else TransactionType.CREDIT
                )
                onSuccess()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Add Transaction", fontWeight = FontWeight.SemiBold)
        }
    }
}
