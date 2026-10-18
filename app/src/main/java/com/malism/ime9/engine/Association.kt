package com.malism.ime9.engine

class Association(private val lexicon: Lexicon) {
    fun suggest(prev: String, chinese: Boolean, limit: Int = 16): List<Candidate> {
        if (prev.isEmpty()) {
            return if (chinese) lexicon.zhHighFreq.take(limit) else emptyList()
        }
        val table = if (chinese) lexicon.assocZh else lexicon.assocEn
        val key = if (chinese) prev else prev.lowercase()
        val hits = table[key].orEmpty().take(limit).map { Candidate(it.first, it.second) }
        if (hits.isNotEmpty()) return hits
        return if (chinese) lexicon.zhHighFreq.take(limit) else emptyList()
    }

    fun suggestTable(
        prev: String,
        table: Map<String, List<Pair<String, Int>>>,
        fold: Boolean,
        limit: Int = 16,
    ): List<Candidate> {
        if (prev.isEmpty()) return emptyList()
        val key = if (fold) prev.lowercase() else prev
        return table[key].orEmpty().take(limit).map { Candidate(it.first, it.second) }
    }
}
