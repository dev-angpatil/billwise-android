package com.billwise.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import kotlinx.coroutines.launch

/**
 * Handles PDF bank statement upload and parsing.
 * Simplified: no BillRepository dependency — transactions go directly
 * through the onTransactionParsed callback into TransactionViewModel.
 */
class BillViewModel(
    private val onTransactionParsed: (Transaction) -> Unit
) : ViewModel() {

    fun uploadBankStatement(pdfText: String) {
        viewModelScope.launch {
            // Try specific parsers first
            val phonePeResult = com.billwise.app.data.parser.PhonePePdfParser.parse(pdfText)
            if (phonePeResult.transactions.isNotEmpty()) {
                phonePeResult.transactions.forEach { onTransactionParsed(it) }
            }

            // Also try generic tabular parser
            val genericResult = com.billwise.app.data.parser.BankPdfParser.parseTabular(pdfText)
            if (genericResult.isNotEmpty()) {
                genericResult.forEach { onTransactionParsed(it) }
            }

            // Only use AI fallback if both failed or produced very few transactions
            if (phonePeResult.transactions.isEmpty() && genericResult.isEmpty()) {
                val apiKey = com.billwise.app.core.Config.OPENAI_API_KEY
                pdfText.lines().chunked(40).forEach { chunk ->
                    processWithAi(chunk.joinToString("\n"), apiKey)
                }
            }
        }
    }

    /**
     * Process arbitrary text (from paste or simple bills) using AI.
     */
    fun uploadBill(text: String) {
        viewModelScope.launch {
            val apiKey = com.billwise.app.core.Config.OPENAI_API_KEY
            processWithAi(text, apiKey)
        }
    }

    private suspend fun processWithAi(text: String, apiKey: String) {
        if (text.isBlank()) return
        val gson = com.google.gson.Gson()
        val typeToken = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
        try {
            val response = com.billwise.app.data.remote.RetrofitClient.openAiApi.categorizeTransaction(
                "Bearer $apiKey",
                com.billwise.app.data.remote.OpenAiRequest(
                    messages = listOf(
                        com.billwise.app.data.remote.Message(
                            role = "system",
                            content = "Bank statement parser for India. Extract all transactions. Reply ONLY as JSON array: [{\"amount\":150.0,\"type\":\"DEBIT\",\"merchant\":\"ZOMATO\",\"date\":\"2024-04-29\"}]. Use YYYY-MM-DD for date. Return [] if none."
                        ),
                        com.billwise.app.data.remote.Message(role = "user", content = "Statement:\n$text")
                    )
                )
            )
            val raw = response.choices.firstOrNull()?.message?.content?.trim() ?: "[]"
            val json = if (raw.contains("[")) raw.substring(raw.indexOf("["), raw.lastIndexOf("]") + 1) else "[]"
            val list: List<Map<String, Any>> = gson.fromJson(json, typeToken) ?: return

            list.forEach { m ->
                val amount   = (m["amount"] as? Number)?.toDouble() ?: return@forEach
                val typeStr  = m["type"] as? String ?: "DEBIT"
                val merchant = (m["merchant"] as? String)?.uppercase() ?: "UNKNOWN"
                val dateStr  = m["date"] as? String ?: ""
                val ts = runCatching {
                    java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).parse(dateStr)?.time
                }.getOrNull() ?: System.currentTimeMillis()
                val type = if (typeStr.uppercase() == "CREDIT") TransactionType.CREDIT else TransactionType.DEBIT
                val id = com.billwise.app.core.TransactionUtils.generateDeterministicId("pdf", amount, type, merchant, ts)
                onTransactionParsed(Transaction(
                    id = id, amount = amount, merchant = merchant, datetime = ts,
                    type = type, category = "Uncategorized", source = TransactionSource.PDF,
                    confidenceScore = 0.7f
                ))
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun addManualTransaction(amount: Double, merchant: String, category: String, type: TransactionType, date: Long = System.currentTimeMillis()) {
        onTransactionParsed(Transaction(
            id = java.util.UUID.randomUUID().toString(),
            amount = amount, merchant = merchant,
            datetime = date,
            type = type, category = category,
            source = TransactionSource.MANUAL
        ))
    }
}
