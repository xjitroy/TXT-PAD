package com.secure.textpad.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.secure.textpad.data.PadFile
import com.secure.textpad.ui.theme.liquidGlass
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    files: List<PadFile>, busy: Boolean, workspace: Boolean, defaultPrivate: Boolean, appLock: Boolean,
    onOpen: (PadFile) -> Unit, onFolder: () -> Unit, onCreate: () -> Unit,
    onMove: (PadFile) -> Unit, onBackup: () -> Unit, onRestore: () -> Unit,
    onPrivateDefault: (Boolean) -> Unit, onAppLock: (Boolean) -> Unit, onRefresh: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }
    var backup by remember { mutableStateOf(false) }
    val visible = remember(files, search, filter) {
        files.filter { it.name.contains(search, true) && when (filter) {
            1 -> it.encrypted == false && !it.name.endsWith(".docx", true)
            2 -> it.encrypted == true
            3 -> it.name.endsWith(".docx", true)
            4 -> it.vault
            else -> true
        } }
    }
    if (!workspace && files.isEmpty()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(Modifier.liquidGlass().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("TXT", style = MaterialTheme.typography.displayMedium)
                Text("A space for your words", style = MaterialTheme.typography.titleLarge)
                Text("Choose or create a folder to keep your documents. Everything works offline.", Modifier.padding(vertical = 20.dp))
                Button(onClick = onFolder, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Text("Choose workspace folder") }
            }
        }
        return
    }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onCreate, modifier = Modifier.liquidGlass(), icon = { Icon(Icons.Outlined.Add, null) }, text = { Text("New File") })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Column(Modifier.fillMaxWidth().liquidGlass().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("TXT", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                        Text("Your words. Your space.", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { backup = true }, enabled = !busy) { Icon(Icons.Outlined.Backup, "Backup & restore") }
                    IconButton(onClick = onFolder, enabled = !busy) { Icon(Icons.Outlined.FolderOpen, "Switch workspace folder") }
                    IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Settings, "Settings") }
                }
                OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Search files") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("All", "Plain", "Locked", "Docs", "Private Vault").forEachIndexed { i, title ->
                        FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(title) })
                    }
                }
            }
            TextButton(onClick = { backup = true }, enabled = !busy, modifier = Modifier.heightIn(min = 48.dp)) { Icon(Icons.Outlined.Backup, null); Text(" Backup & Restore") }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${visible.size} documents · offline", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onRefresh, enabled = !busy) { Icon(Icons.Outlined.Refresh, "Refresh file list") }
            }
            if (!workspace) {
                Column(Modifier.fillMaxWidth().liquidGlass().padding(20.dp)) {
                    Text("Choose your workspace", style = MaterialTheme.typography.titleLarge)
                    Text("Select or create a folder for your files. Private vault files stay in this app's storage.", Modifier.padding(vertical = 8.dp))
                    Button(onClick = onFolder, enabled = !busy) { Text("Choose folder") }
                }
            }
            if (visible.isEmpty() && !busy) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(if (search.isBlank()) "A clear space for your next idea.\nTap + to create a document." else "No matching documents.")
                }
            } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visible, key = { it.uri.toString() }) { file ->
                    Row(Modifier.fillMaxWidth().liquidGlass().clickable(enabled = !busy) { onOpen(file) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (file.encrypted == true) Icons.Outlined.Lock else Icons.Outlined.Description,
                            if (file.encrypted == true) "Encrypted" else "Document", tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                            Text(file.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(".${file.name.substringAfterLast('.', "txt")} · ${if (file.encrypted == true) "Password Locked" else "Plain"} · ${if (file.vault) "Isolated Vault" else "Workspace"} · ${formatSize(file.size)}", style = MaterialTheme.typography.bodySmall)
                            if (file.modified > 0) Text(DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(file.modified)), style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = { onMove(file) }, enabled = !busy) {
                            Icon(if (file.vault) Icons.Outlined.FolderOpen else Icons.Outlined.Shield,
                                if (file.vault) "Move to workspace" else "Hide from other apps: move to private vault")
                        }
                    }
                }
            }
        }
    }
    if (backup) AlertDialog(onDismissRequest = { backup = false }, title = { Text("Backup & restore") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Keep a .tpbak copy outside this phone before resetting it. You will need its master password on your next device.")
            FilledTonalButton(onClick = { backup = false; onBackup() }, modifier = Modifier.fillMaxWidth()) { Text("Create full offline backup") }
            OutlinedButton(onClick = { backup = false; onRestore() }, modifier = Modifier.fillMaxWidth()) { Text("Restore offline backup") }
        }
    }, confirmButton = { TextButton(onClick = { backup = false }) { Text("Close") } })
    if (settings) AlertDialog(onDismissRequest = { settings = false }, title = { Text("TXT settings") }, text = {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("New files in private vault", Modifier.weight(1f)); Switch(defaultPrivate, onPrivateDefault) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Fingerprint / screen lock", Modifier.weight(1f)); Switch(appLock, onAppLock) }
            Text("Off by default. When enabled, Android verifies your fingerprint or device screen lock whenever TXT returns from the background.", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(16.dp))
            Text("Private vault uses Android app isolation. Uninstalling TXT erases it. Export a backup first.", style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { settings = false }) { Text("Done") } })
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
