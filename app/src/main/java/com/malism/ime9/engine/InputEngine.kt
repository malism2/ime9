package com.malism.ime9.engine

class InputEngine(val lexicon: Lexicon) {
    val pinyin = PinyinDecoder(lexicon)
    val english = EnglishPredictor(lexicon)
    val association = Association(lexicon)

    fun chineseCandidates(composing: String): List<Candidate> = pinyin.suggest(composing)

    fun englishCandidates(composing: String, shift: Boolean): List<Candidate> =
        english.suggest(composing, shift)

    fun associate(prev: String, chinese: Boolean): List<Candidate> =
        association.suggest(prev, chinese)
}
