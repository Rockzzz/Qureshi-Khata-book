package com.example.tabelahisabapp.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local Voice Parser - No AI Required!
 * Parses Hindi/Urdu voice input using keyword matching
 * 
 * Supports 3 types of transactions:
 * 1. CUSTOMER_TRANSACTION: "Ejaz ko 50000 diya" / "Ejaz se 30000 mila"
 * 2. EXPENSE: "100 rs for milk" / "petrol bill 500" / "truck expense 2000"
 * 3. PAYMENT: "halkari ko 1000 diya" / "driver amjad ko 5000 diya"
 */
@Singleton
class GeminiRepository @Inject constructor() {

    // Keywords indicating money was GIVEN (CREDIT - you gave money)
    private val creditKeywords = listOf(
        "diya", "dia", "de diya", "de dia", "diye", "die",
        "udhaar diya", "udhaar dia", "udhar diya", "udhar dia",
        "bheja", "bhej diya", "transfer kiya", "sent",
        "ko diya", "ko dia", "payment", "paid"
    )

    // Keywords indicating money was RECEIVED (DEBIT - you received money)
    private val debitKeywords = listOf(
        // Money received patterns
        "mila", "mili", "mile", "mil gaya", "mil gaye",
        "aaya", "aaye", "aa gaya", "aa gaye", "received",
        "liya", "le liya", "payment mili",
        "se mila", "se liya",
        // Return/repayment patterns (wapas = back/return)
        "wapas kiya", "wapis kiya", "wapas diya", "wapis diya",
        "wapas kar diya", "wapis kar diya",
        "wapas mila", "wapis mila",
        "returned", "repaid", "repayment",
        "paisa wapas", "paise wapas", "rupees wapas", "rupaye wapas",
        // Someone gave patterns
        "ne diya", "ne diye", "ne dia", // When someone "ne diya" = they gave
        "ne kiya", "ne bheja", "ne transfer kiya",
        // Clear/settle patterns
        "clear kiya", "settle kiya", "chuka diya", "chukaya"
    )

    // Expense keywords
    private val expenseKeywords = listOf(
        "expense", "kharcha", "bill", "for", "ka", "ki", "ke liye",
        "petrol", "diesel", "chai", "khaana", "khana", "food",
        "medical", "medicine", "dawa", "dawai",
        "truck", "gaadi", "gadi", "vehicle",
        "chai pani", "nashta", "lunch", "dinner",
        "rent", "kiraya", "bijli", "light", "pani", "water",
        "phone", "mobile", "recharge"
    )

    // Common expense categories (not person names)
    private val expenseCategories = listOf(
        "milk", "dudh", "doodh",
        "petrol", "diesel", "gas",
        "medical", "medicine", "dawa", "dawai",
        "chai", "tea", "nashta", "breakfast", "lunch", "dinner", "khana", "khaana", "food",
        "truck", "gaadi", "gadi", "vehicle", "transport",
        "electricity", "bijli", "light",
        "water", "pani",
        "phone", "mobile", "recharge",
        "rent", "kiraya",
        "office", "daftar",
        "stationery", "paper",
        "misc", "miscellaneous", "other"
    )

    // Worker/role titles (payment to workers, not udhaar)
    private val workerTitles = listOf(
        "driver", "halkari", "helper", "worker", "mazdoor", "mistri",
        "cleaner", "safai", "watchman", "chowkidar", "guard",
        "peon", "chaprasi", "office boy", "servant", "naukar",
        "coolie", "loader", "unloader"
    )
    
    // Purchase keywords - for buying assets, livestock, goods from sellers
    private val purchaseKeywords = listOf(
        "purchase", "khareedari", "kharidi", "kharida", "kharid",
        "bought", "buy", "liya", "le liya", "lena",
        "bhains", "bakri", "bakra", "gaye", "bail", // Livestock
        "maal", "goods", "stock", "inventory",
        "asset", "machine", "equipment"
    )

