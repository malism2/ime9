package com.malism.ime9.keyboard

import com.malism.ime9.R

enum class LanguageMode(val tag: String, val label: String, val rtl: Boolean) {
    Chinese("zh", "中", false),
    English("en", "EN", false),
    Arabic("ar", "ع", true),
    Spanish("es", "ES", false),
    Japanese("ja", "あ", false),
    German("de", "DE", false),
    Russian("ru", "РУ", false),
    ;

    fun next(): LanguageMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromTag(tag: String?): LanguageMode? {
            val lang = tag?.lowercase()?.substringBefore('-')?.substringBefore('_') ?: return null
            return entries.find { it.tag == lang }
        }
    }
}

enum class ChineseLayout { Qwerty, T9 }

enum class KeyboardPage { Letters, Symbols, Numbers }

sealed class KeyAction {
    data class Letter(val ch: Char) : KeyAction()
    data class T9Digit(val digit: Char) : KeyAction()
    data class Text(val value: String) : KeyAction()
    data object Shift : KeyAction()
    data object Delete : KeyAction()
    data object Space : KeyAction()
    data object Enter : KeyAction()
    data object Language : KeyAction()
    data object LayoutToggle : KeyAction()
    data object Symbols : KeyAction()
    data object Numbers : KeyAction()
    data object Letters : KeyAction()
    data object Separator : KeyAction()
    data object Clear : KeyAction()
}

data class KeyDef(
    val label: String,
    val action: KeyAction,
    val hint: String = "",
    val weight: Float = 1f,
    val style: KeyStyle = KeyStyle.Normal,
    val longPress: List<String> = emptyList(),
    val iconRes: Int = 0,
    val iconTintRes: Int = 0,
)

enum class KeyStyle { Normal, Special, Enter }

sealed class KeyboardSpec {
    data class Grid(val rows: List<List<KeyDef>>, val rowHeightDp: Int = 48) : KeyboardSpec()
    data class WeChatT9(
        val left: List<KeyDef>,
        val pad: List<List<KeyDef>>,
        val del: KeyDef,
        val enter: KeyDef,
        val rowHeightDp: Int = 56,
    ) : KeyboardSpec()
}

object KeyboardLayouts {
    private val qwertyRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val cnPunct = listOf("，", "。", "？", "！", "、", "；", "：")
    private val t9Long = mapOf(
        '2' to "abc",
        '3' to "def",
        '4' to "ghi",
        '5' to "jkl",
        '6' to "mno",
        '7' to "pqrs",
        '8' to "tuv",
        '9' to "wxyz",
    )

    fun current(
        page: KeyboardPage,
        language: LanguageMode,
        chineseLayout: ChineseLayout,
        shift: Boolean,
        numberPad: NumberPadMode = NumberPadMode.Optional,
    ): KeyboardSpec {
        return when (page) {
            KeyboardPage.Symbols -> symbols(language)
            KeyboardPage.Numbers -> numbers(language, numberPad)
            KeyboardPage.Letters -> when (language) {
                LanguageMode.English -> qwerty(LanguageMode.English, shift, qwertyRows)
                LanguageMode.Japanese -> qwerty(LanguageMode.Japanese, shift, qwertyRows)
                LanguageMode.Spanish -> qwerty(LanguageMode.Spanish, shift, spanishRows, esLong)
                LanguageMode.German -> qwerty(LanguageMode.German, shift, germanRows, deLong, indentMiddle = false)
                LanguageMode.Russian -> qwerty(LanguageMode.Russian, shift, russianRows, ruLong, indentMiddle = false)
                LanguageMode.Arabic -> arabic()
                LanguageMode.Chinese -> if (chineseLayout == ChineseLayout.T9) chineseT9() else chineseQwerty()
            }
        }
    }

    fun chineseQwerty(): KeyboardSpec.Grid {
        val rows = ArrayList<List<KeyDef>>()
        rows += qwertyRows[0].map { letter(it) }
        rows += indented(qwertyRows[1])
        rows += listOf(symKey(LanguageMode.Chinese, weight = 1.4f)) + qwertyRows[2].map { letter(it) } + listOf(delKey(1.4f))
        rows += bottom(LanguageMode.Chinese)
        return KeyboardSpec.Grid(rows, rowHeightDp = 48)
    }

