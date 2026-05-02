package com.billwise.app.data.parser

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.*

object PhonePePdfParser {

    // Formats for combined "Apr 03, 2026 02:01 pm"
    private val COMBINED_DATE_FORMATS = listOf(
        SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.ENGLISH),
        SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.ENGLISH),
        SimpleDateFormat("MMM d, yyyy hh:mm a", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    )

    // Format for date-only line: "Apr 03, 2026"
    private val DATE_ONLY_FORMATS = listOf(
        SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH),
        SimpleDateFormat("MMM d, yyyy", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    )

    // Format for time-only line: "02:01 pm"
    private val TIME_ONLY_FORMATS = listOf(
        SimpleDateFormat("hh:mm a", Locale.ENGLISH),
        SimpleDateFormat("HH:mm", Locale.ENGLISH)
    )

    data class ParseResult(
        val transactions: List<Transaction>,
        val unparsedBlocks: List<String>
    )

    fun parse(rawText: String): ParseResult {
        val rawLines = rawText.lines().map { it.trim() }.filter { shouldKeepLine(it) }
        val transactions = mutableListOf<Transaction>()
        val unparsedBlocks = mutableListOf<String>()

        // First pass: stitch date + time lines into a single "DATE|TIMESTAMP" marker,
        // while leaving all other lines intact.
        val processedLines = mutableListOf<String>()
        var i = 0
        while (i < rawLines.size) {
            val line = rawLines[i]

            // Check if this line is a combined datetime (single line with date + time)
            val combinedDate = tryParseCombinedDate(line)
            if (combinedDate != null) {
                processedLines.add("__DATE__${combinedDate.time}")
                i++
                continue
            }

            // Check if this line is a date-only line
            val dateOnly = tryParseDateOnly(line)
            if (dateOnly != null) {
                // Peek at the next line to see if it's a time
                val nextLine = rawLines.getOrNull(i + 1)
                if (nextLine != null) {
                    val timeOnly = tryParseTimeOnly(nextLine)
                    if (timeOnly != null) {
                        // Combine date + time
                        val cal = Calendar.getInstance()
                        cal.time = dateOnly
                        val timeCal = Calendar.getInstance()
                        timeCal.time = timeOnly
                        cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                        cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                        cal.set(Calendar.SECOND, 0)
                        cal.set(Calendar.MILLISECOND, 0)
                        processedLines.add("__DATE__${cal.timeInMillis}")
                        i += 2 // skip both the date and the time line
                        continue
                    }
                }
                // Date-only, no time on next line — use midnight
                processedLines.add("__DATE__${dateOnly.time}")
                i++
                continue
            }

            processedLines.add(line)
            i++
        }

        // Second pass: split by __DATE__ markers and parse blocks
        var currentTimestamp: Long = 0
        val currentBuffer = mutableListOf<String>()

        for (line in processedLines) {
            if (line.startsWith("__DATE__")) {
                if (currentBuffer.isNotEmpty()) {
                    val result = parseBlocksFromBuffer(currentBuffer, currentTimestamp)
                    transactions.addAll(result.transactions)
                    unparsedBlocks.addAll(result.unparsedBlocks)
                    currentBuffer.clear()
                }
                currentTimestamp = line.removePrefix("__DATE__").toLongOrNull() ?: 0
            } else {
                currentBuffer.add(line)
            }
        }

        if (currentBuffer.isNotEmpty()) {
            val result = parseBlocksFromBuffer(currentBuffer, currentTimestamp)
            transactions.addAll(result.transactions)
            unparsedBlocks.addAll(result.unparsedBlocks)
        }

        return ParseResult(transactions, unparsedBlocks)
    }

    private fun shouldKeepLine(line: String): Boolean {
        if (line.isBlank()) return false
        val ignoreList = listOf(
            "Page ", "This is a system generated", "support.phonepe.com",
            "Disclaimer", "terms-conditions", "Customer(s) are requested",
            "PhonePe Terms", "automatically generated"
        )
        return ignoreList.none { line.contains(it, ignoreCase = true) }
    }

    private fun tryParseCombinedDate(line: String): Date? {
        for (format in COMBINED_DATE_FORMATS) {
            try {
                val date = format.parse(line)
                if (date != null) return date
            } catch (e: Exception) { }
        }
        return null
    }

    private fun tryParseDateOnly(line: String): Date? {
        for (format in DATE_ONLY_FORMATS) {
            try {
                val date = format.parse(line)
                // Make sure the whole line is a date (not partial match)
                if (date != null && line.matches(Regex("""[A-Za-z]{3}\s+\d{1,2},\s+\d{4}"""))) {
                    return date
                }
            } catch (e: Exception) { }
        }
        return null
    }

    private fun tryParseTimeOnly(line: String): Date? {
        // Time-only lines look like "02:01 pm", "10:32 am", "22:00"
        if (!line.matches(Regex("""^\d{1,2}:\d{2}(\s*[aApP][mM])?$"""))) return null
        for (format in TIME_ONLY_FORMATS) {
            try {
                val date = format.parse(line)
                if (date != null) return date
            } catch (e: Exception) { }
        }
        return null
    }

    private fun isTransactionStart(line: String): Boolean {
        val starts = listOf(
            "Paid to", "Received from", "Payment to",
            "Mobile recharged", "Transfer to", "Money sent", "Money received"
        )
        return starts.any { line.contains(it, ignoreCase = true) }
    }

    private fun parseBlocksFromBuffer(buffer: List<String>, timestamp: Long): ParseResult {
        val transactions = mutableListOf<Transaction>()
        val unparsedBlocks = mutableListOf<String>()
        val currentBlock = mutableListOf<String>()

        for (line in buffer) {
            if (isTransactionStart(line) && currentBlock.isNotEmpty()) {
                val tx = parseBlock(currentBlock, timestamp)
                if (tx != null) {
                    transactions.add(tx)
                } else {
                    unparsedBlocks.add(currentBlock.joinToString("\n"))
                }
                currentBlock.clear()
            }
            currentBlock.add(line)
        }
        if (currentBlock.isNotEmpty()) {
            val tx = parseBlock(currentBlock, timestamp)
            if (tx != null) {
                transactions.add(tx)
            } else {
                unparsedBlocks.add(currentBlock.joinToString("\n"))
            }
        }
        return ParseResult(transactions, unparsedBlocks)
    }

    private fun parseBlock(block: List<String>, timestamp: Long): Transaction? {
        if (block.isEmpty()) return null

        val fullText = block.joinToString(" ")

        val type = when {
            fullText.contains("Paid to", ignoreCase = true) ||
            fullText.contains("Payment to", ignoreCase = true) ||
            fullText.contains("Mobile recharged", ignoreCase = true) ||
            fullText.contains("Transfer to", ignoreCase = true) ||
            fullText.contains("Money sent", ignoreCase = true) -> TransactionType.DEBIT
            fullText.contains("Received from", ignoreCase = true) ||
            fullText.contains("Money received", ignoreCase = true) -> TransactionType.CREDIT
            else -> return null
        }

        val amount        = extractAmount(fullText) ?: return null
        val merchant      = extractMerchant(fullText) ?: "Unknown"
        val transactionId = extractTransactionId(fullText)
        val utr           = extractUTR(fullText)
        val accountHint   = extractAccount(fullText)

        // Confidence: 1.0 if we have a bank-assigned ID, 0.75 if hash-only
        val confidence = if (!transactionId.isNullOrBlank() || !utr.isNullOrBlank()) 1.0f else 0.75f

        val finalId = if (!transactionId.isNullOrBlank()) {
            "pdf_$transactionId"
        } else {
            generateDeterministicId(amount, type, merchant, timestamp)
        }

        return Transaction(
            id              = finalId,
            amount          = amount,
            merchant        = merchant.uppercase(),
            datetime        = timestamp,
            type            = type,
            category        = "Uncategorized",
            source          = TransactionSource.PDF,
            accountHint     = accountHint,
            transactionId   = transactionId,
            utr             = utr,
            confidenceScore = confidence
        )
    }

    private fun extractAmount(text: String): Double? {
        val regex = Regex("""(?:₹|Rs\.?)\s?([\d,]+(?:\.\d{1,2})?)""", RegexOption.IGNORE_CASE)
        val match = regex.find(text)
        return match?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }

    private fun extractMerchant(text: String): String? {
        // Pattern covers "Paid to <NAME> DEBIT ₹X" or "Received from ******0231 CREDIT ₹600"
        val regex = Regex(
            """(?:Paid to|Received from|Payment to|Mobile recharged|Transfer to|Money sent|Money received)\s+(.*?)\s+(?:DEBIT|CREDIT|₹|Rs\.?|Transaction ID)""",
            RegexOption.IGNORE_CASE
        )
        return regex.find(text)?.groupValues?.get(1)?.trim()
    }

    private fun extractTransactionId(text: String): String? {
        val regex = Regex("""Transaction ID\s+([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)
    }

    private fun extractUTR(text: String): String? {
        val regex = Regex("""UTR No\.?\s*(\d+)""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)
    }

    private fun extractAccount(text: String): String? {
        val regex = Regex("""(?:Paid by|Credited to)\s+X*(\d{4})""", RegexOption.IGNORE_CASE)
        return regex.find(text)?.groupValues?.get(1)
    }

    private fun generateDeterministicId(amount: Double, type: TransactionType, merchant: String, timestamp: Long): String {
        return com.billwise.app.core.TransactionUtils.generateDeterministicId("pdf", amount, type, merchant, timestamp)
    }
}
