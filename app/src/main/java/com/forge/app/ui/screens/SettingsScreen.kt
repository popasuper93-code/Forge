package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ForgeApp
import com.forge.app.ui.ForgeViewModel
import com.forge.app.ui.theme.Accent
import com.forge.app.ui.theme.Black
import com.forge.app.ui.theme.Border
import com.forge.app.ui.theme.Surface
import com.forge.app.ui.theme.TextMuted
import com.forge.app.ui.theme.TextPrimary
import com.forge.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    app: ForgeApp,
    vm: ForgeViewModel,
    onBack: () -> Unit
) {
    val settings = app.settings
    val scope = rememberCoroutineScope()

    val curBase by settings.apiBaseUrl.collectAsState(initial = "https://api.openai.com/v1")
    val curKey by settings.apiKey.collectAsState(initial = "")
    val curModel by settings.model.collectAsState(initial = "gpt-4o-mini")
    val curGh by settings.githubToken.collectAsState(initial = "")
    val curSystem by settings.systemPrompt.collectAsState(initial = "")
    val status by vm.status.collectAsState()

    var base by remember { mutableStateOf("") }
    var key by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var gh by remember { mutableStateOf("") }
    var system by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf(false) }

    LaunchedEffect(curBase, curKey, curModel, curGh, curSystem) {
        base = curBase
        key = curKey
        model = curModel
        gh = curGh
        system = curSystem
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }
            Text("Settings", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Column(modifier = Modifier.padding(16.dp)) {
            SectionTitle("AI API")
            Spacer(modifier = Modifier.height(12.dp))
            Field("Base URL", base, { base = it; saved = false }, "https://api.openai.com/v1")
            Spacer(modifier = Modifier.height(12.dp))
            Field("API Key", key, { key = it; saved = false }, "sk-...", secret = true)
            Spacer(modifier = Modifier.height(12.dp))
            Field("Model", model, { model = it; saved = false }, "gpt-4o-mini")
            Spacer(modifier = Modifier.height(12.dp))
            Field("System prompt", system, { system = it; saved = false }, "You are Forge...", singleLine = false)

            Spacer(modifier = Modifier.height(28.dp))
            SectionTitle("GitHub")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Personal access token with repo scope. Used to create repos and push HTML.",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Field("Token", gh, { gh = it; saved = false }, "ghp_...", secret = true)

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    scope.launch {
                        settings.setApiBaseUrl(base.trim())
                        settings.setApiKey(key.trim())
                        settings.setModel(model.trim().ifBlank { "gpt-4o-mini" })
                        settings.setGithubToken(gh.trim())
                        settings.setSystemPrompt(system.trim())
                        saved = true
                        if (gh.isNotBlank()) vm.connectGitHub()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)
            ) {
                Text(
                    if (saved) "Saved" else "Save",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            if (status != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(status ?: "", color = Accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Flow: chat task -> HTML preview -> push to GitHub. APK builds run in Actions on the target repo if a workflow exists.",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text.uppercase(),
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.2.sp,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    secret: Boolean = false,
    singleLine: Boolean = true
) {
    Text(label, color = TextSecondary, fontSize = 12.sp)
    Spacer(modifier = Modifier.height(6.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = TextMuted) },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 6,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = Border,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = Accent,
            focusedContainerColor = Surface,
            unfocusedContainerColor = Surface
        ),
        visualTransformation = if (secret) PasswordVisualTransformation() else VisualTransformation.None
    )
}
