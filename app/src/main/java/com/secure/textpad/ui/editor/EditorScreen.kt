package com.secure.textpad.ui.editor

import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Redo
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import com.secure.textpad.data.DocxHandler
import com.secure.textpad.data.PadFile
import com.secure.textpad.ui.theme.liquidGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

class EditorDocument(file: PadFile, text: String, locked: Boolean, fresh: Boolean = false) {
    var file by mutableStateOf(file)
    var value by mutableStateOf(TextFieldValue(text))
        private set
    var baseline by mutableStateOf(text)
        private set
    var encrypted by mutableStateOf(locked)
        private set
    var fresh by mutableStateOf(fresh)
    val dirty: Boolean get() = fresh || value.text != baseline
    private val undo = ArrayDeque<TextFieldValue>()
    private val redo = ArrayDeque<TextFieldValue>()
    var revision by mutableIntStateOf(0)
        private set
    val canUndo: Boolean get() { revision; return undo.isNotEmpty() }
    val canRedo: Boolean get() { revision; return redo.isNotEmpty() }
    fun edit(next: TextFieldValue) {
        if (next.text.length > DocxHandler.MAX_TEXT) return
        if (next.text != value.text) {
            undo.addLast(value.copy(composition = null)); redo.clear()
            var bytes = undo.sumOf { it.text.length.toLong() * 2 }
            while (undo.size > 100 || bytes > 16L * 1024 * 1024) bytes -= undo.removeFirst().text.length * 2L
            revision++
        }
        value = next
    }
    fun select(start: Int, end: Int) { value = value.copy(selection = TextRange(start, end), composition = null) }
    fun undo() { if (undo.isNotEmpty()) { redo.addLast(value.copy(composition = null)); value = undo.removeLast(); revision++ } }
    fun redo() { if (redo.isNotEmpty()) { undo.addLast(value.copy(composition = null)); value = redo.removeLast(); revision++ } }
    fun saved(file: PadFile, text: String, encrypted: Boolean) { this.file = file; baseline = text; this.encrypted = encrypted; fresh = false }
    fun clear() { undo.clear(); redo.clear(); value = TextFieldValue(); baseline = ""; revision++ }
}

private data class Hit(val start: Int, val end: Int)
private data class SearchResult(val hits: List<Hit> = emptyList(), val error: String? = null, val source: String = "")

/** Checks time from the regex engine's character reads, preventing pathological patterns hanging a worker. */
private class TimedSequence(private val value: String, private val deadline: Long) : CharSequence {
    override val length get() = value.length
    override fun get(index: Int): Char {
        check(System.nanoTime() <= deadline) { "Pattern took too long. Simplify the expression." }
        return value[index]
    }
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = TimedSequence(value.substring(startIndex, endIndex), deadline)
    override fun toString() = value
}

