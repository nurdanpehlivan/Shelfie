package com.shelfie.app.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.shelfie.app.domain.model.ExpiryDateParser
import com.shelfie.app.domain.repository.ExpiryDateReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * ML Kit's on-device Latin text recognition (served by Play services, so it adds almost
 * nothing to the APK), then [ExpiryDateParser] picks the expiry out of the text.
 */
@Singleton
class MlKitExpiryDateReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : ExpiryDateReader {

    private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    override suspend fun read(imageUri: String, today: LocalDate): LocalDate? {
        val image = try {
            InputImage.fromFilePath(context, Uri.parse(imageUri))
        } catch (e: IOException) {
            return null
        }
        val text = suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { continuation.resume(it.text) }
                // Model still downloading, or the image unreadable: same as finding nothing.
                .addOnFailureListener { continuation.resume("") }
        }
        return ExpiryDateParser.parse(text, today)
    }
}
