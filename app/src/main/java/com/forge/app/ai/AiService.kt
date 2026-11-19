package com.forge.app.ai

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class ChatTurn(val role: String, val content: String)

class AiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun chat(
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<ChatTurn>
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("API key not set"))
        }
        val root = baseUrl.trimEnd('/')
        val url = if (root.endsWith("/v1")) "$root/chat/completions" else "$root/v1/chat/completions"

        val messages = JsonArray()
        if (systemPrompt.isNotBlank()) {
            messages.add(JsonObject().apply {
                addProperty("role", "system")
                addProperty("content", systemPrompt)
            })
        }
        history.forEach { turn ->
            messages.add(JsonObject().apply {
                addProperty("role", turn.role)
                addProperty("content", turn.content)
            })
        }

        val body = JsonObject().apply {
            addProperty("model", model.ifBlank { "gpt-4o-mini" })
            add("messages", messages)
            addProperty("temperature", 0.4)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(gson.toJson(body).toRequestBody(jsonMedia))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("API ${response.code}: ${raw.take(300)}")
                    )
                }
                val parsed = gson.fromJson(raw, JsonObject::class.java)
                val content = parsed
                    .getAsJsonArray("choices")
                    ?.get(0)?.asJsonObject
                    ?.getAsJsonObject("message")
                    ?.get("content")?.asString
                    ?: return@withContext Result.failure(IllegalStateException("Empty response"))
                Result.success(content.trim())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun extractHtml(text: String): String? {
            val fence = Regex(
                "```(?:html|HTML)?\\s*\\n([\\s\\S]*?)```",
                RegexOption.IGNORE_CASE
            )
            fence.find(text)?.groupValues?.getOrNull(1)?.trim()?.let { return it }
            val trimmed = text.trim()
            if (trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
                trimmed.startsWith("<html", ignoreCase = true)
            ) {
                return trimmed
            }
            return null
        }
    }
}
