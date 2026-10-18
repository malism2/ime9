package com.malism.ime9.engine

/** Prefix dictionary for scripts that are not limited to a–z. */
class CharTrie {
    class Node {
        val children = HashMap<Char, Node>()
        val entries = ArrayList<LetterTrie.DictEntry>(2)
    }

    val root = Node()

    fun insert(key: String, word: String, freq: Int, reading: String = key) {
        val letters = key.lowercase()
        if (letters.isEmpty()) return
        var node = root
        for (ch in letters) {
            node = node.children.getOrPut(ch) { Node() }
        }
        val existing = node.entries.indexOfFirst { it.word == word }
        if (existing >= 0) {
            if (freq > node.entries[existing].freq) {
                node.entries[existing] = LetterTrie.DictEntry(word, freq, reading)
            }
        } else {
            node.entries.add(LetterTrie.DictEntry(word, freq, reading))
        }
    }

    fun collectExactAndPrefix(key: String, limit: Int): List<LetterTrie.DictEntry> {
        val node = nodeFor(key.lowercase()) ?: return emptyList()
        val found = ArrayList<LetterTrie.DictEntry>()
        collect(node, found)
        val merged = LinkedHashMap<String, LetterTrie.DictEntry>()
        for (e in found.sortedByDescending { it.freq }) {
            merged.putIfAbsent(e.word, e)
            if (merged.size >= limit) break
        }
        return merged.values.toList()
    }

    private fun nodeFor(key: String): Node? {
        var node = root
        for (ch in key) {
            node = node.children[ch] ?: return null
        }
        return node
    }

    private fun collect(node: Node, out: ArrayList<LetterTrie.DictEntry>) {
        out.addAll(node.entries)
        for (child in node.children.values) {
            collect(child, out)
        }
    }
}
