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
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

class TextRecognizer {

    private var latin: MLKitRecognizer? = null
    private var devanagari: MLKitRecognizer? = null
    private var japanese: MLKitRecognizer? = null
    private var korean: MLKitRecognizer? = null
    private var chinese: MLKitRecognizer? = null

    private val isInitialized = AtomicBoolean(false)

    suspend fun initializeRecognizers() {
        if (isInitialized.compareAndSet(false, true)) {
            withContext(Dispatchers.IO) {
                try {
                    latin = TextRecognition.getClient(LatinTextRecognizerOptions.Builder().build())
                    devanagari = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
                    japanese = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                    korean = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
                    chinese = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                    Log.d("TextRecognizer", "All ML Kit recognizers initialized successfully.")
                } catch (e: Exception) {
                    Log.e("TextRecognizer", "Failed to initialize ML Kit recognizers", e)
                    isInitialized.set(false)
                    // Consider re-throwing or notifying the UI of the failure
                }
            }
        } else {
            Log.d("TextRecognizer", "Recognizers already initialized.")
        }
    }

    suspend fun processImage(image: InputImage): String = coroutineScope {
        initializeRecognizers() // Ensure recognizers are initialized

        // Use Dispatchers.IO for image processing
        withContext(Dispatchers.IO) {
            val currentLatin = latin ?: return@withContext ""

            try {
                // Process only the Latin recognizer for demonstration
                val result = runCatching { currentLatin.process(image).await() }
                if (result.isSuccess) {
                    return@withContext result.getOrThrow().textBlocks
                        .flatMap { it.lines }
                        .map { it.text.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .joinToString("\n")
                } else {
                    Log.e("TextRecognizer", "OCR failed", result.exceptionOrNull())
                    return@withContext ""
                }
            } catch (e: Exception) {
                Log.e("TextRecognizer", "OCR failed", e)
                return@withContext ""
            }
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
        var email = ""
        var address = ""
        var website = ""

        val processedLineIndices = mutableSetOf<Int>()

        // Extract Email
        lines.forEachIndexed { index, line ->
            if (email.isEmpty() && isEmail(line)) {
                email = line
                processedLineIndices.add(index)
            }
        }

        // Extract Multiple Phone Numbers
        val phones = mutableListOf<String>()
        lines.forEachIndexed { index, line ->
            if (!processedLineIndices.contains(index)) {
                val preprocessed = preprocessLineForPhoneNumber(line)
                if (isPhoneNumber(preprocessed)) {
                    phones.add(preprocessed)
                    processedLineIndices.add(index)
                }
            }
        }

        // Extract Website
        lines.forEachIndexed { index, line ->
            if (website.isEmpty() && !processedLineIndices.contains(index) && isWebsite(line)) {
                website = line
                processedLineIndices.add(index)
            }
        }

        // Extract Position and Company
        lines.forEachIndexed { index, line ->
            if (position.isEmpty() && !processedLineIndices.contains(index) && isPosition(line)) {
                position = line
                processedLineIndices.add(index)
            }
        }

        lines.forEachIndexed { index, line ->
            if (company.isEmpty() && !processedLineIndices.contains(index) && isPotentialCompany(line)) {
                company = line
                processedLineIndices.add(index)
            }
        }

        // Extract Name
        val remainingLines = lines.filterIndexed { index, _ -> !processedLineIndices.contains(index) }
        val potentialNames = remainingLines.filter { isPotentialName(it) }.sortedBy { it.length }

        for (pName in potentialNames) {
            if (name.isEmpty()) {
                name = pName
                processedLineIndices.add(lines.indexOf(pName))
                break
            }
        }

        if (name.isEmpty() && email.isNotEmpty()) {
            val emailPrefix = email.substringBefore("@").replace(".", " ").replace("_", " ").trim()
            if (emailPrefix.length in 3..30 && !emailPrefix.contains(Regex("[0-9]"))) {
                name = emailPrefix.split(" ").joinToString(" ") { it.capitalizeWords() }
            }
        }

        // Extract Address
        val addressCandidates = mutableListOf<String>()
        lines.forEachIndexed { index, line ->
            if (!processedLineIndices.contains(index) && isPotentialAddressLine(line)) {
                addressCandidates.add(line)
                processedLineIndices.add(index)
            }
        }

        address = processAddressLines(addressCandidates, company)

        // Return BusinessCard
        return BusinessCard(
            company = company.cleanCompanyName(),
            name = name.cleanName(),
            position = position.cleanPosition(),
            phones = phones.map { it.cleanPhoneNumber() }.filter { it.isNotBlank() },
            email = email.cleanEmail(),
            address = address.cleanAddress(),
            website = website.cleanWebsite(),
            notes = "",
            imagePath = imagePath
        )
    }


    private fun preprocessLineForPhoneNumber(line: String): String {
        // More aggressive cleaning for phone numbers: remove all non-digits except '+'
        return line.replace(Regex("[^0-9+]"), "")
            .replace(Regex("\\s+"), "") // Remove all spaces
            .trim()
    }

    private fun processAddressLines(addressLines: List<String>, company: String): String {
        // Filter out lines that might be company names or other info
        val filtered = addressLines.filter { line ->
            line.length > 5 && // Minimum length for an address line
                    !line.contains(Regex("www\\.|http")) && // Exclude websites
                    !isEmail(line) && // Exclude email addresses
                    !isPhoneNumber(line) && // Exclude phone numbers
                    !isPotentialName(line) && // Exclude potential names
                    !isPosition(line) && // Exclude positions
                    !COMPANY_SUFFIXES.any { suffix -> line.lowercase().contains(Regex("\\b$suffix\\b")) } // Exclude company suffixes
        }

        if (filtered.isEmpty()) return ""

        var bestAddressBlock = ""
        var maxAddressScore = -1

        // Iterate through all possible starting lines to find the best consecutive block
        for (i in filtered.indices) {
            var currentAddressBlock = mutableListOf<String>()
            var currentScore = 0
            for (j in i until filtered.size) {
                val line = filtered[j]
                val isAddressLike = isAddressSegment(line)

                if (isAddressLike) {
                    currentAddressBlock.add(line)
                    currentScore += line.length // Give higher score for longer address lines
                    if (line.contains(Regex("\\d"))) currentScore += 5 // Bonus for numbers
                    if (ADDRESS_INDICATORS.any { term -> line.contains(Regex("\\b$term\\b", RegexOption.IGNORE_CASE)) }) currentScore += 10 // Bonus for keywords
                    if (POSTAL_CODE_PATTERNS.any { pattern -> pattern.matcher(line).find() }) currentScore += 20 // Strong bonus for postal codes
                    if (CITY_STATE_INDICATORS.any { term -> line.contains(Regex("\\b$term\\b", RegexOption.IGNORE_CASE)) }) currentScore += 15 // Bonus for city/state
                } else {
                    // If a line is clearly not an address part but is short, include it to see if it bridges
                    // e.g., a short numerical building number
                    if (line.length < 10 && line.contains(Regex("\\d")) && !isEmail(line) && !isPhoneNumber(line)) {
                        currentAddressBlock.add(line)
                        currentScore += 2 // Small penalty but keeps the block
                    } else {
                        // This indicates a clear break in the address flow
                        break
                    }
                }
            }
            // Update best block if current is better
            if (currentScore > maxAddressScore && currentAddressBlock.joinToString("\n").trim().length > 10) { // Require minimum block length
                maxAddressScore = currentScore
                bestAddressBlock = currentAddressBlock.joinToString("\n").trim()
            }
        }

        // Final cleanup for the best identified block
        return bestAddressBlock
            .replace(company, "", ignoreCase = true) // Remove company name if present in address
            .replace(Regex("\\s{2,}"), " ") // Reduce multiple spaces
            .trim()
    }

    // Helper to assess if a single line is likely part of an address
    private fun isAddressSegment(line: String): Boolean {
        if (line.isBlank() || isLikelyNoise(line)) return false

        val hasNumber = line.contains(Regex("\\d"))
        val hasAddressKeyword = ADDRESS_INDICATORS.any { term ->
            line.contains(Regex("\\b$term\\b", RegexOption.IGNORE_CASE))
        }
        val hasPunctuation = line.contains(Regex("[,.#\\-\\/]")) // Including common address punctuation

        // A line is likely an address segment if it has numbers, OR keywords, OR significant punctuation.
        // Also, it shouldn't be too short if it's not just a number.
        return (hasNumber || hasAddressKeyword || hasPunctuation) &&
                line.length > 2 && // Min length
                !isEmail(line) && !isPhoneNumber(line) && !isWebsite(line) &&
                !isPotentialCompany(line) && !isPosition(line) && !isPotentialName(line)
    }


    private fun isLikelyNoise(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return true
        return trimmed.length <= 2 || // Very short lines are often noise
                trimmed.matches(Regex("[^a-zA-Z0-9\\s]{3,}")) || // Too many special chars
                trimmed.matches(Regex("^[\\W_]+$")) || // Only punctuation or symbols
                trimmed.matches(Regex("^[0-9\\s]{6,}$")) && !isPhoneNumber(trimmed) // Long sequence of just numbers/spaces not a phone
    }

    private fun isPotentialName(line: String): Boolean {
        val lowerLine = line.lowercase()
        // Check for honorifics or common name patterns
        if (HONORIFICS.any { it.toRegex(RegexOption.IGNORE_CASE).containsMatchIn(line) }) {
            return true
        }

        val words = line.split(" ")
        // A name usually has 2-5 words, 3-40 chars, with proper capitalization, no digits.
        return words.size in 2..5 &&
                line.length in 3..40 &&
                words.all { it.length > 1 && it[0].isUpperCase() } && // Ensure proper capitalization
                !line.contains(Regex("[0-9]")) &&
                !isEmail(line) &&
                !isPhoneNumber(line) &&
                !isWebsite(line) &&
                !isPosition(line) &&
                !isLikelyNoise(line) // Add noise check
    }

    private fun isPotentialCompany(line: String): Boolean {
        val lowerLine = line.lowercase()

        // Strong indicators: common company suffixes or abbreviations
        if (COMPANY_SUFFIXES.any { suffix -> lowerLine.contains(Regex("\\b$suffix\\b")) }) {
            return true
        }

        // Industry terms
        if (INDUSTRY_TERMS.any { term -> lowerLine.contains(term) }) {
            return true
        }

        // Check for company name patterns (multiple words with capitalization, not a person's name)
        val words = line.split(" ")
        val hasGoodCapitalization = words.count { it.length > 1 && it[0].isUpperCase() } >= words.size / 2

        return words.size in 1..8 && // Companies can have slightly longer names
                hasGoodCapitalization &&
                line.length > 5 &&
                !isEmail(line) &&
                !isPhoneNumber(line) &&
                !isWebsite(line) &&
                !isPosition(line) &&
                !isLikelyNoise(line) // Add noise check
    }



    private fun isPosition(line: String): Boolean {
        val lowerLine = line.lowercase()

        // Directly check for exact or near-exact matches of common positions (word boundaries crucial)
        if (POSITIONS.any { pos -> lowerLine.contains(Regex("\\b$pos\\b")) }) {
            return true
        }
        return false
    }

    private fun isPotentialAddressLine(line: String): Boolean {
        // Skip if it's clearly another type of info
        if (isEmail(line) || isPhoneNumber(line) || isWebsite(line) ||
            isPosition(line) || isPotentialCompany(line) || isPotentialName(line) || isLikelyNoise(line)) {
            return false
        }

        // Check for strong address indicators (numbers, street types, postal codes, P.O. Box)
        val hasAddressNumber = line.contains(Regex("\\b\\d+[a-zA-Z]?\\b")) || // E.g., "123B", "No. 45"
                line.contains(Regex("No\\.?\\s?\\d+", RegexOption.IGNORE_CASE)) ||
                POSTAL_CODE_PATTERNS.any { pattern -> pattern.matcher(line).find() } || // Specific postal codes
                line.contains(Regex("P\\.?O\\.?\\s?Box", RegexOption.IGNORE_CASE)) // P.O. Box

        val hasAddressTerm = ADDRESS_INDICATORS.any { term ->
            line.contains(Regex("\\b$term\\b", RegexOption.IGNORE_CASE))
        } || CITY_STATE_INDICATORS.any { term -> // Also consider city/state as strong address indicators
            line.contains(Regex("\\b$term\\b", RegexOption.IGNORE_CASE))
        }

        // Address lines often have commas or other specific punctuation
        val hasAddressPunctuation = line.contains(Regex("[,./#-]")) && line.split(" ").size > 1

        // A line is potentially an address line if it has numbers OR keywords OR specific punctuation.
        // Also, it should be of a reasonable length and not just a single, non-address word.
        return (hasAddressNumber || hasAddressTerm || hasAddressPunctuation) &&
                line.length > 5 && // Minimum length for a meaningful address line
                line.split(" ").size > 1 // Must have at least two words/components
    }

    private fun isEmail(line: String): Boolean {
        return EMAIL_PATTERN.matcher(line).matches()
    }

    private fun isPhoneNumber(line: String): Boolean {
        val cleanedLine = line.replace(Regex("[^0-9+]"), "")
        return PHONE_PATTERN.matcher(cleanedLine).matches() &&
                cleanedLine.replace("+", "").length >= 7 &&
                cleanedLine.replace("+", "").length <= 16
    }

    private fun isWebsite(line: String): Boolean {
        return WEBSITE_PATTERN.matcher(line).matches()
    }

    companion object {
        // --- REGEX PATTERNS ---
        internal val EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9-]+(?:\\.[a-zA-Z0-9-]+)*\\.[a-zA-Z]{2,63}$"
        )

        internal val PHONE_PATTERN = Pattern.compile(
            "^\\+?(\\d{1,4})?[\\s\\-.]?(\\(\\d{1,5}\\))?[\\s\\-.]?\\d{1,4}[\\s\\-.]?\\d{1,4}[\\s\\-.]?\\d{1,4}[\\s\\-.]?\\d{1,4}$"
        )

        internal val WEBSITE_PATTERN = Pattern.compile(
            "^(https?://)?(www\\.)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}(:\\d{1,5})?(/[-a-zA-Z0-9@:%_\\+.~#?&//=]*)?$"
        )

        internal val POSTAL_CODE_PATTERNS = listOf(
            Pattern.compile("\\b[A-Z]{1,2}\\d{1,2}[A-Z]?\\s?\\d[A-Z]{2}\\b", Pattern.CASE_INSENSITIVE), // UK Postcode
            Pattern.compile("\\b\\d{5}(-\\d{4})?\\b"), // US Zip code
            Pattern.compile("\\b\\d{6}\\b") // Indian Pincode
        )

        // --- KEYWORD LISTS ---
        internal val HONORIFICS = listOf(
            "mr\\.?","mrs\\.?","ms\\.?","dr\\.?","prof\\.?","sir\\b","shri\\b","smt\\b","er\\b"
        )

        internal val COMPANY_SUFFIXES = listOf(
            "inc\\b", "llc\\b", "corp\\b", "pvt\\b", "ltd\\b", "plc\\b", "gmbh\\b", "co\\b",
            "limited\\b", "corporation\\b", "enterprises\\b", "solutions\\b",
            "technologies\\b", "systems\\b", "consulting\\b", "group\\b", "holdings\\b",
            "प्रा\\. लि\\b", "लिमिटेड\\b", "प्राइवेट\\b", "पब्लिक\\b", "कंपनी\\b",
            "会社\\b", "株式会社\\b", "산업\\b", "그룹\\b", // Japanese, Korean
            "labs\\b", "studio\\b", "agency\\b", "incorp\\b", "partners\\b", "ventures\\b",
            "industries\\b", "constructions\\b", "factory\\b", "manufacturing\\b", "company\\b", "Company\\b"
        )

        internal val INDUSTRY_TERMS = listOf(
            "tech", "software", "bank", "financial", "capital", "insurance",
            "media", "network", "digital", "global", "international", "ventures",
            "retail", "healthcare", "education", "energy", "logistics", "automotive",
            "design", "marketing", "advertis", "real estate"
        )

        internal val POSITIONS = listOf(
            "manager", "director", "ceo", "cto", "cfo", "coo", "founder", "head",
            "president", "vp", "vice president", "executive", "partner", "lead",
            "specialist", "consultant", "engineer", "developer", "designer",
            "architect", "analyst", "officer", "administrator", "coordinator",
            "associate", "supervisor", "principal", "chief", "chairman", "secretary",
            "representative", "consultant", "agent", "analyst", "sales", "marketing",
            "hr", "human resources", "operations", "finance", "accounts", "legal",
            "research", "product", "community", "relations","Manager", "Solution", "solution"
        )

        internal val ADDRESS_INDICATORS = listOf(
            "street", "st\\b", "road", "rd\\b", "avenue", "ave\\b", "lane", "ln\\b",
            "drive", "dr\\b", "boulevard", "blvd\\b", "circle", "cir\\b", "place", "pl\\b",
            "floor", "fl\\b", "suite", "ste\\b", "apartment", "apt\\b", "building", "bldg\\b",
            "city", "state", "country", "zip", "postal", "pincode", "area", "district",
            "colony", "nagar", "village", "town", "sector", "block", "house", "no\\.", "flat",
            "मार्ग", "रोड", "सड़क", "गली", "स्ट्रीट", "क्षेत्र", "शहर", "राज्य", "देश",
            "தெரு", "சாலை", "வீதி", "மாவட்டம்", "ஊர்", "நகர்",
            "வீధి", "రోడ్", "స్ట్రీట్", "జిల్లా", "గ్రామం", "నగరం",
            "রোড", "স্ট্রিট", "গলি", "শহর", "জেলা", "গ্রাম",
            "도로", "가", "시", "구", "동", "번지", // Korean
            "丁目", "市", "区", "町", "村", "番地" // Japanese
        )

        internal val CITY_STATE_INDICATORS = listOf(
            "madurai", "chennai", "bengaluru", "mumbai", "delhi", "kolkata", // Indian cities
            "tamil nadu", "karnataka", "maharashtra", "delhi", "west bengal", // Indian states
            "tn", "ka", "mh", // Common state abbreviations
            "london", "new york", "tokyo", "seoul", "beijing", // International cities
            "ca", "ny", "tx", "fl", "il" // US state abbreviations
        )
    }
}

