package com.example.tabelahisabapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToBackup: () -> Unit,
    onNavigateToExport: () -> Unit,
    onNavigateToTheme: () -> Unit,
    onNavigateToCompany: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToCompany) {
                        Icon(Icons.Default.Settings, contentDescription = "Company Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Company (at top with gear icon)
            SettingsItem(
                icon = Icons.Default.Business,
                title = "Companies",
                subtitle = "Manage company information",
                onClick = onNavigateToCompany
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Backup & Restore
            SettingsItem(
                icon = Icons.Default.Backup,
                title = "Backup & Restore",
                subtitle = "Protect your business data",
                onClick = onNavigateToBackup
            )

            // Export & Print
            SettingsItem(
                icon = Icons.Default.Description,
                title = "Export & Print",
                subtitle = "Generate reports & exports",
                onClick = onNavigateToExport
            )

            // Theme
            SettingsItem(
                icon = Icons.Default.Palette,
                title = "Theme",
                subtitle = "Light • Dark • Auto",
                onClick = onNavigateToTheme
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // About
            SettingsItem(
                icon = Icons.Default.Info,
                title = "About App",
                subtitle = "Version 1.0.0",
                onClick = onNavigateToAbout
            )
        }
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