    fun englishQwerty(shifted: Boolean): KeyboardSpec.Grid = qwerty(LanguageMode.English, shifted, qwertyRows)

    private val spanishRows = listOf("qwertyuiop", "asdfghjklñ", "zxcvbnm")
    private val germanRows = listOf("qwertzuiopü", "asdfghjklöä", "yxcvbnmß")
    private val russianRows = listOf("йцукенгшщзх", "фывапролджэ", "ячсмитьбю")
    private val esLong = mapOf(
        'a' to listOf("á", "à"),
        'e' to listOf("é", "è"),
        'i' to listOf("í", "ì"),
        'o' to listOf("ó", "ò"),
        'u' to listOf("ú", "ü", "ù"),
        'n' to listOf("ñ"),
    )
    private val deLong = mapOf(
        'a' to listOf("ä"),
        'o' to listOf("ö"),
        'u' to listOf("ü"),
        's' to listOf("ß"),
    )
    private val ruLong = mapOf('е' to listOf("ё"))

    fun qwerty(
        language: LanguageMode,
        shifted: Boolean,
        rows: List<String>,
        longPress: Map<Char, List<String>> = emptyMap(),
        indentMiddle: Boolean = true,
    ): KeyboardSpec.Grid {
        fun letters(s: String) = s.map { letterKey(it, shifted, longPress[it.lowercaseChar()].orEmpty()) }
        val built = ArrayList<List<KeyDef>>()
        built += letters(rows[0])
        val middle = letters(rows[1]).toMutableList()
        if (indentMiddle) {
            middle.add(0, spacer(0.45f))
            middle.add(spacer(0.45f))
        }
        built += middle
        built += listOf(shiftKey(shifted)) + letters(rows[2]) + listOf(delKey(1.5f))
        built += bottom(language)
        return KeyboardSpec.Grid(built, rowHeightDp = 48)
    }

    fun arabic(): KeyboardSpec.Grid {
        val rows = listOf("ضصثقفغعهخحج", "شسيبلاتنمكط", "ءظذدزروىلا")
        val built = ArrayList<List<KeyDef>>()
        built += rows[0].map { arKey(it) }
        built += rows[1].map { arKey(it) }
        built += listOf(symKey(LanguageMode.Arabic, 1.3f)) + rows[2].map { arKey(it) } + listOf(delKey(1.3f))
        built += bottom(LanguageMode.Arabic)
        return KeyboardSpec.Grid(built, rowHeightDp = 48)
    }

    fun chineseT9(): KeyboardSpec.WeChatT9 {
        fun t9(digit: Char, hint: String) = KeyDef(
            label = digit.toString(),
            action = KeyAction.T9Digit(digit),
            hint = hint,
            longPress = t9Long[digit]?.map { it.toString() }.orEmpty(),
        )
        return KeyboardSpec.WeChatT9(
            left = listOf(
                KeyDef("重输", KeyAction.Clear, style = KeyStyle.Special),
                KeyDef("符", KeyAction.Symbols, style = KeyStyle.Special),
                KeyDef("123", KeyAction.Numbers, style = KeyStyle.Special),
                KeyDef("中", KeyAction.Language, style = KeyStyle.Special, longPress = languageLabels()),
            ),
            pad = listOf(
                listOf(
                    KeyDef(
                        "1",
                        KeyAction.Separator,
                        hint = "，。",
                        longPress = cnPunct,
                    ),
                    t9('2', "ABC"),
                    t9('3', "DEF"),
                ),
                listOf(t9('4', "GHI"), t9('5', "JKL"), t9('6', "MNO")),
                listOf(t9('7', "PQRS"), t9('8', "TUV"), t9('9', "WXYZ")),
                listOf(
                    KeyDef("，", KeyAction.Text("，")),
                    KeyDef("空格", KeyAction.Space),
                    KeyDef("。", KeyAction.Text("。")),
                ),
            ),
            del = delKey(1f),
            enter = enterKey(LanguageMode.Chinese),
        )
    }