private fun search(text: String, query: String, case: Boolean, regex: Boolean): SearchResult {
    if (query.isEmpty()) return SearchResult()
    return try {
        require(query.length <= 1000) { "Search expression is too long." }
        val flags = if (case) 0 else Pattern.CASE_INSENSITIVE or Pattern.UNICODE_CASE
        val pattern = Pattern.compile(if (regex) query else Pattern.quote(query), flags)
        val matcher = pattern.matcher(TimedSequence(text, System.nanoTime() + 150_000_000))
        val hits = mutableListOf<Hit>()
        while (matcher.find()) {
            require(hits.size < 10000) { "More than 10,000 matches. Use a narrower search." }
            hits.add(Hit(matcher.start(), matcher.end()))
        }
        SearchResult(hits, source = text)
    } catch (e: Exception) { SearchResult(error = e.message ?: "Invalid expression.") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    doc: EditorDocument, busy: Boolean, onBack: () -> Unit,
    onSave: (Boolean, CharArray?) -> Unit, onMove: () -> Unit,
    onSaveAs: (String, CharArray?) -> Unit,
    onSensitivePrompt: (Boolean) -> Unit
) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { if (doc.fresh) { focus.requestFocus(); keyboard?.show() } }
    var exportPassword by remember { mutableStateOf(false) }
    var wrap by remember { mutableStateOf(true) }
    var numbers by remember { mutableStateOf(false) }
    var size by remember { mutableIntStateOf(16) }
    var family by remember { mutableIntStateOf(0) }
    var tools by remember { mutableStateOf(false) }
    var finding by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var replacement by remember { mutableStateOf("") }
    var case by remember { mutableStateOf(false) }
    var regex by remember { mutableStateOf(false) }
    var current by remember { mutableIntStateOf(0) }
    var autoSave by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf(false) }
    var plainConfirm by remember { mutableStateOf(false) }
    val result by produceState(SearchResult(), doc.value.text, query, case, regex, finding) {
        value = if (finding) withContext(Dispatchers.Default) { search(doc.value.text, query, case, regex) } else SearchResult()
    }
    val stats by produceState("", doc.value.text) {
        val text = doc.value.text
        value = withContext(Dispatchers.Default) {
            var words = 0; var inWord = false; var lines = 1
            for (c in text) { if (c == '\n') lines++; if (c.isWhitespace()) inWord = false else if (!inWord) { words++; inWord = true } }
            "$lines lines · $words words · ${text.codePointCount(0, text.length)} chars"
        }
    }
    LaunchedEffect(doc.value.text, autoSave, busy) {
        if (autoSave && !doc.encrypted && doc.dirty && !busy) { delay(1800); onSave(false, null) }
    }
    BackHandler(onBack = onBack)
    fun save() { if (doc.encrypted) password = true else onSave(false, null) }
    fun navigate(delta: Int) {
        if (result.hits.isEmpty() || result.source != doc.value.text) return
        current = Math.floorMod(current + delta, result.hits.size)
        result.hits[current].let { doc.select(it.start, it.end) }
    }
    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth().liquidGlass().padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !busy) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back to files") }
            Column(Modifier.weight(1f)) {
                Text(doc.file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                Text(if (busy) "Working…" else if (doc.dirty) "Unsaved changes" else "Saved on this device", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onMove, enabled = !busy && !doc.dirty) { Icon(if (doc.file.vault) Icons.Outlined.FolderOpen else Icons.Outlined.Shield, if (doc.file.vault) "Move to workspace" else "Move to private vault") }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp).liquidGlass().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { save() }, enabled = !busy) { Icon(Icons.Outlined.Save, null); Spacer(Modifier.width(5.dp)); Text("Save") }
            TextButton(onClick = { password = true }, enabled = !busy) { Icon(Icons.Outlined.Lock, null); Spacer(Modifier.width(5.dp)); Text("Lock") }
            TextButton(onClick = { finding = !finding }) { Icon(Icons.Outlined.Search, null); Spacer(Modifier.width(5.dp)); Text("Find") }
            TextButton(onClick = { tools = true }) { Icon(Icons.Outlined.Tune, null); Text("Tools ⋯") }
        }
        if (finding) Column(Modifier.fillMaxWidth().liquidGlass().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(query, { query = it; current = -1 }, Modifier.weight(1f), label = { Text("Find") }, singleLine = true)
                IconButton(onClick = { navigate(-1) }) { Icon(Icons.Outlined.KeyboardArrowUp, "Previous match") }
                IconButton(onClick = { navigate(1) }) { Icon(Icons.Outlined.KeyboardArrowDown, "Next match") }
                IconButton(onClick = { finding = false }) { Icon(Icons.Outlined.Close, "Close find & replace") }
            }
            Text(result.error ?: if (result.hits.isEmpty()) "0 matches" else "${(current + 1).coerceIn(1, result.hits.size)} of ${result.hits.size}", style = MaterialTheme.typography.labelSmall,
                color = if (result.error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            OutlinedTextField(replacement, { replacement = it }, Modifier.fillMaxWidth(), label = { Text("Replace with (literal text)") }, singleLine = true)
            Row(Modifier.horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(case, { case = !case }, label = { Text("Aa") })
                Spacer(Modifier.width(6.dp)); FilterChip(regex, { regex = !regex }, label = { Text("Regex") })
                TextButton(enabled = !busy && result.hits.isNotEmpty() && result.source == doc.value.text, onClick = {
                    result.hits.getOrNull(current.coerceAtLeast(0))?.let { h ->
                        val text = doc.value.text.replaceRange(h.start, h.end, replacement)
                        doc.edit(TextFieldValue(text, TextRange(h.start + replacement.length)))
                    }
                }) { Text("Replace") }
                TextButton(enabled = !busy && result.hits.isNotEmpty() && result.source == doc.value.text, onClick = {
                    val old = doc.value.text
                    val estimated = old.length.toLong() + result.hits.sumOf { replacement.length.toLong() - (it.end - it.start) }
                    if (estimated <= DocxHandler.MAX_TEXT) {
                        val text = buildString { var start = 0; result.hits.forEach { append(old, start, it.start); append(replacement); start = it.end }; append(old, start, old.length) }
                        doc.edit(TextFieldValue(text))
                    }
                }) { Text("All") }
            }
        }
        val vertical = rememberScrollState()
        val horizontal = rememberScrollState()
        var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
        val font = when (family) { 1 -> FontFamily.SansSerif; 2 -> FontFamily.Serif; else -> FontFamily.Monospace }
        val textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = size.sp, lineHeight = (size * 1.5f).sp, fontFamily = font)
        val color = MaterialTheme.colorScheme.onSurfaceVariant
        val density = LocalDensity.current
        Row(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp).liquidGlass().padding(12.dp).verticalScroll(vertical)) {
            if (numbers) {
                val text = doc.value.text
                val starts = remember(text) { buildList { add(0); text.forEachIndexed { i, c -> if (c == '\n') add(i + 1) } } }
                val height = with(density) { (layout?.size?.height ?: 0).toDp() }
                val paint = remember(color, size, density) { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = android.graphics.Color.argb((color.alpha * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
                    textSize = with(density) { size.sp.toPx() }; typeface = android.graphics.Typeface.MONOSPACE
                } }
                Canvas(Modifier.width(52.dp).height(height)) {
                    layout?.let { l ->
                        starts.forEachIndexed { index, offset ->
                            val line = l.getLineForOffset(offset.coerceAtMost(text.length))
                            val top = l.getLineTop(line)
                            if (top >= vertical.value - 80 && top <= vertical.value + vertical.viewportSize + 80)
                                drawContext.canvas.nativeCanvas.drawText((index + 1).toString(), 0f, l.getLineBaseline(line), paint)
                        }
                    }
                }
            }
            val fieldModifier = Modifier.weight(1f).then(if (!wrap) Modifier.horizontalScroll(horizontal) else Modifier)
            BasicTextField(value = doc.value, onValueChange = { doc.edit(it) }, modifier = fieldModifier.focusRequester(focus).defaultMinSize(minHeight = 240.dp),
                readOnly = busy, textStyle = textStyle, cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                onTextLayout = { layout = it })
        }
        Text("$stats\nUTF-8 · ${if (doc.encrypted) "Encrypted" else "Plain"} · ${if (doc.file.vault) "Private vault" else "Workspace"}",
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), style = MaterialTheme.typography.labelSmall)
    }
    if (password) PasswordDialog(title = if (doc.encrypted) "Password to save" else "Lock this document", confirm = !doc.encrypted,
        onDismiss = { password = false }, onSensitive = onSensitivePrompt,
        onPassword = { password = false; onSave(true, it) })
    if (tools) ModalBottomSheet(onDismissRequest = { tools = false }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
            Text("Tools", style = MaterialTheme.typography.titleLarge)
            Row {
                TextButton(onClick = { doc.undo() }, enabled = !busy && doc.canUndo) { Text("Undo") }
                TextButton(onClick = { doc.redo() }, enabled = !busy && doc.canRedo) { Text("Redo") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Word wrap", Modifier.weight(1f)); Switch(wrap, { wrap = it }) }
            Text("Comfort font size · ${size}sp")
            Slider(value = size.toFloat(), onValueChange = { size = it.toInt() }, valueRange = 12f..30f, steps = 17)
            TextButton(onClick = { tools = false; onMove() }, enabled = !busy && !doc.dirty) { Text(if (doc.file.vault) "Move to workspace" else "Isolate from other apps") }
            Text("Save As", style = MaterialTheme.typography.labelLarge)
            Row {
                TextButton(onClick = { tools = false; onSaveAs("txt", null) }, enabled = !busy) { Text(".txt") }
                TextButton(onClick = { tools = false; onSaveAs("docx", null) }, enabled = !busy) { Text(".docx") }
                TextButton(onClick = { tools = false; exportPassword = true }, enabled = !busy) { Text(".stxt") }
            }
            Text(stats + " · UTF-8", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Line numbers", Modifier.weight(1f)); Switch(numbers, { numbers = it }) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Auto-save plain files", Modifier.weight(1f)); Switch(autoSave, { autoSave = it }, enabled = !doc.encrypted) }
            Text("Font family", style = MaterialTheme.typography.labelLarge)
            listOf("Monospace", "Sans serif", "Serif").forEachIndexed { i, name -> TextButton(onClick = { family = i }) { Text(if (family == i) "✓ $name" else name) } }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { size = (size - 1).coerceAtLeast(12) }) { Text("A−") }
                Text("${size}sp")
                TextButton(onClick = { size = (size + 1).coerceAtMost(30) }) { Text("A+") }
            }
            if (doc.encrypted) TextButton(onClick = { tools = false; plainConfirm = true }) { Text("Remove file encryption") }
            if (doc.file.name.endsWith(".docx", true)) Text("DOCX saves keep paragraph text. Formatting, images and tables are not preserved.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { tools = false }) { Text("Done") }
        }
    }
    if (exportPassword) PasswordDialog("Password for .stxt copy", confirm = true, onDismiss = { exportPassword = false }, onSensitive = onSensitivePrompt,
        onPassword = { exportPassword = false; onSaveAs("stxt", it) })
    if (plainConfirm) AlertDialog(onDismissRequest = { plainConfirm = false }, title = { Text("Save without encryption?") },
        text = { Text("The saved file will no longer need a password. Anyone who can access its folder can read it.") },
        confirmButton = { TextButton(onClick = { plainConfirm = false; onSave(false, null) }) { Text("Save plain file") } },
        dismissButton = { TextButton(onClick = { plainConfirm = false }) { Text("Cancel") } })
}

