package com.example.focusguard_v20.ui.focus.keywords

import android.content.Context

internal object KeywordEraserPrefs {
    private const val Name = "keyword_eraser_prefs"
    private const val KeyKeywords = "keywords"

    fun getKeywords(context: Context): Set<String> {
        return context.getSharedPreferences(Name, Context.MODE_PRIVATE)
            .getStringSet(KeyKeywords, emptySet()) ?: emptySet()
    }

    fun setKeywords(context: Context, keywords: Set<String>) {
        context.getSharedPreferences(Name, Context.MODE_PRIVATE).edit()
            .putStringSet(KeyKeywords, keywords)
            .apply()
    }

    fun addKeyword(context: Context, keyword: String) {
        val current = getKeywords(context).toMutableSet()
        if (current.add(keyword)) {
            setKeywords(context, current)
        }
    }

    fun removeKeyword(context: Context, keyword: String) {
        val current = getKeywords(context).toMutableSet()
        if (current.remove(keyword)) {
            setKeywords(context, current)
        }
    }
}
