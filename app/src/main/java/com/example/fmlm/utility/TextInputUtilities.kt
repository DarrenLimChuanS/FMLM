package com.example.fmlm.utility

import com.google.android.material.textfield.TextInputLayout

object TextInputUtilities {
    fun clearText(textInputLayout: TextInputLayout) {
        textInputLayout.editText?.text?.clear()
    }

    fun isTextInputEmpty(textInputLayout: TextInputLayout): Boolean {
        val text = textInputLayout.editText?.text?.trim()

        if (text.isNullOrBlank())
            return true

        return false
    }

    fun getString(textInputLayout: TextInputLayout): String {
        return textInputLayout.editText?.text?.trim().toString()
    }
}