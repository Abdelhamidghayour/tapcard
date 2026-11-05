package com.tapcard.app.data.ocr

/**
 * Pure, stateless extraction of business-card fields from raw OCR text
 * (Google ML Kit text recognition output). Deterministic and unit-tested:
 * the editor always lets the user review/correct the prefill.
 */
object OcrFieldParser {

    data class ParsedFields(
        val name: String? = null,
        val jobTitle: String? = null,
        val company: String? = null,
        val phone: String? = null,
        val email: String? = null,
        val website: String? = null,
        val address: String? = null,
    )

    private val emailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val urlRegex = Regex("(?:https?://)?(?:www\\.)?[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+")
    private val phoneRegex = Regex("(?:\\+?\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2,4}\\)?[\\s.-]?){2,3}\\d{2,4}")

    private val roleWords = listOf(
        "ceo", "cto", "coo", "cfo", "founder", "manager", "director", "designer",
        "engineer", "developer", "consultant", "partner", "president", "officer",
        "architect", "lead", "specialist", "analyst", "head of", "vp ",
    )
    private val companyWords = listOf(
        " llc", " inc.", " inc,", " inc ", " inc", "ltd", " gmbh", "b.v.", " studio",
        " labs", " group", " solutions", " consulting", " associates", " agency",
        " partners", " holdings", "& co", " company", " enterprises",
    )

    fun parse(rawText: String): ParsedFields {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return ParsedFields()

        // 1) Extract unambiguous contact tokens first.
        var email: String? = null
        var phone: String? = null
        var website: String? = null
        val leftover = mutableListOf<String>()

        for (line in lines) {
            val e = emailRegex.find(line)
            val p = if (e == null) findPhone(line) else null
            val u = if (e == null && p == null) findUrl(line) else null
            when {
                e != null && email == null -> email = e.value
                p != null && phone == null -> phone = p
                u != null && website == null -> website = normalizeUrl(u)
                else -> leftover.add(line)
            }
        }

        // 2) Classify remaining lines: role / company / address / name.
        val roleLines = mutableListOf<String>()
        val companyLines = mutableListOf<String>()
        val addressLines = mutableListOf<String>()
        val nameLines = mutableListOf<String>()

        for (line in leftover) {
            val lower = line.lowercase()
            val digits = line.count { it.isDigit() }
            when {
                line.containsAny(roleWords) && line.split(" ").size <= 8 -> roleLines.add(line)
                line.containsAny(companyWords) -> companyLines.add(line)
                line.isSingleWordBrand() -> companyLines.add(line)
                digits >= 3 || lower.containsAny(
                    listOf("street", "st.", "ave", "road", "suite", "floor", "p.o.", "po box"),
                ) -> addressLines.add(line)
                else -> nameLines.add(line)
            }
        }

        // 3) Prefer short alphabetic first lines as the person's name.
        val name = nameLines.firstOrNull { line ->
            val words = line.split(Regex("\\s+"))
            words.size in 1..4 && line.none { it.isDigit() }
        } ?: nameLines.firstOrNull()

        val jobTitle = roleLines.firstOrNull()
            ?: nameLines.getOrNull(1)?.takeIf { it != name && it.split(" ").size <= 6 }

        return ParsedFields(
            name = name,
            jobTitle = jobTitle,
            company = companyLines.firstOrNull(),
            phone = phone,
            email = email,
            website = website,
            address = addressLines.joinToString(", ").ifBlank { null },
        )
    }

    private fun findPhone(line: String): String? {
        // Guard against matching digits inside URLs/emails and garbage digit runs.
        if (emailRegex.containsMatchIn(line) || urlRegex.containsMatchIn(line)) return null
        if (line.count { it.isDigit() } !in 7..16) return null
        val candidate = phoneRegex.find(line)?.value ?: return null
        val digitCount = candidate.count { it.isDigit() }
        return if (digitCount in 7..15) candidate else null
    }

    private fun findUrl(line: String): String? {
        val match = urlRegex.find(line) ?: return null
        val value = match.value.lowercase()
        val tld = value.substringAfterLast('.', "")
        return if (tld.length in 2..6 && tld.all { it.isLetter() }) match.value else null
    }

    private fun normalizeUrl(url: String): String =
        if (url.startsWith("http")) url else "https://$url"

    private fun String.isSingleWordBrand(): Boolean =
        !contains(' ') && uppercase() == this && length in 4..30 &&
            any { it.isLetter() } && !containsAny(companyWords)

    private fun String.containsAny(needles: List<String>): Boolean =
        needles.any { contains(it, ignoreCase = true) }
}
