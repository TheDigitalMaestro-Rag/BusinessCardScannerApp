package com.raghu.businesscardscanner2.MLkitTextRec

import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer as MLKitRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions as LatinTextRecognizerOptions
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.Locale
import java.util.regex.Pattern

class TextRecognizer {
    private val latin: MLKitRecognizer = TextRecognition.getClient(LatinTextRecognizerOptions.Builder().build())
    private val devanagari: MLKitRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
    private val japanese: MLKitRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    private val korean: MLKitRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    private val chinese: MLKitRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    suspend fun processImage(image: InputImage): String = coroutineScope {
        try {
            val recognizers = listOf(
                async(Dispatchers.IO) { runCatching { latin.process(image).await() } },
                async(Dispatchers.IO) { runCatching { devanagari.process(image).await() } },
                async(Dispatchers.IO) { runCatching { japanese.process(image).await() } },
                async(Dispatchers.IO) { runCatching { korean.process(image).await() } },
                async(Dispatchers.IO) { runCatching { chinese.process(image).await() } }
            )

            recognizers.awaitAll()
                .filter { it.isSuccess }
                .flatMap { it.getOrThrow().textBlocks }
                .flatMap { it.lines }
                .map { it.text.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString("\n")
        } catch (e: Exception) {
            Log.e("TextRecognizer", "OCR failed", e)
            ""
        }
    }

    fun extractBusinessCardInfo(text: String, imagePath: String? = null): BusinessCard {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { isLikelyNoise(it) }

        var name = ""
        var company = ""
        var position = ""
        var phone = ""
        var email = ""
        var address: String
        var website = ""
        val addressLines = mutableListOf<String>()

        for (line in lines) {
            when {
                email.isEmpty() && isEmail(line) -> email = line
                phone.isEmpty() && isPhoneNumber(line) -> phone = line
                website.isEmpty() && isWebsite(line) -> website = line
                name.isEmpty() && isPotentialName(line) -> name = line
                position.isEmpty() && isPosition(line) -> position = line
                isPotentialAddressLine(line) -> addressLines.add(line)
                company.isEmpty() && isPotentialCompany(line) -> company = line
            }
        }

        // Try to find name from email prefix if not found
        if (name.isEmpty() && email.isNotEmpty()) {
            val emailPrefix = email.substringBefore("@")
            if (emailPrefix.length in 3..30 && !emailPrefix.contains(Regex("[0-9_]"))) {
                name = emailPrefix.split(".", "-").joinToString(" ") { it.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.ROOT
                    ) else it.toString()
                } }
            }
        }

        // Fallback name detection if still empty
        if (name.isEmpty() && lines.isNotEmpty()) {
            name = lines.first {
                it.length in 3..30 &&
                        !it.contains(Regex("[0-9]")) &&
                        !isPotentialCompany(it)
            }
        }

        // Process address lines
        address = processAddressLines(addressLines, company)

        // Clean up company name if it appears in address
        if (company.isNotEmpty() && address.contains(company)) {
            address = address.replace(company, "").trim()
        }

