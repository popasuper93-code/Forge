package com.forge.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.forge.app.ui.AgentPhase
import com.forge.app.ui.ForgeViewModel
import com.forge.app.ui.theme.Accent
import com.forge.app.ui.theme.Black
import com.forge.app.ui.theme.Border
import com.forge.app.ui.theme.Danger
import com.forge.app.ui.theme.Surface
import com.forge.app.ui.theme.Surface2
import com.forge.app.ui.theme.TextMuted
import com.forge.app.ui.theme.TextPrimary
import com.forge.app.ui.theme.TextSecondary

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PreviewScreen(
    vm: ForgeViewModel,
    onBack: () -> Unit,
    onOpenProjects: () -> Unit
) {
    val html by vm.previewHtml.collectAsState()
    val phase by vm.phase.collectAsState()
    val status by vm.status.collectAsState()
    val error by vm.error.collectAsState()
    val selected by vm.selectedRepo.collectAsState()
    val ghReady by vm.ghReady.collectAsState()

    var showPush by remember { mutableStateOf(false) }
    var newRepoName by remember { mutableStateOf("forge-preview") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    text = "Preview",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row {
                IconButton(onClick = { showPush = !showPush }) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = "Push", tint = Accent)
                }
                IconButton(onClick = onOpenProjects) {
                    Icon(Icons.Outlined.CreateNewFolder, contentDescription = "Repos", tint = TextSecondary)
                }
            }
        }

        Box(Modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))

        if (showPush) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface2)
                    .padding(14.dp)
            ) {
                Text(
                    text = if (ghReady) "Push HTML to GitHub" else "Connect GitHub in Settings first",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (selected != null) {
                    Text(
                        text = "Target: ${selected?.fullName}",
                        color = Accent,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { vm.pushHtmlToGitHub() },
                        enabled = phase != AgentPhase.PUSHING && ghReady,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Push index.html to selected repo", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = newRepoName,
                    onValueChange = { newRepoName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("New repo name", color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Accent
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { vm.createRepoAndPush(newRepoName) },
                    enabled = phase != AgentPhase.PUSHING && ghReady,
                    colors = ButtonDefaults.buttonColors(containerColor = Surface, contentColor = TextPrimary),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                ) {
                    Text("Create repo and push")
                }
                if (phase == AgentPhase.PUSHING) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.height(14.dp).padding(end = 8.dp), strokeWidth = 2.dp, color = Accent)
                        Text(status ?: "Working...", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))
        }

        if (error != null) {
            Text(
                text = error ?: "",
                color = Danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(12.dp)
            )
        }
        if (status != null && phase != AgentPhase.PUSHING) {
            Text(
                text = status ?: "",
                color = Accent,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }

        if (html.isNullOrBlank()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No preview yet", color = TextMuted)
            }
        } else {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        setBackgroundColor(0xFF000000.toInt())
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = WebViewClient()
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL(
                        null,
                        html ?: "",
                        "text/html",
                        "UTF-8",
                        null
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black)
            )
        }
    }
}
