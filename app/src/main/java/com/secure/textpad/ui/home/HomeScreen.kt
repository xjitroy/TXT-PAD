package com.secure.textpad.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.secure.textpad.data.PadFile
import com.secure.textpad.data.TrashItem
import com.secure.textpad.ui.theme.liquidGlass
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    files: List<PadFile>, busy: Boolean, workspace: Boolean, defaultPrivate: Boolean, appLock: Boolean,
    onOpen: (PadFile) -> Unit, onFolder: () -> Unit, onCreate: () -> Unit,
    onMove: (PadFile) -> Unit, onBackup: () -> Unit, onRestore: () -> Unit,
    onPrivateDefault: (Boolean) -> Unit, onAppLock: (Boolean) -> Unit, onRefresh: () -> Unit,
    onRename: (PadFile, String) -> Unit, onTrash: (PadFile) -> Unit, onShowTrash: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }
    var options by remember { mutableStateOf<PadFile?>(null) }
    var rename by remember { mutableStateOf<PadFile?>(null) }
    var trash by remember { mutableStateOf<PadFile?>(null) }
    var move by remember { mutableStateOf<PadFile?>(null) }
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
                TextButton(onClick = onShowTrash) { Text("Open Trash") }
            }
        }
        return
    }
    val colors = MaterialTheme.colorScheme
    val fabInteraction = remember { MutableInteractionSource() }
    val dockInteraction = remember { MutableInteractionSource() }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent, contentColor = colors.onSurface,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate, modifier = Modifier.heightIn(min = 64.dp).widthIn(min = 160.dp).liquidGlass(fabInteraction),
                containerColor = Color.Transparent, contentColor = colors.onSurface, interactionSource = fabInteraction,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)) {
                Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Add, null)
                    Text("New File", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                }
            }
        },
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(Modifier.fillMaxWidth().liquidGlass(dockInteraction).padding(horizontal = 4.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = { filter = 0; search = "" }, modifier = Modifier.weight(1f), interactionSource = dockInteraction) { Text("Files") }
                    TextButton(onClick = { filter = 4 }, modifier = Modifier.weight(1f), interactionSource = dockInteraction) { Text("Vault") }
                    TextButton(onClick = onShowTrash, modifier = Modifier.weight(1f), interactionSource = dockInteraction) { Text("Trash") }
                }
            }

        }
    ) { padding ->
        // The complete header scrolls with the list on short screens / enlarged fonts.
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), color = colors.primaryContainer) {
                        Icon(Icons.Outlined.Description, null, Modifier.padding(12.dp).size(26.dp), tint = colors.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text("TXT", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                        Text("Notes, documents & private files", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = onShowTrash) { Icon(Icons.Outlined.DeleteOutline, null, Modifier.size(20.dp)); Text("Trash") }
                    IconButton(onClick = { settings = true }) { Icon(Icons.Outlined.Settings, "Settings") }
                }
            }
            item {
                Column(Modifier.fillMaxWidth().liquidGlass().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
                        label = { Text("Search files") }, leadingIcon = { Icon(Icons.Outlined.Search, null) })
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("All", "Plain", "Locked", "Docs", "Private Vault").forEachIndexed { i, title ->
                            FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(title) })
                        }
                    }
                    val largeLabels = LocalDensity.current.fontScale > 1.25f
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { backup = true }, enabled = !busy, modifier = (if (largeLabels) Modifier.fillMaxWidth() else Modifier.weight(1f)).heightIn(min = 48.dp)) {
                            Icon(Icons.Outlined.Backup, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Backup", maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(onClick = onFolder, enabled = !busy, modifier = (if (largeLabels) Modifier.fillMaxWidth() else Modifier.weight(1f)).heightIn(min = 48.dp)) {
                            Icon(Icons.Outlined.FolderOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Workspace", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Documents", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text("${visible.size} ${if (visible.size == 1) "file" else "files"} · On this device", color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onRefresh, enabled = !busy) { Icon(Icons.Outlined.Refresh, "Refresh file list") }
                }
            }
            if (!workspace) item {
                Column(Modifier.fillMaxWidth().liquidGlass().padding(20.dp)) {
                    Text("Choose your workspace", style = MaterialTheme.typography.titleLarge)
                    Text("Select or create a folder for your files. Private vault files stay in this app's storage.", Modifier.padding(vertical = 8.dp))
                    Button(onClick = onFolder, enabled = !busy) { Text("Choose folder") }
                }
            }
            if (visible.isEmpty() && !busy) item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Description, null, Modifier.size(44.dp), tint = colors.primary)
                    Spacer(Modifier.height(16.dp))
                    Text(if (search.isBlank() && filter == 0) "A fresh page awaits" else "No matching files",
                        style = MaterialTheme.typography.titleLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Text(if (search.isBlank() && filter == 0) "Tap New File to capture an idea. Your words stay on your device." else "Try another search or choose a different filter.",
                        Modifier.padding(top = 8.dp), color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            items(visible, key = { it.uri.toString() }) { file ->
                val interaction = remember(file.uri) { MutableInteractionSource() }
                Row(Modifier.fillMaxWidth().animateItem().liquidGlass(interaction).clickable(interactionSource = interaction, indication = null, enabled = !busy) { onOpen(file) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), color = colors.primaryContainer) {
                        Icon(if (file.encrypted == true) Icons.Outlined.Lock else Icons.Outlined.Description,
                            null, Modifier.padding(10.dp).size(22.dp), tint = colors.onPrimaryContainer)
                    }
                    Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(file.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FileBadge("." + file.name.substringAfterLast('.', "txt"))
                            FileBadge(if (file.encrypted == true) "Password locked" else if (file.encrypted == false) "Plain text" else "Checking…")
                            if (file.vault) FileBadge("Private vault")
                        }
                        Text(formatSize(file.size) + if (file.modified > 0) " · " + DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(file.modified)) else "",
                            color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { focusManager.clearFocus(); keyboard?.hide(); options = file }, enabled = !busy) {
                        Icon(Icons.Outlined.MoreVert, "File options: ${file.name}")
                    }
                }
            }
        }
    }
    options?.let { file ->
        ModalBottomSheet(sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), modifier = Modifier.liquidGlass(), containerColor = Color.Transparent, onDismissRequest = { options = null }) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
                Text(file.name, style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { options = null; onOpen(file) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.Description, null); Spacer(Modifier.width(8.dp)); Text("Open") }
                TextButton(onClick = { options = null; rename = file }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.Edit, null); Spacer(Modifier.width(8.dp)); Text("Rename") }
                TextButton(onClick = { options = null; move = file }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Move to…") }
                TextButton(onClick = { options = null; trash = file }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.DeleteOutline, null); Spacer(Modifier.width(8.dp)); Text("Move to Trash") }
            }
        }
    }
    rename?.let { file ->
        var name by remember(file.uri) { mutableStateOf(file.name) }
        AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { rename = null }, title = { Text("Rename file") }, text = {
            OutlinedTextField(name, { name = it }, label = { Text("Filename") }, supportingText = { Text("Keep the same extension. Use Save As to change format.") })
        }, confirmButton = { TextButton(onClick = { rename = null; onRename(file, name.trim()) }, enabled = name.isNotBlank()) { Text("Rename") } },
            dismissButton = { TextButton(onClick = { rename = null }) { Text("Cancel") } })
    }
    move?.let { file -> AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { move = null }, title = { Text("Move file") }, text = {
        Text(if (file.vault) "Move this file to the active workspace? Other apps with folder access may see it." else "Move this file to the private vault? It will be hidden from ordinary file managers.")
    }, confirmButton = { TextButton(onClick = { move = null; onMove(file) }, enabled = !file.vault || workspace) { Text(if (file.vault) "Move to workspace" else "Move to private vault") } },
        dismissButton = { TextButton(onClick = { move = null }) { Text("Cancel") } }) }
    trash?.let { file -> AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { trash = null }, title = { Text("Move to Trash?") }, text = {
        Text("${file.name} will leave its folder. You can restore it from Trash later. Locked files keep their password protection.")
    }, confirmButton = { TextButton(onClick = { trash = null; onTrash(file) }) { Text("Move to Trash") } },
        dismissButton = { TextButton(onClick = { trash = null }) { Text("Cancel") } }) }
    if (backup) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { backup = false }, title = { Text("Backup & restore") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Keep a .tpbak copy outside this phone before resetting it. You will need its master password on your next device.")
            FilledTonalButton(onClick = { backup = false; onBackup() }, modifier = Modifier.fillMaxWidth()) { Text("Create full offline backup") }
            OutlinedButton(onClick = { backup = false; onRestore() }, modifier = Modifier.fillMaxWidth()) { Text("Restore offline backup") }
        }
    }, confirmButton = { TextButton(onClick = { backup = false }) { Text("Close") } })
    if (settings) AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { settings = false }, title = { Text("TXT settings") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Appearance follows your device’s Light / Dark setting.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 12.dp))
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

