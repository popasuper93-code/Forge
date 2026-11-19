package com.forge.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.forge.app.ui.ForgeViewModel
import com.forge.app.ui.screens.ChatScreen
import com.forge.app.ui.screens.PreviewScreen
import com.forge.app.ui.screens.ProjectsScreen
import com.forge.app.ui.screens.SettingsScreen
import com.forge.app.ui.theme.Black
import com.forge.app.ui.theme.ForgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ForgeTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Black) {
                    ForgeNav()
                }
            }
        }
    }
}

@Composable
fun ForgeNav() {
    val nav = rememberNavController()
    val vm: ForgeViewModel = viewModel()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ForgeApp

    NavHost(navController = nav, startDestination = "chat") {
        composable("chat") {
            ChatScreen(
                vm = vm,
                onOpenPreview = { nav.navigate("preview") },
                onOpenProjects = { nav.navigate("projects") },
                onOpenSettings = { nav.navigate("settings") }
            )
        }
        composable("preview") {
            PreviewScreen(
                vm = vm,
                onBack = { nav.popBackStack() },
                onOpenProjects = { nav.navigate("projects") }
            )
        }
        composable("projects") {
            ProjectsScreen(
                vm = vm,
                onBack = { nav.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                app = app,
                vm = vm,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
