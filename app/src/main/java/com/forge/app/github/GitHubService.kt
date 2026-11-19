package com.forge.app.github

import android.util.Base64
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class RepoInfo(val name: String, val fullName: String, val htmlUrl: String)

class GitHubService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun validateToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        if (token.isBlank()) return@withContext Result.failure(IllegalStateException("Token empty"))
        val request = Request.Builder()
            .url("https://api.github.com/user")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("GitHub ${response.code}"))
                }
                val login = gson.fromJson(raw, JsonObject::class.java).get("login")?.asString
                    ?: return@withContext Result.failure(IllegalStateException("No login"))
                Result.success(login)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRepo(
        token: String,
        name: String,
        description: String = "Created by Forge",
        isPrivate: Boolean = false
    ): Result<RepoInfo> = withContext(Dispatchers.IO) {
        val body = JsonObject().apply {
            addProperty("name", name)
            addProperty("description", description)
            addProperty("private", isPrivate)
            addProperty("auto_init", true)
        }
        val request = Request.Builder()
            .url("https://api.github.com/user/repos")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .post(gson.toJson(body).toRequestBody(jsonMedia))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Create repo ${response.code}: ${raw.take(200)}")
                    )
                }
                val obj = gson.fromJson(raw, JsonObject::class.java)
                Result.success(
                    RepoInfo(
                        name = obj.get("name").asString,
                        fullName = obj.get("full_name").asString,
                        htmlUrl = obj.get("html_url").asString
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun putFile(
        token: String,
        owner: String,
        repo: String,
        path: String,
        content: String,
        message: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val encoded = Base64.encodeToString(content.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        val sha = getFileSha(token, owner, repo, path)

        val body = JsonObject().apply {
            addProperty("message", message)
            addProperty("content", encoded)
            if (sha != null) addProperty("sha", sha)
        }

        val request = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/contents/$path")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .put(gson.toJson(body).toRequestBody(jsonMedia))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        IllegalStateException("Put file ${response.code}: ${raw.take(200)}")
                    )
                }
                Result.success("ok")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getFileSha(token: String, owner: String, repo: String, path: String): String? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$owner/$repo/contents/$path")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .get()
            .build()
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val raw = response.body?.string().orEmpty()
                gson.fromJson(raw, JsonObject::class.java).get("sha")?.asString
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun listRepos(token: String): Result<List<RepoInfo>> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/user/repos?sort=updated&per_page=30")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "application/vnd.github+json")
            .get()
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IllegalStateException("Repos ${response.code}"))
                }
                val arr = gson.fromJson(raw, Array<JsonObject>::class.java)
                Result.success(arr.map {
                    RepoInfo(
                        name = it.get("name").asString,
                        fullName = it.get("full_name").asString,
                        htmlUrl = it.get("html_url").asString
                    )
                })
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
