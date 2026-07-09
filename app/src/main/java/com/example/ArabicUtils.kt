package com.example

object ArabicUtils {

    fun normalizeArabic(text: String): String {
        return text.lowercase()
            .replace("[\u064B-\u0652\u0670]".toRegex(), "") // Strip all Arabic diacritics (harakat/tashkeel)
            .replace("[إأآا]".toRegex(), "ا")
            .replace("[ىي]".toRegex(), "ي")
            .replace("[ةه]".toRegex(), "ه")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    fun normalizeWord(word: String): String {
        var w = word.lowercase()
            .replace("[\u064B-\u0652\u0670]".toRegex(), "") // Strip diacritics
            .replace("[إأآا]".toRegex(), "ا")
            .replace("[ىي]".toRegex(), "ي")
            .replace("[ةه]".toRegex(), "ه")
        
        // Remove definite article "ال" if the word is long enough
        if (w.startsWith("ال") && w.length > 3) {
            w = w.substring(2)
        }
        
        // Strip trailing letters (h/t/y) that often change with typos/grammar
        if (w.length >= 3 && (w.endsWith("ه") || w.endsWith("ي"))) {
            w = w.substring(0, w.length - 1)
        }
        return w
    }

    /**
     * Checks if the fullText contains the query using robust Arabic word stemming.
     */
    fun matches(fullText: String, query: String): Boolean {
        val normFull = normalizeArabic(fullText)
        val normQuery = normalizeArabic(query)
        if (normFull == normQuery) {
            return true
        }

        // Tokenize and check for stemmed matches
        val fullWords = normFull.split(" ").map { normalizeWord(it) }.filter { it.isNotEmpty() }
        val queryWords = normQuery.split(" ").map { normalizeWord(it) }.filter { it.isNotEmpty() }

        if (queryWords.isEmpty() || fullWords.isEmpty()) return false

        // Search for the queryWords sequence inside fullWords
        for (i in 0..fullWords.size - queryWords.size) {
            var match = true
            for (j in queryWords.indices) {
                if (fullWords[i + j] != queryWords[j]) {
                    match = false
                    break
                }
            }
            if (match) return true
        }

        return false
    }
}
