package com.billwise.app.data.parser

import com.billwise.app.domain.model.Transaction
import com.billwise.app.domain.model.TransactionSource
import com.billwise.app.domain.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Five-layer SMS/Notification parser.
 *
 * Layer 1 — Detection:   is this a financial message?
 * Layer 2 — Extraction:  amount, type, merchant (rule-based)
 * Layer 3 — Normalization: clean merchant names
 * Layer 4 — Confidence:  score based on how much was rule-extracted vs AI
 * Layer 5 — AI Fallback: only called if confidence < threshold
 */
object SmsParser {

    private const val AI_THRESHOLD = 0.6f

    private val CURRENCY_KEYWORDS  = listOf("₹", "rs", "inr")
    private val DEBIT_KEYWORDS     = listOf(
        "debited", "dr.", "dr ", "spent", "paid", "sent",
        "withdrawn", "deducted", "payment to", "purchase of", "recharge of"
    )
    private val CREDIT_KEYWORDS    = listOf(
        "credited", "cr.", "cr ", "received", "deposit", "added",
        "refund", "payment from", "payment received", "cashback received"
    )
    private val SPAM_KEYWORDS      = listOf(
        "otp", "verification code", "offer", "sale", "discount",
        "loan", "win", "won", "reward", "lucky", "congratulations",
        "click here", "apply now"
    )
    private val BANK_ACTION_WORDS  = listOf(
        "paid", "sent", "received", "payment", "debited", "credited",
        "dr", "cr", "a/c", "acct", "balance", "deducted", "withdrawn", "refund", "spent"
    )
    private val SHOPPING_KEYWORDS  = listOf("order", "shipped", "delivered", "out for delivery")
    private val WALLET_KEYWORDS    = listOf("paytm", "phonepe", "amazon pay", "gpay", "wallet reload")

    // ── Layer 1: Detection ────────────────────────────────────────────────

    private fun isTransactionMessage(normalized: String, sender: String): Boolean {
        // 1. Mandatory numeric content
        if (!normalized.any { it.isDigit() }) return false
        
        // 2. Strict OTP / Verification / Login Block
        val spamPatterns = listOf(
            "otp", "verification code", "secret code", "login code", 
            "security code", "is your code", "one time password",
            "do not share", "authorized login", "verification link"
        )
        if (spamPatterns.any { normalized.contains(it) }) return false

        // 3. Mandatory Currency Context
        // A valid transaction message ALMOST ALWAYS has a currency symbol (₹, Rs, INR) 
        // near a number. If it doesn't, it's likely an order number or reference code.
        val hasCurrencyWithAmount = Regex("""(?:₹|rs\.?|inr)\s?\d+""", RegexOption.IGNORE_CASE).containsMatchIn(normalized) ||
                                    Regex("""\d+\s?(?:₹|rs\.?|inr)""", RegexOption.IGNORE_CASE).containsMatchIn(normalized)
        
        if (!hasCurrencyWithAmount) return false

        // 4. Banking Action Verb Requirement
        val hasBankWord = BANK_ACTION_WORDS.any { normalized.contains(it) }
        val isShopping  = SHOPPING_KEYWORDS.any { normalized.contains(it) }
        
        // 5. Sender Validation
        // Financial SMS usually come from alphanumeric senders (SBI-UPI) or short codes.
        // If it's from a standard 10-digit number, we require double the confidence.
        val isPersonalSender = sender.matches(Regex("""\+?\d{10,13}"""))
        
        return when {
            hasCurrencyWithAmount && hasBankWord -> true
            hasCurrencyWithAmount && isShopping -> true
            hasCurrencyWithAmount && !isPersonalSender -> true // Trust alpha-headers with currency
            else -> false
        }
    }

    // ── Layer 2: Rule-Based Extraction ────────────────────────────────────

    private fun extractType(normalized: String): TransactionType? {
        var debitIdx  = Int.MAX_VALUE
        var creditIdx = Int.MAX_VALUE
        DEBIT_KEYWORDS.forEach  { k -> normalized.indexOf(k).takeIf { it >= 0 }?.let { if (it < debitIdx)  debitIdx  = it } }
        CREDIT_KEYWORDS.forEach { k -> normalized.indexOf(k).takeIf { it >= 0 }?.let { if (it < creditIdx) creditIdx = it } }
        return when {
            debitIdx  < creditIdx -> TransactionType.DEBIT
            creditIdx < debitIdx  -> TransactionType.CREDIT
            else -> null
        }
    }

