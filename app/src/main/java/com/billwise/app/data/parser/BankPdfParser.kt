package com.billwise.app.data.parser

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

object BankPdfParser {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMM dd yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd/MM/yy", Locale.ENGLISH)
    )

    fun parseTabular(rawText: String): List<Transaction> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val transactions = mutableListOf<Transaction>()
        
        var lastSeenDate: Long? = null
        
        for (line in lines) {
            // 1. Strip headers, footers, page numbers
            if (isHeaderOrFooter(line)) continue
            
            // 2. Try to extract date
            val dateResult = extractDate(line)
            val currentDate = dateResult ?: lastSeenDate
            if (currentDate != null) lastSeenDate = currentDate
            
            // 3. Extract amount and type
            val (amount, type) = extractAmountAndType(line)
            
            // 4. Extract description/merchant
            val merchant = extractMerchant(line)
            
            // 5. Extract balance
            val balance = extractBalance(line)
            
            // 6. Extract transaction ID / UTR
            val txnId = extractRefNo(line)

            // 7. Validate: must have Date, Amount, and Merchant
            if (currentDate != null && amount != null && merchant != null) {
                val hashId = com.billwise.app.core.TransactionUtils.generateDeterministicId(
                    "pdf", amount, type ?: TransactionType.DEBIT, merchant, currentDate
                )
                
                transactions.add(
                    Transaction(
                        id = "pdf_$hashId",
                        amount = amount,
                        type = type ?: TransactionType.DEBIT,
                        merchant = merchant.uppercase(),
                        datetime = currentDate,
                        source = TransactionSource.PDF,
                        transactionId = txnId,
                        balance = balance,
                        category = "Uncategorized",
                        confidenceScore = 0.9f
                    )
                )
            }
        }
        
        return transactions
    }

    private fun isHeaderOrFooter(line: String): Boolean {
        val keywords = listOf(
            "statement", "summary", "page", "opening balance", "closing balance",
            "transaction date", "value date", "description", "debit", "credit",
            "balance", "account number", "customer care", "generated on"
        )
        // If line is mostly keywords, it's likely a header
        val lower = line.lowercase()
        return keywords.count { lower.contains(it) } >= 2 || lower.startsWith("page ")
    }

    private fun extractDate(line: String): Long? {
        // Look for date patterns at the start or middle of the line
        val dateRegex = Regex("""\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4}|\d{1,2}\s+[A-Za-z]{3}\s+\d{4})\b""")
        val match = dateRegex.find(line) ?: return null
        val dateStr = match.groupValues[1]
        
        for (format in DATE_FORMATS) {
            try {
                return format.parse(dateStr.replace("-", "/").replace(" ", "/"))?.time
            } catch (e: Exception) {}
        }
        return null
    }

    private fun extractAmountAndType(line: String): Pair<Double?, TransactionType?> {
        // Typically tabular formats have Debit and Credit columns.
        // We look for numbers that aren't the date or balance.
        // Simplified: look for amounts and try to infer from context keywords
        val amountRegex = Regex("""\b(\d{1,5}(?:,\d{3})*(?:\.\d{2}))\b""")
        val matches = amountRegex.findAll(line).toList()
        
        if (matches.isEmpty()) return null to null
        
        // Often: Date | Description | Debit | Credit | Balance
        // If there are 3 amounts, last one is balance, middle two are debit/credit
        val amounts = matches.map { it.groupValues[1].replace(",", "").toDouble() }
        
        val lower = line.lowercase()
        val isCredit = lower.contains("credit") || lower.contains("cr") || lower.contains("received")
        val isDebit = lower.contains("debit") || lower.contains("dr") || lower.contains("spent")
        
        return when {
            amounts.size >= 2 -> {
                // If there are multiple amounts, we need to pick the right one.
                // Usually the one before the balance.
                val amt = amounts[amounts.size - 2]
                amt to (if (isCredit) TransactionType.CREDIT else TransactionType.DEBIT)
            }
            amounts.size == 1 -> {
                amounts[0] to (if (isCredit) TransactionType.CREDIT else TransactionType.DEBIT)
            }
            else -> null to null
        }
    }

    private fun extractMerchant(line: String): String? {
        // The merchant is usually the longest text block that isn't headers or dates
        val parts = line.split(Regex("""\s{2,}""")).filter { it.length > 3 }
        for (part in parts) {
            if (!part.any { it.isDigit() } && !isHeaderOrFooter(part)) {
                return part.trim()
            }
        }
        // Fallback to the largest non-numeric part
        return parts.maxByOrNull { it.length }?.trim()
    }

    private fun extractBalance(line: String): Double? {
        val amountRegex = Regex("""\b(\d{1,5}(?:,\d{3})*(?:\.\d{2}))\b""")
        val matches = amountRegex.findAll(line).toList()
        if (matches.size >= 2) {
            return matches.last().groupValues[1].replace(",", "").toDoubleOrNull()
        }
        return null
    }

    private fun extractRefNo(line: String): String? {
        val refRegex = Regex("""\b([A-Z0-9]{10,20})\b""")
        return refRegex.find(line)?.groupValues?.get(1)
    }
}