    fun symbols(language: LanguageMode): KeyboardSpec.Grid {
        val chars = when (language) {
            LanguageMode.Chinese -> listOf("，。？！、；：", "“”‘’（）", "《》【】…—·")
            LanguageMode.Arabic -> listOf("،؛؟!.:", "«»()[]", "@#&*-_/")
            LanguageMode.Spanish -> listOf(",.?!;:'", "¿¡«»()\"", "@#&*-_/")
            LanguageMode.Japanese -> listOf("、。？！・", "「」『』（）", "…—・@#")
            LanguageMode.German -> listOf(",.?!;:'", "„“»«()", "@#&*-_/")
            LanguageMode.Russian -> listOf(",.?!;:'", "«»—()", "@#&*-_/")
            LanguageMode.English -> listOf(",.?!;:'", "\"()[]{}", "@#&*-_/")
        }
        val rows = chars.map { row ->
            row.map { ch -> KeyDef(ch.toString(), KeyAction.Text(ch.toString())) }
        }.toMutableList()
        rows += bottom(language, lettersLabel = lettersLabel(language))
        return KeyboardSpec.Grid(rows, rowHeightDp = 48)
    }

    fun numbers(language: LanguageMode, numberPad: NumberPadMode = NumberPadMode.Optional): KeyboardSpec.Grid {
        if (numberPad.locked) {
            val row4 = when {
                numberPad.phone -> listOf(
                    KeyDef("*", KeyAction.Text("*"), style = KeyStyle.Special),
                    num("0"),
                    KeyDef("#", KeyAction.Text("#"), style = KeyStyle.Special),
                )
                numberPad.decimal && numberPad.signed -> listOf(
                    KeyDef("-", KeyAction.Text("-"), style = KeyStyle.Special),
                    num("0"),
                    KeyDef(".", KeyAction.Text("."), style = KeyStyle.Special),
                )
                numberPad.decimal -> listOf(
                    KeyDef(".", KeyAction.Text("."), style = KeyStyle.Special),
                    num("0"),
                    delKey(),
                )
                numberPad.signed -> listOf(
                    KeyDef("-", KeyAction.Text("-"), style = KeyStyle.Special),
                    num("0"),
                    delKey(),
                )
                else -> listOf(delKey(), num("0"), enterKey(language))
            }
            val rows = mutableListOf(
                listOf(num("1"), num("2"), num("3")),
                listOf(num("4"), num("5"), num("6")),
                listOf(num("7"), num("8"), num("9")),
                row4,
            )
            if (row4.none { it.action is KeyAction.Enter }) {
                rows += listOf(listOf(delKey(1f), enterKey(language, weight = 2f)))
            }
            return KeyboardSpec.Grid(rows, rowHeightDp = 56)
        }
        return KeyboardSpec.Grid(
            rows = listOf(
                listOf(num("1"), num("2"), num("3")),
                listOf(num("4"), num("5"), num("6")),
                listOf(num("7"), num("8"), num("9")),
                listOf(
                    symKey(language, weight = 1.2f),
                    num("0"),
                    KeyDef(".", KeyAction.Text("."), weight = 1.2f),
                ),
                bottom(language, lettersLabel = lettersLabel(language)),
            ),
            rowHeightDp = 52,
        )
    }

    private fun bottom(language: LanguageMode, lettersLabel: String? = null): List<KeyDef> {
        val first = if (lettersLabel != null) {
            KeyDef(lettersLabel, KeyAction.Letters, style = KeyStyle.Special, weight = 1.15f)
        } else {
            KeyDef("123", KeyAction.Numbers, style = KeyStyle.Special, weight = 1.15f)
        }
        val (comma, period) = punctuation(language)
        return listOf(
            first,
            KeyDef(language.label, KeyAction.Language, style = KeyStyle.Special, weight = 1.05f, longPress = languageLabels()),
            KeyDef(comma, KeyAction.Text(comma), weight = 1.0f),
            KeyDef(spaceLabel(language), KeyAction.Space, weight = 2.6f),
            KeyDef(period, KeyAction.Text(period), weight = 1.0f),
            enterKey(language, weight = 1.25f),
        )
    }

