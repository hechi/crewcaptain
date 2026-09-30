package com.peoplemanager.domain.service

/** A keyword/phrase and how many response texts it appeared in. */
data class ThemeCount(
    val theme: String,
    val count: Int
)

/**
 * Deterministic, AI-free keyword frequency extraction over free-text feedback.
 *
 * Tokenizes text, drops a small English stopword list and very short tokens, and counts
 * how many distinct texts each remaining word appears in (document frequency, so a single
 * ranty response can't dominate). Returns the most common themes.
 *
 * Pure domain logic — no framework or AI dependency.
 */
object ThemeExtractor {

    private val STOPWORDS: Set<String> = setOf(
        "the", "and", "for", "are", "but", "not", "you", "your", "with", "this", "that",
        "have", "has", "had", "was", "were", "will", "would", "should", "could", "can",
        "they", "them", "their", "there", "here", "when", "what", "which", "who", "whom",
        "how", "why", "from", "into", "onto", "over", "under", "about", "some", "any",
        "all", "very", "more", "most", "much", "many", "such", "than", "then", "also",
        "just", "like", "well", "good", "great", "really", "quite", "been", "being",
        "does", "did", "done", "get", "got", "his", "her", "him", "she", "its", "our",
        "out", "off", "too", "own", "per", "via", "yet", "him", "himself", "herself",
        "themselves", "myself", "yourself", "ourselves", "it's", "i've", "we've",
        "he's", "she's", "isn't", "don't", "doesn't", "didn't", "can't", "won't",
        "person", "people", "team", "work", "working", "always", "often", "sometimes",
        "one", "two", "three", "would", "could", "might", "must", "shall"
    )

    fun extract(texts: List<String>, maxThemes: Int = 10): List<ThemeCount> {
        if (texts.isEmpty() || maxThemes <= 0) return emptyList()

        val counts = mutableMapOf<String, Int>()
        for (text in texts) {
            val words = tokenize(text)
            // Count each distinct word once per text (document frequency).
            for (word in words.toSet()) {
                counts[word] = (counts[word] ?: 0) + 1
            }
        }

        return counts.entries
            .asSequence()
            .filter { it.value >= 1 }
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(maxThemes)
            .map { ThemeCount(it.key, it.value) }
            .toList()
    }

    private fun tokenize(text: String): List<String> =
        text.lowercase()
            .split(Regex("[^\\p{L}']+"))
            .map { it.trim('\'') }
            .filter { it.length >= 4 && it !in STOPWORDS && !it.all { c -> c.isDigit() } }
}
