package com.malism.ime9.engine

class LetterTrie {
    class Node {
        val children = arrayOfNulls<Node>(26)
        val entries = ArrayList<DictEntry>(2)
        var hasSubtree = false
    }

    data class DictEntry(val word: String, val freq: Int, val pinyin: String)

    val root = Node()

    fun insert(key: String, word: String, freq: Int, pinyin: String = key) {
        val letters = key.lowercase().filter { it in 'a'..'z' }
        if (letters.isEmpty()) return
        var node = root
        for (ch in letters) {
            val idx = ch - 'a'
            val next = node.children[idx] ?: Node().also { node.children[idx] = it }
            node = next
            node.hasSubtree = true
        }
        val existing = node.entries.indexOfFirst { it.word == word }
        if (existing >= 0) {
            if (freq > node.entries[existing].freq) {
                node.entries[existing] = DictEntry(word, freq, pinyin)
            }
        } else {
            node.entries.add(DictEntry(word, freq, pinyin))
        }
    }

    fun nodeFor(key: String): Node? {
        val letters = key.lowercase().filter { it in 'a'..'z' }
        var node = root
        for (ch in letters) {
            node = node.children[ch - 'a'] ?: return null
        }
        return node
    }

    fun collectPrefix(key: String, limit: Int): List<DictEntry> {
        val node = nodeFor(key) ?: return emptyList()
        val found = ArrayList<DictEntry>(limit)
        collect(node, found, limit)
        return found.sortedByDescending { it.freq }.take(limit)
    }

    fun collectExactAndPrefix(key: String, limit: Int): List<DictEntry> {
        val node = nodeFor(key) ?: return emptyList()
        val found = ArrayList<DictEntry>(limit * 2)
        collect(node, found, Int.MAX_VALUE)
        val merged = LinkedHashMap<String, DictEntry>()
        for (e in found.sortedByDescending { it.freq }) {
            merged.putIfAbsent(e.word, e)
            if (merged.size >= limit) break
        }
        return merged.values.toList()
    }

    fun collectT9(digitsOrLetters: String, limit: Int): List<DictEntry> {
        val found = ArrayList<DictEntry>()
        walkT9(root, digitsOrLetters, 0, found)
        val merged = LinkedHashMap<String, DictEntry>()
        for (e in found.sortedByDescending { it.freq }) {
            merged.putIfAbsent(e.word, e)
            if (merged.size >= limit) break
        }
        return merged.values.toList()
    }

    fun bestT9Letters(digitsOrLetters: String): String? {
        val found = ArrayList<DictEntry>()
        walkT9(root, digitsOrLetters, 0, found)
        val best = found.maxByOrNull { it.freq } ?: return null
        val letters = best.pinyin.lowercase().filter { it in 'a'..'z' }
        val len = digitsOrLetters.count { it in 'a'..'z' || it in '0'..'9' }
        if (letters.isEmpty()) return null
        return letters.take(len)
    }

    private fun walkT9(node: Node, seq: String, index: Int, out: ArrayList<DictEntry>) {
        if (index == seq.length) {
            collect(node, out, Int.MAX_VALUE)
            return
        }
        val ch = seq[index]
        val letters = if (ch in '0'..'9') T9Index.lettersFor(ch) else if (ch in 'a'..'z') charArrayOf(ch) else charArrayOf()
        for (letter in letters) {
            val child = node.children[letter - 'a'] ?: continue
            walkT9(child, seq, index + 1, out)
        }
    }

    private fun collect(node: Node, out: ArrayList<DictEntry>, cap: Int) {
        if (out.size >= cap) return
        out.addAll(node.entries)
        if (out.size >= cap) return
        for (child in node.children) {
            if (child != null) {
                collect(child, out, cap)
                if (out.size >= cap) return
            }
        }
    }
}