        return BusinessCard(
            name = name.cleanName(),
            company = company.cleanCompanyName(),
            position = position.cleanPosition(),
            phone = phone.cleanPhoneNumber(),
            email = email,
            address = address.cleanAddress(),
            website = website.cleanWebsite(),
            notes = "",
            imagePath = imagePath
        )
    }

    private fun processAddressLines(addressLines: List<String>, company: String): String {
        // Filter out lines that might be company names or other info
        val filtered = addressLines.filter { line ->
            line.length > 10 &&
                    !line.contains(Regex("www\\.|http")) &&
                    !isPotentialCompany(line) &&
                    !isPosition(line) &&
                    !isEmail(line) &&
                    !isPhoneNumber(line)
        }

        // Try to find the most likely address block (longest consecutive lines)
        var bestStart = 0
        var bestLength = 0
        var currentStart = 0
        var currentLength = 0

        for (i in filtered.indices) {
            if (i > 0 && (filtered[i].length > 10 || filtered[i-1].length > 10)) {
                currentLength++
            } else {
                if (currentLength > bestLength) {
                    bestLength = currentLength
                    bestStart = currentStart
                }
                currentStart = i
                currentLength = 1
            }
        }

        if (currentLength > bestLength) {
            bestLength = currentLength
            bestStart = currentStart
        }

        return filtered.subList(bestStart, bestStart + bestLength.coerceAtMost(filtered.size))
            .joinToString("\n")
            .trim()
    }

    private fun isLikelyNoise(line: String): Boolean {
        return line.length == 1 ||
                line.matches(Regex("[^a-zA-Z0-9]{3,}")) || // Too many special chars
                line.matches(Regex("[0-9\\s]{8,}")) // Just numbers and spaces
    }

    private fun isPotentialName(line: String): Boolean {
        // Check for honorifics first
        if (line.split(" ").any { it.matches(Regex("(?i)mr|mrs|ms|dr|prof|sir|shri|smt")) }) {
            return true
        }

        // Name should be 2-4 words, 3-30 chars, with proper capitalization
        val words = line.split(" ")
        return words.size in 2..4 &&
                line.length in 3..30 &&
                words.all { it.length > 1 && it[0].isUpperCase() } &&
                !line.contains(Regex("[0-9]")) &&
                !isEmail(line) &&
                !isPhoneNumber(line) &&
                !isWebsite(line) &&
                !isPotentialCompany(line)
    }

    private fun isPotentialCompany(line: String): Boolean {
        val lowerLine = line.lowercase()

        // Check for company suffixes
        val companySuffixes = listOf(
            "inc", "llc", "corp", "pvt", "ltd", "plc", "gmbh", "co",
            "limited", "corporation", "enterprises", "solutions",
            "technologies", "systems", "consulting", "group", "holdings",
            "प्रा. लि", "लिमिटेड", "प्राइवेट", "पब्लिक", "会社", "株式会社"
        )

        if (companySuffixes.any { suffix ->
                lowerLine.contains(suffix) ||
                        line.contains(Regex("\\b$suffix\\b", RegexOption.IGNORE_CASE))
            }) {
            return true
        }

        // Check for industry terms
        val industryTerms = listOf(
            "tech", "software", "bank", "financial", "capital", "insurance",
            "media", "network", "digital", "global", "international", "ventures"
        )

        if (industryTerms.any { term -> lowerLine.contains(term) }) {
            return true
        }

        // Check for company name patterns (multiple words with capitalization)
        val words = line.split(" ")
        return words.size in 1..5 &&
                words.all { it.length > 2 } &&
                words.count { it[0].isUpperCase() } >= words.size / 2 &&
                !isEmail(line) &&
                !isPhoneNumber(line) &&
                !isWebsite(line)
    }

    private fun isPosition(line: String): Boolean {
        val lowerLine = line.lowercase()

        // English positions
        val englishPositions = listOf(
            "manager", "director", "ceo", "cto", "cfo", "coo", "founder", "head",
            "president", "vp", "vice president", "executive", "partner", "lead",
            "specialist", "consultant", "engineer", "developer", "designer",
            "architect", "analyst", "officer", "administrator", "coordinator"
        )

        if (englishPositions.any { lowerLine.contains(it) }) {
            return true
        }

        // Indian language positions
        val indianPositions = listOf(
            "प्रबंधक", "निर्देशक", "संचालक", "अध्यक्ष",
            "இயக்குனர்", "மேலாளர்",
            "నిర్వాహకుడు", "మేనేజర్", "డైరెక్టర్",
            "ব্যবস্থাপক", "পরিচালক"
        )

        if (indianPositions.any { line.contains(it) }) {
            return true
        }

        // Position patterns (often starts or ends with title)
        return line.matches(Regex("(?i).*(manager|director|executive|head|lead|chief)\\b")) ||
                line.matches(Regex("\\b(manager|director|executive|head|lead|chief).*", RegexOption.IGNORE_CASE))
    }

    private fun isPotentialAddressLine(line: String): Boolean {
        // Skip if it's clearly something else
        if (isEmail(line) || isPhoneNumber(line) || isWebsite(line) ||
            isPosition(line) || isPotentialCompany(line) || isPotentialName(line)) {
            return false
        }

        // Address indicators
        val addressIndicators = listOf(
            "street", "st", "road", "rd", "avenue", "ave", "lane", "ln",
            "drive", "dr", "boulevard", "blvd", "circle", "cir", "place", "pl",
            "floor", "fl", "suite", "ste", "apartment", "apt", "building", "bldg",
            "city", "state", "country", "zip", "postal", "pincode", "area",
            "colony", "nagar", "village", "town", "district", "sector", "block",
            "मार्ग", "रोड", "सड़क", "गली", "स्ट्रीट",
            "தெரு", "சாலை", "வீதி",
            "వీధి", "రోడ్", "స్ట్రీట్",
            "রোড", "স্ট্রিট", "গলি"
        )

        // Check for address numbers (house numbers, etc)
        val hasAddressNumber = line.contains(Regex("\\b\\d+[a-zA-Z]?\\b")) ||
                line.contains(Regex("No\\.?\\s?\\d+", RegexOption.IGNORE_CASE))

        // Check for address indicators
        val hasAddressTerm = addressIndicators.any { term ->
            line.contains(Regex("\\b$term\\b", RegexOption.IGNORE_CASE))
        }

        // Check for long strings that might be addresses
        val isLongEnough = line.length > 15 && line.split(" ").size > 2

        return hasAddressNumber || hasAddressTerm || isLongEnough
    }

    private fun isEmail(line: String): Boolean {
        return EMAIL_PATTERN.matcher(line).find()
    }

    private fun isPhoneNumber(line: String): Boolean {
        // Remove all non-digit characters except '+' for initial check
        val digitsOnly = line.replace(Regex("[^0-9+]"), "")

        // Basic length check (minimum 7 digits for phone numbers)
        if (digitsOnly.length < 7) return false

        // Common phone number patterns
        val phonePatterns = listOf(
            // International formats
            Regex("\\+[0-9]{1,3}[\\s-]?[0-9]{2,4}[\\s-]?[0-9]{3,4}[\\s-]?[0-9]{3,4}"), // +1 234 567 8901
            Regex("\\+[0-9]{1,3}[\\s-]?[0-9]{6,14}"), // +12345678901
            Regex("\\+[0-9]{1,3}[\\s-]?\\([0-9]{2,4}\\)[\\s-]?[0-9]{3,4}[\\s-]?[0-9]{3,4}"), // +1 (234) 567-8901

            // National formats
            Regex("\\(?[0-9]{2,4}\\)?[\\s-]?[0-9]{3,4}[\\s-]?[0-9]{3,4}"), // (123) 456-7890 or 123-456-7890
            Regex("[0-9]{4,5}[\\s-]?[0-9]{4,6}"), // 12345 67890 or 1234 567890
            Regex("[0-9]{8,15}"), // Continuous numbers (common in some countries)

            // Special service numbers
            Regex("1[0-9]{3}[\\s-]?[0-9]{3,4}"), // 1800-123-456
            Regex("[0-9]{3,4}[\\s-]?[0-9]{3,4}") // Short numbers like 911 or 112
        )

        // Check if line matches any phone pattern
        if (phonePatterns.any { it.matches(line) }) return true

        // Additional checks for false positives
        if (digitsOnly.length > 15) return false // Too long for a phone number
        if (line.contains(Regex("[a-df-zA-DF-Z]"))) return false // Contains letters that aren't phone-related

        // Common non-phone patterns to exclude
        val nonPhonePatterns = listOf(
            Regex("[0-9]{16,}"), // Credit card numbers
            Regex("[0-9]{3}-[0-9]{2}-[0-9]{4}"), // Social security-like numbers
            Regex("[0-9]{4}-[0-9]{4}-[0-9]{4}-[0-9]{4}"), // Credit card format
            Regex("[0-9]{5,}-[0-9]{5,}") // Long numbers with dash (likely not phone)
        )

        if (nonPhonePatterns.any { it.matches(line) }) return false

        // Check for common phone number prefixes by country
        val countryPrefixes = listOf(
            "+1", "+44", "+91", "+86", "+81", "+33", "+49", "+7", "+61"
        )

        if (countryPrefixes.any { digitsOnly.startsWith(it.replace("+", "")) || line.startsWith(it) }) {
            return true
        }

        // Final check - if it looks like a sequence of numbers with possible separators
        return line.matches(Regex("^[+0-9][0-9\\s-()]{5,20}$")) &&
                digitsOnly.length in 7..15
    }


    private fun isWebsite(line: String): Boolean {
        return WEBSITE_PATTERN.matcher(line).find() &&
                !line.contains(" ") &&
                line.length in 5..50
    }

    companion object {
        private val EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b")
        private val PHONE_PATTERN = Pattern.compile("((\\+\\d{1,3}[\\s-]?)?\\d{2,4}[\\s-]?\\d{3,4}[\\s-]?\\d{3,4})")
        private val WEBSITE_PATTERN = Pattern.compile("(https?:\\/\\/)?(www\\.)?[-a-zA-Z0-9@:%._\\+~#=]{2,256}\\.[a-z]{2,6}\\b([-a-zA-Z0-9@:%_\\+.~#?&//=]*)")
    }
}

