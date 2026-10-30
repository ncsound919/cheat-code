package com.propslab.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Reads the text out of a screenshot. The implementation is bundled with the app and runs
 * on the device, so importing a slip adds no network path of its own.
 */
interface ScreenshotReader {
    suspend fun read(context: Context, uri: Uri): String
}

class MlKitScreenshotReader : ScreenshotReader {

    private val recogniser by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun read(context: Context, uri: Uri): String = suspendCancellableCoroutine { cont ->
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (e: Exception) {
            cont.resumeWithException(e)
            return@suspendCancellableCoroutine
        }
        // One client for the process: creating a recogniser per screenshot holds native models
        // until each task is collected, which is a slow leak on a screen opened repeatedly.
        recogniser.process(image)
            .addOnSuccessListener { result -> if (cont.isActive) cont.resume(result.text) }
            .addOnFailureListener { e -> if (cont.isActive) cont.resumeWithException(e) }
    }
}
