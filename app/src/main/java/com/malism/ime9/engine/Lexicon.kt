package com.malism.ime9.engine

data class Candidate(
    val text: String,
    val freq: Int,
    val hint: String = "",
)

class Lexicon {
    val zh = LetterTrie()
    val en = LetterTrie()
    val assocZh = HashMap<String, MutableList<Pair<String, Int>>>()
    val assocEn = HashMap<String, MutableList<Pair<String, Int>>>()
    val zhHighFreq = ArrayList<Candidate>()

    fun addZh(word: String, pinyin: String, freq: Int) {
        val key = pinyin.lowercase().filter { it in 'a'..'z' }
        zh.insert(key, word, freq, key)
        if (word.length == 1) {
            zhHighFreq.add(Candidate(word, freq))
        }
    }

    fun addEn(word: String, freq: Int) {
        en.insert(word.lowercase(), word.lowercase(), freq, word.lowercase())
    }

    fun addAssocZh(prev: String, next: String, freq: Int) {
        assocZh.getOrPut(prev) { ArrayList() }.add(next to freq)
    }

    fun addAssocEn(prev: String, next: String, freq: Int) {
        assocEn.getOrPut(prev.lowercase()) { ArrayList() }.add(next.lowercase() to freq)
    }

    fun freeze() {
        zhHighFreq.sortByDescending { it.freq }
        for (list in assocZh.values) list.sortByDescending { it.second }
        for (list in assocEn.values) list.sortByDescending { it.second }
    }

    companion object {
        fun load(
            zhLines: Sequence<String>,
            enLines: Sequence<String>,
            assocZhLines: Sequence<String>,
            assocEnLines: Sequence<String>,
        ): Lexicon {
            val lex = Lexicon()
            for (line in zhLines) {
                val parts = line.trim().split('\t')
                if (parts.size < 3) continue
                lex.addZh(parts[0], parts[1], parts[2].toIntOrNull() ?: 1)
            }
            for (line in enLines) {
                val parts = line.trim().split('\t')
                if (parts.size < 2) continue
                lex.addEn(parts[0], parts[1].toIntOrNull() ?: 1)
            }
            for (line in assocZhLines) {
                val parts = line.trim().split('\t')
                if (parts.size < 3) continue
                lex.addAssocZh(parts[0], parts[1], parts[2].toIntOrNull() ?: 1)
            }
            for (line in assocEnLines) {
                val parts = line.trim().split('\t')
                if (parts.size < 3) continue
                lex.addAssocEn(parts[0], parts[1], parts[2].toIntOrNull() ?: 1)
            }
            lex.freeze()
            return lex
        }
    }
}
