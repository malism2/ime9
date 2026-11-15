package com.malism.ime9

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.TextView
import com.malism.ime9.R
import com.malism.ime9.engine.Candidate
import com.malism.ime9.engine.InputEngine
import com.malism.ime9.engine.Romaji
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
    private val moreButton: TextView,
    private val candidatePanel: LinearLayout,
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
    private var candidatesExpanded = false
    private var candidatePage = 0

    fun start(info: EditorInfo?, subtypeLanguageTag: String? = null) {
        composing.setLength(0)
        candidatesExpanded = false
        candidatePage = 0
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

    private fun languageFromTag(tag: String?): LanguageMode? = LanguageMode.fromTag(tag)

    fun finish() {
        commitRawIfNeeded()
        composing.setLength(0)
        candidates = emptyList()
        candidatesExpanded = false
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
                switchLanguage(language.next())
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
        }
    }

    fun onLongLetter(text: String) {
        val picked = LanguageMode.entries.find { it.label == text }
        if (picked != null) {
            if (!numberPad.locked) switchLanguage(picked)
            return
        }
        if (text.length == 1 && text[0].isLetter()) {
            val ch = text[0]
            if (language == LanguageMode.Chinese && ch in 'a'..'z') {
                composing.append(ch)
                associating = false
                updateCandidatesFromComposing()
            } else if (language != LanguageMode.Chinese) {
                onLetter(ch)
            } else {
                commitDirect(text)
            }
        } else {
            commitDirect(text)
        }
    }

    private fun switchLanguage(next: LanguageMode) {
        if (next == language) return
        commitRawIfNeeded()
        language = next
        page = KeyboardPage.Letters
        shift = false
        refresh()
    }

    fun pickCandidate(index: Int) {
        val item = candidates.getOrNull(index) ?: return
        val keepOpen = candidatesExpanded
        candidatesExpanded = false
        val raw = composing.toString()
        val rest = if (language == LanguageMode.Chinese && raw.isNotEmpty()) {
            engine.pinyin.remaining(raw, item.hint)
        } else {
            ""
        }
        if (rest.isNotEmpty()) {
            val ic = connection() ?: return
            ic.commitText(item.text, 1)
            composing.setLength(0)
            composing.append(rest)
            lastCommit = item.text
            associating = false
            updateCandidatesFromComposing()
        } else {
            commitText(item.text)
        }
        candidatesExpanded = keepOpen && candidates.isNotEmpty()
        candidatePage = 0
        updateCandidateChrome()
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
            if (convertsOnSpace && first != null) {
                pickCandidate(0)
            } else {
                commitText(composing.toString())
                commitDirect(" ")
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
        if (convertsOnSpace && first != null) {
            val ic = connection()
            if (language == LanguageMode.Chinese) {
                val rest = engine.pinyin.remaining(composing.toString(), first.hint)
                ic?.commitText(first.text, 1)
                if (rest.isNotEmpty()) ic?.commitText(rest, 1)
            } else {
                ic?.commitText(first.text, 1)
            }
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
        candidates = when (language) {
            LanguageMode.Chinese -> engine.chineseCandidates(raw)
            LanguageMode.English -> engine.englishCandidates(raw, shift)
            LanguageMode.Spanish -> engine.spanishCandidates(raw, shift)
            LanguageMode.German -> engine.germanCandidates(raw, shift)
            LanguageMode.Russian -> engine.russianCandidates(raw, shift)
            LanguageMode.Arabic -> engine.arabicCandidates(raw)
            LanguageMode.Japanese -> engine.japaneseCandidates(raw)
        }
        val shown = when (language) {
            LanguageMode.Chinese -> engine.pinyin.displayComposing(raw, candidates.firstOrNull()?.hint)
            LanguageMode.Japanese -> Romaji.toHiragana(raw)
            else -> raw
        }
        ic?.setComposingText(shown, 1)
        composingView.text = shown
        renderCandidates()
    }

    private fun showAssociation() {
        connection()?.finishComposingText()
        composingView.text = ""
        candidates = when (language) {
            LanguageMode.Chinese -> engine.associate(lastCommit, chinese = true)
            LanguageMode.English -> engine.associate(lastCommit, chinese = false)
            LanguageMode.Spanish -> engine.associateEs(lastCommit)
            LanguageMode.German -> engine.associateDe(lastCommit)
            LanguageMode.Russian -> engine.associateRu(lastCommit)
            LanguageMode.Arabic -> engine.associateAr(lastCommit)
            LanguageMode.Japanese -> engine.associateJa(lastCommit)
        }
        renderCandidates()
    }

    private fun refresh() {
        composing.setLength(0)
        candidatesExpanded = false
        candidatePage = 0
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
            rtl = language.rtl && page != KeyboardPage.Numbers,
        )
        applyDirection()
        updateCandidateChrome()
    }

    private val convertsOnSpace: Boolean
        get() = language == LanguageMode.Chinese || language == LanguageMode.Japanese

    private fun applyDirection() {
        val rtl = language.rtl
        val dir = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        val textDir = if (rtl) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
        (composingView.parent as? View)?.layoutDirection = dir
        composingView.textDirection = textDir
        candidateBar.layoutDirection = dir
        candidatePanel.layoutDirection = dir
    }

    private fun updateCandidateChrome() {
        val showMore = !numberPad.locked && language == LanguageMode.Chinese && page == KeyboardPage.Letters
        if (!showMore) candidatesExpanded = false
        if (candidates.isEmpty()) candidatesExpanded = false
        moreButton.visibility = if (showMore) View.VISIBLE else View.GONE
        moreButton.text = if (candidatesExpanded) "收起" else "更多"
        moreButton.alpha = if (candidates.isEmpty()) 0.35f else 1f
        moreButton.setOnClickListener {
            if (candidates.isEmpty()) return@setOnClickListener
            candidatesExpanded = !candidatesExpanded
            candidatePage = 0
            updateCandidateChrome()
        }
        candidatePanel.visibility = if (candidatesExpanded) View.VISIBLE else View.GONE
        if (candidatesExpanded) renderCandidatePage() else candidatePanel.removeAllViews()
    }

    private fun renderCandidatePage() {
        candidatePanel.removeAllViews()
        val columns = 4
        val rows = 3
        val pageSize = columns * rows
        val pages = ((candidates.size + pageSize - 1) / pageSize).coerceAtLeast(1)
        if (candidatePage > pages - 1) candidatePage = pages - 1
        if (candidatePage < 0) candidatePage = 0
        val start = candidatePage * pageSize
        val pageItems = candidates.subList(start, minOf(start + pageSize, candidates.size))
        pageItems.chunked(columns).forEachIndexed { rowIndex, row ->
            val rowView = LinearLayout(candidatePanel.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(48),
                )
            }
            row.forEachIndexed { col, candidate ->
                val index = start + rowIndex * columns + col
                rowView.addView(candidateCell(candidate.text) { pickCandidate(index) })
            }
            candidatePanel.addView(rowView)
        }
        if (pages > 1) {
            val pager = LinearLayout(candidatePanel.context).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(44),
                )
            }
            pager.addView(candidateCell("上页", enabled = candidatePage > 0) {
                if (candidatePage > 0) {
                    candidatePage--
                    renderCandidatePage()
                }
            })
            pager.addView(candidateCell("${candidatePage + 1}/$pages", enabled = false) {})
            pager.addView(candidateCell("下页", enabled = candidatePage < pages - 1) {
                if (candidatePage < pages - 1) {
                    candidatePage++
                    renderCandidatePage()
                }
            })
            candidatePanel.addView(pager)
        }
    }

    private fun candidateCell(label: String, enabled: Boolean = true, onClick: () -> Unit): TextView {
        return TextView(candidatePanel.context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                val m = dp(3)
                setMargins(m, m, m, m)
            }
            gravity = Gravity.CENTER
            text = label
            maxLines = 2
            textSize = when {
                label.length <= 2 -> 18f
                label.length <= 4 -> 15f
                else -> 13f
            }
            alpha = if (enabled) 1f else 0.35f
            setTextColor(context.getColor(R.color.ime_text))
            setBackgroundResource(R.drawable.bg_key)
            if (enabled) setOnClickListener { onClick() }
        }
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        v.toFloat(),
        candidateBar.resources.displayMetrics,
    ).toInt()

    private fun renderCandidates() {
        candidateBar.removeAllViews()
        val barCount = minOf(candidates.size, BAR_LIMIT)
        for (index in 0 until barCount) {
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
            val candidate = candidates[index]
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
        if (numberPad.locked) {
            candidatesExpanded = false
            candidatePanel.visibility = View.GONE
            return
        }
        if (!associating && composing.isEmpty()) {
            composingView.visibility = View.GONE
        } else {
            composingView.visibility = if (composing.isEmpty()) View.GONE else View.VISIBLE
        }
        updateCandidateChrome()
    }

    private companion object {
        const val BAR_LIMIT = 8
    }
}
