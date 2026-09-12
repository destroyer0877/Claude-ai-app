package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiService
import com.example.data.ai.AiActionResult
import com.example.data.ai.ConversationTurn
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.data.preferences.AppPreferences
import com.example.ui.components.CodeBlockView
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "bot" or "user"
    val text: String,
    val attachedFileName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val noteResults: List<com.example.data.model.NoteEntity>? = null,
    val pendingAction: PendingAiAction? = null
)

/** A destructive app-command AU Bot wants to run (move or delete-to-trash a note).
 * Nothing happens until the user taps Confirm in the chat UI. */
data class PendingAiAction(
    val type: String, // "move" or "delete"
    val noteId: Long,
    val noteTitle: String,
    val targetFolder: String? = null,
    val status: String = "pending" // pending | confirmed | cancelled
)

data class ArchivedChat(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val messages: List<ChatMessage>,
    val timestamp: Long = System.currentTimeMillis()
)

private fun renderPdfPagesToBase64(context: android.content.Context, uri: Uri, maxPages: Int): List<String> {
    val images = mutableListOf<String>()
    try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return emptyList()
        pfd.use { descriptor ->
            val renderer = android.graphics.pdf.PdfRenderer(descriptor)
            renderer.use {
                val pageCount = minOf(it.pageCount, maxPages)
                for (i in 0 until pageCount) {
                    it.openPage(i).use { page ->
                        val bitmap = android.graphics.Bitmap.createBitmap(
                            page.width.coerceAtLeast(1) * 2,
                            page.height.coerceAtLeast(1) * 2,
                            android.graphics.Bitmap.Config.ARGB_8888
                        )
                        bitmap.eraseColor(android.graphics.Color.WHITE)
                        page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        val stream = java.io.ByteArrayOutputStream()
                        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 90, stream)
                        images.add(android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP))
                        bitmap.recycle()
                    }
                }
            }
        }
    } catch (e: Exception) {
        return emptyList()
    }
    return images
}

