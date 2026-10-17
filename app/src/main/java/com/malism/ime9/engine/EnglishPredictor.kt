package com.malism.ime9.engine

class EnglishPredictor(private val lexicon: Lexicon) {
    fun suggest(composing: String, shift: Boolean, limit: Int = 16): List<Candidate> {
        val raw = composing.filter { it.isLetter() }
        if (raw.isEmpty()) return emptyList()
        val key = raw.lowercase()
        return lexicon.en.collectExactAndPrefix(key, limit).map { entry ->
            Candidate(applyCase(entry.word, raw, shift), entry.freq, entry.word)
        }
    }

    private fun applyCase(word: String, typed: String, shift: Boolean): String {
        if (typed.length >= 2 && typed.all { it.isUpperCase() }) return word.uppercase()
        if (typed.first().isUpperCase() || (typed.length == 1 && shift)) {
            return word.replaceFirstChar { it.uppercaseChar() }
        }
        return word
    }
}
