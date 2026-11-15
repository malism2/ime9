package com.malism.ime9.engine

data class Candidate(
    val text: String,
    val freq: Int,
    val hint: String = "",
)

class Lexicon {
    val zh = LetterTrie()
    val en = LetterTrie()
    val es = CharTrie()
    val de = CharTrie()
    val ru = CharTrie()
    val ar = CharTrie()
    val ja = LetterTrie()
    val assocZh = HashMap<String, MutableList<Pair<String, Int>>>()
    val assocEn = HashMap<String, MutableList<Pair<String, Int>>>()
    val assocEs = HashMap<String, MutableList<Pair<String, Int>>>()
    val assocDe = HashMap<String, MutableList<Pair<String, Int>>>()
    val assocRu = HashMap<String, MutableList<Pair<String, Int>>>()
    val assocAr = HashMap<String, MutableList<Pair<String, Int>>>()
    val assocJa = HashMap<String, MutableList<Pair<String, Int>>>()
    val zhHighFreq = ArrayList<Candidate>()

    fun addZh(word: String, pinyin: String, freq: Int) {
        val key = pinyin.lowercase().filter { it in 'a'..'z' }
        if (key.isEmpty() || word.isEmpty()) return
        zh.insert(key, word, freq, key)
        if (word.length == 1) {
            zhHighFreq.add(Candidate(word, freq))
            return
        }
        for (i in 1 until word.length) {
            addAssocZh(word.substring(0, i), word.substring(i), freq)
        }
        val parts = PinyinSyllables.segmentComplete(key) ?: return
        if (parts.size != word.length) return
        val pieceFreq = (freq / 2).coerceIn(1, 90)
        for (i in word.indices) {
            val py = parts[i]
            zh.insert(py, word.substring(i, i + 1), pieceFreq, py)
            zhHighFreq.add(Candidate(word.substring(i, i + 1), pieceFreq))
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

    fun addScript(trie: CharTrie, word: String, freq: Int, aliases: List<String> = emptyList()) {
        val surface = word.lowercase()
        trie.insert(surface, surface, freq, surface)
        val folded = WordPredictor().fold(surface)
        if (folded != surface) trie.insert(folded, surface, freq, surface)
        for (alias in aliases) {
            val key = alias.lowercase()
            if (key.isNotEmpty() && key != surface) trie.insert(key, surface, freq, surface)
        }
    }

    fun addJa(word: String, romaji: String, freq: Int) {
        val key = romaji.lowercase().filter { it in 'a'..'z' }
        if (key.isEmpty()) return
        ja.insert(key, word, freq, key)
    }

    fun addAssocExact(table: HashMap<String, MutableList<Pair<String, Int>>>, prev: String, next: String, freq: Int) {
        table.getOrPut(prev) { ArrayList() }.add(next to freq)
    }

    fun addAssocFolded(table: HashMap<String, MutableList<Pair<String, Int>>>, prev: String, next: String, freq: Int) {
        table.getOrPut(prev.lowercase()) { ArrayList() }.add(next to freq)
    }

    fun freeze() {
        val bestChar = LinkedHashMap<String, Candidate>()
        for (c in zhHighFreq) {
            val old = bestChar[c.text]
            if (old == null || c.freq > old.freq) bestChar[c.text] = c
        }
        zhHighFreq.clear()
        zhHighFreq.addAll(bestChar.values.sortedByDescending { it.freq })
        for (table in listOf(assocZh, assocEn, assocEs, assocDe, assocRu, assocAr, assocJa)) {
            for (list in table.values) {
                val merged = LinkedHashMap<String, Int>()
                for ((text, freq) in list) {
                    val old = merged[text]
                    if (old == null || freq > old) merged[text] = freq
                }
                list.clear()
                for ((text, freq) in merged.entries.sortedByDescending { it.value }) {
                    list.add(text to freq)
                }
            }
        }
    }

    companion object {
        fun load(
            zhLines: Sequence<String>,
            enLines: Sequence<String>,
            assocZhLines: Sequence<String>,
            assocEnLines: Sequence<String>,
            esLines: Sequence<String> = emptySequence(),
            deLines: Sequence<String> = emptySequence(),
            ruLines: Sequence<String> = emptySequence(),
            arLines: Sequence<String> = emptySequence(),
            jaLines: Sequence<String> = emptySequence(),
            assocEsLines: Sequence<String> = emptySequence(),
            assocDeLines: Sequence<String> = emptySequence(),
            assocRuLines: Sequence<String> = emptySequence(),
            assocArLines: Sequence<String> = emptySequence(),
            assocJaLines: Sequence<String> = emptySequence(),
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
            loadScript(esLines) { word, freq, aliases -> lex.addScript(lex.es, word, freq, aliases) }
            loadScript(deLines) { word, freq, aliases -> lex.addScript(lex.de, word, freq, aliases) }
            loadScript(ruLines) { word, freq, aliases -> lex.addScript(lex.ru, word, freq, aliases) }
            loadScript(arLines) { word, freq, aliases -> lex.addScript(lex.ar, word, freq, aliases) }
            for (line in jaLines) {
                val parts = line.trim().split('\t')
                if (parts.size < 3) continue
                lex.addJa(parts[0], parts[1], parts[2].toIntOrNull() ?: 1)
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
            loadAssoc(assocEsLines) { a, b, f -> lex.addAssocFolded(lex.assocEs, a, b, f) }
            loadAssoc(assocDeLines) { a, b, f -> lex.addAssocFolded(lex.assocDe, a, b, f) }
            loadAssoc(assocRuLines) { a, b, f -> lex.addAssocFolded(lex.assocRu, a, b, f) }
            loadAssoc(assocArLines) { a, b, f -> lex.addAssocExact(lex.assocAr, a, b, f) }
            loadAssoc(assocJaLines) { a, b, f -> lex.addAssocExact(lex.assocJa, a, b, f) }
            lex.freeze()
            return lex
        }

        private fun loadScript(lines: Sequence<String>, add: (String, Int, List<String>) -> Unit) {
            for (line in lines) {
                val parts = line.trim().split('\t')
                if (parts.size < 2) continue
                val freq = parts[1].toIntOrNull() ?: continue
                add(parts[0], freq, parts.drop(2))
            }
        }

        private fun loadAssoc(lines: Sequence<String>, add: (String, String, Int) -> Unit) {
            for (line in lines) {
                val parts = line.trim().split('\t')
                if (parts.size < 3) continue
                add(parts[0], parts[1], parts[2].toIntOrNull() ?: 1)
            }
        }
    }
}
