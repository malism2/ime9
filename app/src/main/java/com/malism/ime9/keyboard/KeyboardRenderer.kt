package com.malism.ime9.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.malism.ime9.R

class KeyboardRenderer(
    private val context: Context,
    private val host: LinearLayout,
    private val onAction: (KeyAction) -> Unit,
    private val onLongText: (String) -> Unit,
) {
    fun render(spec: KeyboardSpec, rtl: Boolean = false) {
        host.layoutDirection = if (rtl) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
        host.removeAllViews()
        when (spec) {
            is KeyboardSpec.Grid -> renderGrid(spec)
            is KeyboardSpec.WeChatT9 -> renderWeChatT9(spec)
        }
    }

    private fun renderGrid(spec: KeyboardSpec.Grid) {
        val pad = dp(3)
        for (row in spec.rows) {
            host.addView(makeRow(row, spec.rowHeightDp, pad))
        }
    }

    private fun renderWeChatT9(spec: KeyboardSpec.WeChatT9) {
        val pad = dp(3)
        val height = dp(spec.rowHeightDp) * 4
        val main = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                height,
            )
        }
        main.addView(column(spec.left, 1.05f, pad))
        val center = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 3.35f)
        }
        for (row in spec.pad) {
            center.addView(
                makeRow(row, spec.rowHeightDp, pad).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1f,
                    )
                },
            )
        }
        main.addView(center)
        val right = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1.15f)
        }
        right.addView(makeKey(spec.del, pad, fillWeight = 1.15f))
        right.addView(makeKey(spec.enter, pad, fillWeight = 1.85f))
        main.addView(right)
        host.addView(main)
    }

    private fun column(keys: List<KeyDef>, weight: Float, pad: Int): LinearLayout {
        val col = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, weight)
        }
        for (key in keys) {
            col.addView(makeKey(key, pad, fillWeight = 1f))
        }
        return col
    }

    private fun makeRow(row: List<KeyDef>, rowHeightDp: Int, pad: Int): LinearLayout {
        val rowView = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(rowHeightDp),
            )
        }
        for (key in row) {
            if (key.label.isEmpty() && key.action is KeyAction.Text && key.action.value.isEmpty()) {
                val spacer = View(context)
                spacer.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, key.weight)
                rowView.addView(spacer)
                continue
            }
            rowView.addView(makeKey(key, pad))
        }
        return rowView
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun makeKey(key: KeyDef, pad: Int, fillWeight: Float? = null): View {
        val layoutParams = if (fillWeight != null) {
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, fillWeight).apply {
                setMargins(pad, pad, pad, pad)
            }
        } else {
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, key.weight).apply {
                setMargins(pad, pad, pad, pad)
            }
        }
        val bg = when (key.style) {
            KeyStyle.Enter -> R.drawable.bg_key_enter
            KeyStyle.Special -> R.drawable.bg_key_special
            KeyStyle.Normal -> R.drawable.bg_key
        }
        val view: View = if (key.iconRes != 0) {
            ImageView(context).apply {
                this.layoutParams = layoutParams
                scaleType = ImageView.ScaleType.CENTER_INSIDE
                setPadding(dp(10), dp(10), dp(10), dp(10))
                setImageResource(key.iconRes)
                val tint = ContextCompat.getColor(
                    context,
                    if (key.iconTintRes != 0) key.iconTintRes else R.color.ime_text,
                )
                colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_IN)
                contentDescription = when (key.action) {
                    KeyAction.Shift -> "Shift"
                    KeyAction.Delete -> "删除"
                    else -> key.label
                }
                setBackgroundResource(bg)
            }
        } else {
            TextView(context).apply {
                this.layoutParams = layoutParams
                gravity = Gravity.CENTER
                val onEnter = key.style == KeyStyle.Enter
                setTextColor(
                    ContextCompat.getColor(context, if (onEnter) R.color.ime_on_enter else R.color.ime_text),
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, if (key.hint.isEmpty()) 17f else 15f)
                text = if (key.hint.isNotEmpty()) "${key.label}\n${key.hint}" else key.label
                setBackgroundResource(bg)
            }
        }
        var popup: PopupWindow? = null
        view.setOnClickListener { onAction(key.action) }
        view.setOnLongClickListener {
            if (key.longPress.isEmpty()) return@setOnLongClickListener false
            val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
            for (item in key.longPress) {
                val itemView = TextView(context).apply {
                    text = item
                    setPadding(dp(14), dp(10), dp(14), dp(10))
                    setTextColor(ContextCompat.getColor(context, R.color.ime_text))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    setOnClickListener {
                        onLongText(item)
                        popup?.dismiss()
                    }
                }
                row.addView(itemView)
            }
            popup = PopupWindow(row, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
                setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.bg_key_special))
                elevation = 8f
                showAsDropDown(view, 0, -view.height)
            }
            true
        }
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> v.alpha = 0.72f
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.alpha = 1f
            }
            false
        }
        return view
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        v.toFloat(),
        context.resources.displayMetrics,
    ).toInt()
}
