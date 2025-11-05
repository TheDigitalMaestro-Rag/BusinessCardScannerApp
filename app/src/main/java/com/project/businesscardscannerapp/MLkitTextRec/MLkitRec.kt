package com.project.businesscardscannerapp.MLkitTextRec

import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer as MLKitRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.google.mlkit.vision.text.latin.TextRecognizerOptions as LatinTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Pattern

// Data class to preserve bounding box information
data class TextLine(
    val text: String,
    val boundingBox: Rect? = null,
    val confidence: Float = 0.0f
)

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
                }
            }
        }
    }

    // Improved OCR with bounding box preservation
    suspend fun processImageWithBoundingBox(image: InputImage): List<TextLine> = coroutineScope {
        initializeRecognizers()

        withContext(Dispatchers.IO) {
            val currentLatin = latin ?: return@withContext emptyList()

            try {
                val result = runCatching { currentLatin.process(image).await() }
                if (result.isSuccess) {
                    val textLines = mutableListOf<TextLine>()
                    result.getOrThrow().textBlocks.forEach { block ->
                        block.lines.forEach { line ->
                            textLines.add(TextLine(
                                text = line.text.trim(),
                                boundingBox = line.boundingBox,
                                confidence = line.confidence ?: 0.0f
                            ))
                        }
                    }
                    return@withContext textLines
                } else {
                    Log.e("TextRecognizer", "OCR failed", result.exceptionOrNull())
                    return@withContext emptyList()
                }
            } catch (e: Exception) {
                Log.e("TextRecognizer", "OCR failed", e)
                return@withContext emptyList()
            }
        }
    }

    // Backward compatibility
    suspend fun processImage(image: InputImage): String {
        val textLines = processImageWithBoundingBox(image)
        return textLines.joinToString("\n") { it.text }
    }

    fun extractBusinessCardInfo(textLines: List<TextLine>, imagePath: String? = null): BusinessCard {
        val filteredLines = textLines
            .map { it.text.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { isLikelyNoise(it) }

        return extractBusinessCardInfo(filteredLines.joinToString("\n"), imagePath)
    }

    fun extractBusinessCardInfo(text: String, imagePath: String? = null): BusinessCard {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { isLikelyNoise(it) }

        Log.d("TextRecognizer", "RAW LINES: ${lines.joinToString(" | ")}")

        var name = ""
        var company = ""
        var position = ""
        var email = ""
        var address = ""
        var website = ""

        val processedLineIndices = mutableSetOf<Int>()

        // STEP 1: Extract Email (HIGHEST PRIORITY)
        lines.forEachIndexed { index, line ->
            if (email.isEmpty()) {
                val emailFound = extractEmailFromLine(line)
                if (emailFound.isNotEmpty()) {
                    email = emailFound
                    processedLineIndices.add(index)
                    Log.d("TextRecognizer", "Found email: $email")
                }
            }
        }

        // STEP 2: Extract Phone Numbers (HIGH PRIORITY)
        val phones = mutableListOf<String>()
        lines.forEachIndexed { index, line ->
            if (!processedLineIndices.contains(index)) {
                val phoneFound = extractPhoneFromLine(line)
                if (phoneFound.isNotEmpty()) {
                    phones.add(phoneFound)
                    processedLineIndices.add(index)
                    Log.d("TextRecognizer", "Found phone: $phoneFound")
                }
            }
        }

        // STEP 3: Extract Position (MEDIUM PRIORITY)
        lines.forEachIndexed { index, line ->
            if (position.isEmpty() && !processedLineIndices.contains(index)) {
                if (isPosition(line)) {
                    position = line
                    processedLineIndices.add(index)
                    Log.d("TextRecognizer", "Found position: $position")
                }
            }
        }

        // STEP 4: Extract Name - IMPROVED LOGIC
        // First, try to find name patterns like "s.selvakumar"
        lines.forEachIndexed { index, line ->
            if (name.isEmpty() && !processedLineIndices.contains(index)) {
                val nameCandidate = extractNameFromLine(line)
                if (nameCandidate.isNotEmpty()) {
                    name = nameCandidate
                    processedLineIndices.add(index)
                    Log.d("TextRecognizer", "Found name: $name")
                }
            }
        }

        // STEP 5: Extract Company - IMPROVED LOGIC
        lines.forEachIndexed { index, line ->
            if (company.isEmpty() && !processedLineIndices.contains(index)) {
                val companyCandidate = extractCompanyFromLine(line)
                if (companyCandidate.isNotEmpty()) {
                    company = companyCandidate
                    processedLineIndices.add(index)
                    Log.d("TextRecognizer", "Found company: $company")
                }
            }
        }

        // STEP 6: Extract Website - ONLY REAL WEBSITES
        lines.forEachIndexed { index, line ->
            if (website.isEmpty() && !processedLineIndices.contains(index)) {
                if (isRealWebsite(line)) {
                    website = line
                    processedLineIndices.add(index)
                    Log.d("TextRecognizer", "Found website: $website")
                }
            }
        }

        // STEP 7: Extract Address - IMPROVED
        address = extractAddress(lines, processedLineIndices)

        // STEP 8: FALLBACKS FOR MISSING FIELDS

        // Fallback for Name: Extract from email if still empty
        if (name.isEmpty() && email.isNotEmpty()) {
            name = extractNameFromEmail(email)
            Log.d("TextRecognizer", "Extracted name from email: $name")
        }

        // Fallback for Company: Look for enterprise patterns
        if (company.isEmpty()) {
            lines.forEachIndexed { index, line ->
                if (!processedLineIndices.contains(index)) {
                    if (line.contains("enterprises", ignoreCase = true) ||
                        line.contains("s.v.", ignoreCase = true) ||
                        line.matches(Regex(".*[A-Z]\\.[A-Z]\\..*", RegexOption.IGNORE_CASE))) {
                        company = line
                        processedLineIndices.add(index)
                        Log.d("TextRecognizer", "Fallback company: $company")
                    }
                }
            }
        }

        // STEP 9: CLEANUP AND VALIDATION

        // If company contains name, clean it up
        if (company.contains(name, ignoreCase = true) && name.length > 2) {
            company = company.replace(name, "", ignoreCase = true).trim()
        }

        // If name looks like a location, clear it
        if (isLocationName(name)) {
            name = ""
        }

        Log.d("TextRecognizer", "FINAL RESULTS:")
        Log.d("TextRecognizer", "Name: '$name'")
        Log.d("TextRecognizer", "Company: '$company'")
        Log.d("TextRecognizer", "Position: '$position'")
        Log.d("TextRecognizer", "Email: '$email'")
        Log.d("TextRecognizer", "Website: '$website'")
        Log.d("TextRecognizer", "Address: '$address'")
        Log.d("TextRecognizer", "Phones: ${phones.joinToString()}")

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

    // NEW: Improved email extraction
    private fun extractEmailFromLine(line: String): String {
        // Direct email pattern match
        if (isEmail(line)) {
            return line
        }

        // Look for email patterns within text
        val emailRegex = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
        val match = emailRegex.find(line)
        return match?.value ?: ""
    }

    // NEW: Improved phone extraction
    private fun extractPhoneFromLine(line: String): String {
        val cleaned = line.replace(Regex("[^0-9+]"), "").replace(Regex("\\s+"), "").trim()
        if (isPhoneNumber(cleaned)) {
            return cleaned
        }
        return ""
    }

    // NEW: Improved name extraction
    private fun extractNameFromLine(line: String): String {
        // Skip if it's clearly not a name
        if (line.length < 3 || line.length > 40) return ""
        if (line.contains(Regex("[0-9]")) || isEmail(line) || isPhoneNumber(line) || isRealWebsite(line)) return ""

        // Pattern for "s.selvakumar" or similar
        if (line.matches(Regex("^[a-zA-Z]\\.?[a-zA-Z]?\\.[a-zA-Z]{3,}$")) ||
            line.matches(Regex("^[a-zA-Z]\\.?[a-zA-Z]?\\s+[a-zA-Z]{3,}$"))) {
            return line
        }

        // Check for proper name characteristics
        val words = line.split(" ").filter { it.isNotBlank() }
        if (words.size in 1..3) {
            val capitalizedWords = words.count { it.length > 1 && it[0].isUpperCase() }
            if (capitalizedWords >= words.size / 2) {
                return line
            }
        }

        return ""
    }

    // NEW: Extract name from email
    private fun extractNameFromEmail(email: String): String {
        val prefix = email.substringBefore("@")
            .replace(Regex("[0-9_]"), " ") // Remove numbers and underscores
            .replace(".", " ") // Replace dots with spaces
            .trim()

        if (prefix.length in 3..30) {
            return prefix.split(" ")
                .joinToString(" ") { it.capitalizeWords() }
        }
        return ""
    }

    // NEW: Improved company extraction
    private fun extractCompanyFromLine(line: String): String {
        // Skip if too short or clearly not a company
        if (line.length < 3 || line.length > 60) return ""
        if (isEmail(line) || isPhoneNumber(line) || isRealWebsite(line)) return ""

        val lowerLine = line.lowercase()

        // Strong company indicators
        if (COMPANY_SUFFIXES.any { lowerLine.contains(Regex("\\b$it\\b")) }) {
            return line
        }

        // Specific pattern for "S.V.Enterprises"
        if (line.matches(Regex(".*[A-Z]\\.[A-Z]\\..*", RegexOption.IGNORE_CASE)) ||
            line.contains("Enterprises", ignoreCase = true)) {
            return line
        }

        // Company name patterns with capitalization
        val words = line.split(" ").filter { it.isNotBlank() }
        if (words.size in 1..5) {
            val capitalizedWords = words.count { it.length > 1 && it[0].isUpperCase() }
            if (capitalizedWords >= words.size / 2) {
                return line
            }
        }

        return ""
    }

    // NEW: Strict website detection
    private fun isRealWebsite(line: String): Boolean {
        // Only accept real website patterns, not name patterns
        if (line.matches(Regex("^[a-zA-Z]\\.?[a-zA-Z]?\\.[a-zA-Z]{3,}$"))) {
            // This looks more like a name pattern (s.selvakumar)
            return false
        }

        return WEBSITE_PATTERN.matcher(line).matches() ||
                line.contains(Regex("^www\\.[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}"))
    }

    // NEW: Simplified address extraction
    private fun extractAddress(lines: List<String>, processedIndices: MutableSet<Int>): String {
        val addressLines = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            if (!processedIndices.contains(index) && isPotentialAddressLine(line)) {
                addressLines.add(line)
                processedIndices.add(index)
            }
        }

        return addressLines.joinToString("\n")
    }

    // IMPROVED: Address line detection
    private fun isPotentialAddressLine(line: String): Boolean {
        if (line.length < 10) return false
        if (isEmail(line) || isPhoneNumber(line) || isRealWebsite(line)) return false
        if (isPosition(line) || isPotentialCompany(line) || isPotentialName(line)) return false

        // Strong address indicators
        val hasNumber = line.contains(Regex("\\b\\d+\\b"))
        val hasStreetTerms = line.contains(Regex("street|st\\b|road|rd\\b|nagar|avenue", RegexOption.IGNORE_CASE))
        val hasCity = line.contains(Regex("chennai|city|town", RegexOption.IGNORE_CASE))

        return hasNumber && (hasStreetTerms || hasCity)
    }

    // IMPROVED: Name detection
    private fun isPotentialName(line: String): Boolean {
        if (line.length < 3 || line.length > 40) return false
        if (line.contains(Regex("[0-9]")) || isEmail(line) || isPhoneNumber(line) || isRealWebsite(line)) return false

        // Pattern for name with initials
        if (line.matches(Regex("^[A-Za-z]\\.?[A-Za-z]?\\.[A-Za-z]{3,}$"))) return true

        val words = line.split(" ").filter { it.isNotBlank() }
        return words.size in 1..3 && words.all { it.length > 1 }
    }

// Keep the rest of your existing helper functions the same...

    // NEW: Check if a string looks like a location name rather than person name
    private fun isLocationName(text: String): Boolean {
        if (text.length < 3) return false

        val lowerText = text.lowercase()
        return LOCATION_INDICATORS.any { lowerText.contains(it) } ||
                text.contains(",") || // Locations often have commas
                text.split(" ").any { it.length > 10 } // Long words are often locations
    }

    // NEW: Find a better name candidate
    private fun findBetterName(lines: List<String>, processedIndices: MutableSet<Int>, currentName: String): String {
        lines.forEachIndexed { index, line ->
            if (!processedIndices.contains(index) &&
                !isLocationName(line) &&
                isPotentialName(line) &&
                line != currentName) {
                return line
            }
        }
        return ""
    }

    // NEW: Improved name pattern detection for "s.selvakumar" or "S. Selvakumar"
    private fun isNamePattern(line: String): Boolean {
        if (line.length < 5 || line.length > 30) return false
        if (line.contains(Regex("[0-9]")) || isEmail(line) || isPhoneNumber(line) || isWebsite(line)) return false

        // Pattern for "s.selvakumar" or "S. Selvakumar"
        val namePattern1 = Regex("^[A-Za-z]\\.?\\s?[A-Za-z]{3,}$")
        val namePattern2 = Regex("^[A-Za-z]\\.?[A-Za-z]?\\.?\\s?[A-Za-z]{3,}$")

        return namePattern1.matches(line) || namePattern2.matches(line)
    }

    // NEW: Improved website extraction function
    private fun extractWebsiteFromLine(line: String): String {
        // First try exact website pattern
        if (isWebsite(line)) {
            return line
        }

        // Don't treat name patterns as websites
        if (isNamePattern(line) || isPotentialName(line)) {
            return ""
        }

        return ""
    }

    // IMPROVED ADDRESS EXTRACTION WITH BETTER HEURISTICS
    private fun extractAddressAdvanced(lines: List<String>, processedIndices: MutableSet<Int>, company: String): String {
        val addressCandidates = mutableListOf<String>()

        // First pass: collect all potential address lines
        lines.forEachIndexed { index, line ->
            if (!processedIndices.contains(index) && isPotentialAddressLine(line)) {
                addressCandidates.add(line)
            }
        }

        if (addressCandidates.isEmpty()) return ""

        // Group consecutive address lines (assuming they're in order)
        val addressBlocks = mutableListOf<MutableList<String>>()
        var currentBlock = mutableListOf<String>()

        lines.forEachIndexed { index, line ->
            if (!processedIndices.contains(index) && isPotentialAddressLine(line)) {
                if (currentBlock.isEmpty() || isConsecutiveAddressLine(currentBlock.last(), line)) {
                    currentBlock.add(line)
                } else {
                    if (currentBlock.isNotEmpty()) {
                        addressBlocks.add(currentBlock)
                    }
                    currentBlock = mutableListOf(line)
                }
            }
        }
        if (currentBlock.isNotEmpty()) {
            addressBlocks.add(currentBlock)
        }

        // Score each address block and pick the best one
        var bestAddress = ""
        var bestScore = -1

        addressBlocks.forEach { block ->
            val blockText = block.joinToString("\n")
            val score = calculateAddressScore(blockText)

            if (score > bestScore) {
                bestScore = score
                bestAddress = blockText
            }
        }

        // Also consider individual high-scoring lines
        addressCandidates.forEach { candidate ->
            val score = calculateAddressScore(candidate)
            if (score > bestScore && score > 15) {
                bestScore = score
                bestAddress = candidate
            }
        }

        // Mark processed indices for the best address
        if (bestAddress.isNotBlank() && bestScore >= 10) {
            lines.forEachIndexed { index, line ->
                if (bestAddress.contains(line) && !processedIndices.contains(index)) {
                    processedIndices.add(index)
                }
            }
        }

        return bestAddress
    }

    private fun isConsecutiveAddressLine(prevLine: String, currentLine: String): Boolean {
        val prevHasNumber = prevLine.contains(Regex("\\d"))
        val currentHasNumber = currentLine.contains(Regex("\\d"))
        val prevHasAddressTerm = ADDRESS_INDICATORS.any { prevLine.contains(Regex(it, RegexOption.IGNORE_CASE)) }
        val currentHasAddressTerm = ADDRESS_INDICATORS.any { currentLine.contains(Regex(it, RegexOption.IGNORE_CASE)) }

        return (prevHasNumber && currentHasNumber) ||
                (prevHasAddressTerm && currentHasAddressTerm) ||
                Math.abs(prevLine.length - currentLine.length) < 10
    }

    private fun calculateAddressScore(addressText: String): Int {
        var score = 0

        // Strong indicators
        if (POSTAL_CODE_PATTERNS.any { pattern -> pattern.matcher(addressText).find() }) {
            score += 25
        }

        // Address structure indicators
        if (addressText.contains(Regex("\\b\\d+[a-zA-Z]?\\s*([A-Za-z]+\\b|$)"))) {
            score += 15
        }

        // Address keywords
        ADDRESS_INDICATORS.forEach { indicator ->
            if (addressText.contains(Regex("\\b$indicator\\b", RegexOption.IGNORE_CASE))) {
                score += 8
            }
        }

        // City/state indicators
        CITY_STATE_INDICATORS.forEach { indicator ->
            if (addressText.contains(Regex("\\b$indicator\\b", RegexOption.IGNORE_CASE))) {
                score += 10
            }
        }

        // Punctuation common in addresses
        if (addressText.contains(Regex("[,.#\\-\\/]"))) {
            score += 5
        }

        // Length bonus
        if (addressText.length > 15) score += 3
        if (addressText.length > 25) score += 5

        // Multi-line bonus
        if (addressText.contains("\n")) score += 10

        // Penalties for non-address content
        if (addressText.contains(Regex("(?i)email|phone|www|\\.com|@|http"))) {
            score -= 20
        }

        if (addressText.contains(Regex("(?i)manager|director|ceo|company|ltd|inc"))) {
            score -= 15
        }

        return score
    }

    // IMPROVED HELPER FUNCTIONS

    private fun preprocessLineForPhoneNumber(line: String): String {
        return line.replace(Regex("[^0-9+]"), "").replace(Regex("\\s+"), "").trim()
    }

    private fun isLikelyNoise(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) return true
        if (trimmed.matches(Regex("^[^a-zA-Z0-9]{2,}$"))) return true
        if (trimmed.matches(Regex("^[\\d\\W]+$")) && !isPhoneNumber(trimmed)) return true
        if (trimmed.length <= 2) return true
        return false
    }

    private fun isPotentialCompany(line: String): Boolean {
        if (line.length < 3) return false
        if (isEmail(line) || isPhoneNumber(line) || isWebsite(line)) return false

        val lowerLine = line.lowercase()

        // Strong company indicators
        if (COMPANY_SUFFIXES.any { lowerLine.contains(Regex("\\b$it\\b")) }) return true
        if (INDUSTRY_TERMS.any { lowerLine.contains(it) }) return true

        // Company name patterns
        val words = line.split(" ").filter { it.isNotBlank() }
        if (words.size > 6) return false

        // Companies often have abbreviations or specific patterns
        if (line.contains(Regex("[A-Z]{2,}")) || line.contains("&") || line.contains("/")) return true

        return false
    }

    private fun isPosition(line: String): Boolean {
        if (line.length < 3) return false
        val lowerLine = line.lowercase()
        return POSITIONS.any { lowerLine.contains(Regex("\\b$it\\b")) }
    }


    private fun isEmail(line: String): Boolean {
        return EMAIL_PATTERN.matcher(line).matches()
    }

    private fun isPhoneNumber(line: String): Boolean {
        val cleanedLine = line.replace(Regex("[^0-9+]"), "")
        return PHONE_PATTERN.matcher(cleanedLine).matches() &&
                cleanedLine.replace("+", "").length in 7..15
    }

    private fun isWebsite(line: String): Boolean {
        return WEBSITE_PATTERN.matcher(line).matches() ||
                line.contains(Regex("^www\\.[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}"))
    }

    companion object {
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
            Pattern.compile("\\b[A-Z]{1,2}\\d{1,2}[A-Z]?\\s?\\d[A-Z]{2}\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\b\\d{5}(-\\d{4})?\\b"),
            Pattern.compile("\\b\\d{6}\\b"),
            Pattern.compile("\\b\\d{3}-\\d{4}\\b")
        )

        internal val HONORIFICS = listOf(
            "mr\\.?", "mrs\\.?", "ms\\.?", "dr\\.?", "prof\\.?", "sir\\b", "shri\\b", "smt\\b", "er\\b"
        )

        internal val COMPANY_SUFFIXES = listOf(
            "inc\\b", "llc\\b", "corp\\b", "pvt\\b", "ltd\\b", "plc\\b", "gmbh\\b", "co\\b",
            "limited\\b", "corporation\\b", "enterprises\\b", "solutions\\b",
            "technologies\\b", "systems\\b", "consulting\\b", "group\\b", "holdings\\b",
            "company\\b", "Company\\b"
        )

        internal val INDUSTRY_TERMS = listOf(
            "tech", "software", "bank", "financial", "capital", "insurance",
            "media", "network", "digital", "global", "international", "ventures"
        )

        internal val POSITIONS = listOf(
            "manager", "director", "ceo", "cto", "cfo", "coo", "founder", "head",
            "president", "vp", "vice president", "executive", "partner", "lead",
            "specialist", "consultant", "engineer", "developer", "designer"
        )

        internal val ADDRESS_INDICATORS = listOf(
            "street", "st\\b", "road", "rd\\b", "avenue", "ave\\b", "lane", "ln\\b",
            "drive", "dr\\b", "boulevard", "blvd\\b", "circle", "cir\\b",
            "floor", "fl\\b", "suite", "ste\\b", "apartment", "apt\\b", "building", "bldg\\b",
            "city", "state", "country", "zip", "postal", "pincode", "area", "district",
            "colony", "nagar", "village", "town", "sector", "block", "house", "no\\.", "flat",
            "address", "location", "plot", "sector", "phase"
        )

        internal val CITY_STATE_INDICATORS = listOf(
            "chennai", "kolathur", "puthagaram", "madurai", "bengaluru", "mumbai", "delhi",
            "tamil nadu", "karnataka", "maharashtra", "tn", "ka", "mh"
        )

        // NEW: Location indicators to avoid mistaking locations for names
        internal val LOCATION_INDICATORS = listOf(
            "puthagaram", "kolathur", "chennai", "madurai", "street", "road", "nagar",
            "area", "district", "city", "state", "country"
        )
    }
}

