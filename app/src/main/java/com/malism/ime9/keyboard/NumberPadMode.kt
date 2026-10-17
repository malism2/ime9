package com.malism.ime9.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo

data class NumberPadMode(
    val locked: Boolean,
    val phone: Boolean = false,
    val decimal: Boolean = false,
    val signed: Boolean = false,
) {
    companion object {
        val Optional = NumberPadMode(locked = false)

        fun from(info: EditorInfo?): NumberPadMode {
            val inputType = info?.inputType ?: return Optional
            return fromInputType(inputType)
        }

        fun fromInputType(inputType: Int): NumberPadMode {
            if (!isNumeric(inputType)) return Optional
            val cls = inputType and InputType.TYPE_MASK_CLASS
            val flags = inputType and InputType.TYPE_MASK_FLAGS
            return when (cls) {
                InputType.TYPE_CLASS_PHONE -> NumberPadMode(locked = true, phone = true)
                InputType.TYPE_CLASS_DATETIME -> NumberPadMode(
                    locked = true,
                    decimal = true,
                    signed = true,
                )
                else -> NumberPadMode(
                    locked = true,
                    decimal = flags and InputType.TYPE_NUMBER_FLAG_DECIMAL != 0,
                    signed = flags and InputType.TYPE_NUMBER_FLAG_SIGNED != 0,
                )
            }
        }

        fun isNumeric(inputType: Int): Boolean {
            if (inputType == InputType.TYPE_NULL || inputType == 0) return false
            return when (inputType and InputType.TYPE_MASK_CLASS) {
                InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_CLASS_PHONE,
                InputType.TYPE_CLASS_DATETIME,
                -> true
                else -> false
            }
        }
    }
}