@Composable
fun AiChatScreen(
    isOpen: Boolean,
    isDarkMode: Boolean,
    preferences: AppPreferences,
    repository: NoteRepository,
    currentNoteContent: String? = null,
    onClose: () -> Unit,
    onCreateNoteFromAi: (String, String, String) -> Unit,
    onOpenNoteFromAi: (NoteEntity) -> Unit = {}
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { AiService() }

    var isCompactMode by remember { mutableStateOf(false) }
    var activeTab by remember { mutableStateOf("Chat") }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Attachment state
    var attachedFileName by remember { mutableStateOf<String?>(null) }
    var attachedContent by remember { mutableStateOf<String?>(null) }
    // Rendered PDF page images (base64 PNG) — set instead of attachedContent for PDFs,
    // since PdfRenderer gives page bitmaps, not extractable text.
    var attachedPdfImages by remember { mutableStateOf<List<String>?>(null) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                sender = "bot",
                text = "Hello! I am your AU Bot assistant. I can summarize notes, explain code, write scripts, and organize your workspace. How may I assist you today?"
            )
        )
    }

    val archivedChats = remember { mutableStateListOf<ArchivedChat>() }
    val listState = rememberLazyListState()

    // File picker for attachments
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                var fileName = "attachment"
                var mimeType = context.contentResolver.getType(uri) ?: ""
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIdx != -1) {
                        fileName = cursor.getString(nameIdx)
                    }
                }
                attachedFileName = fileName
                attachedContent = null
                attachedPdfImages = null

                if (mimeType == "application/pdf" || fileName.endsWith(".pdf", ignoreCase = true)) {
                    // Real PDF understanding: render pages to images and send them to
                    // the model as actual visual input, instead of garbling raw PDF
                    // bytes as if they were plain text.
                    val images = renderPdfPagesToBase64(context, uri, maxPages = 6)
                    if (images.isEmpty()) {
                        Toast.makeText(context, "Couldn't read that PDF — it may be corrupted or password-protected", Toast.LENGTH_SHORT).show()
                        attachedFileName = null
                    } else {
                        attachedPdfImages = images
                        Toast.makeText(context, "Attached $fileName (${images.size} page(s))", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    attachedContent = text.take(5000) // Keep reasonable prompt size
                    Toast.makeText(context, "Attached $fileName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not read attachment: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Voice input result — fills the chat input with recognized speech; sending it
    // still goes through the normal tool-calling pipeline like typed text.
    val voiceInputLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                inputText = spoken
            }
        }
    }

    fun findMessageIndex(id: String): Int = messages.indexOfFirst { it.id == id }

    fun onResolveAiAction(messageId: String, action: PendingAiAction, confirmed: Boolean) {
        val idx = findMessageIndex(messageId)
        if (idx == -1) return
        if (!confirmed) {
            messages[idx] = messages[idx].copy(pendingAction = action.copy(status = "cancelled"))
            return
        }
        coroutineScope.launch {
            try {
                when (action.type) {
                    "delete" -> repository.moveToTrash(action.noteId)
                    "move" -> {
                        val existing = repository.getNoteByIdOnce(action.noteId)
                        if (existing != null && !action.targetFolder.isNullOrBlank()) {
                            repository.saveNote(existing.copy(folder = action.targetFolder, updatedAt = System.currentTimeMillis()))
                        }
                    }
                }
                val i = findMessageIndex(messageId)
                if (i != -1) messages[i] = messages[i].copy(pendingAction = action.copy(status = "confirmed"))
            } catch (e: Exception) {
                Toast.makeText(context, "Could not complete that action", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Executes a model-requested function call. Read-only (search) runs immediately;
    // create is non-destructive so it runs immediately too; move/delete only ever
    // post a confirmation card — see onResolveAiAction for where they actually run.
    suspend fun handleFunctionCall(name: String, args: org.json.JSONObject) {
        when (name) {
            "search_notes" -> {
                val query = args.optString("query", "")
                val results = try { repository.searchNotes(query).first() } catch (e: Exception) { emptyList() }
                if (results.isEmpty()) {
                    messages.add(ChatMessage(sender = "bot", text = "I couldn't find any notes matching \"$query\"."))
                } else {
                    messages.add(
                        ChatMessage(
                            sender = "bot",
                            text = "Found ${results.size} note(s) for \"$query\" — tap one to open it:",
                            noteResults = results.take(5)
                        )
                    )
                }
            }
            "create_note" -> {
                val title = args.optString("title", "Untitled Note")
                val content = args.optString("content", "")
                val category = args.optString("category", "General")
                onCreateNoteFromAi(title, content, category)
                messages.add(ChatMessage(sender = "bot", text = "Created a new note: \"$title\"."))
            }
            "move_note_to_folder" -> {
                val noteId = args.optLong("note_id", -1L)
                val folder = args.optString("folder", "")
                val note = if (noteId >= 0) repository.getNoteByIdOnce(noteId) else null
                if (note == null) {
                    messages.add(ChatMessage(sender = "bot", text = "I need to find that note first — try asking me to search for it by name."))
                } else {
                    messages.add(
                        ChatMessage(
                            sender = "bot",
                            text = "Move \"${note.title}\" to $folder?",
                            pendingAction = PendingAiAction("move", note.id, note.title, folder)
                        )
                    )
                }
            }
            "delete_note" -> {
                val noteId = args.optLong("note_id", -1L)
                val note = if (noteId >= 0) repository.getNoteByIdOnce(noteId) else null
                if (note == null) {
                    messages.add(ChatMessage(sender = "bot", text = "I need to find that note first — try asking me to search for it by name."))
                } else {
                    messages.add(
                        ChatMessage(
                            sender = "bot",
                            text = "Move \"${note.title}\" to the recycle bin?",
                            pendingAction = PendingAiAction("delete", note.id, note.title)
                        )
                    )
                }
            }
            else -> {
                messages.add(ChatMessage(sender = "bot", text = "I tried to run an action I don't recognize, so I skipped it."))
            }
        }
    }

    fun handleSend(promptText: String) {
        if (promptText.isBlank() && attachedContent.isNullOrBlank() && attachedPdfImages.isNullOrEmpty()) return
        if (isLoading) return

        val userMsg = promptText.trim().ifBlank { "Please analyze this attached file." }
        val currentAttachmentName = attachedFileName
        val currentAttachmentText = attachedContent
        val currentPdfImages = attachedPdfImages

        inputText = ""
        attachedFileName = null
        attachedContent = null
        attachedPdfImages = null

        messages.add(
            ChatMessage(
                sender = "user",
                text = userMsg,
                attachedFileName = currentAttachmentName
            )
        )
        isLoading = true

        coroutineScope.launch {
            val key = preferences.getEffectiveApiKey()
            val model = preferences.selectedModel.value
            val history = messages.map { ConversationTurn(it.sender, it.text) }

            // Attachments (text files or PDF page images) go through the plain
            // multimodal path; everything else goes through the tool-enabled path so
            // AU Bot can act on real app data when the user asks it to.
            if (!currentAttachmentText.isNullOrBlank() || !currentPdfImages.isNullOrEmpty()) {
                val response = aiService.generateResponse(
                    prompt = userMsg,
                    apiKey = key,
                    model = model,
                    noteContext = currentNoteContent,
                    attachmentContent = currentAttachmentText,
                    history = history,
                    attachmentImagesBase64 = currentPdfImages ?: emptyList()
                )
                isLoading = false
                messages.add(ChatMessage(sender = "bot", text = response))
            } else {
                when (val result = aiService.generateWithTools(
                    prompt = userMsg,
                    apiKey = key,
                    model = model,
                    noteContext = currentNoteContent,
                    history = history
                )) {
                    is AiActionResult.Text -> {
                        isLoading = false
                        messages.add(ChatMessage(sender = "bot", text = result.text))
                    }
                    is AiActionResult.FunctionCall -> {
                        handleFunctionCall(result.name, result.args)
                        isLoading = false
                    }
                }
            }
        }
    }

    fun startNewChat() {
        if (messages.size > 1) {
            val firstUserMsg = messages.firstOrNull { it.sender == "user" }?.text?.take(25) ?: "Session"
            archivedChats.add(
                ArchivedChat(
                    title = "Chat: $firstUserMsg",
                    messages = messages.toList()
                )
            )
        }
        messages.clear()
        messages.add(
            ChatMessage(
                sender = "bot",
                text = "New chat session started. Ready for your notes, questions, or documents!"
            )
        )
        Toast.makeText(context, "New chat started (previous archived)", Toast.LENGTH_SHORT).show()
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(350)) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Container (Either full height or compact floating card)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isCompactMode) Modifier
                            .height(420.dp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.5.dp, CrimsonPrimary, RoundedCornerShape(24.dp))
                        else Modifier.fillMaxHeight()
                    )
                    .background(
                        if (isDarkMode) {
                            Brush.verticalGradient(listOf(Color(0xF514030B), Color(0xFB1E0512), Color(0xFF0C0106)))
                        } else {
                            Brush.verticalGradient(listOf(Color(0xF8FFFFFF), Color(0xF4FFF0F4)))
                        }
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFF7928CA)))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "AU AI ASSISTANT",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color.White else Color(0xFF111111)
                                )
                                Text(
                                    text = if (isCompactMode) "Compact Overlay" else "Full Workspace View",
                                    fontSize = 10.sp,
                                    color = CrimsonPrimary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Expand/Collapse Compact Toggle
                            IconButton(onClick = { isCompactMode = !isCompactMode }) {
                                Icon(
                                    imageVector = if (isCompactMode) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isCompactMode) "Expand Fullscreen" else "Collapse to Compact",
                                    tint = if (isDarkMode) Color.White else Color.Black
                                )
                            }

                            IconButton(onClick = onClose) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close AU Bot",
                                    tint = if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.Black
                                )
                            }
                        }
                    }

                    // Tabs: Chat, History, + NEW CHAT
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (activeTab == "Chat") Color(0xFF3B82F6) else if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000))
                                .clickable { activeTab = "Chat" }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "● Chat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeTab == "Chat") Color.White else if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (activeTab == "History") Color(0xFF3B82F6) else if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000))
                                .clickable { activeTab = "History" }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "History (${archivedChats.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (activeTab == "History") Color.White else if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF8E53))))
                                .clickable { startNewChat() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "+ New Chat",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    if (activeTab == "History") {
                        // History list
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (archivedChats.isEmpty()) {
                                item {
                                    Text(
                                        "No archived chats yet. Tap '+ New Chat' to save the current session.",
                                        fontSize = 12.sp,
                                        color = if (isDarkMode) Color.White.copy(0.6f) else Color.Gray
                                    )
                                }
                            } else {
                                items(archivedChats) { chat ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000))
                                            .clickable {
                                                messages.clear()
                                                messages.addAll(chat.messages)
                                                activeTab = "Chat"
                                                Toast.makeText(context, "Loaded ${chat.title}", Toast.LENGTH_SHORT).show()
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Text(chat.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CrimsonPrimary)
                                            Text("${Date(chat.timestamp)} • ${chat.messages.size} messages", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Chat Messages List
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(messages) { msg ->
                                if (msg.sender == "bot") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isDarkMode) Color(0x388B152A) else Color(0x1AFF2D55))
                                            .border(1.dp, if (isDarkMode) Color(0x66FF2D55) else Color(0x44FF2D55), RoundedCornerShape(16.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "AU BOT INTELLIGENCE",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = CrimsonPrimary
                                                )

                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    // Save as note button
                                                    IconButton(
                                                        onClick = {
                                                            onCreateNoteFromAi(
                                                                "AU Note ${Date()}",
                                                                msg.text,
                                                                if (msg.text.contains("```")) "Code" else "General"
                                                            )
                                                            Toast.makeText(context, "Saved as new note!", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.NoteAdd, contentDescription = "Save Note", tint = CrimsonPrimary, modifier = Modifier.size(14.dp))
                                                    }

                                                    // Copy response button
                                                    IconButton(
                                                        onClick = {
                                                            clipboard.setText(AnnotatedString(msg.text))
                                                            Toast.makeText(context, "Copied response", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CrimsonPrimary, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(6.dp))

                                            // Check if msg contains code block
                                            if (msg.text.contains("```")) {
                                                RenderMessageWithCodeBlocks(msg.text, isDarkMode)
                                            } else {
                                                SelectionContainer {
                                                    Text(
                                                        text = msg.text,
                                                        fontSize = 13.sp,
                                                        lineHeight = 18.sp,
                                                        color = if (isDarkMode) Color.White.copy(alpha = 0.95f) else Color(0xFF111111)
                                                    )
                                                }
                                            }

                                            // Note search results — tap to pick which one you meant.
                                            msg.noteResults?.let { results ->
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    results.forEach { note ->
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(10.dp))
                                                                .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x14FF2D55))
                                                                .clickable {
                                                                    onOpenNoteFromAi(note)
                                                                }
                                                                .padding(10.dp)
                                                        ) {
                                                            Column {
                                                                Text(note.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                                                                Text("${note.category} • ${note.content.take(50)}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Destructive action (move/delete) — never runs until the user taps Confirm.
                                            msg.pendingAction?.let { action ->
                                                Spacer(modifier = Modifier.height(8.dp))
                                                if (action.status == "pending") {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                        androidx.compose.material3.Button(
                                                            onClick = { onResolveAiAction(msg.id, action, true) },
                                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                                                        ) {
                                                            Text(if (action.type == "delete") "Confirm Delete" else "Confirm Move", fontSize = 11.sp)
                                                        }
                                                        androidx.compose.material3.OutlinedButton(
                                                            onClick = { onResolveAiAction(msg.id, action, false) }
                                                        ) {
                                                            Text("Cancel", fontSize = 11.sp)
                                                        }
                                                    }
                                                } else {
                                                    Text(
                                                        text = if (action.status == "confirmed") "✓ Done" else "Cancelled",
                                                        fontSize = 11.sp,
                                                        color = if (action.status == "confirmed") Color(0xFF2CF95F) else Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // User message bubble
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        if (msg.attachedFileName != null) {
                                            Row(
                                                modifier = Modifier
                                                    .padding(bottom = 4.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0x33FF2D55))
                                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.AttachFile, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(msg.attachedFileName, fontSize = 10.sp, color = CrimsonPrimary, fontWeight = FontWeight.SemiBold)
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF2563EB))
                                                .padding(horizontal = 14.dp, vertical = 9.dp)
                                        ) {
                                            SelectionContainer {
                                                Text(text = msg.text, fontSize = 13.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }

                            if (isLoading) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CrimsonPrimary, strokeWidth = 2.dp)
                                        Text("AU Bot is generating...", fontSize = 12.sp, color = CrimsonPrimary)
                                    }
                                }
                            }
                        }

                        // Smart Action Quick Prompts (Only show if not in compact mode)
                        if (!isCompactMode) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    QuickActionButton("📝 Summarize", isDarkMode, Modifier.weight(1f)) {
                                        handleSend("Summarize current note or workspace in clear bullet points")
                                    }
                                    QuickActionButton("🐍 Generate Code", isDarkMode, Modifier.weight(1f)) {
                                        handleSend("Write a clean Python script and explain it step by step")
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    QuickActionButton("🔍 Explain Concept", isDarkMode, Modifier.weight(1f)) {
                                        handleSend("Explain how AU Notes SQLite Room persistence and glassmorphism work")
                                    }
                                    QuickActionButton("📚 Study Guide", isDarkMode, Modifier.weight(1f)) {
                                        handleSend("Generate 3 key study questions from this note")
                                    }
                                }
                            }
                        }

                        // Attached file pill if any
                        attachedFileName?.let { fname ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x33FF2D55))
                                        .border(1.dp, CrimsonPrimary, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.AttachFile, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Attached: $fname", fontSize = 11.sp, color = CrimsonPrimary, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = CrimsonPrimary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable {
                                                    attachedFileName = null
                                                    attachedContent = null
                                                }
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Input Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // File Attachment Button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color(0x26FFFFFF) else Color(0x14000000))
                                    .clickable {
                                        attachmentPicker.launch(arrayOf("text/*", "application/pdf", "application/json", "*/*"))
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = "Attach File", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                            }

                            // Input textfield — grows with content up to a max height and
                            // scrolls internally past that (was a fixed single line before).
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 42.dp, max = 120.dp)
                                    .clip(RoundedCornerShape(21.dp))
                                    .background(if (isDarkMode) Color(0x2BFFFFFF) else Color(0x14000000))
                                    .border(1.dp, Color(0x33FF2D55), RoundedCornerShape(21.dp))
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                TextField(
                                    value = inputText,
                                    onValueChange = { inputText = it },
                                    placeholder = {
                                        Text("Ask AU AI, write, or translate...", fontSize = 12.sp, color = if (isDarkMode) Color.White.copy(0.4f) else Color.Gray)
                                    },
                                    maxLines = 5,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                                        unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                )
                            }

                            // Voice input — real speech-to-text via Android's system
                            // recognizer. The recognized text just becomes the chat prompt,
                            // so it goes through the same tool-calling + confirmation safety
                            // as typed text; it can never unlock folders or touch security.
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkMode) Color(0x26FFFFFF) else Color(0x14000000))
                                    .clickable {
                                        try {
                                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Speak your command…")
                                            }
                                            voiceInputLauncher.launch(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Voice input isn't available on this device", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Mic, contentDescription = "Voice input", tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                            }

                            // Send button
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))))
                                    .clickable { handleSend(inputText) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000))
            .border(1.dp, CrimsonPrimary.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White.copy(alpha = 0.9f) else Color.DarkGray
        )
    }
}

@Composable
private fun RenderMessageWithCodeBlocks(text: String, isDarkMode: Boolean) {
    val parts = text.split("```")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                // Code block
                val firstLineEnd = part.indexOf('\n')
                val language = if (firstLineEnd != -1) part.substring(0, firstLineEnd).trim().ifBlank { "Code" } else "Code"
                val codeContent = if (firstLineEnd != -1) part.substring(firstLineEnd + 1) else part
                CodeBlockView(
                    code = codeContent,
                    language = language,
                    isDarkMode = isDarkMode
                )
            } else {
                // Normal text
                val trimmed = part.trim()
                if (trimmed.isNotBlank()) {
                    SelectionContainer {
                        Text(
                            text = trimmed,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = if (isDarkMode) Color.White.copy(alpha = 0.95f) else Color(0xFF111111)
                        )
                    }
                }
            }
        }
    }
}
