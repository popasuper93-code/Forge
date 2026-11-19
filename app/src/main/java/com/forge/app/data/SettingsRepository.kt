package com.forge.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("forge_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val API_BASE = stringPreferencesKey("api_base")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL = stringPreferencesKey("model")
        val GITHUB_TOKEN = stringPreferencesKey("github_token")
        val GITHUB_USER = stringPreferencesKey("github_user")
        val SYSTEM = stringPreferencesKey("system_prompt")
    }

    val apiBaseUrl: Flow<String> = context.dataStore.data.map {
        it[Keys.API_BASE] ?: "https://api.openai.com/v1"
    }
    val apiKey: Flow<String> = context.dataStore.data.map { it[Keys.API_KEY] ?: "" }
    val model: Flow<String> = context.dataStore.data.map { it[Keys.MODEL] ?: "gpt-4o-mini" }
    val githubToken: Flow<String> = context.dataStore.data.map { it[Keys.GITHUB_TOKEN] ?: "" }
    val githubUser: Flow<String> = context.dataStore.data.map { it[Keys.GITHUB_USER] ?: "" }
    val systemPrompt: Flow<String> = context.dataStore.data.map {
        it[Keys.SYSTEM] ?: DEFAULT_SYSTEM
    }

    suspend fun setApiBaseUrl(v: String) = context.dataStore.edit { it[Keys.API_BASE] = v }
    suspend fun setApiKey(v: String) = context.dataStore.edit { it[Keys.API_KEY] = v }
    suspend fun setModel(v: String) = context.dataStore.edit { it[Keys.MODEL] = v }
    suspend fun setGithubToken(v: String) = context.dataStore.edit { it[Keys.GITHUB_TOKEN] = v }
    suspend fun setGithubUser(v: String) = context.dataStore.edit { it[Keys.GITHUB_USER] = v }
    suspend fun setSystemPrompt(v: String) = context.dataStore.edit { it[Keys.SYSTEM] = v }

    companion object {
        const val DEFAULT_SYSTEM = """You are Forge, a precise coding agent. Rules:
- No fluff. Short answers.
- When asked to build UI or a page, output a complete self-contained HTML document only, inside a markdown code fence marked html.
- Prefer black / dark UI unless asked otherwise.
- No emoji unless explicitly requested.
- If the user asks for an app structure, list files clearly.
- HTML must be valid and runnable in a mobile WebView."""
    }
}
