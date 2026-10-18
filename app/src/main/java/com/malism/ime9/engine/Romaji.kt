package com.malism.ime9.engine

object Romaji {
    private val syllables: List<Pair<String, String>> = listOf(
        "kya" to "きゃ", "kyu" to "きゅ", "kyo" to "きょ",
        "sha" to "しゃ", "shu" to "しゅ", "sho" to "しょ",
        "sya" to "しゃ", "syu" to "しゅ", "syo" to "しょ",
        "cha" to "ちゃ", "chu" to "ちゅ", "cho" to "ちょ",
        "tya" to "ちゃ", "tyu" to "ちゅ", "tyo" to "ちょ",
        "nya" to "にゃ", "nyu" to "にゅ", "nyo" to "にょ",
        "hya" to "ひゃ", "hyu" to "ひゅ", "hyo" to "ひょ",
        "mya" to "みゃ", "myu" to "みゅ", "myo" to "みょ",
        "rya" to "りゃ", "ryu" to "りゅ", "ryo" to "りょ",
        "gya" to "ぎゃ", "gyu" to "ぎゅ", "gyo" to "ぎょ",
        "ja" to "じゃ", "ju" to "じゅ", "jo" to "じょ",
        "jya" to "じゃ", "jyu" to "じゅ", "jyo" to "じょ",
        "bya" to "びゃ", "byu" to "びゅ", "byo" to "びょ",
        "pya" to "ぴゃ", "pyu" to "ぴゅ", "pyo" to "ぴょ",
        "shi" to "し", "chi" to "ち", "tsu" to "つ", "fu" to "ふ",
        "ka" to "か", "ki" to "き", "ku" to "く", "ke" to "け", "ko" to "こ",
        "sa" to "さ", "si" to "し", "su" to "す", "se" to "せ", "so" to "そ",
        "ta" to "た", "ti" to "ち", "tu" to "つ", "te" to "て", "to" to "と",
        "na" to "な", "ni" to "に", "nu" to "ぬ", "ne" to "ね", "no" to "の",
        "ha" to "は", "hi" to "ひ", "hu" to "ふ", "he" to "へ", "ho" to "ほ",
        "ma" to "ま", "mi" to "み", "mu" to "む", "me" to "め", "mo" to "も",
        "ya" to "や", "yu" to "ゆ", "yo" to "よ",
        "ra" to "ら", "ri" to "り", "ru" to "る", "re" to "れ", "ro" to "ろ",
        "wa" to "わ", "wo" to "を",
        "ga" to "が", "gi" to "ぎ", "gu" to "ぐ", "ge" to "げ", "go" to "ご",
        "za" to "ざ", "zi" to "じ", "ji" to "じ", "zu" to "ず", "ze" to "ぜ", "zo" to "ぞ",
        "da" to "だ", "di" to "ぢ", "du" to "づ", "de" to "で", "do" to "ど",
        "ba" to "ば", "bi" to "び", "bu" to "ぶ", "be" to "べ", "bo" to "ぼ",
        "pa" to "ぱ", "pi" to "ぴ", "pu" to "ぷ", "pe" to "ぺ", "po" to "ぽ",
        "a" to "あ", "i" to "い", "u" to "う", "e" to "え", "o" to "お",
    )

    /** Converts romaji, leaving an unfinished syllable as Latin letters. */
    fun toHiragana(raw: String): String {
        val s = raw.lowercase().filter { it in 'a'..'z' }
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            if (isSokuon(s, i)) {
                out.append('っ')
                i++
                continue
            }
            val nAdvance = consumeN(s, i)
            if (nAdvance > 0) {
                out.append('ん')
                i += nAdvance
                continue
            }
            var matched = false
            for ((k, v) in syllables) {
                if (s.startsWith(k, i)) {
                    out.append(v)
                    i += k.length
                    matched = true
                    break
                }
            }
            if (!matched) {
                out.append(s[i])
                i++
            }
        }
        return out.toString()
    }

    private fun isSokuon(s: String, i: Int): Boolean {
        if (i + 1 >= s.length) return false
        val ch = s[i]
        if (ch == 'n' || ch !in 'a'..'z' || ch != s[i + 1]) return false
        if (ch in "aeiou") return false
        return true
    }

    /**
     * ん before a consonant, at end of input, or as the first of "nn" when the
     * second n starts na/ni/nu/ne/no/nya (konnichiwa). Returns chars consumed, or 0
     * when n should stay part of a longer syllable.
     */
    private fun consumeN(s: String, i: Int): Int {
        if (s[i] != 'n') return 0
        val next = s.getOrNull(i + 1) ?: return 1
        if (next in "aiueoy") return 0
        if (next != 'n') return 1
        val next2 = s.getOrNull(i + 2)
        return if (next2 != null && next2 in "aiueoy") 1 else 2
    }
}
