package com.malism.ime9.engine

class PinyinDecoder(private val lexicon: Lexicon) {
    fun suggest(composing: String, limit: Int = 24): List<Candidate> {
        val raw = composing.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }
        if (raw.isEmpty()) return emptyList()
        val hasDigit = raw.any { it in '0'..'9' }
        val entries = if (hasDigit) {
            lexicon.zh.collectT9(raw, limit)
        } else {
            val letters = raw.filter { it in 'a'..'z' }
            val prefix = lexicon.zh.collectExactAndPrefix(letters, limit)
            if (prefix.isNotEmpty()) prefix else lexicon.zh.collectPrefix(letters, limit)
        }
        return entries.map { Candidate(it.word, it.freq, it.pinyin) }
    }

    fun displayComposing(composing: String, topPinyin: String? = null): String {
        val seq = composing.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }
        if (seq.isEmpty()) return ""
        val lettersOnly = seq.all { it in 'a'..'z' }
        if (lettersOnly) return PinyinSyllables.display(seq)
        val fromCandidate = topPinyin?.lowercase()?.filter { it in 'a'..'z' }.orEmpty()
        if (fromCandidate.isNotEmpty()) {
            return PinyinSyllables.display(fromCandidate.take(seq.length))
        }
        return PinyinSyllables.display(lexicon.zh.bestT9Letters(seq) ?: seq)
    }
}