    private fun extractAmount(normalized: String): Double? {
        val patterns = listOf(
            // "debited by 190.00", "spent ₹150", "paid 20.00"
            """(?:debited|credited|spent|paid|sent|received|amounting)\s*(?:by|for|of|to)?\s*(?:₹|rs\.?|inr)?\s*(\d+(?:\.\d{1,2})?)""",
            // "₹ 100.50", "rs 500"
            """(?:₹|rs\.?|inr)\s?(\d+(?:\.\d{1,2})?)""",
            // "100.50 ₹"
            """(\d+(?:\.\d{1,2})?)\s?(?:₹|rs\.?|inr)""",
            // Generic fallback for any number next to currency keyword
            """(?:₹|rs\.?|inr)\s*(\d+(?:\.\d{1,2})?)"""
        )
        for (p in patterns) {
            Regex(p, RegexOption.IGNORE_CASE).find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }
        }
        return null
    }

    private fun extractMerchant(normalized: String): String? {
        val patterns = listOf(
            // SBI: "trf to <name>", "paid to <name>"
            """(?:trf to|paid to|payment to|spent at|towards|info:)\s+([a-z0-9.\-_ ]{3,40}?)(?:\s+(?:refno|ref|via|on|at|using|link|val|ref|upi)|\.|,|$)""",
            // UPI VPA: "to <name>@upi"
            """(?:to|paid to|vpa|sent to)\s+([a-z0-9.\-_]{3,40}?)@[a-z0-9]+""",
            // ICICI/HDFC: "at <name> on <date>"
            """at\s+([a-z0-9.\-_ ]{3,30}?)\s+(?:on|using|via|at)""",
            // UPI patterns: "UPI/<id>/<merchant>/..."
            """upi/[a-z0-9]+?/\s?([a-z0-9.\-_ ]{3,30}?)\s?/""",
            // Generic trailing merchant: "debited by ₹100 for <merchant>"
            """(?:for|at)\s+([a-z0-9.\-_ ]{3,30}?)(?:\.|,|$)""",
            // GPay/PhonePe: "payment to <merchant> successful"
            """payment to\s+([a-z0-9.\-_ ]{3,40}?)\s+successful""",
            // Generic: "at <name>"
            """at\s+([a-z0-9.\-_ ]{3,30}?)(?:\s+(?:on|via|ref|upi|using)|\.|,|$)""",
            // Info block: "Info: <merchant>"
            """info[:\-\s]+([a-z0-9.\-_ ]{3,30}?)(?:\s|/|\.|,|$)"""
        )
        for (p in patterns) {
            val match = Regex(p, RegexOption.IGNORE_CASE).find(normalized)
            if (match != null) {
                val name = match.groupValues[1].trim()
                if (name.isNotBlank() && !name.matches(Regex("""^\d+$"""))) return name.uppercase()
            }
        }
        return null
    }

    private fun categorizeMerchant(merchant: String, body: String): String {
        val m = merchant.uppercase()
        val b = body.lowercase()
        
        return when {
            m.contains("ZOMATO") || m.contains("SWIGGY") || m.contains("EATS") || m.contains("DINING") || b.contains("restaurant") -> "Food & Dining"
            m.contains("UBER") || m.contains("OLA") || m.contains("RAPIDO") || m.contains("METRO") || m.contains("IRCTC") || b.contains("railway") -> "Transport"
            m.contains("AMAZON") || m.contains("FLIPKART") || m.contains("MYNTRA") || m.contains("SHOP") || b.contains("shopping") -> "Shopping"
            m.contains("RELIANCE") || m.contains("BIGBASKET") || m.contains("BLINKIT") || m.contains("ZEPTO") || m.contains("GROCERY") -> "Groceries"
            m.contains("NETFLIX") || m.contains("SPOTIFY") || m.contains("HOTSTAR") || m.contains("YOUTUBE") || m.contains("PRIME") -> "Subscriptions"
            m.contains("AIRTEL") || m.contains("JIO") || m.contains("VI ") || b.contains("recharge") || b.contains("bill") -> "Bills & Utilities"
            else -> "Others"
        }
    }

    private fun extractDate(normalized: String, fallback: Long): Long {
        val patterns = listOf(
            // 12-dec-2023, 12/12/23, 12-12-2023
            """\b(\d{1,2}[/-]\d{1,2}[/-]\d{2,4})\b""",
            """\b(\d{1,2}\s+(?:jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\s+\d{2,4})\b"""
        )
        
        val dateFormats = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd MMMM yyyy", Locale.ENGLISH),
            SimpleDateFormat("dd/MM/yy", Locale.ENGLISH),
            SimpleDateFormat("dd-MM-yy", Locale.ENGLISH)
        )

        for (p in patterns) {
            val match = Regex(p, RegexOption.IGNORE_CASE).find(normalized)
            if (match != null) {
                val dateStr = match.groupValues[1]
                for (format in dateFormats) {
                    try {
                        val parsed = format.parse(dateStr.replace("-", "/"))
                        if (parsed != null) return parsed.time
                    } catch (e: Exception) {}
                }
            }
        }
        return fallback
    }

    private fun extractAccount(normalized: String): String? =
        Regex("""(?:a/c|acct|ac|account|ending|card)\s*x*(\d{2,4})\b""", RegexOption.IGNORE_CASE)
            .find(normalized)?.groupValues?.get(1)

    private fun extractTransactionId(normalized: String): String? {
        val patterns = listOf(
            """(?:ref\s?no|txn\s?id|ref|utr|upi\s?ref)\s*(?:is|:)?\s*([a-z0-9]{8,20})""",
            """(?:val|ref)\s*([0-9]{10,14})"""
        )
        for (p in patterns) {
            Regex(p, RegexOption.IGNORE_CASE).find(normalized)?.groupValues?.get(1)?.let { return it }
        }
        return null
    }

    private fun cleanMerchantName(raw: String): String {
        var name = raw.uppercase().trim()
        
        // Remove common prefixes
        val prefixes = listOf("TRF TO", "PAID TO", "VPA:", "TO", "PURCHASE AT", "SPENT AT", "PAYMENT TO")
        for (p in prefixes) {
            if (name.startsWith(p)) name = name.substring(p.length).trim()
        }
        
        // Remove common suffixes
        val suffixes = listOf("PRIVATE LIMITED", "PVT LTD", "LTD", "LIMITED", "SERVICES", "INDIA")
        for (s in suffixes) {
            if (name.endsWith(s)) name = name.substring(0, name.length - s.length).trim()
        }
        
        // Remove symbols and extra spaces
        name = name.replace(Regex("""[*#_]"""), " ")
        name = name.replace(Regex("""\s+"""), " ").trim()
        
        return name.takeIf { it.isNotBlank() } ?: "UNCATEGORISED"
    }

    private fun extractBalance(normalized: String): Double? {
        val patterns = listOf(
            """(?:bal|balance|bal\s?is|avl\s?bal)\s*(?:is|:)?\s*(?:₹|rs\.?|inr)?\s*(\d+(?:\.\d{1,2})?)""",
            """(?:bal|balance)\s*(?:₹|rs\.?|inr)\s*(\d+(?:\.\d{1,2})?)"""
        )
        for (p in patterns) {
            Regex(p, RegexOption.IGNORE_CASE).find(normalized)?.groupValues?.get(1)?.toDoubleOrNull()?.let { return it }
        }
        return null
    }

    // ── Layer 3: Normalization ────────────────────────────────────────────

    private fun normalize(text: String): String {
        var s = text.lowercase().replace("\n", " ").replace("\r", " ")
        s = s.replace(Regex("""(?i)\b(rs\.?|inr)\b"""), "₹")
        s = s.replace(Regex("""(?<=\d),(?=\d)"""), "")   // remove thousands commas
        return s.replace(Regex("""\s+"""), " ").trim()
    }

    // ── Layer 4: Confidence Scoring ───────────────────────────────────────

    private fun score(amount: Double?, type: TransactionType?, merchant: String?): Float {
        var hits = 0
        if (amount   != null) hits++
        if (type     != null) hits++
        if (merchant != null) hits++
        return hits / 3.0f
    }

    // ── Layer 5: AI Fallback ──────────────────────────────────────────────

    private suspend fun aiExtract(smsBody: String): Triple<Double?, TransactionType?, String?>? {
        return try {
            val apiKey = com.billwise.app.core.Config.OPENAI_API_KEY
            val response = com.billwise.app.data.remote.RetrofitClient.openAiApi.categorizeTransaction(
                "Bearer $apiKey",
                com.billwise.app.data.remote.OpenAiRequest(
                    messages = listOf(
                        com.billwise.app.data.remote.Message(
                            role = "system",
                            content = "Financial SMS parser. Extract from valid completed transactions: amount, type (DEBIT/CREDIT), merchant. Reply EXACTLY as: AMOUNT|TYPE|MERCHANT (e.g. 150.0|DEBIT|ZOMATO). If not a valid transaction reply INVALID."
                        ),
                        com.billwise.app.data.remote.Message(role = "user", content = "SMS: $smsBody")
                    )
                )
            )
            val text = response.choices.firstOrNull()?.message?.content?.trim() ?: return null
            if (text == "INVALID" || !text.contains("|")) return null
            val parts = text.split("|")
            if (parts.size < 3) return null
            Triple(
                parts[0].toDoubleOrNull(),
                if (parts[1].trim().uppercase() == "CREDIT") TransactionType.CREDIT else TransactionType.DEBIT,
                parts[2].trim().uppercase().takeIf { it.isNotBlank() }
            )
        } catch (e: Exception) {
            null
        }
    }

    // ── Public parse entry point ──────────────────────────────────────────

    suspend fun parse(smsBody: String, sender: String, timestamp: Long, useAi: Boolean = true): Transaction? {
        val normalized = normalize(smsBody)
        if (!isTransactionMessage(normalized, sender)) return null

        var type     = extractType(normalized)
        var amount   = extractAmount(normalized)
        var merchant = extractMerchant(normalized)
        val account  = extractAccount(normalized)
        val txnId    = extractTransactionId(normalized)
        val balance  = extractBalance(normalized)
        val txDate   = extractDate(normalized, timestamp)

        // Shopping notification override
        if (SHOPPING_KEYWORDS.any { normalized.contains(it) }) {
            if (merchant == null) merchant = sender.uppercase()
        }

        var confidence = score(amount, type, merchant)

        // AI fallback: Call if confidence is low OR if the merchant is still missing
        if (useAi && (confidence < AI_THRESHOLD || merchant == null)) {
            val ai = aiExtract(smsBody)
            if (ai != null) {
                if (amount   == null) amount   = ai.first
                if (type     == null) type     = ai.second
                if (merchant == null) merchant = ai.third
                confidence = score(amount, type, merchant) * 0.85f // slight penalty for AI assist
            }
        }

        // Never drop a transaction if it looks financial - flag as Uncategorised
        if (type == null) type = TransactionType.DEBIT // Default to debit for safety
        if (amount == null) return null // still need an amount to save

        val isWalletTransfer = WALLET_KEYWORDS.any { normalized.contains(it) } && type == TransactionType.DEBIT
        var resolvedMerchant = if (merchant != null) cleanMerchantName(merchant) else if (isWalletTransfer) "WALLET RELOAD" else null
        
        // Final fallback: Use sender name if merchant is STILL null
        if (resolvedMerchant == null) {
            resolvedMerchant = cleanMerchantName(sender.replace("-", " ").replace("AD ", ""))
        }
        
        val hashId = com.billwise.app.core.TransactionUtils.generateDeterministicId(
            "sms", amount, type, resolvedMerchant, txDate
        )

        return Transaction(
            id              = "sms_$hashId",
            amount          = amount,
            merchant        = resolvedMerchant,
            datetime        = txDate,
            type            = type,
            category        = categorizeMerchant(resolvedMerchant, smsBody),
            source          = TransactionSource.SMS,
            isIgnored       = isWalletTransfer,
            accountHint     = account,
            transactionId   = txnId,
            balance         = balance,
            confidenceScore = confidence
        )
    }
}