    private fun languageLabels(): List<String> = LanguageMode.entries.map { it.label }

    private fun lettersLabel(language: LanguageMode): String = when (language) {
        LanguageMode.Chinese -> "返回"
        LanguageMode.Japanese -> "あ"
        LanguageMode.Russian -> "АБВ"
        LanguageMode.Arabic -> "أ"
        else -> "ABC"
    }

    private fun spaceLabel(language: LanguageMode): String = when (language) {
        LanguageMode.Chinese -> "空格"
        LanguageMode.Arabic -> "مسافة"
        LanguageMode.Spanish -> "espacio"
        LanguageMode.Japanese -> "空白"
        LanguageMode.German -> "Leer"
        LanguageMode.Russian -> "пробел"
        LanguageMode.English -> "space"
    }

    private fun punctuation(language: LanguageMode): Pair<String, String> = when (language) {
        LanguageMode.Chinese -> "，" to "。"
        LanguageMode.Arabic -> "،" to "."
        LanguageMode.Japanese -> "、" to "。"
        else -> "," to "."
    }

    private fun letter(ch: Char) = KeyDef(ch.toString(), KeyAction.Letter(ch))

    private fun indented(letters: String): List<KeyDef> {
        val keys = letters.map { letter(it) }.toMutableList()
        keys.add(0, spacer(0.45f))
        keys.add(spacer(0.45f))
        return keys
    }

    private fun letterKey(ch: Char, shifted: Boolean, longPress: List<String>): KeyDef {
        val base = ch.lowercaseChar()
        val shown = if (shifted) base.uppercaseChar() else base
        return KeyDef(shown.toString(), KeyAction.Letter(base), longPress = longPress)
    }

    private fun arKey(ch: Char): KeyDef {
        val extras = when (ch) {
            'ا' -> listOf("أ", "إ", "آ")
            'ه' -> listOf("ة")
            'و' -> listOf("ؤ")
            'ي' -> listOf("ئ", "ى")
            else -> emptyList()
        }
        return KeyDef(ch.toString(), KeyAction.Letter(ch), longPress = extras)
    }

    private fun shiftKey(shifted: Boolean) = KeyDef(
        label = "",
        action = KeyAction.Shift,
        style = KeyStyle.Special,
        weight = 1.5f,
        iconRes = R.drawable.ic_shift,
        iconTintRes = if (shifted) R.color.ime_accent else R.color.ime_text,
    )

    private fun spacer(weight: Float) =
        KeyDef("", KeyAction.Text(""), weight = weight)

    private fun delKey(weight: Float = 1f) =
        KeyDef(
            label = "",
            action = KeyAction.Delete,
            style = KeyStyle.Special,
            weight = weight,
            iconRes = R.drawable.ic_backspace,
            iconTintRes = R.color.ime_text,
        )

    private fun enterKey(language: LanguageMode, weight: Float = 1f) =
        KeyDef(enterLabel(language), KeyAction.Enter, style = KeyStyle.Enter, weight = weight)

    private fun enterLabel(language: LanguageMode): String = when (language) {
        LanguageMode.Chinese -> "换行"
        LanguageMode.Arabic -> "تم"
        LanguageMode.Spanish -> "intro"
        LanguageMode.Japanese -> "改行"
        LanguageMode.German -> "Enter"
        LanguageMode.Russian -> "ввод"
        LanguageMode.English -> "return"
    }

    private fun symKey(language: LanguageMode, weight: Float) =
        KeyDef(symLabel(language), KeyAction.Symbols, style = KeyStyle.Special, weight = weight)

    private fun symLabel(language: LanguageMode): String = when (language) {
        LanguageMode.Chinese -> "符"
        LanguageMode.Arabic -> "رمز"
        LanguageMode.Japanese -> "記号"
        LanguageMode.Russian -> "сим"
        LanguageMode.Spanish -> "sím"
        else -> "sym"
    }

    private fun num(v: String) = KeyDef(v, KeyAction.Text(v))
}
