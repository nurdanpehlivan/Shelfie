package com.shelfie.app.data.mapper

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiCheckTest {

    @Test
    fun `real emojis pass`() {
        listOf("🍅", "🥛", "♻️", "👩‍🍳", "🧑🏽‍🍳", "#️⃣").forEach { assertTrue(it, it.looksLikeEmoji()) }
    }

    @Test
    fun `keyboard symbols and words do not`() {
        listOf("|", "-", "*", "?", "", "egg", "🍅 tomato", "1").forEach { assertFalse(it, it.looksLikeEmoji()) }
    }
}