// Extension functions for cleaning extracted data
private fun String.cleanName(): String {
    return this.replace(Regex("(?i)\\b(mr|mrs|ms|dr|prof)\\.?\\s*"), "")
        .trim()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
        } }
}

private fun String.cleanCompanyName(): String {
    return this.replace(Regex("(?i)\\b(co|inc|llc|ltd|gmbh|plc)\\.?\\s*$"), "")
        .trim()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
        } }
}

private fun String.cleanPosition(): String {
    return this.replace(Regex("[,.]$"), "")
        .trim()
        .split(" ")
        .joinToString(" ") { it.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
        } }
}

private fun String.cleanPhoneNumber(): String {
    // First extract all numbers and +
    val digitsOnly = this.replace(Regex("[^0-9+]"), "")

    // Format international numbers
    if (digitsOnly.startsWith("+")) {
        val countryCode = digitsOnly.takeWhile { it == '+' || it.isDigit() }
        val rest = digitsOnly.drop(countryCode.length)

        return when (countryCode.length) {
            2 -> "+${countryCode.drop(1)} ${rest.chunked(3).joinToString(" ")}" // +1 234 567 8901
            3 -> "+${countryCode.drop(1)} ${rest.chunked(2).joinToString(" ")}" // +44 12 34 56 78
            else -> "+${countryCode.drop(1)} $rest" // Fallback
        }
    }

    // Format national numbers based on length
    return when (digitsOnly.length) {
        7 -> digitsOnly.chunked(3).joinToString("-") // 123-4567
        8 -> "${digitsOnly.take(4)}-${digitsOnly.drop(4)}" // 1234-5678
        10 -> "(${digitsOnly.take(3)}) ${digitsOnly.drop(3).take(3)}-${digitsOnly.drop(6)}" // (123) 456-7890
        11 -> "${digitsOnly.take(1)} (${digitsOnly.drop(1).take(3)}) ${digitsOnly.drop(4).take(3)}-${digitsOnly.drop(7)}" // 1 (234) 567-8901
        else -> digitsOnly // Return as is if no specific format matches
    }
}

private fun String.cleanAddress(): String {
    return this.replace(Regex("\\s{2,}"), " ")
        .replace(Regex("(?i)\\b(bldg|building)\\.?\\s*"), "Building ")
        .replace(Regex("(?i)\\b(fl|floor)\\.?\\s*"), "Floor ")
        .replace(Regex("(?i)\\b(apt|apartment)\\.?\\s*"), "Apartment ")
        .trim()
}

private fun String.cleanWebsite(): String {
    return this.replace(Regex("^https?://"), "")
        .replace(Regex("/+$"), "")
        .trim()
        .lowercase()
}