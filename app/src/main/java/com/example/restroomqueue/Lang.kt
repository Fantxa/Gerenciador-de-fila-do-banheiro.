package com.example.restroomqueue

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class Lang { PT, EN }

/** Remembers the chosen language on the device. Default: Brazilian Portuguese. */
object Settings {
    private fun prefs(c: Context) = c.getSharedPreferences("settings", Context.MODE_PRIVATE)
    fun lang(c: Context): Lang = if (prefs(c).getString("lang", "pt-BR") == "en") Lang.EN else Lang.PT
    fun setLang(c: Context, l: Lang) = prefs(c).edit().putString("lang", if (l == Lang.EN) "en" else "pt-BR").apply()
}

val LocalLang = staticCompositionLocalOf { Lang.PT }

/** t("texto em português", "english text") – picks the string for the current language. */
@Composable fun t(pt: String, en: String): String = if (LocalLang.current == Lang.PT) pt else en
