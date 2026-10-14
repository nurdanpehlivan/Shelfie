package com.shelfie.app.domain.model

/**
 * Typed failures of a shelfie photo scan, carried inside `Result.failure` so the UI can show a
 * specific message (and decide whether a retry makes sense) without knowing about the network.
 */
sealed class PantryScanException(message: String, cause: Throwable? = null) :
    Exception(message, cause) {

    /** The AI service is not configured (e.g. missing API key in the build). */
    class NotConfigured(message: String) : PantryScanException(message)

    /** The photo could not be opened or decoded. */
    class ImageUnreadable(cause: Throwable) :
        PantryScanException("Photo could not be read", cause)

    /** No connectivity or a timeout; usually worth retrying. */
    class Network(cause: Throwable) : PantryScanException("Network error", cause)

    /** The AI service answered with a non-2xx status. */
    class Api(
        val httpCode: Int,
        cause: Throwable? = null,
        /** The daily free-tier quota is used up, so retrying before tomorrow is pointless. */
        val isDailyQuotaExhausted: Boolean = false,
    ) : PantryScanException("AI service returned HTTP $httpCode", cause) {
        val isRateLimited: Boolean get() = httpCode == 429
    }

    /** The AI service refused to analyse the photo (safety filters etc.). */
    class Blocked(val reason: String) : PantryScanException("Analysis blocked: $reason")

    /** The AI service answered, but not with the expected JSON structure. */
    class MalformedResponse(message: String, cause: Throwable? = null) :
        PantryScanException(message, cause)

    class Unknown(cause: Throwable) : PantryScanException("Unexpected scan failure", cause)
}
