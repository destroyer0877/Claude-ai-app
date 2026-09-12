package com.example.ui.screens

import android.widget.Toast
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiService
import com.example.data.model.AutoClassifier
import com.example.data.model.NoteEntity
import com.example.ui.components.CodeBlockView
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.InteractiveChecklistView
import com.example.ui.components.InteractiveTableView
import com.example.ui.theme.CrimsonPrimary
import com.example.ui.util.RichTextFormatter
import com.example.ui.util.AttachmentStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    initialNote: NoteEntity?,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onSaveNote: (
        id: Long,
        title: String,
        content: String,
        category: String,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderline: Boolean,
        isStrikethrough: Boolean,
        isCodeFormat: Boolean,
        fontSize: Int,
        fontColorHex: String,
        alignment: String,
        listType: String,
        tableData: String,
        styleSpansJson: String,
        attachmentsJson: String,
        onSaved: (Long) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { AiService() }

    var title by remember { mutableStateOf(initialNote?.title ?: "") }
    var contentValue by remember { mutableStateOf(TextFieldValue(initialNote?.content ?: "")) }
    var selectedCategory by remember { mutableStateOf(initialNote?.category ?: "Normal") }

    // Tracks the real DB id for this note across auto-saves. Starts at the id we were
    // opened with (0L = brand new note); updated once the very first auto-save/manual
    // save assigns a real id, so later auto-saves UPDATE instead of inserting duplicates.
    var currentNoteId by remember { mutableStateOf(initialNote?.id ?: 0L) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }

    // Independent per-range formatting spans (source of truth for rich text).
    // Formatting is NEVER written into `contentValue.text` as raw markup.
    var spans by remember {
        mutableStateOf(RichTextFormatter.deserializeSpans(initialNote?.styleSpansJson ?: "[]"))
    }

    // Real attachments (actual files copied into app storage), never placeholder text.
    // See AttachmentStorage.copyToAppStorage and RichTextFormatter.AttachmentInfo.
    var attachments by remember {
        mutableStateOf(RichTextFormatter.deserializeAttachments(initialNote?.attachmentsJson ?: "[]"))
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            coroutineScope.launch {
                val file = java.io.File(context.filesDir, "attachments").apply { mkdirs() }
                    .resolve("camera_${System.currentTimeMillis()}.jpg")
                file.outputStream().use { out ->
                    bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                }
                attachments = attachments + RichTextFormatter.AttachmentInfo(
                    uri = file.absolutePath,
                    fileName = file.name,
                    mimeType = "image/jpeg",
                    sizeBytes = file.length()
                )
                Toast.makeText(context, "Photo attached", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun attachFromUri(uri: Uri) {
        coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val info = AttachmentStorage.copyToAppStorage(context, uri)
            withContext(kotlinx.coroutines.Dispatchers.Main) {
                if (info != null) {
                    attachments = attachments + info
                    Toast.makeText(context, "Attached: ${info.fileName}", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Could not attach that file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { attachFromUri(it) } }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { attachFromUri(it) } }

    // Local, in-place PDF viewer overlay for PDF attachments: keeps the editor mounted
    // (so no unsaved-edit / stale-content risk from navigating away and back) and just
    // draws the AU PDF Viewer on top while open.
    var pdfAttachmentToView by remember { mutableStateOf<Uri?>(null) }

    fun openAttachment(attachment: RichTextFormatter.AttachmentInfo) {
        try {
            if (attachment.mimeType == "application/pdf") {
                pdfAttachmentToView = Uri.fromFile(java.io.File(attachment.uri))
                return
            }
            val shareUri = AttachmentStorage.getShareableUri(context, attachment.uri)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(shareUri, attachment.mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    // "Pending" formatting toggles: these only apply to the NEXT characters typed
    // when there is no active selection (exactly like Word/Google Docs). When a
    // selection IS active, toolbar buttons instead toggle the format on that
    // selection directly via `spans` and these pending flags are not used.
    var isBold by remember { mutableStateOf(false) }
    var isItalic by remember { mutableStateOf(false) }
    var isUnderline by remember { mutableStateOf(false) }
    var isStrikethrough by remember { mutableStateOf(false) }
    var isCodeFormat by remember { mutableStateOf(initialNote?.isCodeFormat ?: false) }
    var pendingColorHex by remember { mutableStateOf<String?>(null) }

    var fontSize by remember { mutableStateOf(initialNote?.fontSize ?: 16) }
    var selectedColorHex by remember { mutableStateOf(initialNote?.fontColorHex ?: "#FFFFFF") }
    var alignment by remember { mutableStateOf(initialNote?.alignment ?: "left") }
    var listType by remember { mutableStateOf(initialNote?.listType ?: "none") }
    var tableData by remember { mutableStateOf(initialNote?.tableData ?: "") }

    // Single shared save path used by both the manual Save button and auto-save, so
    // they can never drift out of sync. Uses currentNoteId (not initialNote.id) so
    // that once the very first save assigns a real id, every save after that is an
    // UPDATE, never a duplicate INSERT.
    fun performSave(showToast: Boolean, thenNavigateBack: Boolean) {
        if (title.isBlank() && contentValue.text.isBlank() && attachments.isEmpty()) {
            if (thenNavigateBack) onBack()
            return
        }
        val autoCat = if (selectedCategory == "Normal") {
            AutoClassifier.detectCategory(title, contentValue.text)
        } else {
            selectedCategory
        }
        onSaveNote(
            currentNoteId,
            title,
            contentValue.text,
            autoCat,
            isBold,
            isItalic,
            isUnderline,
            isStrikethrough,
            isCodeFormat,
            fontSize,
            selectedColorHex,
            alignment,
            listType,
            tableData,
            RichTextFormatter.serializeSpans(spans),
            RichTextFormatter.serializeAttachments(attachments)
        ) { savedId ->
            currentNoteId = savedId
            hasUnsavedChanges = false
            if (showToast) {
                Toast.makeText(context, "Note Saved in $autoCat", Toast.LENGTH_SHORT).show()
            }
            if (thenNavigateBack) onBack()
        }
    }

    // Debounced auto-save: fires ~1.2s after the user stops changing anything, so
    // work is never lost even if the app is killed before the user taps Save.
    LaunchedEffect(
        title, contentValue.text, spans, tableData, attachments,
        isBold, isItalic, isUnderline, isStrikethrough, isCodeFormat,
        fontSize, selectedColorHex, alignment, listType
    ) {
        hasUnsavedChanges = true
        kotlinx.coroutines.delay(1200)
        performSave(showToast = false, thenNavigateBack = false)
    }

    // Save immediately if the app is backgrounded/killed while editing, instead of
    // relying only on the debounce timer.
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE ||
                event == androidx.lifecycle.Lifecycle.Event.ON_STOP
            ) {
                if (hasUnsavedChanges) {
                    performSave(showToast = false, thenNavigateBack = false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Final safety net: also save when this screen leaves composition for ANY reason
    // (system/gesture back button, programmatic navigation), not just app-level pause.
    // Without this, a system back press within the debounce window could discard the
    // last ~1.2s of edits since the LaunchedEffect above gets cancelled on disposal.
    DisposableEffect(Unit) {
        onDispose {
            if (hasUnsavedChanges) {
                performSave(showToast = false, thenNavigateBack = false)
            }
        }
    }

    // Undo/Redo history stack (stores both text and its matching spans so undo/redo
    // never leaves formatting misaligned with the text).
    data class HistoryEntry(val text: String, val spansJson: String)
    val undoStack = remember { mutableStateListOf<HistoryEntry>() }
    val redoStack = remember { mutableStateListOf<HistoryEntry>() }

    // Sheets & Dialog states
    var showTextStylesSheet by remember { mutableStateOf(false) }
    var showParagraphStylesSheet by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showAiSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    // AI in Editor state
    var aiPromptInput by remember { mutableStateOf("") }
    var aiGeneratedResult by remember { mutableStateOf("") }
    var isAiGenerating by remember { mutableStateOf(false) }

    val formattedDate = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM dd 'at' HH:mm", Locale.US)
        sdf.format(Date())
    }

    val paletteColors = listOf(
        "#FFFFFF", "#FF2D55", "#38BDF8", "#34D399",
        "#FBBF24", "#A78BFA", "#F472B6", "#9CA3AF"
    )

    fun pushUndo(currentText: String, currentSpans: List<RichTextFormatter.TextSpan>) {
        if (undoStack.size > 25) undoStack.removeAt(0)
        undoStack.add(HistoryEntry(currentText, RichTextFormatter.serializeSpans(currentSpans)))
        redoStack.clear()
    }

    // What the toolbar buttons should show as "active" right now: if there's a
    // selection, reflect whether that selection is fully styled; otherwise reflect
    // the pending typing state.
    val sel = contentValue.selection
    val boldActive = if (sel.collapsed) isBold else RichTextFormatter.isPropertyActiveThroughout(spans, "bold", sel.min, sel.max)
    val italicActive = if (sel.collapsed) isItalic else RichTextFormatter.isPropertyActiveThroughout(spans, "italic", sel.min, sel.max)
    val underlineActive = if (sel.collapsed) isUnderline else RichTextFormatter.isPropertyActiveThroughout(spans, "underline", sel.min, sel.max)
    val strikeActive = if (sel.collapsed) isStrikethrough else RichTextFormatter.isPropertyActiveThroughout(spans, "strikethrough", sel.min, sel.max)
    val codeActive = if (sel.collapsed) isCodeFormat else RichTextFormatter.isPropertyActiveThroughout(spans, "code", sel.min, sel.max)

    // Applies a raw text edit: adjusts existing spans to the new text, then, if this
    // was a pure insertion (typing/pasting with no deletion), tags the newly
    // inserted range with any pending formatting toggles. This is the single choke
    // point all text changes go through, so formatting never bleeds or leaks as
    // raw markup.
    fun handleContentChange(newVal: TextFieldValue) {
        val oldText = contentValue.text
        val newText = newVal.text
        if (newText != oldText) {
            pushUndo(oldText, spans)
            var updated = RichTextFormatter.adjustSpansForEdit(spans, oldText, newText)

            val minLen = minOf(oldText.length, newText.length)
            var prefixLen = 0
            while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) prefixLen++
            var suffixLen = 0
            val maxSuffix = minLen - prefixLen
            while (suffixLen < maxSuffix &&
                oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]
            ) suffixLen++
            val oldMiddleLen = oldText.length - prefixLen - suffixLen
            val newMiddleLen = newText.length - prefixLen - suffixLen

            if (oldMiddleLen == 0 && newMiddleLen > 0) {
                val insStart = prefixLen
                val insEnd = prefixLen + newMiddleLen
                if (isBold) updated = RichTextFormatter.addSpan(updated, insStart, insEnd, "bold")
                if (isItalic) updated = RichTextFormatter.addSpan(updated, insStart, insEnd, "italic")
                if (isUnderline) updated = RichTextFormatter.addSpan(updated, insStart, insEnd, "underline")
                if (isStrikethrough) updated = RichTextFormatter.addSpan(updated, insStart, insEnd, "strikethrough")
                if (isCodeFormat) updated = RichTextFormatter.addSpan(updated, insStart, insEnd, "code")
                pendingColorHex?.let { updated = RichTextFormatter.addSpan(updated, insStart, insEnd, "color", it) }

                // Auto-continue ordered/bulleted lists: if the inserted text is a
                // single newline right after a "1. ", "a. ", "• " etc. line, insert
                // the next marker automatically.
                if (newMiddleLen == 1 && newText.getOrNull(insStart) == '\n') {
                    val lineStart = newText.lastIndexOf('\n', insStart - 1).let { if (it == -1) 0 else it + 1 }
                    val prevLine = newText.substring(lineStart, insStart)
                    val digitMatch = Regex("""^(\s*)(\d+)([.)])\s+\S""").find(prevLine)
                    val bulletMatch = Regex("""^(\s*)([•\-*])\s+\S""").find(prevLine)
                    val letterMatch = Regex("""^(\s*)([a-zA-Z])([.)])\s+\S""").find(prevLine)
                    val autoInsert: String? = when {
                        digitMatch != null -> {
                            val indent = digitMatch.groupValues[1]
                            val num = digitMatch.groupValues[2].toIntOrNull() ?: 1
                            val sep = digitMatch.groupValues[3]
                            "$indent${num + 1}$sep "
                        }
                        bulletMatch != null -> {
                            val indent = bulletMatch.groupValues[1]
                            val marker = bulletMatch.groupValues[2]
                            "$indent$marker "
                        }
                        letterMatch != null -> {
                            val indent = letterMatch.groupValues[1]
                            val letter = letterMatch.groupValues[2]
                            val sep = letterMatch.groupValues[3]
                            val nextLetter = if (letter[0].isLowerCase()) letter[0] + 1 else letter[0] + 1
                            "$indent$nextLetter$sep "
                        }
                        else -> null
                    }
                    if (autoInsert != null) {
                        val finalText = newText.substring(0, insEnd) + autoInsert + newText.substring(insEnd)
                        updated = RichTextFormatter.adjustSpansForEdit(updated, newText, finalText)
                        spans = updated
                        contentValue = TextFieldValue(finalText, selection = TextRange(insEnd + autoInsert.length))
                        return
                    }
                }
            }
            spans = updated
        }
        contentValue = newVal
    }

    // Toolbar toggle: applies to the current selection if there is one, otherwise
    // arms/disarms the pending-format-for-next-typed-text state.
    fun toggleBooleanFormat(type: String) {
        val s = contentValue.selection
        if (!s.collapsed) {
            pushUndo(contentValue.text, spans)
            spans = RichTextFormatter.toggleBooleanProperty(spans, type, s.min, s.max)
        } else {
            when (type) {
                "bold" -> isBold = !isBold
                "italic" -> isItalic = !isItalic
                "underline" -> isUnderline = !isUnderline
                "strikethrough" -> isStrikethrough = !isStrikethrough
                "code" -> isCodeFormat = !isCodeFormat
            }
        }
    }

    fun applyColor(hex: String) {
        selectedColorHex = hex
        val s = contentValue.selection
        if (!s.collapsed) {
            pushUndo(contentValue.text, spans)
            spans = RichTextFormatter.setValueProperty(spans, "color", s.min, s.max, hex)
        } else {
            pendingColorHex = hex
        }
    }

    fun hasCheckboxes(): Boolean {
        return contentValue.text.lines().any { it.trimStart().startsWith("[ ]") || it.trimStart().startsWith("[x]") }
    }

    fun executeAiAction(prompt: String) {
        isAiGenerating = true
        aiGeneratedResult = ""
        coroutineScope.launch {
            val key = com.example.data.preferences.AppPreferences(context).getEffectiveApiKey()
            val result = aiService.generateResponse(
                prompt = prompt,
                apiKey = key,
                noteContext = "Title: $title\n\nContent:\n${contentValue.text}"
            )
            isAiGenerating = false
            aiGeneratedResult = result
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
    GlassBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        performSave(showToast = false, thenNavigateBack = true)
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (initialNote == null) "New Note" else "Edit Note",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF111111)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // AI Quick Assistant Button
                    IconButton(
                        onClick = { showAiSheet = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFF7C3AED))))
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AU AI Assistant",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Undo Button
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                redoStack.add(HistoryEntry(contentValue.text, RichTextFormatter.serializeSpans(spans)))
                                val previous = undoStack.removeAt(undoStack.size - 1)
                                spans = RichTextFormatter.deserializeSpans(previous.spansJson)
                                contentValue = TextFieldValue(previous.text, selection = TextRange(previous.text.length))
                            }
                        },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) (if (isDarkMode) Color.White else Color.Black) else Color.Gray.copy(0.3f)
                        )
                    }

                    // Redo Button
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                undoStack.add(HistoryEntry(contentValue.text, RichTextFormatter.serializeSpans(spans)))
                                val next = redoStack.removeAt(redoStack.size - 1)
                                spans = RichTextFormatter.deserializeSpans(next.spansJson)
                                contentValue = TextFieldValue(next.text, selection = TextRange(next.text.length))
                            }
                        },
                        enabled = redoStack.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) (if (isDarkMode) Color.White else Color.Black) else Color.Gray.copy(0.3f)
                        )
                    }

                    // Save Button
                    Button(
                        onClick = {
                            performSave(showToast = true, thenNavigateBack = true)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Save",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // Note Content Editor Container
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title Field
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF111111)
                    ),
                    cursorBrush = SolidColor(CrimsonPrimary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "Title",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White.copy(0.35f) else Color.LightGray
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Date and Character Count Metadata
                Text(
                    text = "$formattedDate | ${contentValue.text.length} characters",
                    fontSize = 11.sp,
                    color = if (isDarkMode) Color.White.copy(0.45f) else Color.Gray
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mode / Classification Mode Header & Chips
                Text(
                    text = "Mode/Classification Mode:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color.White.copy(0.6f) else Color.DarkGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("Normal", "API", "Code", "Media", "Personal")
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) CrimsonPrimary else if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000)
                                )
                                .border(
                                    1.dp,
                                    if (isSelected) CrimsonPrimary else Color(0x33FF2D55),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "$cat ●",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else if (isDarkMode) Color.White.copy(0.8f) else Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Real attachment previews (actual images render, other files show a card).
                if (attachments.isNotEmpty()) {
                    Text(
                        text = "Attachments (${attachments.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        attachments.forEach { attachment ->
                            AttachmentPreviewCard(
                                attachment = attachment,
                                isDarkMode = isDarkMode,
                                onOpen = { openAttachment(attachment) },
                                onRemove = {
                                    attachments = attachments.filter { it.uri != attachment.uri }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Interactive Table Component (if tableData is present)
                if (tableData.isNotBlank()) {
                    Text(
                        text = "Embedded Table (Interactive)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    InteractiveTableView(
                        tableData = tableData,
                        isDarkMode = isDarkMode,
                        onTableChange = { newMarkdown ->
                            pushUndo(contentValue.text, spans)
                            tableData = newMarkdown
                        },
                        onDeleteTable = {
                            tableData = ""
                            Toast.makeText(context, "Table removed", Toast.LENGTH_SHORT).show()
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Interactive Checklist Component (if note has checkboxes)
                if (hasCheckboxes()) {
                    Text(
                        text = "Checklist Tasks (Tap to Toggle)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    InteractiveChecklistView(
                        content = contentValue.text,
                        isDarkMode = isDarkMode,
                        onContentChange = { newText ->
                            handleContentChange(TextFieldValue(newText, selection = TextRange(newText.length)))
                        }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Code block preview if category is Code or has triple backticks
                if (selectedCategory == "Code" && contentValue.text.contains("```")) {
                    val codeContent = contentValue.text.substringAfter("```").substringBeforeLast("```")
                    val language = codeContent.lines().firstOrNull()?.trim()?.ifBlank { "Code" } ?: "Code"
                    val pureCode = codeContent.lines().drop(1).joinToString("\n")
                    CodeBlockView(code = pureCode, language = language, isDarkMode = isDarkMode)
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Note Body Text Field
                val textAlign = when (alignment) {
                    "center" -> TextAlign.Center
                    "right" -> TextAlign.Right
                    else -> TextAlign.Left
                }

                val textDeco = when {
                    isUnderline && isStrikethrough -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                    isUnderline -> TextDecoration.Underline
                    isStrikethrough -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                }

                val currentFontColor = try {
                    Color(android.graphics.Color.parseColor(selectedColorHex))
                } catch (e: Exception) {
                    if (isDarkMode) Color.White else Color.Black
                }

                BasicTextField(
                    value = contentValue,
                    onValueChange = { newVal -> handleContentChange(newVal) },
                    textStyle = TextStyle(
                        fontSize = fontSize.sp,
                        fontFamily = if (isCodeFormat || selectedCategory == "Code") FontFamily.Monospace else FontFamily.Default,
                        color = currentFontColor,
                        textAlign = textAlign,
                        lineHeight = (fontSize + 6).sp
                    ),
                    // Renders each character's independent bold/italic/underline/color/etc
                    // from `spans` on top of the raw text, WITHOUT altering the raw text
                    // itself (identity offset mapping = no characters added/removed).
                    // This is what fixes the "changing one word's color changes the whole
                    // note" bug and the "raw **/<u> tags become visible" bug: formatting
                    // never touches the text buffer at all.
                    visualTransformation = remember(spans, currentFontColor, fontSize) {
                        VisualTransformation { annotated ->
                            val styled = RichTextFormatter.buildStyledText(
                                annotated.text, spans, currentFontColor, fontSize
                            )
                            TransformedText(styled, OffsetMapping.Identity)
                        }
                    },
                    cursorBrush = SolidColor(CrimsonPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 320.dp),
                    decorationBox = { innerTextField ->
                        if (contentValue.text.isEmpty()) {
                            Text(
                                text = "Write your notes, code lines, checklists, or personal thoughts here...",
                                fontSize = 14.sp,
                                color = if (isDarkMode) Color.White.copy(0.35f) else Color.Gray,
                                lineHeight = 20.sp
                            )
                        }
                        innerTextField()
                    }
                )

                // Extra bottom breathing room so the last lines of a long note are never
                // hidden behind the floating toolbar / keyboard / navigation bar.
                Spacer(modifier = Modifier.height(140.dp))
            }

            // Bottom Floating Rich Toolbar
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                isDarkMode = isDarkMode,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Aa (Text Styles)
                    IconButton(onClick = {
                        showTextStylesSheet = !showTextStylesSheet
                        showParagraphStylesSheet = false
                    }) {
                        Text(
                            text = "Aa",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (showTextStylesSheet) CrimsonPrimary else (if (isDarkMode) Color.White else Color.Black)
                        )
                    }

                    // Paragraph Style
                    IconButton(onClick = {
                        showParagraphStylesSheet = !showParagraphStylesSheet
                        showTextStylesSheet = false
                    }) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Paragraph Style",
                            tint = if (showParagraphStylesSheet) CrimsonPrimary else (if (isDarkMode) Color.White else Color.Black),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Checkbox / Todo item
                    IconButton(onClick = {
                        val sel = contentValue.selection.start
                        val text = contentValue.text
                        val prefix = if (sel == 0 || text.getOrNull(sel - 1) == '\n') "[ ] " else "\n[ ] "
                        val newText = text.substring(0, sel) + prefix + text.substring(sel)
                        handleContentChange(TextFieldValue(newText, selection = TextRange(sel + prefix.length)))
                        Toast.makeText(context, "Added Checkbox", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Default.CheckBox,
                            contentDescription = "Checkbox",
                            tint = if (isDarkMode) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Clock / Reminder
                    IconButton(onClick = {
                        val timestampNote = "⏰ ${SimpleDateFormat("hh:mm a, MMM dd", Locale.US).format(Date())}"
                        val text = contentValue.text
                        val sel = contentValue.selection.start
                        val insert = if (sel == 0 || text.getOrNull(sel - 1) == '\n') "$timestampNote\n" else "\n$timestampNote\n"
                        val newText = text.substring(0, sel) + insert + text.substring(sel)
                        handleContentChange(TextFieldValue(newText, selection = TextRange(sel + insert.length)))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Reminder",
                            tint = if (isDarkMode) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Attachment + (Camera, Albums, Doodle)
                    IconButton(onClick = { showAttachmentSheet = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Attachment",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Table insertion button
                    IconButton(onClick = {
                        if (tableData.isBlank()) {
                            tableData = """
| Col 1 | Col 2 | Col 3 |
| --- | --- | --- |
| Row 1 | Data A | Value 1 |
| Row 2 | Data B | Value 2 |
                            """.trimIndent()
                            Toast.makeText(context, "Table inserted into note", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Note already has an active table", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.TableChart,
                            contentDescription = "Table",
                            tint = if (isDarkMode) Color.White else Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Overflow ⋮ menu
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
                                tint = if (isDarkMode) Color.White else Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Insert Code Block") },
                                onClick = {
                                    val sel = contentValue.selection
                                    val text = contentValue.text
                                    val start = sel.min
                                    val end = sel.max
                                    val selectedText = text.substring(start, end)
                                    val newText = text.substring(0, start) + "```python\n" + selectedText + "\n```" + text.substring(end)
                                    handleContentChange(
                                        TextFieldValue(newText, selection = TextRange(start + "```python\n".length + selectedText.length))
                                    )
                                    showMoreMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Reset Table") },
                                onClick = {
                                    tableData = ""
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // TEXT STYLES POPUP SHEET
            AnimatedVisibility(visible = showTextStylesSheet) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    isDarkMode = isDarkMode,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Text Styles (Applies to Selection)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                            IconButton(
                                onClick = { showTextStylesSheet = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = if (isDarkMode) Color.White else Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Selection-based B, I, U, S, </> Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StyleToggleButton("B", boldActive) { toggleBooleanFormat("bold") }
                            StyleToggleButton("I", italicActive) { toggleBooleanFormat("italic") }
                            StyleToggleButton("U", underlineActive) { toggleBooleanFormat("underline") }
                            StyleToggleButton("S", strikeActive) { toggleBooleanFormat("strikethrough") }
                            StyleToggleButton("</>", codeActive) { toggleBooleanFormat("code") }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Font size slider: 10 - 36pt
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Font Size", fontSize = 12.sp, color = CrimsonPrimary, fontWeight = FontWeight.SemiBold)
                            Text("${fontSize}pt", fontSize = 12.sp, color = if (isDarkMode) Color.White else Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = fontSize.toFloat(),
                            onValueChange = { fontSize = it.toInt() },
                            valueRange = 10f..36f,
                            steps = 12,
                            colors = SliderDefaults.colors(
                                thumbColor = CrimsonPrimary,
                                activeTrackColor = CrimsonPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Font Color Palette
                        Text("Font Color", fontSize = 12.sp, color = CrimsonPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            paletteColors.forEach { hex ->
                                val colorObj = Color(android.graphics.Color.parseColor(hex))
                                val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(colorObj)
                                        .border(
                                            if (isSelected) 3.dp else 1.dp,
                                            if (isSelected) CrimsonPrimary else Color.Gray.copy(0.5f),
                                            CircleShape
                                        )
                                        .clickable { applyColor(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (hex == "#FFFFFF") Color.Black else Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // PARAGRAPH STYLES SHEET
            AnimatedVisibility(visible = showParagraphStylesSheet) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    isDarkMode = isDarkMode,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Paragraph Style",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                            IconButton(
                                onClick = { showParagraphStylesSheet = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = if (isDarkMode) Color.White else Color.Black
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Alignment Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = { alignment = "left" }) {
                                Icon(
                                    imageVector = Icons.Default.FormatAlignLeft,
                                    contentDescription = "Left",
                                    tint = if (alignment == "left") CrimsonPrimary else Color.Gray
                                )
                            }
                            IconButton(onClick = { alignment = "center" }) {
                                Icon(
                                    imageVector = Icons.Default.FormatAlignCenter,
                                    contentDescription = "Center",
                                    tint = if (alignment == "center") CrimsonPrimary else Color.Gray
                                )
                            }
                            IconButton(onClick = { alignment = "right" }) {
                                Icon(
                                    imageVector = Icons.Default.FormatAlignRight,
                                    contentDescription = "Right",
                                    tint = if (alignment == "right") CrimsonPrimary else Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Bullet, Digit, Letter lists
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ParagraphTypeButton(
                                label = "Bullet list",
                                isSelected = listType == "bullet",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    listType = "bullet"
                                    val sel = contentValue.selection.start
                                    val newText = contentValue.text.substring(0, sel) + "\n• " + contentValue.text.substring(sel)
                                    handleContentChange(TextFieldValue(newText, selection = TextRange(sel + 3)))
                                }
                            )
                            ParagraphTypeButton(
                                label = "Numbered list",
                                isSelected = listType == "digit",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    listType = "digit"
                                    val sel = contentValue.selection.start
                                    val newText = contentValue.text.substring(0, sel) + "\n1. " + contentValue.text.substring(sel)
                                    handleContentChange(TextFieldValue(newText, selection = TextRange(sel + 4)))
                                }
                            )
                            ParagraphTypeButton(
                                label = "Alphabetical list (A, B, C)",
                                isSelected = listType == "letter",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    listType = "letter"
                                    val sel = contentValue.selection.start
                                    val newText = contentValue.text.substring(0, sel) + "\nA. " + contentValue.text.substring(sel)
                                    handleContentChange(TextFieldValue(newText, selection = TextRange(sel + 4)))
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ParagraphTypeButton(
                                label = "Lowercase list (a, b, c)",
                                isSelected = listType == "letter_lower",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    listType = "letter_lower"
                                    val sel = contentValue.selection.start
                                    val newText = contentValue.text.substring(0, sel) + "\na. " + contentValue.text.substring(sel)
                                    handleContentChange(TextFieldValue(newText, selection = TextRange(sel + 4)))
                                }
                            )
                            ParagraphTypeButton(
                                label = "Roman numerals (i, ii, iii)",
                                isSelected = listType == "roman",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    listType = "roman"
                                    val sel = contentValue.selection.start
                                    val newText = contentValue.text.substring(0, sel) + "\ni. " + contentValue.text.substring(sel)
                                    handleContentChange(TextFieldValue(newText, selection = TextRange(sel + 4)))
                                }
                            )
                            ParagraphTypeButton(
                                label = "Quote",
                                isSelected = listType == "quote",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    listType = "quote"
                                    val sel = contentValue.selection.start
                                    val newText = contentValue.text.substring(0, sel) + "\n> " + contentValue.text.substring(sel)
                                    handleContentChange(TextFieldValue(newText, selection = TextRange(sel + 3)))
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ParagraphTypeButton(
                                label = "Increase indent",
                                isSelected = false,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val sel = contentValue.selection.start
                                    val text = contentValue.text
                                    val lineStart = text.lastIndexOf('\n', (sel - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                                    val newText = text.substring(0, lineStart) + "    " + text.substring(lineStart)
                                    handleContentChange(TextFieldValue(newText, selection = TextRange(sel + 4)))
                                }
                            )
                            ParagraphTypeButton(
                                label = "Decrease indent",
                                isSelected = false,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    val sel = contentValue.selection.start
                                    val text = contentValue.text
                                    val lineStart = text.lastIndexOf('\n', (sel - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
                                    val removable = text.substring(lineStart).takeWhile { it == ' ' }.length.coerceAtMost(4)
                                    if (removable > 0) {
                                        val newText = text.substring(0, lineStart) + text.substring(lineStart + removable)
                                        handleContentChange(TextFieldValue(newText, selection = TextRange((sel - removable).coerceAtLeast(0))))
                                    }
                                }
                            )
                            ParagraphTypeButton(
                                label = "Normal",
                                isSelected = listType == "none",
                                modifier = Modifier.weight(1f),
                                onClick = { listType = "none" }
                            )
                        }
                    }
                }
            }

            // ATTACHMENT POPUP SHEET
            AnimatedVisibility(visible = showAttachmentSheet) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    isDarkMode = isDarkMode,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Insert Media Attachment",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPrimary
                            )
                            IconButton(onClick = { showAttachmentSheet = false }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkMode) Color.White else Color.Black)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Real attachments: the file is actually copied into app storage and
                        // linked to this note (see AttachmentStorage + attachments state above).
                        // Nothing is inserted into the note text as a placeholder.
                        AttachmentOptionItem("📸 Camera") {
                            showAttachmentSheet = false
                            cameraLauncher.launch()
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AttachmentOptionItem("🖼 Albums / Photo Library") {
                            showAttachmentSheet = false
                            galleryLauncher.launch("image/*")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        AttachmentOptionItem("📎 File / Document / PDF") {
                            showAttachmentSheet = false
                            filePickerLauncher.launch(arrayOf("*/*"))
                        }
                    }
                }
            }

            // AI ASSISTANT BOTTOM SHEET INSIDE NOTE EDITOR
            if (showAiSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showAiSheet = false },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = if (isDarkMode) Color(0xFF14030B) else Color(0xFFFFF0F4)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SmartToy, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AU AI Note Assistant", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CrimsonPrimary)
                            }
                            IconButton(onClick = { showAiSheet = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = if (isDarkMode) Color.White else Color.Black)
                            }
                        }

                        Text("Quick AI Actions for this Note:", fontSize = 12.sp, color = if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { executeAiAction("Summarize this note into concise bullet points.") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Summarize", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { executeAiAction("Enhance the writing, fix any grammar, and polish the structure.") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Enhance", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { executeAiAction("Generate 3 high-yield study questions based on this note.") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Questions", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedTextField(
                            value = aiPromptInput,
                            onValueChange = { aiPromptInput = it },
                            placeholder = { Text("Or ask a custom question about this note...", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                if (aiPromptInput.isNotBlank()) {
                                    executeAiAction(aiPromptInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Ask AU AI", fontWeight = FontWeight.Bold)
                        }

                        if (isAiGenerating) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = CrimsonPrimary, strokeWidth = 2.dp)
                                Text("AU AI is thinking...", fontSize = 12.sp, color = CrimsonPrimary)
                            }
                        }

                        if (aiGeneratedResult.isNotBlank()) {
                            GlassCard(
                                modifier = Modifier.fillMaxWidth(),
                                isDarkMode = isDarkMode
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("AU AI Response:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = CrimsonPrimary)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    SelectionContainer {
                                        Text(
                                            text = aiGeneratedResult,
                                            fontSize = 12.sp,
                                            lineHeight = 17.sp,
                                            color = if (isDarkMode) Color.White else Color.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                val sel = contentValue.selection.start
                                                val insert = "\n\n--- AU AI Assistance ---\n$aiGeneratedResult\n"
                                                val newText = contentValue.text.substring(0, sel) + insert + contentValue.text.substring(sel)
                                                contentValue = TextFieldValue(newText, selection = TextRange(sel + insert.length))
                                                showAiSheet = false
                                                Toast.makeText(context, "Inserted AI response into note", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Insert into Note", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        TextButton(
                                            onClick = {
                                                clipboard.setText(AnnotatedString(aiGeneratedResult))
                                                Toast.makeText(context, "Copied AI response", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Copy Response", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // PDF attachment viewer overlay — opened without navigating away, so the
        // note's in-progress edits are never at risk of looking stale on return.
        pdfAttachmentToView?.let { uri ->
            com.example.ui.screens.PdfViewerScreen(
                pdfUri = uri,
                isDarkMode = isDarkMode,
                onClose = { pdfAttachmentToView = null }
            )
        }

        // Floating, draggable AI button — the user can drag it anywhere on screen;
        // a plain tap (movement below a small threshold) opens the same AI sheet as
        // the toolbar AI button, now positioned wherever they left it.
        var floatingAiOffsetX by remember { mutableStateOf(0f) }
        var floatingAiOffsetY by remember { mutableStateOf(300f) }
        var floatingAiDragDistance by remember { mutableStateOf(0f) }
        Box(
            modifier = Modifier
                .offset { androidx.compose.ui.unit.IntOffset(floatingAiOffsetX.toInt(), floatingAiOffsetY.toInt()) }
                .size(46.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFF7C3AED))))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { floatingAiDragDistance = 0f },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            floatingAiDragDistance += kotlin.math.abs(dragAmount.x) + kotlin.math.abs(dragAmount.y)
                            floatingAiOffsetX += dragAmount.x
                            floatingAiOffsetY += dragAmount.y
                        },
                        onDragEnd = {
                            // Treat a near-stationary press-and-release as a tap, since
                            // drag gestures otherwise swallow simple clicks.
                            if (floatingAiDragDistance < 12f) {
                                showAiSheet = true
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = "Floating AU AI Assistant",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun StyleToggleButton(
    symbol: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isActive) CrimsonPrimary else Color(0x22FFFFFF))
            .border(1.dp, if (isActive) CrimsonPrimary else Color(0x33FF2D55), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbol,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (isActive) Color.White else Color.White.copy(0.85f)
        )
    }
}

@Composable
fun ParagraphTypeButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) CrimsonPrimary else Color(0x22FFFFFF))
            .border(1.dp, if (isSelected) CrimsonPrimary else Color(0x33FF2D55), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

@Composable
fun AttachmentOptionItem(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x22FFFFFF))
            .border(1.dp, Color(0x33FF2D55), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
    }
}

@Composable
fun AttachmentPreviewCard(
    attachment: RichTextFormatter.AttachmentInfo,
    isDarkMode: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit
) {
    val isImage = attachment.mimeType.startsWith("image/")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDarkMode) Color(0x1AFFFFFF) else Color(0x14FF2D55))
            .border(1.dp, Color(0x33FF2D55), RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isImage) {
                AsyncImage(
                    model = attachment.uri,
                    contentDescription = attachment.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(CrimsonPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (attachment.mimeType == "application/pdf") Icons.Default.PictureAsPdf else Icons.Default.InsertDriveFile,
                        contentDescription = "File",
                        tint = CrimsonPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attachment.fileName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color.White else Color.Black,
                    maxLines = 1
                )
                Text(
                    text = formatFileSize(attachment.sizeBytes),
                    fontSize = 11.sp,
                    color = if (isDarkMode) Color.White.copy(0.5f) else Color.Gray
                )
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Remove attachment", tint = Color(0xFFFF5252))
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}
