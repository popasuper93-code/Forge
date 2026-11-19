package com.forge.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.forge.app.ui.ForgeViewModel
import com.forge.app.ui.theme.Accent
import com.forge.app.ui.theme.Black
import com.forge.app.ui.theme.Border
import com.forge.app.ui.theme.Surface
import com.forge.app.ui.theme.TextMuted
import com.forge.app.ui.theme.TextPrimary
import com.forge.app.ui.theme.TextSecondary

@Composable
fun ProjectsScreen(
    vm: ForgeViewModel,
    onBack: () -> Unit
) {
    val repos by vm.repos.collectAsState()
    val selected by vm.selectedRepo.collectAsState()
    val ghReady by vm.ghReady.collectAsState()
    val ghUser by vm.ghUser.collectAsState()
    val status by vm.status.collectAsState()

    LaunchedEffect(ghReady) {
        if (ghReady) vm.refreshRepos()
    }

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
                Column {
                    Text("Projects", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (ghUser.isNotBlank()) {
                        Text(ghUser, color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            IconButton(onClick = { vm.refreshRepos() }) {
                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh", tint = TextSecondary)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Border))

        if (!ghReady) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("GitHub not connected", color = TextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Add token in Settings", color = TextMuted, fontSize = 13.sp)
                }
            }
        } else if (repos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No repositories", color = TextSecondary)
                    TextButton(onClick = { vm.refreshRepos() }) {
                        Text("Refresh", color = Accent)
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(repos, key = { it.fullName }) { repo ->
                    val isSelected = selected?.fullName == repo.fullName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Surface)
                            .border(
                                1.dp,
                                if (isSelected) Accent else Border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { vm.selectRepo(repo) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(repo.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(repo.fullName, color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        if (isSelected) {
                            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Accent)
                        }
                    }
                }
            }
        }

        if (status != null) {
            Text(
                text = status ?: "",
                color = Accent,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}
