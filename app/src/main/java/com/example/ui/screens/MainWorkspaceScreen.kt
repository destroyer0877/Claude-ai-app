package com.example.ui.screens

import com.example.R

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Brightness7
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
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
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.components.PinLockDialog
import com.example.ui.theme.CategoryApiColor
import com.example.ui.theme.CategoryCodeColor
import com.example.ui.theme.CategoryGeneralColor
import com.example.ui.theme.CategoryMediaColor
import com.example.ui.theme.CategoryPersonalColor
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.launch

@Composable
fun MainWorkspaceScreen(
    repository: NoteRepository,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onOpenSidebar: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiChat: () -> Unit,
    onOpenNote: (NoteEntity) -> Unit,
    onCreateNote: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val blurApis by preferences.blurApis.collectAsState()
    val lockPin by preferences.lockPin.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFolder by remember { mutableStateOf("All Notes") }
    var isBulkSelectMode by remember { mutableStateOf(false) }
    val selectedNoteIds = remember { mutableStateListOf<Long>() }

    // PIN lock prompt for protected notes or APIs folder
    var pendingLockedNote by remember { mutableStateOf<NoteEntity?>(null) }
    // Generic per-folder lock: a folder only prompts for a PIN if the user has
    // explicitly locked it from Settings -> Security (preferences.isFolderLocked).
    // Opening a normal, non-locked folder must never trigger any security prompt.
    var unlockedFoldersThisSession by remember { mutableStateOf(setOf<String>()) }
    var pendingFolderToOpen by remember { mutableStateOf<String?>(null) }
    fun openFolder(folderName: String) {
        // "Hidden" always requires the PIN, regardless of the Settings toggle — that's
        // the whole point of a hidden folder. Other folders stay opt-in via Settings.
        val requiresLock = folderName == "Hidden" || preferences.isFolderLocked(folderName)
        if (requiresLock && folderName !in unlockedFoldersThisSession) {
            pendingFolderToOpen = folderName
        } else {
            selectedFolder = folderName
        }
    }

    // Observe active notes
    val allNotes by repository.allActiveNotes.collectAsState(initial = emptyList())
    val favoriteNotes by repository.favoriteNotes.collectAsState(initial = emptyList())

    // Counts for filter pills
    val allCount = allNotes.size
    val favCount = favoriteNotes.size
    val apiCount = allNotes.count { it.category == "API" }
    val codeCount = allNotes.count { it.category == "Code" }
    val mediaCount = allNotes.count { it.category == "Media" }
    val personalCount = allNotes.count { it.category == "Personal" }
    val hiddenCount = allNotes.count { it.folder == "Hidden" }

    val displayedNotes = remember(allNotes, selectedFolder, searchQuery) {
        var list = when (selectedFolder) {
            "Favorites" -> allNotes.filter { it.isFavorite && it.folder != "Hidden" }
            "APIs Keys" -> allNotes.filter { it.category == "API" && it.folder != "Hidden" }
            "Code" -> allNotes.filter { it.category == "Code" && it.folder != "Hidden" }
            "Media" -> allNotes.filter { it.category == "Media" && it.folder != "Hidden" }
            "Personal" -> allNotes.filter { it.category == "Personal" && it.folder != "Hidden" }
            // Hidden notes never show up outside the Hidden folder itself — not in
            // "All Notes", not in any other folder, not in search from those views.
            "Hidden" -> allNotes.filter { it.folder == "Hidden" }
            else -> allNotes.filter { it.folder != "Hidden" }
        }
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.content.contains(searchQuery, ignoreCase = true)
            }
        }
        list
    }

    // Smooth sun/moon rotation animation
    val sunMoonRotation by animateFloatAsState(
        targetValue = if (isDarkMode) 360f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "sun_moon_rotation"
    )

    GlassBackground(isDarkMode = isDarkMode) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // TOP BAR (Screenshot 2 / 5)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Hamburger Menu
                    IconButton(onClick = onOpenSidebar) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = if (isDarkMode) Color.White else Color(0xFF111111),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Center: "AU NOTES" with glowing crimson gradient
                    Text(
                        text = "AU NOTES",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = CrimsonPrimary,
                        letterSpacing = 2.sp
                    )

                    // Right: Sun/Moon Day-Night toggle & Settings Gear
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Sun to Moon toggle with smooth animated swipe
                        IconButton(onClick = onToggleDarkMode) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.Brightness7 else Icons.Default.Brightness4,
                                contentDescription = "Toggle Day/Night Mode",
                                tint = if (isDarkMode) Color(0xFFFFD93D) else Color(0xFFFF6B7D),
                                modifier = Modifier
                                    .size(22.dp)
                                    .rotate(sunMoonRotation)
                            )
                        }

                        // Settings Gear
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = if (isDarkMode) Color.White else Color(0xFF111111),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // SEARCH BAR (Screenshot 2 / 5: "Search notes by title or content...")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text(
                                text = "Search notes by title or content...",
                                fontSize = 13.sp,
                                color = if (isDarkMode) Color.White.copy(0.4f) else Color.Gray
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0x33FF2D55),
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            focusedContainerColor = if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000),
                            unfocusedContainerColor = if (isDarkMode) Color(0x1AFFFFFF) else Color(0x0D000000)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                }

                // FILTER CHIPS / FOLDERS (Screenshot 2 / 5: All Logs, Favorites, APIs Keys, Code, Media...)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChipItem(
                        title = "All Notes",
                        count = allCount,
                        isSelected = selectedFolder == "All Notes",
                        isDarkMode = isDarkMode,
                        iconRes = R.drawable.ic_svg_document,
                        onClick = { selectedFolder = "All Notes" }
                    )

                    FilterChipItem(
                        title = "Favorites",
                        count = favCount,
                        isSelected = selectedFolder == "Favorites",
                        isDarkMode = isDarkMode,
                        iconRes = R.drawable.ic_svg_favorite,
                        onClick = { selectedFolder = "Favorites" }
                    )

                    FilterChipItem(
                        title = "APIs Keys",
                        count = apiCount,
                        isSelected = selectedFolder == "APIs Keys",
                        isDarkMode = isDarkMode,
                        iconRes = R.drawable.ic_svg_api,
                        onClick = { openFolder("APIs Keys") }
                    )

                    FilterChipItem(
                        title = "Code",
                        count = codeCount,
                        isSelected = selectedFolder == "Code",
                        isDarkMode = isDarkMode,
                        iconRes = R.drawable.ic_svg_code,
                        onClick = { openFolder("Code") }
                    )

                    FilterChipItem(
                        title = "Media",
                        count = mediaCount,
                        isSelected = selectedFolder == "Media",
                        isDarkMode = isDarkMode,
                        iconRes = R.drawable.ic_svg_media,
                        onClick = { openFolder("Media") }
                    )

                    FilterChipItem(
                        title = "Personal",
                        count = personalCount,
                        isSelected = selectedFolder == "Personal",
                        isDarkMode = isDarkMode,
                        iconRes = R.drawable.ic_svg_folder,
                        onClick = { openFolder("Personal") }
                    )

                    FilterChipItem(
                        title = "Hidden",
                        count = hiddenCount,
                        isSelected = selectedFolder == "Hidden",
                        isDarkMode = isDarkMode,
                        iconRes = R.drawable.ic_svg_lock,
                        onClick = { openFolder("Hidden") }
                    )
                }

                // SECTION HEADER & BULK SELECT (Screenshot 2 / 5: "ALL NOTES   3 notes | Bulk Select")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedFolder.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary,
                        letterSpacing = 1.sp
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${displayedNotes.size} notes",
                            fontSize = 11.sp,
                            color = if (isDarkMode) Color.White.copy(0.6f) else Color.Gray
                        )
                        Text(
                            text = " | ",
                            color = if (isDarkMode) Color.White.copy(0.3f) else Color.LightGray,
                            fontSize = 11.sp
                        )
                        Text(
                            text = if (isBulkSelectMode) "Done" else "Bulk Select",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier.clickable {
                                isBulkSelectMode = !isBulkSelectMode
                                if (!isBulkSelectMode) selectedNoteIds.clear()
                            }
                        )
                    }
                }

                // NOTES LIST (Screenshot 2 / 5 with glass cards and blur borders)
                if (displayedNotes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No notes in $selectedFolder",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color.White.copy(0.7f) else Color.DarkGray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap + to create your first note",
                                fontSize = 12.sp,
                                color = CrimsonPrimary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = rememberLazyListState(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(displayedNotes, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                isDarkMode = isDarkMode,
                                blurApis = blurApis,
                                isBulkMode = isBulkSelectMode,
                                isSelected = selectedNoteIds.contains(note.id),
                                onSelectToggle = {
                                    if (selectedNoteIds.contains(note.id)) {
                                        selectedNoteIds.remove(note.id)
                                    } else {
                                        selectedNoteIds.add(note.id)
                                    }
                                },
                                onClick = {
                                    if (isBulkSelectMode) {
                                        if (selectedNoteIds.contains(note.id)) {
                                            selectedNoteIds.remove(note.id)
                                        } else {
                                            selectedNoteIds.add(note.id)
                                        }
                                    } else if (note.isLocked) {
                                        pendingLockedNote = note
                                    } else {
                                        onOpenNote(note)
                                    }
                                },
                                onCopy = {
                                    clipboard.setText(AnnotatedString(note.content))
                                    Toast.makeText(context, "Copied note to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                onToggleFavorite = {
                                    coroutineScope.launch {
                                        repository.toggleFavorite(note.id, note.isFavorite)
                                    }
                                },
                                onTogglePin = {
                                    coroutineScope.launch {
                                        repository.togglePin(note.id, note.isPinned)
                                        val msg = if (!note.isPinned) "Note pinned to top" else "Note unpinned"
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onLongPress = {
                                    isBulkSelectMode = true
                                    selectedNoteIds.add(note.id)
                                }
                            )
                        }
                    }
                }

                // BULK ACTION BAR (if bulk mode active - spec 9: export zip / bulk delete)
                AnimatedVisibility(visible = isBulkSelectMode) {
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        isDarkMode = isDarkMode
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedNoteIds.size} selected",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CrimsonPrimary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Export as ZIP / combined archive
                                IconButton(
                                    onClick = {
                                        if (selectedNoteIds.isEmpty()) {
                                            Toast.makeText(context, "No notes selected", Toast.LENGTH_SHORT).show()
                                            return@IconButton
                                        }
                                        val combined = displayedNotes
                                            .filter { selectedNoteIds.contains(it.id) }
                                            .joinToString("\n\n====================\n\n") { "Title: ${it.title}\nCategory: ${it.category}\n\n${it.content}" }

                                        val shareIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_SUBJECT, "AU_Notes_Archive.txt")
                                            putExtra(Intent.EXTRA_TEXT, combined)
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export selected notes"))
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Archive, contentDescription = "Archive", tint = Color(0xFF38BDF8))
                                }

                                // Hide / Unhide selected — moves notes into or out of the
                                // Hidden folder (which always requires the PIN to open).
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val movingToHidden = selectedFolder != "Hidden"
                                            selectedNoteIds.forEach { id ->
                                                val existing = repository.getNoteByIdOnce(id)
                                                if (existing != null) {
                                                    repository.saveNote(
                                                        existing.copy(
                                                            folder = if (movingToHidden) "Hidden" else "All Notes",
                                                            updatedAt = System.currentTimeMillis()
                                                        )
                                                    )
                                                }
                                            }
                                            selectedNoteIds.clear()
                                            isBulkSelectMode = false
                                            Toast.makeText(
                                                context,
                                                if (movingToHidden) "Moved to Hidden" else "Unhidden",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (selectedFolder == "Hidden") Icons.Default.VisibilityOff else Icons.Default.Lock,
                                        contentDescription = if (selectedFolder == "Hidden") "Unhide" else "Hide",
                                        tint = Color(0xFFFFC107)
                                    )
                                }

                                // Delete selected
                                IconButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            selectedNoteIds.forEach { id ->
                                                repository.moveToTrash(id)
                                            }
                                            selectedNoteIds.clear()
                                            isBulkSelectMode = false
                                            Toast.makeText(context, "Moved selected to Recycle Bin", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252))
                                }
                            }
                        }
                    }
                }
            }

            // FLOATING ACTION BUTTONS (Screenshot 2 / 5: Center/Right + button and AI Bot button)
            // navigationBarsPadding() keeps these above the device's system nav bar / gesture
            // area on any screen size, instead of overlapping it.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Spacer(modifier = Modifier.size(54.dp))

                    // Main (+) Glowing FAB button (Screenshot 2/5)
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(CrimsonPrimary, Color(0xFFC026D3))
                                )
                            )
                            .clickable(onClick = onCreateNote),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Note",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // AU AI Robot Floating Button (Screenshot 2/5 bottom right)
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF2563EB), Color(0xFF7C3AED))
                                )
                            )
                            .clickable(onClick = onOpenAiChat),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = "AU AI Smart Engine",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Single Note PIN unlock dialog
        pendingLockedNote?.let { note ->
            PinLockDialog(
                correctPin = lockPin,
                title = "Unlock Note",
                subtitle = "Enter 4-digit PIN to open \"${note.title}\"",
                securityQuestion = preferences.securityQuestion.value,
                securityAnswer = preferences.securityAnswer.value,
                isDarkMode = isDarkMode,
                onDismiss = { pendingLockedNote = null },
                onUnlocked = {
                    val target = pendingLockedNote
                    pendingLockedNote = null
                    if (target != null) {
                        onOpenNote(target)
                    }
                },
                onPinReset = { newPin ->
                    preferences.setLockPin(newPin)
                }
            )
        }

        // Locked-folder PIN unlock dialog — only shown for folders the user has
        // explicitly locked from Settings -> Security. Never shown otherwise.
        pendingFolderToOpen?.let { folderName ->
            PinLockDialog(
                correctPin = lockPin,
                title = "\"$folderName\" is Locked",
                subtitle = "Enter 4-digit PIN to access this folder",
                securityQuestion = preferences.securityQuestion.value,
                securityAnswer = preferences.securityAnswer.value,
                isDarkMode = isDarkMode,
                onDismiss = { pendingFolderToOpen = null },
                onUnlocked = {
                    unlockedFoldersThisSession = unlockedFoldersThisSession + folderName
                    selectedFolder = folderName
                    pendingFolderToOpen = null
                },
                onPinReset = { newPin ->
                    preferences.setLockPin(newPin)
                }
            )
        }
    }
}