// --- EXTENSION FUNCTIONS (Clean-up and Capitalization) ---
private fun String.cleanName(): String {
    return this.replace(Regex("(?i)\\b(mr|mrs|ms|dr|prof|er|sir|shri|smt)\\.?\\s*"), "")
        .replace(Regex("[\\s,]+$"), "") // Remove trailing spaces/commas
        .trim()
        .split(" ")
        .joinToString(" ") { it.capitalizeWords() }
}

private fun String.cleanCompanyName(): String {
    // Aggressively remove common company suffixes if they are at the end of the string
    val cleaned = TextRecognizer.COMPANY_SUFFIXES.fold(this) { acc: String, suffix: String ->
        acc.replace(Regex("(?i)\\b${suffix}\\.?\\s*$", RegexOption.IGNORE_CASE), "")
    }.trim()
    return cleaned.split(" ")
        .joinToString(" ") { it.capitalizeWords() }
}

private fun String.cleanPosition(): String {
    return this.replace(Regex("[,.;]$"), "") // Remove trailing commas, periods, semicolons
        .trim()
        .split(" ")
        .joinToString(" ") { it.capitalizeWords() }
}

private fun String.cleanPhoneNumber(): String {
    // Remove all non-digits except a leading plus sign
    var cleaned = this.replace(Regex("[^0-9+]"), "")
    // Ensure only one leading '+' and no '+' in the middle
    if (cleaned.startsWith("+")) {
        cleaned = "+" + cleaned.replace(Regex("\\+"), "").trim()
    } else {
        cleaned = cleaned.replace(Regex("\\+"), "").trim()
    }
    return cleaned
}

private fun String.cleanEmail(): String {
    // Ensure email is lowercase and trimmed
    return this.trim().lowercase()
}

private fun String.cleanAddress(): String {
    return this.replace(Regex("\\s{2,}"), " ") // Reduce multiple spaces
        .replace(Regex("(?i)\\b(bldg|building)\\.?\\s*"), "Building ")
        .replace(Regex("(?i)\\b(fl|floor)\\.?\\s*"), "Floor ")
        .replace(Regex("(?i)\\b(apt|apartment)\\.?\\s*"), "Apartment ")
        .replace(Regex("(?i)\\b(ste|suite)\\.?\\s*"), "Suite ")
        .trim()
        .split("\n")
        .filter { it.isNotBlank() }
        .joinToString("\n") { it.capitalizeWords() }
}

private fun String.cleanWebsite(): String {
    return this.replace(Regex("^https?://"), "")
        .replace(Regex("/+$"), "") // Remove trailing slashes
        .trim()
        .lowercase()
}

// Helper extension function for capitalization
private fun String.capitalizeWords(): String =
    lowercase().split(" ", "-").joinToString(" ") { word -> // Split by space and hyphen
        word.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }