package com.malism.ime9.engine

object T9Index {
    private val map = arrayOf(
        charArrayOf(), // 0
        charArrayOf(), // 1
        charArrayOf('a', 'b', 'c'),
        charArrayOf('d', 'e', 'f'),
        charArrayOf('g', 'h', 'i'),
        charArrayOf('j', 'k', 'l'),
        charArrayOf('m', 'n', 'o'),
        charArrayOf('p', 'q', 'r', 's'),
        charArrayOf('t', 'u', 'v'),
        charArrayOf('w', 'x', 'y', 'z'),
    )

    fun lettersFor(digit: Char): CharArray {
        if (digit !in '0'..'9') return charArrayOf()
        return map[digit - '0']
    }

    fun digitFor(letter: Char): Char? {
        val c = letter.lowercaseChar()
        if (c !in 'a'..'z') return null
        return when (c) {
            in 'a'..'c' -> '2'
            in 'd'..'f' -> '3'
            in 'g'..'i' -> '4'
            in 'j'..'l' -> '5'
            in 'm'..'o' -> '6'
            in 'p'..'s' -> '7'
            in 't'..'v' -> '8'
            else -> '9'
        }
    }

    fun toDigits(pinyin: String): String {
        val sb = StringBuilder()
        for (ch in pinyin.lowercase()) {
            digitFor(ch)?.let { sb.append(it) }
        }
        return sb.toString()
    }
}
