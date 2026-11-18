package com.nexavpn.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nexavpn.app.data.SplitTunnelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Split tunneling screen: pick apps to EXCLUDE from the VPN.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitTunnelScreen(modifier: Modifier = Modifier, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { SplitTunnelRepository }

    var apps by remember { mutableStateOf<List<SplitTunnelRepository.InstalledApp>>(emptyList()) }
    var excluded by remember { mutableStateOf<Set<String>>(emptySet()) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        // PackageManager queries are blocking — run off the main thread.
        apps = withContext(Dispatchers.IO) { repo.listInstalledApps(context) }
        excluded = repo.excludedApps(context).first()
    }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Split Tunneling", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().then(modifier)) {
            Text(
                "Excluded apps will use your regular network connection instead of the VPN.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search apps") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.extraLarge,
            )

            Spacer(Modifier.height(4.dp))

            if (excluded.isNotEmpty()) {
                Text(
                    "${excluded.size} app(s) excluded",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(filtered) { app ->
                    val isExcluded = app.packageName in excluded
                    ListItem(
                        headlineContent = { Text(app.label) },
                        supportingContent = { Text(app.packageName, fontSize = 11.sp) },
                        trailingContent = {
                            Switch(
                                checked = isExcluded,
                                onCheckedChange = {
                                    scope.launch {
                                        repo.toggle(context, app.packageName)
                                        excluded = repo.excludedApps(context).first()
                                    }
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}
