package com.billwise.app.data.parser

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream

object PdfParser {

    fun init(context: Context) {
        PDFBoxResourceLoader.init(context)
    }

    suspend fun extractTextFromUri(context: Context, uri: Uri): String {
        return withContext(Dispatchers.IO) {
            var text = ""
            var inputStream: InputStream? = null
            var document: PDDocument? = null
            try {
                inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    document = PDDocument.load(inputStream)
                    val stripper = PDFTextStripper()
                    stripper.sortByPosition = true
                    text = stripper.getText(document)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                document?.close()
                inputStream?.close()
            }
            text
        }
    }
}
