package com.malism.ime9

import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import com.malism.ime9.R
import com.malism.ime9.engine.Candidate
import com.malism.ime9.engine.InputEngine
import com.malism.ime9.keyboard.ChineseLayout
import com.malism.ime9.keyboard.KeyAction
import com.malism.ime9.keyboard.KeyboardLayouts
import com.malism.ime9.keyboard.KeyboardPage
import com.malism.ime9.keyboard.KeyboardRenderer
import com.malism.ime9.keyboard.LanguageMode
import com.malism.ime9.keyboard.NumberPadMode

class ImeController(
    private val engine: InputEngine,
    private val composingView: TextView,
    private val candidateBar: LinearLayout,
    private val layoutToggle: TextView,
    private val renderer: KeyboardRenderer,
    private val connection: () -> InputConnection?,
) {
    var language = LanguageMode.English
        private set
    var chineseLayout = ChineseLayout.T9
        private set
    var page = KeyboardPage.Letters
        private set
    var shift = false
        private set

    private val composing = StringBuilder()
    private var candidates: List<Candidate> = emptyList()
    private var lastCommit = ""
    private var associating = false
    private var numberPad = NumberPadMode.Optional
    private var languageInitialized = false

    fun start(info: EditorInfo?, subtypeLanguageTag: String? = null) {
        composing.setLength(0)
        associating = false
        lastCommit = ""
        if (!languageInitialized) {
            language = resolveLanguage(info, subtypeLanguageTag)
            languageInitialized = true
        }
        numberPad = NumberPadMode.from(info)
        page = if (numberPad.locked) KeyboardPage.Numbers else KeyboardPage.Letters
        refresh()
    }

    fun onSubtypeChanged(subtypeLanguageTag: String?) {
        val next = languageFromTag(subtypeLanguageTag) ?: return
        languageInitialized = true
        if (next == language) return
        commitRawIfNeeded()
        language = next
        page = KeyboardPage.Letters
        shift = false
        refresh()
    }

    private fun resolveLanguage(info: EditorInfo?, subtypeLanguageTag: String?): LanguageMode {
        languageFromTag(subtypeLanguageTag)?.let { return it }
        val locales = info?.hintLocales
        val langTag = if (locales != null && locales.size() > 0) locales[0].language else null
        return languageFromTag(langTag) ?: LanguageMode.English
    }

    private fun languageFromTag(tag: String?): LanguageMode? {
        val lang = tag?.lowercase()?.substringBefore('-')?.substringBefore('_') ?: return null
        return when (lang) {
            "zh" -> LanguageMode.Chinese
            "en" -> LanguageMode.English
            else -> null
        }
    }

    fun finish() {
        commitRawIfNeeded()
        composing.setLength(0)
        candidates = emptyList()
        associating = false
    }

    fun handle(action: KeyAction) {
        when (action) {
            is KeyAction.Letter -> onLetter(action.ch)
            is KeyAction.T9Digit -> onT9(action.digit)
            is KeyAction.Text -> commitDirect(action.value)
            KeyAction.Shift -> {
                shift = !shift
                renderKeyboard()
            }
            KeyAction.Delete -> onDelete()
            KeyAction.Space -> onSpace()
            KeyAction.Enter -> onEnter()
            KeyAction.Language -> {
                if (numberPad.locked) return
                commitRawIfNeeded()
                language = if (language == LanguageMode.Chinese) LanguageMode.English else LanguageMode.Chinese
                page = KeyboardPage.Letters
                shift = false
                refresh()
            }
            KeyAction.LayoutToggle -> {
                if (numberPad.locked) return
                commitRawIfNeeded()
                chineseLayout = if (chineseLayout == ChineseLayout.T9) ChineseLayout.Qwerty else ChineseLayout.T9
                page = KeyboardPage.Letters
                refresh()
            }
            KeyAction.Symbols -> {
                if (numberPad.locked) return
                page = KeyboardPage.Symbols
                renderKeyboard()
            }
            KeyAction.Numbers -> {
                page = KeyboardPage.Numbers
                renderKeyboard()
            }
            KeyAction.Letters -> {
                if (numberPad.locked) return
                page = KeyboardPage.Letters
                renderKeyboard()
            }
            KeyAction.Separator -> {
                if (language == LanguageMode.Chinese && composing.isNotEmpty()) {
                    composing.append('\'')
                    updateCandidatesFromComposing()
                } else {
                    commitDirect("，")
                }
            }
            KeyAction.Clear -> {
                composing.setLength(0)
                connection()?.finishComposingText()
                lastCommit = ""
                associating = true
                showAssociation()
            }
        }
    }

    fun onLongLetter(text: String) {
        if (text.length == 1 && text[0] in 'a'..'z') {
            composing.append(text)
            associating = false
            updateCandidatesFromComposing()
        } else {
            commitDirect(text)
        }
    }

    fun pickCandidate(index: Int) {
        val item = candidates.getOrNull(index) ?: return
        commitText(item.text)
    }

    private fun onLetter(ch: Char) {
        associating = false
        if (language == LanguageMode.Chinese) {
            composing.append(ch.lowercaseChar())
            updateCandidatesFromComposing()
        } else {
            composing.append(if (shift) ch.uppercaseChar() else ch)
            if (shift) {
                shift = false
                renderKeyboard()
            }
            updateCandidatesFromComposing()
        }
    }

    private fun onT9(digit: Char) {
        associating = false
        composing.append(digit)
        updateCandidatesFromComposing()
    }

    private fun onDelete() {
        val ic = connection() ?: return
        if (composing.isNotEmpty()) {
            composing.deleteCharAt(composing.lastIndex)
            if (composing.isEmpty()) {
                associating = true
                showAssociation()
            } else {
                associating = false
                updateCandidatesFromComposing()
            }
            return
        }
        ic.deleteSurroundingText(1, 0)
        lastCommit = ""
        associating = true
        showAssociation()
    }

    private fun onSpace() {
        if (composing.isNotEmpty()) {
            val first = candidates.firstOrNull()
            if (first != null && language == LanguageMode.Chinese) {
                commitText(first.text)
            } else if (language == LanguageMode.English) {
                commitText(composing.toString())
                commitDirect(" ")
            } else {
                commitText(composing.toString())
            }
        } else {
            commitDirect(" ")
            associating = false
            lastCommit = ""
            candidates = emptyList()
            renderCandidates()
        }
    }

    private fun onEnter() {
        if (composing.isNotEmpty()) {
            commitText(composing.toString())
            return
        }
        val ic = connection() ?: return
        ic.sendKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER),
        )
        ic.sendKeyEvent(
            android.view.KeyEvent(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER),
        )
    }

    private fun commitDirect(text: String) {
        if (text.isEmpty()) return
        commitRawIfNeeded()
        connection()?.commitText(text, 1)
        lastCommit = ""
        associating = false
        candidates = emptyList()
        renderCandidates()
    }

    private fun commitRawIfNeeded() {
        if (composing.isEmpty()) return
        val first = candidates.firstOrNull()
        if (language == LanguageMode.Chinese && first != null) {
            connection()?.commitText(first.text, 1)
            lastCommit = first.text
        } else {
            connection()?.commitText(composing.toString(), 1)
            lastCommit = composing.toString()
        }
        composing.setLength(0)
    }

    private fun commitText(text: String) {
        val ic = connection() ?: return
        ic.commitText(text, 1)
        composing.setLength(0)
        lastCommit = text
        associating = true
        showAssociation()
    }

    private fun updateCandidatesFromComposing() {
        val ic = connection()
        val raw = composing.toString()
        candidates = if (language == LanguageMode.Chinese) {
            engine.chineseCandidates(raw)
        } else {
            engine.englishCandidates(raw, shift)
        }
        val shown = if (language == LanguageMode.Chinese) {
            engine.pinyin.displayComposing(raw, candidates.firstOrNull()?.hint)
        } else {
            raw
        }
        ic?.setComposingText(shown, 1)
        composingView.text = shown
        renderCandidates()
    }

    private fun showAssociation() {
        connection()?.finishComposingText()
        composingView.text = ""
        candidates = engine.associate(lastCommit, language == LanguageMode.Chinese)
        renderCandidates()
    }

    private fun refresh() {
        composing.setLength(0)
        connection()?.finishComposingText()
        composingView.text = ""
        associating = !numberPad.locked
        if (numberPad.locked) {
            candidates = emptyList()
            renderCandidates()
        } else {
            showAssociation()
        }
        renderKeyboard()
    }

    private fun renderKeyboard() {
        renderer.render(
            KeyboardLayouts.current(page, language, chineseLayout, shift, numberPad),
        )
        updateLayoutToggle()
    }

    private fun updateLayoutToggle() {
        val show = !numberPad.locked && language == LanguageMode.Chinese && page == KeyboardPage.Letters
        layoutToggle.visibility = if (show) View.VISIBLE else View.GONE
        layoutToggle.text = if (chineseLayout == ChineseLayout.T9) "26键" else "九宫"
        layoutToggle.setOnClickListener { handle(KeyAction.LayoutToggle) }
    }

    private fun renderCandidates() {
        candidateBar.removeAllViews()
        candidates.forEachIndexed { index, candidate ->
            if (index > 0) {
                val divider = View(candidateBar.context).apply {
                    layoutParams = LinearLayout.LayoutParams(2, LinearLayout.LayoutParams.MATCH_PARENT).apply {
                        topMargin = 12
                        bottomMargin = 12
                    }
                    setBackgroundColor(candidateBar.context.getColor(R.color.ime_candidate_divider))
                }
                candidateBar.addView(divider)
            }
            val tv = TextView(candidateBar.context).apply {
                text = candidate.text
                setPadding(32, 8, 32, 8)
                setTextColor(candidateBar.context.getColor(R.color.ime_text))
                textSize = 17f
                setOnClickListener { pickCandidate(index) }
            }
            candidateBar.addView(tv)
        }
        val candidateRow = composingView.parent as? View
        candidateRow?.visibility = if (numberPad.locked) View.GONE else View.VISIBLE
        if (numberPad.locked) return
        if (!associating && composing.isEmpty()) {
            composingView.visibility = View.GONE
        } else {
            composingView.visibility = if (composing.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}
