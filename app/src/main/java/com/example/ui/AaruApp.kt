package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BuildCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BuildCircle
import androidx.compose.material.icons.outlined.MicNone
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.MemoryVaultScreen
import com.example.ui.screens.PhoneControlScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.AssistantViewModel

enum class AaruNavDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    ASSISTANT("assistant", "Aaru Core", Icons.Filled.Mic, Icons.Outlined.MicNone),
    MEMORY_VAULT("memory_vault", "Memory", Icons.Filled.Psychology, Icons.Outlined.Psychology),
    PHONE_CONTROL("phone_control", "Control", Icons.Filled.PhoneAndroid, Icons.Outlined.PhoneAndroid),
    SETTINGS("settings", "Voice & AI", Icons.Filled.Settings, Icons.Outlined.Settings),
    DIAGNOSTICS("diagnostics", "Diagnostics", Icons.Filled.BuildCircle, Icons.Outlined.BuildCircle)
}

@Composable
fun AaruApp(
    viewModel: AssistantViewModel = viewModel()
) {
    var currentDestination by rememberSaveable { mutableStateOf(AaruNavDestination.ASSISTANT) }

    if (currentDestination != AaruNavDestination.ASSISTANT) {
        BackHandler {
            currentDestination = AaruNavDestination.ASSISTANT
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        AaruNavDestination.entries.forEach { destination ->
                            val selected = currentDestination == destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = { Text(destination.label) },
                                modifier = Modifier.testTag("nav_${destination.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        AaruNavDestination.entries.forEach { destination ->
                            val selected = currentDestination == destination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { currentDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                        contentDescription = destination.label
                                    )
                                },
                                label = { Text(destination.label) },
                                modifier = Modifier.testTag("nav_rail_${destination.route}")
                            )
                        }
                    }
                }

                val contentModifier = Modifier.weight(1f).fillMaxSize()
                when (currentDestination) {
                    AaruNavDestination.ASSISTANT -> AssistantScreen(
                        viewModel = viewModel,
                        modifier = contentModifier
                    )
                    AaruNavDestination.MEMORY_VAULT -> MemoryVaultScreen(
                        viewModel = viewModel,
                        modifier = contentModifier
                    )
                    AaruNavDestination.PHONE_CONTROL -> PhoneControlScreen(
                        viewModel = viewModel,
                        modifier = contentModifier
                    )
                    AaruNavDestination.SETTINGS -> SettingsScreen(
                        viewModel = viewModel,
                        modifier = contentModifier
                    )
                    AaruNavDestination.DIAGNOSTICS -> DiagnosticsScreen(
                        viewModel = viewModel,
                        modifier = contentModifier
                    )
                }
            }
        }
    }
}
