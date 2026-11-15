package com.malism.ime9.engine

class Association(private val lexicon: Lexicon) {
    fun suggest(prev: String, chinese: Boolean, limit: Int = 48): List<Candidate> {
        if (prev.isEmpty()) {
            return if (chinese) lexicon.zhHighFreq.take(limit) else emptyList()
        }
        if (!chinese) {
            val hits = lexicon.assocEn[prev.lowercase()].orEmpty().take(limit).map { Candidate(it.first, it.second) }
            return hits.ifEmpty { emptyList() }
        }
        val merged = LinkedHashMap<String, Int>()
        fun take(key: String, bonus: Int) {
            if (key.isEmpty()) return
            for ((text, freq) in lexicon.assocZh[key].orEmpty()) {
                val score = freq + bonus
                val old = merged[text]
                if (old == null || score > old) merged[text] = score
            }
        }
        take(prev, 100_000)
        if (prev.length >= 3) take(prev.takeLast(2), 10_000)
        if (prev.length >= 2) take(prev.takeLast(1), 0)
        if (merged.isEmpty()) return lexicon.zhHighFreq.take(limit)
        return merged.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { Candidate(it.key, it.value) }
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