// Extension functions remain the same
private fun String.cleanName(): String {
    return this.replace(Regex("(?i)\\b(mr|mrs|ms|dr|prof|er|sir|shri|smt)\\.?\\s*"), "")
        .replace(Regex("[\\s,]+$"), "")
        .trim()
        .split(" ")
        .joinToString(" ") { it.capitalizeWords() }
}

private fun String.cleanCompanyName(): String {
    val cleaned = TextRecognizer.COMPANY_SUFFIXES.fold(this) { acc: String, suffix: String ->
        acc.replace(Regex("(?i)\\b${suffix}\\.?\\s*$", RegexOption.IGNORE_CASE), "")
    }.trim()
    return cleaned.split(" ")
        .joinToString(" ") { it.capitalizeWords() }
}

private fun String.cleanPosition(): String {
    return this.replace(Regex("[,.;]$"), "")
        .trim()
        .split(" ")
        .joinToString(" ") { it.capitalizeWords() }
}

private fun String.cleanPhoneNumber(): String {
    var cleaned = this.replace(Regex("[^0-9+]"), "")
    if (cleaned.startsWith("+")) {
        cleaned = "+" + cleaned.replace(Regex("\\+"), "").trim()
    } else {
        cleaned = cleaned.replace(Regex("\\+"), "").trim()
    }
    return cleaned
}

private fun String.cleanEmail(): String {
    return this.trim().lowercase()
}

private fun String.cleanAddress(): String {
    return this.replace(Regex("\\s{2,}"), " ")
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
        .replace(Regex("/+$"), "")
        .trim()
        .lowercase()
}

private fun String.capitalizeWords(): String =
    lowercase().split(" ", "-").joinToString(" ") { word ->
        word.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }