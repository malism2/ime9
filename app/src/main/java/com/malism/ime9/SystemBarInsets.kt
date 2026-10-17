package com.malism.ime9

import android.os.Build
import android.view.View
import android.view.Window
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object SystemBarInsets {
    @Suppress("DEPRECATION")
    fun styleImeWindow(window: Window) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val color = ContextCompat.getColor(window.context, R.color.ime_bg)
        window.navigationBarColor = color
        window.statusBarColor = color
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = false
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightNavigationBars = false
            isAppearanceLightStatusBars = false
        }
    }

    fun padForNavigationBar(view: View, extraBottomPx: Int) {
        val apply: (View, WindowInsetsCompat) -> Unit = { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout(),
            )
            v.setPadding(bars.left, v.paddingTop, bars.right, extraBottomPx + bars.bottom)
        }
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            apply(v, insets)
            WindowInsetsCompat.Builder(insets)
                .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.NONE)
                .build()
        }
        if (view.isAttachedToWindow) {
            ViewCompat.getRootWindowInsets(view)?.let { apply(view, it) }
        } else {
            view.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    v.removeOnAttachStateChangeListener(this)
                    ViewCompat.requestApplyInsets(v)
                    ViewCompat.getRootWindowInsets(v)?.let { apply(v, it) }
                }

                override fun onViewDetachedFromWindow(v: View) = Unit
            })
        }
        ViewCompat.requestApplyInsets(view)
    }

    fun padForSystemBars(view: View, extraPx: Int) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                extraPx + bars.left,
                extraPx + bars.top,
                extraPx + bars.right,
                extraPx + bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
        ViewCompat.requestApplyInsets(view)
    }
}
