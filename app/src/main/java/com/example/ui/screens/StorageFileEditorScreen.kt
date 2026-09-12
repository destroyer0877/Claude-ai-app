package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonPrimary
import java.io.File
import java.io.FileOutputStream
import java.util.Date

data class RealStorageItem(
    val name: String,
    val file: File? = null,
    val uri: Uri? = null,
    val sizeString: String,
    val path: String,
    val lastModified: Long,
    val isPdf: Boolean = false
)

@Composable
fun StorageFileEditorScreen(
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onOpenPdf: (Uri) -> Unit = {},
    onOpenFileInEditor: (title: String, content: String, category: String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFileItem by remember { mutableStateOf<RealStorageItem?>(null) }
    var fileContentEdit by remember { mutableStateOf("") }
    var isEditingInPlace by remember { mutableStateOf(false) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }

    var fileList by remember { mutableStateOf<List<RealStorageItem>>(emptyList()) }
    var showDetailsDialog by remember { mutableStateOf<RealStorageItem?>(null) }

    val documentsDir = remember {
        File(context.filesDir, "documents").apply {
            if (!exists()) mkdirs()
        }
    }

    fun refreshFileList() {
        val files = documentsDir.listFiles() ?: emptyArray()
        if (files.isEmpty()) {
            // Seed a starter script file
            val starterFile = File(documentsDir, "starter_workspace.py")
            starterFile.writeText(
                """# AU Notes Local Workspace
# Created by Anshul (@anxul_ydv)

def analyze_workspace():
    print("AU Notes Storage Engine loaded successfully.")
    return True

if __name__ == "__main__":
    analyze_workspace()
                """.trimIndent()
            )
        }

        val updatedFiles = (documentsDir.listFiles() ?: emptyArray()).map { f ->
            RealStorageItem(
                name = f.name,
                file = f,
                uri = Uri.fromFile(f),
                sizeString = "${(f.length() / 1024).coerceAtLeast(1)} KB",
                path = f.absolutePath,
                lastModified = f.lastModified(),
                isPdf = f.name.endsWith(".pdf", ignoreCase = true)
            )
        }.sortedByDescending { it.lastModified }
        fileList = updatedFiles
    }

    LaunchedEffect(Unit) {
        refreshFileList()
    }

    // SAF File Picker
    val safPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                // Get filename & size from content resolver
                var fileName = "imported_file"
                var fileSize = 0L
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIndex != -1) fileName = cursor.getString(nameIndex)
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                    }
                }

                val isPdf = fileName.endsWith(".pdf", ignoreCase = true)

                if (isPdf) {
                    onOpenPdf(uri)
                } else {
                    val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    val category = when {
                        fileName.endsWith(".py") || fileName.endsWith(".html") || fileName.endsWith(".js") -> "Code"
                        fileName.endsWith(".json") || fileName.endsWith(".xml") -> "API"
                        else -> "General"
                    }
                    onOpenFileInEditor(fileName, content, category)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val filteredList = remember(fileList, searchQuery) {
        if (searchQuery.isBlank()) fileList
        else fileList.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    GlassBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Storage & Files",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF111111)
                        )
                        Text(
                            text = "Browse, edit, and inspect files with SAF",
                            fontSize = 11.sp,
                            color = CrimsonPrimary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = CrimsonPrimary.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = CrimsonPrimary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New File", fontSize = 11.sp, color = CrimsonPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SAF Import Banner
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        safPickerLauncher.launch(
                            arrayOf(
                                "text/*",
                                "application/pdf",
                                "application/json",
                                "application/xml",
                                "*/*"
                            )
                        )
                    },
                isDarkMode = isDarkMode
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrimsonPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = CrimsonPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Open File via Storage Framework (SAF)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        Text(
                            text = "Tap to pick any .txt, .py, .html, .json, or .pdf from your device",
                            fontSize = 11.sp,
                            color = if (isDarkMode) Color.White.copy(0.6f) else Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search files...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CrimsonPrimary)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CrimsonPrimary,
                    unfocusedBorderColor = Color(0x33FF2D55),
                    focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                    unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // File items list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { item ->
                    val isSelected = selectedFileItem?.path == item.path
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        isDarkMode = isDarkMode,
                        borderWidth = if (isSelected) 2.dp else 1.dp,
                        onClick = {
                            selectedFileItem = item
                            if (!item.isPdf && item.file != null) {
                                fileContentEdit = try { item.file.readText() } catch (e: Exception) { "" }
                            }
                        }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = when {
                                            item.isPdf -> Icons.Default.PictureAsPdf
                                            item.name.endsWith(".py") || item.name.endsWith(".html") -> Icons.Default.Code
                                            else -> Icons.Default.Description
                                        },
                                        contentDescription = null,
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.name,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.White else Color.Black
                                        )
                                        Text(
                                            text = "${item.sizeString}  •  ${Date(item.lastModified)}",
                                            fontSize = 10.sp,
                                            color = if (isDarkMode) Color.White.copy(0.5f) else Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = { showDetailsDialog = item },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Details",
                                            tint = CrimsonPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (item.file != null) {
                                        IconButton(
                                            onClick = {
                                                item.file.delete()
                                                if (selectedFileItem?.path == item.path) {
                                                    selectedFileItem = null
                                                }
                                                refreshFileList()
                                                Toast.makeText(context, "Deleted ${item.name}", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete",
                                                tint = Color(0xFFFF5252),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // If selected: options to edit, save, or open
                            if (isSelected) {
                                Spacer(modifier = Modifier.height(10.dp))

                                if (item.isPdf) {
                                    Button(
                                        onClick = {
                                            item.uri?.let { onOpenPdf(it) }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Open in AU PDF Viewer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                } else {
                                    // In-place text editor
                                    OutlinedTextField(
                                        value = fileContentEdit,
                                        onValueChange = { fileContentEdit = it },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp),
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            color = if (isDarkMode) Color.White else Color.Black
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = CrimsonPrimary,
                                            unfocusedBorderColor = Color(0x33FF2D55)
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Save Changes Button
                                        Button(
                                            onClick = {
                                                if (item.file != null) {
                                                    item.file.writeText(fileContentEdit)
                                                    refreshFileList()
                                                    Toast.makeText(context, "Saved changes to ${item.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Save File", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // Open in Note Editor Button
                                        FilledTonalButton(
                                            onClick = {
                                                val category = when {
                                                    item.name.endsWith(".py") || item.name.endsWith(".html") || item.name.endsWith(".js") -> "Code"
                                                    item.name.endsWith(".json") || item.name.endsWith(".xml") -> "API"
                                                    else -> "General"
                                                }
                                                onOpenFileInEditor(item.name, fileContentEdit, category)
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Note Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

    // Create New File Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create New Storage File", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Enter filename with extension (e.g., test.py, notes.txt, doc.html):", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        placeholder = { Text("filename.txt") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = newFileName.trim()
                        if (cleanName.isNotBlank()) {
                            val targetFile = File(documentsDir, cleanName)
                            targetFile.writeText("# New AU file created ${Date()}\n")
                            refreshFileList()
                            showCreateDialog = false
                            newFileName = ""
                            Toast.makeText(context, "Created $cleanName", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // File Details Dialog
    showDetailsDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showDetailsDialog = null },
            title = { Text("File Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = CrimsonPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Name: ${item.name}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Size: ${item.sizeString}", fontSize = 12.sp)
                    Text("Last Modified: ${Date(item.lastModified)}", fontSize = 12.sp)
                    Text("Path: ${item.path}", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showDetailsDialog = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                ) {
                    Text("OK")
                }
            }
        )
    }
}
