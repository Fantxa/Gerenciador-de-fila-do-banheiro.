package com.example.restroomqueue

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/** Android has no PyPDF2; PdfBox-Android is the Kotlin/Java equivalent. */
object PdfNames {
    fun extract(ctx: Context, uri: Uri): List<String> {
        PDFBoxResourceLoader.init(ctx)
        ctx.contentResolver.openInputStream(uri)!!.use { stream ->
            PDDocument.load(stream).use { doc ->
                return PDFTextStripper().getText(doc).lines()
                    .map { it.trim().replace(Regex("^\\d+\\s*[.)\\-–]*\\s*"), "") } // drop "1." / "01 -"
                    .filter { it.length in 3..60 && it.any(Char::isLetter) && none(it) }
                    .distinct()
            }
        }
    }
    private fun none(s: String) = s.any(Char::isDigit) // skip lines with digits (dates, ids, headers)
}
