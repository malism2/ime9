package com.malism.ime9.engine

class JapaneseDecoder(private val lexicon: Lexicon) {
    fun suggest(composing: String, limit: Int = 16): List<Candidate> {
        val key = composing.lowercase().filter { it in 'a'..'z' }
        if (key.isEmpty()) return emptyList()
        val dict = lexicon.ja.collectExactAndPrefix(key, limit)
        val exact = dict.filter { it.pinyin.length == key.length }.sortedByDescending { it.freq }
        val prefix = dict.filter { it.pinyin.length > key.length }.sortedByDescending { it.freq }
        val kana = Romaji.toHiragana(key)
        val out = LinkedHashMap<String, Candidate>()
        for (entry in exact) {
            out.putIfAbsent(entry.word, Candidate(entry.word, entry.freq, entry.pinyin))
        }
        if (kana.isNotEmpty()) {
            out.putIfAbsent(kana, Candidate(kana, 0, key))
        }
        for (entry in prefix) {
            if (out.size >= limit) break
            out.putIfAbsent(entry.word, Candidate(entry.word, entry.freq, entry.pinyin))
        }
        return out.values.take(limit).toList()
    }
}
