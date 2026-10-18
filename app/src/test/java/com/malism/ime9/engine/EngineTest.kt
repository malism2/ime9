package com.malism.ime9.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class EngineTest {
    @Test
    fun pinyinSegmentsNihao() {
        assertEquals(listOf("ni", "hao"), PinyinSyllables.segmentLoose("nihao"))
        assertEquals("ni'hao", PinyinSyllables.display("nihao"))
    }

    @Test
    fun pinyinIncompleteLastSyllable() {
        val parts = PinyinSyllables.segmentLoose("nih")
        assertEquals("ni", parts.first())
        assertEquals("h", parts.last())
    }

    @Test
    fun t9MapsNi() {
        assertEquals("64", T9Index.toDigits("ni"))
        assertEquals("64426", T9Index.toDigits("nihao"))
    }

    @Test
    fun chinesePrefixNihao() {
        val hits = engine.chineseCandidates("nihao").map { it.text }
        assertTrue(hits.contains("你好"))
        assertEquals("你好", hits.first())
    }

    @Test
    fun t9DigitsFindNi() {
        val hits = engine.chineseCandidates("64").map { it.text }
        assertTrue(hits.contains("你"))
        assertTrue(hits.contains("你好") || hits.first().startsWith("你") || hits.contains("你"))
    }

    @Test
    fun t9ComposingShowsPinyinNotDigits() {
        val hits = engine.chineseCandidates("64426")
        val nihao = hits.first { it.text == "你好" }
        assertEquals("ni'hao", engine.pinyin.displayComposing("64426", nihao.hint))
        val shown = engine.pinyin.displayComposing("64426", hits.first().hint)
        assertTrue(shown.none { it.isDigit() })
        assertTrue(shown.any { it in 'a'..'z' })
    }

    @Test
    fun t9PartialComposingShowsPinyinPrefix() {
        val hits = engine.chineseCandidates("64")
        val shown = engine.pinyin.displayComposing("64", hits.first().hint)
        assertTrue(shown, shown.startsWith("n") || shown == "ni")
        assertTrue(shown.none { it.isDigit() })
    }

    @Test
    fun t9DigitsFindNihao() {
        val hits = engine.chineseCandidates("64426").map { it.text }
        assertTrue("expected 你好 in $hits", hits.contains("你好"))
    }

    @Test
    fun rankingPrefersHigherFrequency() {
        val hits = engine.chineseCandidates("de")
        assertTrue(hits.isNotEmpty())
        val freqs = hits.map { it.freq }
        assertEquals(freqs.sortedDescending(), freqs)
    }

    @Test
    fun englishPrefixThe() {
        val hits = engine.englishCandidates("th", shift = false).map { it.text }
        assertTrue(hits.contains("the"))
        assertEquals("the", hits.first())
    }

    @Test
    fun englishShiftCapitalizes() {
        val hits = engine.englishCandidates("Th", shift = true).map { it.text }
        assertTrue(hits.contains("The"))
    }

    @Test
    fun associationAfterNi() {
        val hits = engine.associate("你", chinese = true).map { it.text }
        assertTrue(hits.contains("好"))
        assertTrue(hits.contains("的"))
    }

    @Test
    fun associationEnglishI() {
        val hits = engine.associate("I", chinese = false).map { it.text }
        assertTrue(hits.contains("am"))
    }

    @Test
    fun spanishFoldFindsNino() {
        val hits = engine.spanishCandidates("nino", shift = false).map { it.text }
        assertTrue(hits.contains("niño"))
    }

    @Test
    fun spanishShiftCapitalizes() {
        val hits = engine.spanishCandidates("Hola", shift = true).map { it.text }
        assertTrue(hits.contains("Hola"))
    }

    @Test
    fun germanFoldFindsStrasse() {
        val hits = engine.germanCandidates("stras", shift = false).map { it.text }
        assertTrue(hits.contains("straße"))
    }

    @Test
    fun russianPrefix() {
        val hits = engine.russianCandidates("прив", shift = false).map { it.text }
        assertEquals("привет", hits.first())
    }

    @Test
    fun arabicPrefixAndAssociation() {
        val hits = engine.arabicCandidates("مرح").map { it.text }
        assertTrue(hits.contains("مرحبا"))
        val next = engine.associateAr("مرحبا").map { it.text }
        assertTrue(next.contains("بك"))
    }

    @Test
    fun romajiKonnichiwa() {
        assertEquals("こんにちわ", Romaji.toHiragana("konnichiwa"))
        assertEquals("とうきょう", Romaji.toHiragana("toukyou"))
        assertEquals("がっこう", Romaji.toHiragana("gakkou"))
    }

    @Test
    fun japaneseExactAndKana() {
        val hits = engine.japaneseCandidates("watashi").map { it.text }
        assertEquals("私", hits.first())
        assertTrue(hits.contains("わたし"))
        val hello = engine.japaneseCandidates("konnichiwa").map { it.text }
        assertEquals("こんにちは", hello.first())
    }

    companion object {
        lateinit var engine: InputEngine

        @JvmStatic
        @BeforeClass
        fun load() {
            fun lines(name: String): Sequence<String> {
                val stream = EngineTest::class.java.classLoader!!.getResourceAsStream("dict/$name")
                    ?: error("missing dict/$name")
                return stream.bufferedReader().readLines().asSequence()
            }
            val lexicon = Lexicon.load(
                lines("zh.txt"),
                lines("en.txt"),
                lines("assoc_zh.txt"),
                lines("assoc_en.txt"),
                esLines = lines("es.txt"),
                deLines = lines("de.txt"),
                ruLines = lines("ru.txt"),
                arLines = lines("ar.txt"),
                jaLines = lines("ja.txt"),
                assocEsLines = lines("assoc_es.txt"),
                assocDeLines = lines("assoc_de.txt"),
                assocRuLines = lines("assoc_ru.txt"),
                assocArLines = lines("assoc_ar.txt"),
                assocJaLines = lines("assoc_ja.txt"),
            )
            engine = InputEngine(lexicon)
        }
    }
}
