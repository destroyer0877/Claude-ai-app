package com.example

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.data.db.AppDatabase
import com.example.data.model.NoteEntity
import com.example.data.preferences.AppPreferences
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassSidebar
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.MainWorkspaceScreen
import com.example.ui.screens.NoteEditorScreen
import com.example.ui.screens.PdfViewerScreen
import com.example.ui.screens.ReadNoteScreen
import com.example.ui.screens.RecycleBinScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StorageFileEditorScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

sealed class Screen {
    data object MainWorkspace : Screen()
    data class ReadNote(val note: NoteEntity) : Screen()
    data class EditNote(val note: NoteEntity?) : Screen()
    data object Settings : Screen()
    data object RecycleBin : Screen()
    data object StorageEditor : Screen()
    // returnTo remembers where the PDF was opened from (Storage Editor or a note),
    // so closing it goes back one real level instead of always to MainWorkspace.
    data class PdfViewer(val uri: android.net.Uri, val returnTo: Screen) : Screen()
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = NoteRepository(database.noteDao())
        val preferences = AppPreferences(applicationContext)

        setContent {
            val isDarkMode by preferences.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuNotesApp(
                        repository = repository,
                        preferences = preferences,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = {
                            preferences.setDarkMode(!isDarkMode)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AuNotesApp(
    repository: NoteRepository,
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.MainWorkspace) }
    // Where to return to when leaving Recycle Bin (it can be opened from Settings),
    // so back navigation goes one real level at a time instead of always jumping to
    // MainWorkspace.
    var screenBeforeRecycleBin by remember { mutableStateOf<Screen>(Screen.MainWorkspace) }
    var isSidebarOpen by remember { mutableStateOf(false) }
    var isAiChatOpen by remember { mutableStateOf(false) }

    // Intercept hardware / gesture back button
    BackHandler(enabled = isAiChatOpen || isSidebarOpen || currentScreen !is Screen.MainWorkspace) {
        val screenNow = currentScreen
        when {
            isAiChatOpen -> isAiChatOpen = false
            isSidebarOpen -> isSidebarOpen = false
            screenNow is Screen.RecycleBin -> currentScreen = screenBeforeRecycleBin
            screenNow is Screen.PdfViewer -> currentScreen = screenNow.returnTo
            currentScreen !is Screen.MainWorkspace -> currentScreen = Screen.MainWorkspace
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Screen Routing
        when (val screen = currentScreen) {
            is Screen.MainWorkspace -> {
                MainWorkspaceScreen(
                    repository = repository,
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = onToggleDarkMode,
                    onOpenSidebar = { isSidebarOpen = true },
                    onOpenSettings = { currentScreen = Screen.Settings },
                    onOpenAiChat = { isAiChatOpen = true },
                    onOpenNote = { note ->
                        currentScreen = Screen.ReadNote(note)
                    },
                    onCreateNote = {
                        currentScreen = Screen.EditNote(null)
                    }
                )
            }

            is Screen.ReadNote -> {
                ReadNoteScreen(
                    note = screen.note,
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace },
                    onEditNote = { note ->
                        currentScreen = Screen.EditNote(note)
                    },
                    onDeleteNote = { noteId ->
                        coroutineScope.launch {
                            repository.moveToTrash(noteId)
                            currentScreen = Screen.MainWorkspace
                        }
                    },
                    onToggleLock = { noteId, isUnlocked ->
                        coroutineScope.launch {
                            repository.toggleLock(noteId, !isUnlocked)
                        }
                    }
                )
            }

            is Screen.EditNote -> {
                NoteEditorScreen(
                    initialNote = screen.note,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace },
                    onSaveNote = { id, title, content, category, isBold, isItalic, isUnderline, isStrikethrough, isCodeFormat, fontSize, fontColorHex, alignment, listType, tableData, styleSpansJson, attachmentsJson, onSaved ->
                        coroutineScope.launch {
                            val finalTitle = title.ifBlank { "Untitled Note" }
                            // Preserve fields the editor UI doesn't own (favorite/pin/lock/trash/
                            // folder/createdAt) instead of silently resetting them on every save.
                            val existing = screen.note
                            val targetFolder = when (category) {
                                "API" -> "APIs Keys"
                                "Code" -> "Code"
                                "Media" -> "Media"
                                "Personal" -> "Personal"
                                else -> existing?.folder ?: "All Notes"
                            }
                            val entity = NoteEntity(
                                id = id,
                                title = finalTitle,
                                content = content,
                                category = category,
                                folder = targetFolder,
                                isFavorite = existing?.isFavorite ?: false,
                                isPinned = existing?.isPinned ?: false,
                                isLocked = existing?.isLocked ?: false,
                                isTrash = existing?.isTrash ?: false,
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                isBold = isBold,
                                isItalic = isItalic,
                                isUnderline = isUnderline,
                                isStrikethrough = isStrikethrough,
                                isCodeFormat = isCodeFormat,
                                fontSize = fontSize,
                                fontColorHex = fontColorHex,
                                alignment = alignment,
                                listType = listType,
                                tableData = tableData,
                                styleSpansJson = styleSpansJson,
                                attachmentsJson = attachmentsJson,
                                updatedAt = System.currentTimeMillis()
                            )
                            val savedId = repository.saveNote(entity)
                            onSaved(savedId)
                        }
                    }
                )
            }

            is Screen.Settings -> {
                SettingsScreen(
                    preferences = preferences,
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace },
                    onOpenRecycleBin = {
                        screenBeforeRecycleBin = Screen.Settings
                        currentScreen = Screen.RecycleBin
                    }
                )
            }

            is Screen.RecycleBin -> {
                RecycleBinScreen(
                    repository = repository,
                    isDarkMode = isDarkMode,
                    // Go back to wherever Recycle Bin was opened from (e.g. Settings),
                    // instead of always skipping straight to MainWorkspace.
                    onBack = { currentScreen = screenBeforeRecycleBin }
                )
            }

            is Screen.StorageEditor -> {
                StorageFileEditorScreen(
                    isDarkMode = isDarkMode,
                    onBack = { currentScreen = Screen.MainWorkspace },
                    onOpenPdf = { uri ->
                        currentScreen = Screen.PdfViewer(uri, returnTo = Screen.StorageEditor)
                    },
                    onOpenFileInEditor = { fileTitle, fileContent, category ->
                        val virtualNote = NoteEntity(
                            id = 0L,
                            title = fileTitle,
                            content = fileContent,
                            category = category,
                            isCodeFormat = category == "Code"
                        )
                        currentScreen = Screen.EditNote(virtualNote)
                    }
                )
            }

            is Screen.PdfViewer -> {
                PdfViewerScreen(
                    pdfUri = screen.uri,
                    isDarkMode = isDarkMode,
                    onClose = { currentScreen = screen.returnTo }
                )
            }
        }

        // Navigation Drawer / Sidebar
        GlassSidebar(
            isOpen = isSidebarOpen,
            isDarkMode = isDarkMode,
            onClose = { isSidebarOpen = false },
            onNavigateHome = {
                isSidebarOpen = false
                currentScreen = Screen.MainWorkspace
            },
            onNavigateFolder = { folder ->
                isSidebarOpen = false
                currentScreen = Screen.MainWorkspace
            },
            onOpenStorageEditor = {
                isSidebarOpen = false
                currentScreen = Screen.StorageEditor
            },
            onOpenSettings = {
                isSidebarOpen = false
                currentScreen = Screen.Settings
            },
            onOpenRecycleBin = {
                isSidebarOpen = false
                screenBeforeRecycleBin = Screen.MainWorkspace
                currentScreen = Screen.RecycleBin
            },
            onOpenFeaturesModal = {
                isSidebarOpen = false
                isAiChatOpen = true
            }
        )

        // AI Chat Engine Drawer / Screen
        AiChatScreen(
            isOpen = isAiChatOpen,
            isDarkMode = isDarkMode,
            preferences = preferences,
            repository = repository,
            currentNoteContent = (currentScreen as? Screen.ReadNote)?.note?.content
                ?: (currentScreen as? Screen.EditNote)?.note?.content,
            onClose = { isAiChatOpen = false },
            onCreateNoteFromAi = { title, content, category ->
                coroutineScope.launch {
                    val newEntity = NoteEntity(
                        id = 0L,
                        title = title,
                        content = content,
                        category = category,
                        isCodeFormat = category == "Code"
                    )
                    repository.saveNote(newEntity)
                }
            },
            onOpenNoteFromAi = { note ->
                isAiChatOpen = false
                currentScreen = Screen.ReadNote(note)
            }
        )
    }
}
