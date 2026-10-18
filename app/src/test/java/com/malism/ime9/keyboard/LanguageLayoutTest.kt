package com.malism.ime9.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageLayoutTest {
    @Test
    fun languageCycleOrder() {
        assertEquals(LanguageMode.English, LanguageMode.Chinese.next())
        assertEquals(LanguageMode.Arabic, LanguageMode.English.next())
        assertEquals(LanguageMode.Spanish, LanguageMode.Arabic.next())
        assertEquals(LanguageMode.Japanese, LanguageMode.Spanish.next())
        assertEquals(LanguageMode.German, LanguageMode.Japanese.next())
        assertEquals(LanguageMode.Russian, LanguageMode.German.next())
        assertEquals(LanguageMode.Chinese, LanguageMode.Russian.next())
    }

    @Test
    fun tagsAndRtl() {
        assertEquals(LanguageMode.Arabic, LanguageMode.fromTag("ar-SA"))
        assertEquals(LanguageMode.Japanese, LanguageMode.fromTag("ja_JP"))
        assertTrue(LanguageMode.Arabic.rtl)
        assertTrue(LanguageMode.entries.count { it.rtl } == 1)
    }

    @Test
    fun arabicKeyboardStartsAtDadAndListsLanguages() {
        val spec = KeyboardLayouts.arabic() as KeyboardSpec.Grid
        assertEquals("ض", spec.rows.first().first().label)
        val labels = spec.rows.last().first { it.action == KeyAction.Language }.longPress
        assertEquals(LanguageMode.entries.map { it.label }, labels)
    }

    @Test
    fun germanIsQwertzAndRussianHasYo() {
        val de = KeyboardLayouts.current(
            KeyboardPage.Letters,
            LanguageMode.German,
            ChineseLayout.Qwerty,
            shift = false,
        ) as KeyboardSpec.Grid
        assertEquals("z", de.rows.first()[5].label)
        assertEquals("ü", de.rows.first().last().label)
        val ru = KeyboardLayouts.current(
            KeyboardPage.Letters,
            LanguageMode.Russian,
            ChineseLayout.Qwerty,
            shift = false,
        ) as KeyboardSpec.Grid
        val ye = ru.rows[0].first { it.label == "е" }
        assertEquals(listOf("ё"), ye.longPress)
    }
}
