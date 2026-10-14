package com.shelfie.app.data.local.image

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.shelfie.app.domain.repository.PhotoStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores photos under `filesDir/shelfies` and exposes them through [FileProvider] URIs, which the
 * camera app can write into and Coil / [com.shelfie.app.data.remote.gemini.ImageCompressor] can
 * read from.
 */
@Singleton
class LocalPhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) : PhotoStorage {

    private val authority = "${context.packageName}.$AUTHORITY_SUFFIX"
    private val photoDir: File get() = File(context.filesDir, PHOTO_DIR)

    override suspend fun createCaptureTarget(): String = withContext(Dispatchers.IO) {
        newPhotoFile(EXTENSION_JPEG).toShareableUri()
    }

    override suspend fun importPhoto(sourceUri: String): String = withContext(Dispatchers.IO) {
        if (sourceUri.toOwnedFile() != null) return@withContext sourceUri

        val source = Uri.parse(sourceUri)
        val resolver = context.contentResolver
        val extension = resolver.getType(source)
            ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
            ?: EXTENSION_JPEG
        val target = newPhotoFile(extension)
        try {
            val input = resolver.openInputStream(source)
                ?: throw FileNotFoundException("Cannot open $sourceUri")
            input.use { stream -> target.outputStream().use { stream.copyTo(it) } }
        } catch (e: Exception) {
            target.delete()
            throw e
        }
        target.toShareableUri()
    }

    override suspend fun delete(uri: String) {
        withContext(Dispatchers.IO) { uri.toOwnedFile()?.delete() }
    }

    private fun newPhotoFile(extension: String): File {
        val dir = photoDir
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create $dir")
        return File(dir, "$FILE_PREFIX${UUID.randomUUID()}.$extension").apply {
            if (!createNewFile()) throw IOException("Cannot create $this")
        }
    }

    private fun File.toShareableUri(): String =
        FileProvider.getUriForFile(context, authority, this).toString()

    /** Maps one of our FileProvider URIs back to its file; anything else yields `null`. */
    private fun String.toOwnedFile(): File? {
        val uri = Uri.parse(this)
        if (uri.authority != authority) return null
        val segments = uri.pathSegments
        if (segments.size != 2 || segments[0] != PHOTO_DIR) return null
        val name = segments[1]
        // Our own naming scheme doubles as a guard against path traversal ("..").
        if (!name.startsWith(FILE_PREFIX)) return null
        return File(photoDir, name)
    }

    private companion object {
        /** Must match `res/xml/file_paths.xml` and the provider authority in the manifest. */
        const val PHOTO_DIR = "shelfies"
        const val AUTHORITY_SUFFIX = "fileprovider"
        const val FILE_PREFIX = "shelfie_"
        const val EXTENSION_JPEG = "jpg"
    }
}
