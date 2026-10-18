package com.malism.ime9.engine

class InputEngine(val lexicon: Lexicon) {
    val pinyin = PinyinDecoder(lexicon)
    val english = EnglishPredictor(lexicon)
    val words = WordPredictor()
    val japanese = JapaneseDecoder(lexicon)
    val association = Association(lexicon)

    fun chineseCandidates(composing: String): List<Candidate> = pinyin.suggest(composing)

    fun englishCandidates(composing: String, shift: Boolean): List<Candidate> =
        english.suggest(composing, shift)

    fun spanishCandidates(composing: String, shift: Boolean): List<Candidate> =
        words.suggest(lexicon.es, composing, shift)

    fun germanCandidates(composing: String, shift: Boolean): List<Candidate> =
        words.suggest(lexicon.de, composing, shift)

    fun russianCandidates(composing: String, shift: Boolean): List<Candidate> =
        words.suggest(lexicon.ru, composing, shift)

    fun arabicCandidates(composing: String): List<Candidate> =
        words.suggest(lexicon.ar, composing, shift = false)

    fun japaneseCandidates(composing: String): List<Candidate> = japanese.suggest(composing)

    fun associate(prev: String, chinese: Boolean): List<Candidate> =
        association.suggest(prev, chinese)

    fun associateEs(prev: String): List<Candidate> = association.suggestTable(prev, lexicon.assocEs, fold = true)

    fun associateDe(prev: String): List<Candidate> = association.suggestTable(prev, lexicon.assocDe, fold = true)

    fun associateRu(prev: String): List<Candidate> = association.suggestTable(prev, lexicon.assocRu, fold = true)

    fun associateAr(prev: String): List<Candidate> = association.suggestTable(prev, lexicon.assocAr, fold = false)

    fun associateJa(prev: String): List<Candidate> = association.suggestTable(prev, lexicon.assocJa, fold = false)
}
