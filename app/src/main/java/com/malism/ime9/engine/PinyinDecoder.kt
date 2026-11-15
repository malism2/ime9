package com.malism.ime9.engine

class PinyinDecoder(private val lexicon: Lexicon) {
    private val syllableDigits: Set<String> = HashSet<String>().apply {
        for (syl in PinyinSyllables.all) {
            val digits = T9Index.toDigits(syl)
            if (digits.isNotEmpty()) add(digits)
        }
    }

    fun suggest(composing: String, limit: Int = 200): List<Candidate> {
        val raw = composing.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }
        if (raw.isEmpty()) return emptyList()
        val hasDigit = raw.any { it in '0'..'9' }
        val primary = if (hasDigit) {
            lexicon.zh.collectT9(raw, limit)
        } else {
            val letters = raw.filter { it in 'a'..'z' }
            val prefix = lexicon.zh.collectExactAndPrefix(letters, limit)
            if (prefix.isNotEmpty()) prefix else lexicon.zh.collectPrefix(letters, limit)
        }
        val extras = if (hasDigit) t9PrefixExtras(raw) else letterPrefixExtras(raw.filter { it in 'a'..'z' })
        return merge(primary, extras, limit)
    }

    /**
     * Pinyin still to type after committing a candidate.
     * Empty when the candidate consumes the whole composing string.
     */
    fun remaining(composing: String, chosenPinyin: String): String {
        val units = consumeUnits(composing, chosenPinyin)
        val rawCount = composing.count { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
        if (units >= rawCount) return ""
        var left = units
        val sb = StringBuilder()
        for (ch in composing) {
            val unit = ch in 'a'..'z' || ch in 'A'..'Z' || ch in '0'..'9'
            if (left > 0 && unit) {
                left--
                continue
            }
            if (left > 0) continue
            sb.append(ch)
        }
        return sb.toString().trimStart('\'')
    }

    fun displayComposing(composing: String, topPinyin: String? = null): String {
        val seq = composing.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }
        if (seq.isEmpty()) return ""
        val lettersOnly = seq.all { it in 'a'..'z' }
        if (lettersOnly) return PinyinSyllables.display(seq)
        val fromCandidate = topPinyin?.lowercase()?.filter { it in 'a'..'z' }.orEmpty()
        if (fromCandidate.isNotEmpty() && T9Index.toDigits(fromCandidate).length >= seq.length) {
            return PinyinSyllables.display(fromCandidate.take(seq.length))
        }
        return PinyinSyllables.display(lexicon.zh.bestT9Letters(seq) ?: seq)
    }

    private fun consumeUnits(composing: String, chosenPinyin: String): Int {
        val raw = composing.lowercase().filter { it in 'a'..'z' || it in '0'..'9' }
        val py = chosenPinyin.lowercase().filter { it in 'a'..'z' }
        if (raw.isEmpty()) return 0
        if (raw.all { it in 'a'..'z' }) {
            return if (py.isNotEmpty() && raw.startsWith(py) && py.length < raw.length) py.length else raw.length
        }
        val pyDigits = T9Index.toDigits(py)
        return if (pyDigits.isNotEmpty() && raw.startsWith(pyDigits) && pyDigits.length < raw.length) {
            pyDigits.length
        } else {
            raw.length
        }
    }

    private fun letterPrefixExtras(letters: String): List<LetterTrie.DictEntry> {
        val parts = PinyinSyllables.segmentLoose(letters)
        if (parts.size < 2) return emptyList()
        val spans = ArrayList<Pair<Int, List<LetterTrie.DictEntry>>>()
        var consumed = ""
        for (i in 0 until parts.size - 1) {
            if (parts[i] !in PinyinSyllables.all) break
            consumed += parts[i]
            if (consumed.length >= letters.length) break
            val node = lexicon.zh.nodeFor(consumed) ?: continue
            if (node.entries.isEmpty()) continue
            spans.add(consumed.length to node.entries.sortedByDescending { it.freq })
        }
        val out = ArrayList<LetterTrie.DictEntry>()
        for ((_, entries) in spans.sortedByDescending { it.first }) out.addAll(entries)
        return out
    }

    private fun t9PrefixExtras(digits: String): List<LetterTrie.DictEntry> {
        if (digits.length < 2) return emptyList()
        val reachable = BooleanArray(digits.length + 1)
        reachable[0] = true
        val marks = ArrayList<Int>()
        for (i in digits.indices) {
            if (!reachable[i]) continue
            val maxLen = minOf(6, digits.length - i)
            for (len in 1..maxLen) {
                if (digits.substring(i, i + len) !in syllableDigits) continue
                val next = i + len
                reachable[next] = true
                if (next < digits.length) marks.add(next)
            }
        }
        val out = ArrayList<LetterTrie.DictEntry>()
        for (len in marks.distinct().sortedDescending()) {
            out.addAll(lexicon.zh.collectT9(digits.substring(0, len), 80, exactOnly = true))
        }
        return out
    }

    private fun merge(
        primary: List<LetterTrie.DictEntry>,
        extras: List<LetterTrie.DictEntry>,
        limit: Int,
    ): List<Candidate> {
        val extraUnique = ArrayList<LetterTrie.DictEntry>()
        val seen = HashSet<String>()
        for (e in extras) {
            if (seen.add(e.word)) extraUnique.add(e)
        }
        val room = minOf(24, extraUnique.size)
        val primaryCap = if (room == 0) limit else (limit - room).coerceAtLeast(1)
        val out = LinkedHashMap<String, LetterTrie.DictEntry>()
        for (e in primary) {
            if (out.size >= primaryCap) break
            out.putIfAbsent(e.word, e)
        }
        for (e in extraUnique) {
            if (out.size >= limit) break
            out.putIfAbsent(e.word, e)
        }
        return out.values.map { Candidate(it.word, it.freq, it.pinyin) }
    }
}
