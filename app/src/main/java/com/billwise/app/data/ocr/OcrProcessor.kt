package com.billwise.app.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object OcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Runs ML Kit text recognition on the image at [uri] and returns the extracted
     * text as a single String. Suspends until ML Kit returns a result.
     */
    suspend fun extractText(context: Context, uri: Uri): String {
        val image = InputImage.fromFilePath(context, uri)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(visionText.text)
                }
                .addOnFailureListener { exception ->
                    cont.resumeWithException(exception)
                }
        }
    }
}
