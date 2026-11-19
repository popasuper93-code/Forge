package com.forge.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.forge.app.ForgeApp
import com.forge.app.ai.AiService
import com.forge.app.ai.ChatTurn
import com.forge.app.github.GitHubService
import com.forge.app.github.RepoInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class UiMessage(
    val id: Long,
    val role: String,
    val content: String,
    val html: String? = null
)

enum class AgentPhase {
    IDLE, THINKING, PREVIEW, PUSHING, ERROR
}

class ForgeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as ForgeApp
    private val settings = app.settings
    private val ai = AiService()
    private val github = GitHubService()

    private val _messages = MutableStateFlow<List<UiMessage>>(emptyList())
    val messages: StateFlow<List<UiMessage>> = _messages.asStateFlow()

    private val _phase = MutableStateFlow(AgentPhase.IDLE)
    val phase: StateFlow<AgentPhase> = _phase.asStateFlow()

    private val _previewHtml = MutableStateFlow<String?>(null)
    val previewHtml: StateFlow<String?> = _previewHtml.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _apiReady = MutableStateFlow(false)
    val apiReady: StateFlow<Boolean> = _apiReady.asStateFlow()

    private val _ghReady = MutableStateFlow(false)
    val ghReady: StateFlow<Boolean> = _ghReady.asStateFlow()

    private val _ghUser = MutableStateFlow("")
    val ghUser: StateFlow<String> = _ghUser.asStateFlow()

    private val _repos = MutableStateFlow<List<RepoInfo>>(emptyList())
    val repos: StateFlow<List<RepoInfo>> = _repos.asStateFlow()

    private val _selectedRepo = MutableStateFlow<RepoInfo?>(null)
    val selectedRepo: StateFlow<RepoInfo?> = _selectedRepo.asStateFlow()

    private var seq = 0L

    init {
        viewModelScope.launch {
            settings.apiKey.collect { key ->
                _apiReady.value = key.isNotBlank()
            }
        }
        viewModelScope.launch {
            settings.githubToken.collect { token ->
                _ghReady.value = token.isNotBlank()
                if (token.isNotBlank()) {
                    val user = settings.githubUser.first()
                    if (user.isNotBlank()) _ghUser.value = user
                }
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun send(prompt: String) {
        val text = prompt.trim()
        if (text.isEmpty()) return
        if (_phase.value == AgentPhase.THINKING) return

        _error.value = null
        _status.value = null
        seq += 1
        _messages.value = _messages.value + UiMessage(seq, "user", text)
        _phase.value = AgentPhase.THINKING

        viewModelScope.launch {
            val base = settings.apiBaseUrl.first()
            val key = settings.apiKey.first()
            val model = settings.model.first()
            val system = settings.systemPrompt.first()

            val history = _messages.value.map { ChatTurn(it.role, it.content) }
            val result = ai.chat(base, key, model, system, history)

            result.fold(
                onSuccess = { reply ->
                    val html = AiService.extractHtml(reply)
                    seq += 1
                    _messages.value = _messages.value + UiMessage(seq, "assistant", reply, html)
                    if (html != null) {
                        _previewHtml.value = html
                        _phase.value = AgentPhase.PREVIEW
                        _status.value = "HTML preview ready"
                    } else {
                        _phase.value = AgentPhase.IDLE
                        _status.value = "Reply ready"
                    }
                },
                onFailure = { e ->
                    _phase.value = AgentPhase.ERROR
                    _error.value = e.message ?: "Request failed"
                }
            )
        }
    }

    fun setPreview(html: String) {
        _previewHtml.value = html
        _phase.value = AgentPhase.PREVIEW
    }

    fun clearPreview() {
        _previewHtml.value = null
        if (_phase.value == AgentPhase.PREVIEW) _phase.value = AgentPhase.IDLE
    }

    fun connectGitHub() {
        viewModelScope.launch {
            val token = settings.githubToken.first()
            _status.value = "Checking GitHub..."
            github.validateToken(token).fold(
                onSuccess = { login ->
                    settings.setGithubUser(login)
                    _ghUser.value = login
                    _ghReady.value = true
                    _status.value = "Connected as $login"
                    refreshRepos()
                },
                onFailure = { e ->
                    _error.value = e.message ?: "GitHub auth failed"
                    _status.value = null
                }
            )
        }
    }

    fun refreshRepos() {
        viewModelScope.launch {
            val token = settings.githubToken.first()
            if (token.isBlank()) return@launch
            github.listRepos(token).fold(
                onSuccess = { _repos.value = it },
                onFailure = { e -> _error.value = e.message }
            )
        }
    }

    fun selectRepo(repo: RepoInfo?) {
        _selectedRepo.value = repo
    }

    fun pushHtmlToGitHub(fileName: String = "index.html") {
        val html = _previewHtml.value ?: run {
            _error.value = "No HTML to push"
            return
        }
        viewModelScope.launch {
            val token = settings.githubToken.first()
            val user = settings.githubUser.first().ifBlank { _ghUser.value }
            val repo = _selectedRepo.value

            if (token.isBlank() || user.isBlank()) {
                _error.value = "Connect GitHub in Settings"
                return@launch
            }
            if (repo == null) {
                _error.value = "Select a repository"
                return@launch
            }

            _phase.value = AgentPhase.PUSHING
            _status.value = "Pushing $fileName..."

            github.putFile(
                token = token,
                owner = user,
                repo = repo.name,
                path = fileName,
                content = html,
                message = "Forge: update $fileName"
            ).fold(
                onSuccess = {
                    _phase.value = AgentPhase.IDLE
                    _status.value = "Pushed to ${repo.fullName}/$fileName"
                },
                onFailure = { e ->
                    _phase.value = AgentPhase.ERROR
                    _error.value = e.message ?: "Push failed"
                }
            )
        }
    }

    fun createRepoAndPush(repoName: String, fileName: String = "index.html") {
        val html = _previewHtml.value ?: run {
            _error.value = "No HTML to push"
            return
        }
        val name = repoName.trim().ifBlank { "forge-preview" }
        viewModelScope.launch {
            val token = settings.githubToken.first()
            if (token.isBlank()) {
                _error.value = "Connect GitHub in Settings"
                return@launch
            }
            _phase.value = AgentPhase.PUSHING
            _status.value = "Creating repo $name..."

            github.createRepo(token, name, "Forge generated project").fold(
                onSuccess = { repo ->
                    _selectedRepo.value = repo
                    settings.githubUser.first().let { if (it.isBlank()) settings.setGithubUser(repo.fullName.substringBefore('/')) }
                    val owner = repo.fullName.substringBefore('/')
                    _ghUser.value = owner
                    _status.value = "Uploading $fileName..."
                    github.putFile(
                        token, owner, repo.name, fileName, html, "Forge: initial $fileName"
                    ).fold(
                        onSuccess = {
                            refreshRepos()
                            _phase.value = AgentPhase.IDLE
                            _status.value = "Created ${repo.htmlUrl}"
                        },
                        onFailure = { e ->
                            _phase.value = AgentPhase.ERROR
                            _error.value = e.message
                        }
                    )
                },
                onFailure = { e ->
                    _phase.value = AgentPhase.ERROR
                    _error.value = e.message
                }
            )
        }
    }

    fun clearChat() {
        _messages.value = emptyList()
        _previewHtml.value = null
        _phase.value = AgentPhase.IDLE
        _status.value = null
        _error.value = null
    }
}