@Composable
fun FilterChipItem(
    title: String,
    count: Int,
    isSelected: Boolean,
    isDarkMode: Boolean,
    iconRes: Int? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) Color(0xFF2563EB) else if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000)
            )
            .border(
                1.dp,
                if (isSelected) Color(0xFF3B82F6) else Color(0x33FF2D55),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconRes != null) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = iconRes),
                    contentDescription = null,
                    tint = if (isSelected) Color.White else if (isDarkMode) Color.White.copy(0.85f) else Color.DarkGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else if (isDarkMode) Color.White.copy(0.85f) else Color.DarkGray
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0x40FFFFFF) else Color(0x33FF2D55))
                    .padding(horizontal = 6.dp, vertical = 1.dp)
            ) {
                Text(
                    text = "$count",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else CrimsonPrimary
                )
            }
        }
    }
}

@Composable
fun NoteCardItem(
    note: NoteEntity,
    isDarkMode: Boolean,
    blurApis: Boolean,
    isBulkMode: Boolean,
    isSelected: Boolean,
    onSelectToggle: () -> Unit,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onLongPress: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        isDarkMode = isDarkMode,
        shape = RoundedCornerShape(18.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isBulkMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onSelectToggle() },
                            colors = CheckboxDefaults.colors(checkedColor = CrimsonPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Classification tag prefix (Screenshot 2/5: orange tag, <svg.html>, Untitled Note)
                    val tagColor = when (note.category) {
                        "API" -> CategoryApiColor
                        "Code" -> CategoryCodeColor
                        "Media" -> CategoryMediaColor
                        "Personal" -> CategoryPersonalColor
                        else -> CategoryGeneralColor
                    }

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(tagColor)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = note.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF111111),
                        maxLines = 1
                    )

                    if (note.isPinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = com.example.R.drawable.ic_pin),
                            contentDescription = "Pinned",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    if (note.isLocked) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                // Action icons (Pin, Copy Button, Favorite Star)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Pin toggle button
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(28.dp)) {
                        Icon(
                            painter = painterResource(id = com.example.R.drawable.ic_pin),
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            tint = if (note.isPinned) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Copy Box button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkMode) Color(0x33FFFFFF) else Color(0x14000000))
                            .border(1.dp, Color(0x33FF2D55), RoundedCornerShape(8.dp))
                            .clickable(onClick = onCopy)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Copy",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }

                    // Favorite Star
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = if (note.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = if (note.isFavorite) Color(0xFFFFD93D) else Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Content preview (blurred if API key and blur setting enabled - Screenshot 2/5)
            val previewText = when {
                note.category == "API" && blurApis -> AutoClassifier.maskApiKey(note.content)
                note.isLocked -> "•••••••••••••••••••••••• (Protected Note)"
                else -> note.content.take(90).replace("\n", " ")
            }

            Text(
                text = previewText,
                fontSize = 12.sp,
                color = if (isDarkMode) Color.White.copy(0.6f) else Color.DarkGray,
                fontFamily = if (note.category == "Code" || note.category == "API") FontFamily.Monospace else FontFamily.Default,
                maxLines = 2
            )
        }
    }
}
