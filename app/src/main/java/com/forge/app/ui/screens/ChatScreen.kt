package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.AgentPhase
import com.forge.app.ui.ForgeViewModel
import com.forge.app.ui.theme.Accent
import com.forge.app.ui.theme.AccentMuted
import com.forge.app.ui.theme.Black
import com.forge.app.ui.theme.Border
import com.forge.app.ui.theme.Danger
import com.forge.app.ui.theme.Surface
import com.forge.app.ui.theme.Surface2
import com.forge.app.ui.theme.TextMuted
import com.forge.app.ui.theme.TextPrimary
import com.forge.app.ui.theme.TextSecondary

@Composable
fun ChatScreen(
    vm: ForgeViewModel,
    onOpenPreview: () -> Unit,
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val messages by vm.messages.collectAsState()
    val phase by vm.phase.collectAsState()
    val preview by vm.previewHtml.collectAsState()
    val status by vm.status.collectAsState()
    val error by vm.error.collectAsState()
    val apiReady by vm.apiReady.collectAsState()

    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "FORGE",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (apiReady) "agent online" else "set API key",
                    color = if (apiReady) Accent else TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row {
                if (preview != null) {
                    IconButton(onClick = onOpenPreview) {
                        Icon(Icons.Outlined.Visibility, contentDescription = "Preview", tint = Accent)
                    }
                }
                IconButton(onClick = onOpenProjects) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = "Projects", tint = TextSecondary)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = TextSecondary)
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Border)
        )

        // Messages
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 14.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Column(modifier = Modifier.padding(top = 48.dp)) {
                        Icon(
                            Icons.Outlined.Code,
                            contentDescription = null,
                            tint = Border,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Describe what to build.",
                            color = TextSecondary,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Example: landing page for a coffee shop, black theme, one page HTML",
                            color = TextMuted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                val isUser = msg.role == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                ) {
                    Text(
                        text = if (isUser) "you" else "forge",
                        color = if (isUser) Accent else TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 4.dp, start = 4.dp, end = 4.dp)
                    )
                    Box(
                        modifier = Modifier
                            .widthIn(max = 340.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isUser) AccentMuted else Surface)
                            .border(1.dp, if (isUser) Accent.copy(alpha = 0.3f) else Border, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = msg.content,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    if (msg.html != null) {
                        TextButton(onClick = {
                            vm.setPreview(msg.html)
                            onOpenPreview()
                        }) {
                            Text("Open preview", color = Accent, fontSize = 12.sp)
                        }
                    }
                }
            }
            if (phase == AgentPhase.THINKING) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = Accent
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text("working...", color = TextMuted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        if (error != null) {
            Text(
                text = error ?: "",
                color = Danger,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (status != null && error == null) {
            Text(
                text = status ?: "",
                color = Accent,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Border)
        )

        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface2)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text("Task...", color = TextMuted, fontSize = 14.sp)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Accent,
                    focusedContainerColor = Black,
                    unfocusedContainerColor = Black
                ),
                shape = RoundedCornerShape(10.dp),
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (input.isNotBlank() && phase != AgentPhase.THINKING) {
                            vm.send(input)
                            input = ""
                        }
                    }
                )
            )
            Spacer(modifier = Modifier.size(8.dp))
            IconButton(
                onClick = {
                    if (input.isNotBlank() && phase != AgentPhase.THINKING) {
                        vm.send(input)
                        input = ""
                    }
                },
                enabled = phase != AgentPhase.THINKING
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (phase == AgentPhase.THINKING) TextMuted else Accent
                )
            }
        }
    }
}
