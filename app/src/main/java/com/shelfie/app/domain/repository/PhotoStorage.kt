package com.shelfie.app.domain.repository

/**
 * App-private storage for shelfie photos.
 *
 * Photo Picker and gallery URIs are only readable temporarily, so every photo that may end up in
 * [com.shelfie.app.domain.model.PantryItem.imageUri] is first copied here and referenced by a
 * stable URI that stays readable across restarts.
 */
interface PhotoStorage {

    /**
     * Creates an empty file the camera app can write into and returns its shareable URI.
     *
     * @throws java.io.IOException if the file cannot be created.
     */
    suspend fun createCaptureTarget(): String

    /**
     * Copies the image at [sourceUri] into app storage and returns the stored copy's URI.
     * URIs that already point into this storage are returned unchanged.
     *
     * @throws java.io.IOException if the source cannot be read or the copy fails.
     * @throws SecurityException if read access to [sourceUri] was revoked.
     */
    suspend fun importPhoto(sourceUri: String): String

    /** Deletes a stored photo; URIs that don't belong to this storage are ignored. */
    suspend fun delete(uri: String)
}
