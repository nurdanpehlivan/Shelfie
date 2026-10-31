package com.shelfie.app.data.mapper

/** One emoji can be several code points (skin tones, ZWJ sequences), never a word. */
internal const val MAX_EMOJI_LENGTH = 16

/**
 * Whether the model's answer is a real emoji: no letters, digits or spaces, at least one
 * pictograph-range code point, and no plain keyboard symbols. A bare "|" or "-" (which a model
 * sometimes writes instead of an emoji) would otherwise show as a stray bar on a tile.
 */
internal fun String.looksLikeEmoji(): Boolean {
    if (isEmpty() || length > MAX_EMOJI_LENGTH) return false
    if (any { it.isLetterOrDigit() || it.isWhitespace() }) return false
    val codePoints = codePoints().toArray()
    // Keycaps ("#️⃣", "*️⃣") are the only emojis that contain an ASCII character.
    if (codePoints.any { it < 0x80 && it != '#'.code && it != '*'.code }) return false
    return codePoints.any { it >= PICTOGRAPH_START }
}

/** Below this are letters, punctuation and dingbat-free symbols; emojis start above it. */
private const val PICTOGRAPH_START = 0x2100
