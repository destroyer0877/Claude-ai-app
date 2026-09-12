package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutoClassifier
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.ui.components.ExportDialog
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.PinLockDialog
import com.example.ui.theme.CategoryApiColor
import com.example.ui.theme.CategoryCodeColor
import com.example.ui.theme.CategoryMediaColor
import com.example.ui.theme.CategoryPersonalColor
import com.example.ui.theme.CrimsonPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReadNoteScreen(
    note: NoteEntity,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onEditNote: (NoteEntity) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onToggleLock: (Long, Boolean) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var showExportDialog by remember { mutableStateOf(false) }
    var showPinDialog by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    // Android TextToSpeech engine
    var tts: TextToSpeech? by remember { mutableStateOf(null) }

    DisposableEffect(Unit) {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
            }
        }
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    val dateFormatter = remember {
        SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US)
    }

    GlassBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar: Back, "READ MODE", Edit Icon (Screenshot 4/7)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                }

                Text(
                    text = "READ MODE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF111111),
                    letterSpacing = 1.sp
                )

                IconButton(onClick = { onEditNote(note) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Note",
                        tint = CrimsonPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Note Glass Card (Screenshot 4/7)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                isDarkMode = isDarkMode
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Title
                    Text(
                        text = note.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF111111)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Date
                    Text(
                        text = dateFormatter.format(Date(note.updatedAt)),
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color.White.copy(0.55f) else Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Toolbar Card (Screenshot 4/7: Lock | Export | Delete | TTS | Copy)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDarkMode) Color(0x33000000) else Color(0x14FF2D55))
                            .border(1.dp, Color(0x33FF2D55), RoundedCornerShape(14.dp))
                            .padding(vertical = 8.dp, horizontal = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Lock Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    if (note.isLocked) {
                                        showPinDialog = true
                                    } else {
                                        onToggleLock(note.id, false)
                                        Toast.makeText(context, "Note Locked", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = if (note.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                contentDescription = "Lock",
                                tint = if (note.isLocked) CrimsonPrimary else if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (note.isLocked) "Locked" else "Lock",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                            )
                        }

                        Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                        // Export Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { showExportDialog = true }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FileDownload,
                                contentDescription = "Export",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Export",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                            )
                        }

                        Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                        // Delete Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    onDeleteNote(note.id)
                                    Toast.makeText(context, "Moved to Recycle Bin", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFFF5252),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Delete",
                                fontSize = 11.sp,
                                color = Color(0xFFFF5252)
                            )
                        }

                        Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                        // TTS Button (Speaker - prompt spec 12)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    if (isSpeaking) {
                                        tts?.stop()
                                        isSpeaking = false
                                    } else {
                                        val toSpeak = "${note.title}. ${note.content}"
                                        tts?.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null, "NoteTTS")
                                        isSpeaking = true
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                contentDescription = "TTS",
                                tint = Color(0xFF2CF95F),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSpeaking) "Stop" else "Listen",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                            )
                        }

                        Text("|", color = if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), fontSize = 12.sp)

                        // Copy Button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    clipboard.setText(AnnotatedString(note.content))
                                    Toast.makeText(context, "Note copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = Color(0xFFFFD93D),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Copy",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(0.85f) else Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Content Area
                    val displayContent = if (note.category == "API" && preferences.blurApis.value && note.isLocked) {
                        AutoClassifier.maskApiKey(note.content)
                    } else {
                        note.content
                    }

                    if (note.category == "Code" || note.isCodeFormat) {
                        // Formatted Code Block Container
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isDarkMode) Color(0xFF090B10) else Color(0xFF1E1E1E))
                                .border(1.dp, Color(0x3326C6DA), RoundedCornerShape(12.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                text = displayContent,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF80D8FF),
                                lineHeight = 20.sp
                            )
                        }
                    } else {
                        // General / Formatted text
                        Text(
                            text = displayContent,
                            fontSize = note.fontSize.sp,
                            fontWeight = if (note.isBold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (note.isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                            color = if (isDarkMode) Color.White.copy(0.92f) else Color(0xFF1E1E1E),
                            lineHeight = 22.sp
                        )
                    }

                    // Render Table if table data exists
                    if (note.tableData.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Embedded Table:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        RenderTable(note.tableData, isDarkMode)
                    }
                }
            }
        }

        // Export Dialog
        if (showExportDialog) {
            ExportDialog(
                note = note,
                isDarkMode = isDarkMode,
                onDismiss = { showExportDialog = false }
            )
        }

        // Unlock PIN Dialog
        if (showPinDialog) {
            PinLockDialog(
                correctPin = preferences.lockPin.value,
                title = "Unlock Note",
                isDarkMode = isDarkMode,
                onDismiss = { showPinDialog = false },
                onUnlocked = {
                    showPinDialog = false
                    onToggleLock(note.id, true)
                    Toast.makeText(context, "Note Unlocked", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun RenderTable(tableData: String, isDarkMode: Boolean) {
    val rows = tableData.lines().filter { it.isNotBlank() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, if (isDarkMode) Color(0x33FFFFFF) else Color(0x33000000), RoundedCornerShape(8.dp))
    ) {
        rows.forEachIndexed { rowIndex, row ->
            val cells = row.split("|").filter { it.isNotBlank() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (rowIndex == 0) {
                            if (isDarkMode) Color(0x33FF2D55) else Color(0x14FF2D55)
                        } else if (rowIndex % 2 == 0) {
                            if (isDarkMode) Color(0x14FFFFFF) else Color(0x0A000000)
                        } else {
                            Color.Transparent
                        }
                    )
                    .padding(vertical = 6.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                cells.forEach { cell ->
                    Text(
                        text = cell.trim(),
                        fontSize = 12.sp,
                        fontWeight = if (rowIndex == 0) FontWeight.Bold else FontWeight.Normal,
                        color = if (isDarkMode) Color.White else Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (rowIndex < rows.size - 1) {
                HorizontalDivider(color = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x1A000000))
            }
        }
    }
}
