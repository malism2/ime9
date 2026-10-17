package com.malism.ime9

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodSubtype
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import com.malism.ime9.engine.InputEngine
import com.malism.ime9.engine.Lexicon
import com.malism.ime9.keyboard.KeyboardRenderer

class Input9ImeService : InputMethodService() {
    private lateinit var engine: InputEngine
    private var controller: ImeController? = null
    private var inputRoot: View? = null

    override fun onCreate() {
        super.onCreate()
        engine = InputEngine(loadLexicon())
        window?.window?.let(SystemBarInsets::styleImeWindow)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        window?.window?.let(SystemBarInsets::styleImeWindow)
        inputRoot?.let { ViewCompat.requestApplyInsets(it) }
    }

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.ime_view, null)
        inputRoot = view
        window?.window?.let(SystemBarInsets::styleImeWindow)
        SystemBarInsets.padForNavigationBar(
            view,
            extraBottomPx = resources.getDimensionPixelSize(R.dimen.ime_bottom_padding),
        )
        val composingView = view.findViewById<TextView>(R.id.composing_view)
        val candidateBar = view.findViewById<LinearLayout>(R.id.candidate_bar)
        val layoutToggle = view.findViewById<TextView>(R.id.layout_toggle)
        val host = view.findViewById<LinearLayout>(R.id.keyboard_host)
        lateinit var ctrl: ImeController
        val renderer = KeyboardRenderer(
            context = this,
            host = host,
            onAction = { ctrl.handle(it) },
            onLongText = { ctrl.onLongLetter(it) },
        )
        ctrl = ImeController(
            engine = engine,
            composingView = composingView,
            candidateBar = candidateBar,
            layoutToggle = layoutToggle,
            renderer = renderer,
            connection = { currentInputConnection },
        )
        controller = ctrl
        ctrl.start(currentInputEditorInfo, currentSubtypeLanguageTag())
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        controller?.start(attribute, currentSubtypeLanguageTag())
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        window?.window?.let(SystemBarInsets::styleImeWindow)
        inputRoot?.let { ViewCompat.requestApplyInsets(it) }
        controller?.start(info, currentSubtypeLanguageTag())
    }

    override fun onCurrentInputMethodSubtypeChanged(newSubtype: InputMethodSubtype) {
        super.onCurrentInputMethodSubtypeChanged(newSubtype)
        controller?.onSubtypeChanged(subtypeLanguageTag(newSubtype))
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        controller?.finish()
        super.onFinishInputView(finishingInput)
    }

    private fun currentSubtypeLanguageTag(): String? {
        val imm = getSystemService(InputMethodManager::class.java) ?: return null
        return subtypeLanguageTag(imm.currentInputMethodSubtype)
    }

    private fun subtypeLanguageTag(subtype: InputMethodSubtype?): String? {
        if (subtype == null) return null
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            subtype.languageTag.takeIf { it.isNotEmpty() }
                ?: subtype.locale
        } else {
            @Suppress("DEPRECATION")
            subtype.locale
        }
    }

    private fun deviceProtected(): Context = createDeviceProtectedStorageContext() ?: this

    private fun loadLexicon(): Lexicon {
        val ctx = deviceProtected()
        fun lines(name: String) = ctx.assets.open("dict/$name").bufferedReader().use { it.readLines() }.asSequence()
        return Lexicon.load(
            zhLines = lines("zh.txt"),
            enLines = lines("en.txt"),
            assocZhLines = lines("assoc_zh.txt"),
            assocEnLines = lines("assoc_en.txt"),
        )
    }
}
