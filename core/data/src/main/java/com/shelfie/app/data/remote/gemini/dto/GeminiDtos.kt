package com.shelfie.app.data.remote.gemini.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Minimal subset of the Gemini `generateContent` REST contract that Shelfie needs.
// Nullable fields are omitted from requests (Json.explicitNulls = false).

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null,
)

@Serializable
data class Content(
    val role: String? = null,
    val parts: List<Part> = emptyList(),
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null,
    /** True for "thinking" parts of reasoning models; these never contain the answer. */
    val thought: Boolean? = null,
)

@Serializable
data class InlineData(
    val mimeType: String,
    /** Base64-encoded bytes. */
    val data: String,
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String,
    /** OpenAPI-subset schema the model's JSON output is constrained to. */
    val responseSchema: JsonObject,
    val temperature: Double? = null,
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate> = emptyList(),
    val promptFeedback: PromptFeedback? = null,
)

@Serializable
data class Candidate(
    val content: Content? = null,
    val finishReason: String? = null,
)

@Serializable
data class PromptFeedback(
    val blockReason: String? = null,
)
