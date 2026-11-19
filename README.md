# FORGE

AI coding agent for Android. Black Codex-style UI.

## Features

- Chat with any OpenAI-compatible API
- HTML preview in WebView
- GitHub: connect token, list repos, create repo, push files
- Approve flow: generate -> preview -> push

## Setup

1. Install APK from Actions artifact
2. Settings -> API Base URL + API Key + Model
3. Settings -> GitHub token (scope: `repo`)
4. Save, then chat a task

## Flow

1. Describe UI or page
2. Open Preview when HTML is ready
3. Push to selected repo or create new repo

## Build

GitHub Actions builds debug APK on every push to `main`.

Artifact: **Forge-debug**

## Stack

Kotlin, Jetpack Compose, OkHttp, DataStore, WebView