    // Keywords for bank transactions
    private val bankKeywords = listOf(
        "bank", "transfer", "online", "upi", "gpay", "paytm", "phonepe",
        "neft", "imps", "rtgs", "account"
    )
    
    // ===== NEW: Indian Number System Support =====
    private val indianNumberMultipliers = mapOf(
        "lac" to 100000.0,
        "lakh" to 100000.0,
        "lakhs" to 100000.0,
        "lacs" to 100000.0,
        "hazar" to 1000.0,
        "hazaar" to 1000.0,
        "hazzar" to 1000.0,
        "hajar" to 1000.0,
        "thousand" to 1000.0,
        "crore" to 10000000.0,
        "crores" to 10000000.0,
        "karor" to 10000000.0,
        "karod" to 10000000.0
    )
    
    // Hindi number words to digits
    private val hindiNumbers = mapOf(
        "ek" to 1.0, "one" to 1.0,
        "do" to 2.0, "two" to 2.0,
        "teen" to 3.0, "tin" to 3.0, "three" to 3.0,
        "char" to 4.0, "chaar" to 4.0, "four" to 4.0,
        "paanch" to 5.0, "panch" to 5.0, "five" to 5.0,
        "cheh" to 6.0, "che" to 6.0, "six" to 6.0,
        "saat" to 7.0, "sat" to 7.0, "seven" to 7.0,
        "aath" to 8.0, "aat" to 8.0, "eight" to 8.0,
        "nau" to 9.0, "nine" to 9.0,
        "das" to 10.0, "ten" to 10.0,
        "gyarah" to 11.0, "baarah" to 12.0, "terah" to 13.0,
        "chaudah" to 14.0, "pandrah" to 15.0,
        "bees" to 20.0, "twenty" to 20.0,
        "pachees" to 25.0, "pachhees" to 25.0,
        "tees" to 30.0, "thirty" to 30.0,
        "chalis" to 40.0, "forty" to 40.0,
        "pachas" to 50.0, "pachaas" to 50.0, "fifty" to 50.0,
        "saath" to 60.0, "sixty" to 60.0,
        "sattar" to 70.0, "seventy" to 70.0,
        "assi" to 80.0, "eighty" to 80.0,
        "nabbe" to 90.0, "ninety" to 90.0,
        "sau" to 100.0, "so" to 100.0, "hundred" to 100.0,
        "dedh" to 1.5, // 1.5 (as in dedh lac = 1.5 lac)
        "dhai" to 2.5, // 2.5 (as in dhai lac = 2.5 lac)
        "sava" to 1.25, // 1.25
        "paune" to 0.75 // 0.75
    )
    
    // ===== NEW: Name Suffixes to Strip =====
    private val nameSuffixes = listOf(
        "bhai", "bhi", "sahab", "saheb", "sb", "ji", "jee",
        "wala", "wale", "wali", "waale", "waali",
        "seth", "lala"
    )


