package com.malism.ime9.engine

class WordPredictor {
    fun suggest(trie: CharTrie, composing: String, shift: Boolean, limit: Int = 16): List<Candidate> {
        val raw = composing.filter { it.isLetter() }
        if (raw.isEmpty()) return emptyList()
        val key = raw.lowercase()
        val folded = fold(key)
        val merged = LinkedHashMap<String, LetterTrie.DictEntry>()
        for (entry in trie.collectExactAndPrefix(key, limit)) {
            merged.putIfAbsent(entry.word, entry)
        }
        if (folded != key) {
            for (entry in trie.collectExactAndPrefix(folded, limit)) {
                merged.putIfAbsent(entry.word, entry)
            }
        }
        return merged.values.take(limit).map { entry ->
            Candidate(applyCase(entry.word, raw, shift), entry.freq)
        }
    }

    fun fold(text: String): String = buildString {
        for (ch in text.lowercase()) {
            when (ch) {
                'á', 'à', 'â', 'ä', 'ā' -> append('a')
                'é', 'è', 'ê', 'ë', 'ē' -> append('e')
                'í', 'ì', 'î', 'ï', 'ī' -> append('i')
                'ó', 'ò', 'ô', 'ö', 'ō' -> append('o')
                'ú', 'ù', 'û', 'ü', 'ū' -> append('u')
                'ñ' -> append('n')
                'ё' -> append('е')
                'ß' -> append("ss")
                else -> append(ch)
            }
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
