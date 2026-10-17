package com.malism.ime9.keyboard

import com.malism.ime9.R

enum class LanguageMode { Chinese, English }

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
            KeyboardPage.Symbols -> symbols(language == LanguageMode.Chinese)
            KeyboardPage.Numbers -> numbers(language == LanguageMode.Chinese, numberPad)
            KeyboardPage.Letters -> when {
                language == LanguageMode.English -> englishQwerty(shift)
                chineseLayout == ChineseLayout.T9 -> chineseT9()
                else -> chineseQwerty()
            }
        }
    }

    fun chineseQwerty(): KeyboardSpec.Grid {
        val rows = ArrayList<List<KeyDef>>()
        rows += qwertyRows[0].map { letter(it) }
        rows += indented(qwertyRows[1])
        rows += listOf(symKey(chinese = true, weight = 1.4f)) + qwertyRows[2].map { letter(it) } + listOf(delKey(1.4f))
        rows += wechatBottom(chinese = true)
        return KeyboardSpec.Grid(rows, rowHeightDp = 48)
    }

    fun englishQwerty(shifted: Boolean): KeyboardSpec.Grid {
        fun letters(s: String) = s.map {
            val shown = if (shifted) it.uppercaseChar() else it
            KeyDef(shown.toString(), KeyAction.Letter(it))
        }
        val rows = ArrayList<List<KeyDef>>()
        rows += letters(qwertyRows[0])
        rows += indentedLetters(qwertyRows[1], shifted)
        rows += listOf(
            KeyDef(
                label = "",
                action = KeyAction.Shift,
                style = KeyStyle.Special,
                weight = 1.5f,
                iconRes = R.drawable.ic_shift,
                iconTintRes = if (shifted) R.color.ime_accent else R.color.ime_text,
            ),
        ) + letters(qwertyRows[2]) + listOf(delKey(1.5f))
        rows += wechatBottom(chinese = false)
        return KeyboardSpec.Grid(rows, rowHeightDp = 48)
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
                KeyDef("中", KeyAction.Language, style = KeyStyle.Special),
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
            enter = enterKey(chinese = true),
        )
    }

    fun symbols(chinese: Boolean): KeyboardSpec.Grid {
        val chars = if (chinese) {
            listOf("，。？！、；：", "“”‘’（）", "《》【】…—·")
        } else {
            listOf(",.?!;:'", "\"()[]{}", "@#&*-_/")
        }
        val rows = chars.map { row ->
            row.map { ch -> KeyDef(ch.toString(), KeyAction.Text(ch.toString())) }
        }.toMutableList()
        rows += wechatBottom(chinese = chinese, lettersLabel = if (chinese) "返回" else "ABC")
        return KeyboardSpec.Grid(rows, rowHeightDp = 48)
    }

    fun numbers(chinese: Boolean, numberPad: NumberPadMode = NumberPadMode.Optional): KeyboardSpec.Grid {
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
                else -> listOf(delKey(), num("0"), enterKey(chinese = chinese))
            }
            val rows = mutableListOf(
                listOf(num("1"), num("2"), num("3")),
                listOf(num("4"), num("5"), num("6")),
                listOf(num("7"), num("8"), num("9")),
                row4,
            )
            if (row4.none { it.action is KeyAction.Enter }) {
                rows += listOf(listOf(delKey(1f), enterKey(chinese = chinese, weight = 2f)))
            }
            return KeyboardSpec.Grid(rows, rowHeightDp = 56)
        }
        return KeyboardSpec.Grid(
            rows = listOf(
                listOf(num("1"), num("2"), num("3")),
                listOf(num("4"), num("5"), num("6")),
                listOf(num("7"), num("8"), num("9")),
                listOf(
                    symKey(chinese = chinese, weight = 1.2f),
                    num("0"),
                    KeyDef(".", KeyAction.Text("."), weight = 1.2f),
                ),
                wechatBottom(chinese = chinese, lettersLabel = if (chinese) "返回" else "ABC"),
            ),
            rowHeightDp = 52,
        )
    }

    private fun wechatBottom(chinese: Boolean, lettersLabel: String? = null): List<KeyDef> {
        val first = if (lettersLabel != null) {
            KeyDef(lettersLabel, KeyAction.Letters, style = KeyStyle.Special, weight = 1.15f)
        } else {
            KeyDef("123", KeyAction.Numbers, style = KeyStyle.Special, weight = 1.15f)
        }
        val comma = if (chinese) "，" else ","
        val period = if (chinese) "。" else "."
        return listOf(
            first,
            KeyDef(if (chinese) "中" else "EN", KeyAction.Language, style = KeyStyle.Special, weight = 1.05f),
            KeyDef(comma, KeyAction.Text(comma), weight = 1.0f),
            KeyDef(if (chinese) "空格" else "space", KeyAction.Space, weight = 2.6f),
            KeyDef(period, KeyAction.Text(period), weight = 1.0f),
            enterKey(chinese = chinese, weight = 1.25f),
        )
    }

    private fun letter(ch: Char) = KeyDef(ch.toString(), KeyAction.Letter(ch))

    private fun indented(letters: String): List<KeyDef> {
        val keys = letters.map { letter(it) }.toMutableList()
        keys.add(0, spacer(0.45f))
        keys.add(spacer(0.45f))
        return keys
    }

    private fun indentedLetters(letters: String, shifted: Boolean): List<KeyDef> {
        val keys = letters.map {
            val shown = if (shifted) it.uppercaseChar() else it
            KeyDef(shown.toString(), KeyAction.Letter(it))
        }.toMutableList()
        keys.add(0, spacer(0.45f))
        keys.add(spacer(0.45f))
        return keys
    }

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

    private fun enterKey(chinese: Boolean = false, weight: Float = 1f) =
        KeyDef(if (chinese) "换行" else "return", KeyAction.Enter, style = KeyStyle.Enter, weight = weight)

    private fun symKey(chinese: Boolean, weight: Float) =
        KeyDef(if (chinese) "符" else "sym", KeyAction.Symbols, style = KeyStyle.Special, weight = weight)

    private fun num(v: String) = KeyDef(v, KeyAction.Text(v))
}