    /**
     * Parse voice transcription into structured transaction data
     * Works completely offline - no API needed!
     */
    suspend fun parseTransaction(transcribedText: String, customerNames: List<String>): ParseResult {
        return withContext(Dispatchers.Default) {
            try {
                val text = transcribedText.lowercase().trim()
                
                // Extract amount
                val amount = extractAmount(text)
                if (amount == null || amount <= 0) {
                    return@withContext ParseResult.Error("Could not find amount in: \"$transcribedText\"")
                }
                
                // Determine transaction category
                val category = determineCategory(text, customerNames)
                
                // Determine payment method
                val paymentMethod = if (bankKeywords.any { text.contains(it) }) "BANK" else "CASH"
                
                when (category) {
                    TransactionCategory.EXPENSE -> {
                        val expenseName = extractExpenseName(text)
                        ParseResult.Success(
                            ParsedTransaction(
                                customerName = expenseName,
                                customerId = null,
                                transactionType = "EXPENSE",
                                amount = amount,
                                paymentMethod = paymentMethod,
                                confidence = 0.9f,
                                originalText = transcribedText,
                                isExpense = true
                            )
                        )
                    }
                    TransactionCategory.WORKER_PAYMENT -> {
                        val workerName = extractWorkerName(text)
                        ParseResult.Success(
                            ParsedTransaction(
                                customerName = workerName,
                                customerId = null,
                                transactionType = "EXPENSE",
                                amount = amount,
                                paymentMethod = paymentMethod,
                                confidence = 0.85f,
                                originalText = transcribedText,
                                isExpense = true
                            )
                        )
                    }
                    TransactionCategory.PURCHASE -> {
                        // Purchase from a seller (buying goods, livestock, assets)
                        val sellerName = extractCustomerName(text, customerNames)
                        ParseResult.Success(
                            ParsedTransaction(
                                customerName = if (sellerName.isNotBlank()) sellerName else "Purchase",
                                customerId = null,
                                transactionType = "PURCHASE",
                                amount = amount,
                                paymentMethod = paymentMethod,
                                confidence = 0.9f,
                                originalText = transcribedText,
                                isExpense = true
                            )
                        )
                    }
                    TransactionCategory.CUSTOMER_TRANSACTION -> {
                        val customerName = extractCustomerName(text, customerNames)
                        if (customerName.isBlank()) {
                            return@withContext ParseResult.Clarification(
                                error = "Could not identify customer name",
                                suggestions = findSimilarNames(text, customerNames),
                                transcribedText = transcribedText
                            )
                        }
                        
                        val transactionType = determineTransactionType(text)
                        
                        ParseResult.Success(
                            ParsedTransaction(
                                customerName = customerName,
                                customerId = null,
                                transactionType = transactionType,
                                amount = amount,
                                paymentMethod = paymentMethod,
                                confidence = 0.9f,
                                originalText = transcribedText,
                                isExpense = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                ParseResult.Error("Parse error: ${e.message}")
            }
        }
    }

    /**
     * Determine the category of transaction
     */
    private fun determineCategory(text: String, customerNames: List<String>): TransactionCategory {
        // Check if it's clearly an expense (contains expense keywords but no person name pattern)
        val hasExpenseKeyword = expenseKeywords.any { text.contains(it) }
        val hasExpenseCategory = expenseCategories.any { text.contains(it) }
        
        // NEW: Check if it's a purchase (buying goods, livestock, assets)
        val hasPurchaseKeyword = purchaseKeywords.any { text.contains(it) }
        
        // Check if mentions a worker
        val hasWorkerTitle = workerTitles.any { text.contains(it) }
        
        // Check if matches known customer
        val hasKnownCustomer = customerNames.any { text.contains(it.lowercase()) }
        
        // Check for person name pattern (X ko, X se, X ne)
        val hasPersonPattern = Regex("([a-zA-Z]+)\\s+(ko|se|ne|ka|ki)").containsMatchIn(text)
        
        return when {
            // NEW: If has purchase keyword, it's a purchase transaction
            hasPurchaseKeyword -> TransactionCategory.PURCHASE
            
            // If has expense category without person pattern, it's an expense
            hasExpenseCategory && !hasPersonPattern && !hasKnownCustomer -> TransactionCategory.EXPENSE
            
            // If mentions worker title, it's a worker payment (expense)
            hasWorkerTitle -> TransactionCategory.WORKER_PAYMENT
            
            // If has expense keyword and no known customer
            hasExpenseKeyword && !hasKnownCustomer && !hasPersonPattern -> TransactionCategory.EXPENSE
            
            // Otherwise it's a customer transaction
            else -> TransactionCategory.CUSTOMER_TRANSACTION
        }
    }

    /**
     * Extract expense name from text
     */
    private fun extractExpenseName(text: String): String {
        // Find matching expense category
        for (category in expenseCategories) {
            if (text.contains(category)) {
                return category.replaceFirstChar { it.uppercase() }
            }
        }
        
        // Try to extract words after "for" or "ka/ki/ke"
        val patterns = listOf(
            Regex("for\\s+([a-zA-Z]+)", RegexOption.IGNORE_CASE),
            Regex("([a-zA-Z]+)\\s+(?:ka|ki|ke)\\s+(?:bill|kharcha|expense)", RegexOption.IGNORE_CASE),
            Regex("([a-zA-Z]+)\\s+(?:bill|kharcha|expense)", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                return match.groupValues[1].replaceFirstChar { it.uppercase() }
            }
        }
        
        return "Expense"
    }

    /**
     * Extract worker name from text
     */
    private fun extractWorkerName(text: String): String {
        // Pattern: "worker_title name ko" or "name worker_title ko"
        for (title in workerTitles) {
            if (text.contains(title)) {
                // Try to find name before or after the title
                val beforePattern = Regex("([a-zA-Z]+)\\s+$title", RegexOption.IGNORE_CASE)
                val afterPattern = Regex("$title\\s+([a-zA-Z]+)", RegexOption.IGNORE_CASE)
                
                val beforeMatch = beforePattern.find(text)
                if (beforeMatch != null) {
                    val name = beforeMatch.groupValues[1]
                    return "$name (${title.replaceFirstChar { it.uppercase() }})"
                }
                
                val afterMatch = afterPattern.find(text)
                if (afterMatch != null) {
                    val name = afterMatch.groupValues[1]
                    if (name != "ko" && name != "se" && name != "ne") {
                        return "$name (${title.replaceFirstChar { it.uppercase() }})"
                    }
                }
                
                return title.replaceFirstChar { it.uppercase() }
            }
        }
        return "Worker"
    }

    /**
     * Extract numeric amount from text
     * Enhanced to handle Indian number system:
     * - "1 lac" = 100000
     * - "pachas hazar" = 50000
     * - "dedh lac" = 150000
     * - "50000" = 50000
     */
    private fun extractAmount(text: String): Double? {
        val cleanText = text.lowercase()
            .replace("₹", "")
            .replace("rupees", "")
            .replace("rupee", "")
            .replace("rs", "")
            .replace(",", "")
        
        // First, try to find patterns like "1 lac", "50 hazar", "ek lac", "pachas hazar"
        for ((multiplierWord, multiplierValue) in indianNumberMultipliers) {
            if (cleanText.contains(multiplierWord)) {
                // Try to find number or Hindi word before the multiplier
                val pattern = Regex("(\\d+\\.?\\d*|${hindiNumbers.keys.joinToString("|")})\\s*$multiplierWord", RegexOption.IGNORE_CASE)
                val match = pattern.find(cleanText)
                if (match != null) {
                    val numPart = match.groupValues[1].lowercase()
                    val baseNumber = numPart.toDoubleOrNull() ?: hindiNumbers[numPart] ?: 1.0
                    return baseNumber * multiplierValue
                }
                // If just "lac" or "lakh" found without number, assume 1
                val justMultiplierPattern = Regex("\\b$multiplierWord\\b", RegexOption.IGNORE_CASE)
                if (justMultiplierPattern.containsMatchIn(cleanText)) {
                    // Check if preceded by Hindi number
                    val words = cleanText.split("\\s+".toRegex())
                    val multiplierIndex = words.indexOfFirst { it.contains(multiplierWord) }
                    if (multiplierIndex > 0) {
                        val prevWord = words[multiplierIndex - 1]
                        val baseNumber = prevWord.toDoubleOrNull() ?: hindiNumbers[prevWord] ?: 1.0
                        return baseNumber * multiplierValue
                    }
                    return multiplierValue // Just the multiplier alone
                }
            }
        }
        
        // Fall back to simple number extraction
        val numberPattern = Regex("\\d+\\.?\\d*")
        val matches = numberPattern.findAll(cleanText)
        
        return matches.mapNotNull { it.value.toDoubleOrNull() }
            .maxOrNull()
    }

    /**
     * Determine if money was given (CREDIT) or received (DEBIT)
     * Enhanced logic for Hindi/Urdu patterns
     */
    private fun determineTransactionType(text: String): String {
        val words = text.lowercase().split(Regex("\\s+"))
        
        // First check: Look for "[Name] ne ... [action]" pattern
        // When someone "ne" does something to you, you RECEIVE money = DEBIT
        val neIndex = words.indexOfFirst { it == "ne" }
        if (neIndex >= 0 && neIndex < words.size - 1) {
            // Check if any debit action verb appears after "ne"
            val debitVerbs = listOf("diya", "dia", "diye", "die", "bheja", "kiya", "kiye", 
                                    "transfer", "wapas", "returned", "paid", "repaid")
            val afterNe = words.subList(neIndex + 1, words.size)
            if (afterNe.any { word -> debitVerbs.any { verb -> word.contains(verb) } }) {
                return "DEBIT" // Someone ne [gave/sent] = you received
            }
        }
        
        // Second check: Look for "[Name] ko ... [action]" pattern  
        // When you do something "ko" someone, you GIVE money = CREDIT
        val koIndex = words.indexOfFirst { it == "ko" }
        if (koIndex >= 0 && koIndex < words.size - 1) {
            val creditVerbs = listOf("diya", "dia", "diye", "die", "bheja", "transfer")
            val afterKo = words.subList(koIndex + 1, words.size)
            if (afterKo.any { word -> creditVerbs.any { verb -> word.contains(verb) } }) {
                return "CREDIT" // You gave ko [someone]
            }
        }
        
        // Third check: "se mila/liya" pattern = DEBIT
        val seIndex = words.indexOfFirst { it == "se" }
        if (seIndex >= 0 && seIndex < words.size - 1) {
            val receiveVerbs = listOf("mila", "mili", "mile", "liya", "liye", "aaya", "aaye")
            val afterSe = words.subList(seIndex + 1, words.size)
            if (afterSe.any { word -> receiveVerbs.any { verb -> word.contains(verb) } }) {
                return "DEBIT" // Got from [someone]
            }
        }
        
        // Fourth check: Direct keyword matching from debitKeywords (wapas, mila, etc.)
        for (keyword in debitKeywords) {
            if (text.contains(keyword)) {
                return "DEBIT"
            }
        }
        
        // Fifth check: Credit keywords with context
        for (keyword in creditKeywords) {
            if (text.contains(keyword)) {
                // Only if "ko" appears before the action
                val keywordIndex = text.indexOf(keyword)
                val koPos = text.indexOf("ko")
                if (koPos >= 0 && koPos < keywordIndex) {
                    return "CREDIT"
                }
            }
        }
        
        // Default to CREDIT if nothing matched (user gave money)
        return "CREDIT"
    }

    
    /**
     * Auto-correct common name mistakes from voice recognition
     * Maps: "Ejaz" -> "Aijaz", etc.
     */
    private fun correctCommonNames(name: String): String {
        val nameCorrections = mapOf(
            "ejaz" to "Aijaz",
            "gafar" to "Gaffar",
            "gaffer" to "Gaffar",
            "jaffar" to "Gaffar",
            "rauf" to "Rauf lala",
            "raoof" to "Rauf lala",
            "raouf" to "Rauf lala"
        )
        
        val lowerName = name.lowercase().trim()
        return nameCorrections[lowerName] ?: name
    }

    /**
     * Extract customer name from text
     * Enhanced to strip common suffixes like "bhai", "sahab", "wale"
     * Example: "gaffar bhai ko 50000 diya" -> extracts "Gaffar" (not "bhai")
     */
    private fun extractCustomerName(text: String, customerNames: List<String>): String {
        val lowerText = text.lowercase()
        
        // First, try exact match with known customers (prioritize known names)
        for (name in customerNames) {
            if (lowerText.contains(name.lowercase())) {
                return name
            }
        }
        
        // Try to extract name before "ko", "se", "ne", "ka" with suffix handling
        // Pattern: captures 1-2 words before the postposition
        val patterns = listOf(
            Regex("(?:maine\\s+)?([a-zA-Z]+(?:\\s+[a-zA-Z]+)?)\\s+ko", RegexOption.IGNORE_CASE),
            Regex("([a-zA-Z]+(?:\\s+[a-zA-Z]+)?)\\s+se", RegexOption.IGNORE_CASE),
            Regex("([a-zA-Z]+(?:\\s+[a-zA-Z]+)?)\\s+ne", RegexOption.IGNORE_CASE),
            Regex("([a-zA-Z]+(?:\\s+[a-zA-Z]+)?)\\s+ka", RegexOption.IGNORE_CASE),
            Regex("([a-zA-Z]+(?:\\s+[a-zA-Z]+)?)\\s+ki", RegexOption.IGNORE_CASE)
        )
        
        val commonWords = listOf(
            "maine", "mene", "usne", "unhone", "paisa", "rupee", "rupees", "rs", "home",
            "bank", "cash", "upi", "online", "transfer"
        )
        
        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val extractedPhrase = match.groupValues[1].trim()
                
                // Strip name suffixes from the extracted phrase
                val cleanedName = stripNameSuffixes(extractedPhrase)
                
                if (cleanedName.isNotBlank() && cleanedName.length > 1 && 
                    !commonWords.contains(cleanedName.lowercase()) &&
                    !workerTitles.contains(cleanedName.lowercase()) &&
                    !expenseCategories.contains(cleanedName.lowercase()) &&
                    !nameSuffixes.contains(cleanedName.lowercase())) {
                    
                    // Apply name correction before returning
                    val correctedName = correctCommonNames(cleanedName)
                    return correctedName.replaceFirstChar { it.uppercase() }
                }
            }
        }
        
        // Try fuzzy matching with known customers (check if input word matches start of customer name)
        for (name in customerNames) {
            val nameLower = name.lowercase()
            val words = text.lowercase().split(" ")
            for (word in words) {
                // Skip common suffixes
                if (nameSuffixes.contains(word)) continue
                
                if (word.length >= 3 && (nameLower.startsWith(word) || word.startsWith(nameLower.take(4)))) {
                    return name
                }
            }
        }
        
        return ""
    }
    
    /**
     * Strip common name suffixes like "bhai", "sahab", "wale"
     * Example: "gaffar bhai" -> "gaffar"
     * Example: "milk wale" -> "milk"
     */
    private fun stripNameSuffixes(phrase: String): String {
        val words = phrase.trim().split("\\s+".toRegex())
        if (words.size == 1) return phrase
        
        // Check if the last word is a suffix
        val lastWord = words.last().lowercase()
        if (nameSuffixes.contains(lastWord)) {
            // Return all words except the last suffix
            return words.dropLast(1).joinToString(" ")
        }
        
        // Check if any word in the middle is a suffix (rare case)
        val filteredWords = words.filter { !nameSuffixes.contains(it.lowercase()) }
        return if (filteredWords.isNotEmpty()) {
            filteredWords.joinToString(" ")
        } else {
            phrase
        }
    }

    /**
     * Find similar customer names for suggestions
     */
    private fun findSimilarNames(text: String, customerNames: List<String>): List<String> {
        val words = text.split(" ").filter { it.length >= 2 }
        
        return customerNames.filter { name ->
            val nameLower = name.lowercase()
            words.any { word -> 
                nameLower.startsWith(word.take(2)) || 
                word.startsWith(nameLower.take(2))
            }
        }.take(3)
    }
    
    enum class TransactionCategory {
        CUSTOMER_TRANSACTION,
        EXPENSE,
        WORKER_PAYMENT,
        PURCHASE
    }
}

/**
 * Data class for parsed transaction
 */
data class ParsedTransaction(
    val customerName: String,
    val customerId: Int?,
    val transactionType: String, // CREDIT, DEBIT, or EXPENSE
    val amount: Double,
    val paymentMethod: String, // CASH or BANK
    val confidence: Float,
    val originalText: String,
    val isExpense: Boolean = false
)

/**
 * Result of parsing operation
 */
sealed class ParseResult {
    data class Success(val transaction: ParsedTransaction) : ParseResult()
    data class Clarification(
        val error: String,
        val suggestions: List<String>,
        val transcribedText: String
    ) : ParseResult()
    data class Error(val message: String) : ParseResult()
}