/** Native password fields support Android Autofill; editable buffers and submitted arrays are erased. */
@Composable
fun PasswordDialog(title: String, confirm: Boolean = false, onDismiss: () -> Unit,
    onSensitive: (Boolean) -> Unit, onPassword: (CharArray) -> Unit) {
    var first by remember { mutableStateOf<EditText?>(null) }
    var second by remember { mutableStateOf<EditText?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    DisposableEffect(Unit) {
        onSensitive(true)
        onDispose { first?.text?.clear(); second?.text?.clear(); onSensitive(false) }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn)) {
        Surface(shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
                Text(title, style = MaterialTheme.typography.titleLarge)
                if (confirm) Text("Use a long, unique password. TXT cannot recover it.", Modifier.padding(vertical = 12.dp))
                fun chars(edit: EditText?): CharArray {
                    val text = edit?.text ?: return CharArray(0)
                    return CharArray(text.length) { text[it] }
                }
                AndroidView(factory = { c -> EditText(c).apply {
                    hint = "Password"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setSingleLine(); setAutofillHints(View.AUTOFILL_HINT_PASSWORD); importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
                    first = this
                } }, modifier = Modifier.fillMaxWidth())
                if (confirm) AndroidView(factory = { c -> EditText(c).apply {
                    hint = "Repeat password"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    setSingleLine(); importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO; second = this
                } }, modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(onClick = {
                        val p = chars(first); val repeat = chars(second)
                        if (p.isEmpty() || (confirm && p.size < 10)) { error = "Use at least ${if (confirm) 10 else 1} characters."; p.fill('\u0000') }
                        else if (confirm && !p.contentEquals(repeat)) { error = "Passwords do not match."; p.fill('\u0000') }
                        else { first?.text?.clear(); second?.text?.clear(); onPassword(p) }
                        repeat.fill('\u0000')
                    }) { Text("Continue") }
                }
            }
        }
    }
}
