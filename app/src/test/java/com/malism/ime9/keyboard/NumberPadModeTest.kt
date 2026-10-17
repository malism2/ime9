package com.malism.ime9.keyboard

import android.text.InputType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberPadModeTest {
    @Test
    fun numberClassLocksPad() {
        val mode = NumberPadMode.fromInputType(InputType.TYPE_CLASS_NUMBER)
        assertTrue(mode.locked)
        assertFalse(mode.decimal)
        assertFalse(mode.phone)
    }

    @Test
    fun numberDecimalAndSigned() {
        val mode = NumberPadMode.fromInputType(
            InputType.TYPE_CLASS_NUMBER
                or InputType.TYPE_NUMBER_FLAG_DECIMAL
                or InputType.TYPE_NUMBER_FLAG_SIGNED,
        )
        assertTrue(mode.locked)
        assertTrue(mode.decimal)
        assertTrue(mode.signed)
    }

    @Test
    fun phonePad() {
        val mode = NumberPadMode.fromInputType(InputType.TYPE_CLASS_PHONE)
        assertTrue(mode.locked)
        assertTrue(mode.phone)
    }

    @Test
    fun numberPasswordStillNumeric() {
        val mode = NumberPadMode.fromInputType(
            InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD,
        )
        assertTrue(mode.locked)
    }

    @Test
    fun textFieldIsNotNumeric() {
        assertFalse(NumberPadMode.isNumeric(InputType.TYPE_CLASS_TEXT))
        assertFalse(NumberPadMode.fromInputType(InputType.TYPE_CLASS_TEXT).locked)
    }
}