@Composable
private fun FileBadge(label: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp)) {
        Text(label, Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun TrashScreen(files: List<TrashItem>, busy: Boolean, onBack: () -> Unit, onRestore: (TrashItem) -> Unit, onDelete: (TrashItem) -> Unit) {
    androidx.activity.compose.BackHandler(onBack = onBack)
    var deleting by remember { mutableStateOf<TrashItem?>(null) }
    Scaffold(containerColor = androidx.compose.ui.graphics.Color.Transparent) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                TextButton(onClick = onBack) { Text("‹ Back to documents") }
                Text("Trash", style = MaterialTheme.typography.headlineLarge)
                Text("Restore files to their original storage. Public files return to the active workspace. Matching names are kept as separate copies.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Trash is not included in backups. Restore anything you need before exporting a backup.", Modifier.padding(top = 8.dp), style = MaterialTheme.typography.bodySmall)
            }
            if (busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            if (files.isEmpty()) item { Text("Trash is empty", Modifier.padding(vertical = 40.dp), style = MaterialTheme.typography.titleLarge) }
            items(files, key = { it.id }) { file ->
                Column(Modifier.fillMaxWidth().liquidGlass().padding(16.dp)) {
                    Text(file.name, style = MaterialTheme.typography.titleMedium)
                    Text("${if (file.vault) "Private vault" else "Workspace"} · ${formatSize(file.size)}", style = MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick = { onRestore(file) }, enabled = !busy) { Icon(Icons.Outlined.Restore, null); Spacer(Modifier.width(8.dp)); Text("Restore") }
                        TextButton(onClick = { deleting = file }, enabled = !busy) { Icon(Icons.Outlined.DeleteForever, null); Spacer(Modifier.width(8.dp)); Text("Delete") }
                    }
                }
            }
        }
    }
    deleting?.let { file -> AlertDialog(modifier = Modifier.liquidGlass(), containerColor = androidx.compose.ui.graphics.Color.Transparent, onDismissRequest = { deleting = null }, title = { Text("Delete permanently?") },
        text = { Text("${file.name} cannot be restored after this. This removes only its Trash copy.") },
        confirmButton = { TextButton(onClick = { deleting = null; onDelete(file) }) { Text("Delete permanently") } },
        dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel") } }) }
}
