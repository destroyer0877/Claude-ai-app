package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NoteEntity
import com.example.data.repository.NoteRepository
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonPrimary
import kotlinx.coroutines.launch

@Composable
fun RecycleBinScreen(
    repository: NoteRepository,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val trashNotes by repository.trashNotes.collectAsState(initial = emptyList())
    var showEmptyTrashConfirm by remember { mutableStateOf(false) }
    // A single tap on delete-forever must not instantly destroy data — confirm first.
    var noteToPermanentlyDelete by remember { mutableStateOf<NoteEntity?>(null) }

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
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Recycle Bin",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color(0xFF111111)
                    )
                }

                if (trashNotes.isNotEmpty()) {
                    Button(
                        onClick = { showEmptyTrashConfirm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Empty Trash", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (trashNotes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = CrimsonPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Recycle Bin is Empty",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Text(
                        text = "Deleted notes will appear here and can be restored anytime.",
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color.White.copy(0.6f) else Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(trashNotes, key = { it.id }) { note ->
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            isDarkMode = isDarkMode
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = note.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDarkMode) Color.White else Color.Black
                                    )
                                    Text(
                                        text = "Deleted • ${note.category}",
                                        fontSize = 11.sp,
                                        color = CrimsonPrimary
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = {
                                        coroutineScope.launch {
                                            repository.restoreFromTrash(note.id)
                                            Toast.makeText(context, "Restored note", Toast.LENGTH_SHORT).show()
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Restore,
                                            contentDescription = "Restore",
                                            tint = Color(0xFF2CF95F)
                                        )
                                    }

                                    IconButton(onClick = { noteToPermanentlyDelete = note }) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Delete Forever",
                                            tint = Color(0xFFFF5252)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        noteToPermanentlyDelete?.let { note ->
            AlertDialog(
                onDismissRequest = { noteToPermanentlyDelete = null },
                title = { Text("Delete Forever?") },
                text = { Text("\"${note.title}\" will be permanently erased and cannot be recovered.") },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            repository.permanentDelete(note.id)
                            noteToPermanentlyDelete = null
                            Toast.makeText(context, "Permanently deleted", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Delete Forever", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { noteToPermanentlyDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showEmptyTrashConfirm) {
            AlertDialog(
                onDismissRequest = { showEmptyTrashConfirm = false },
                title = { Text("Empty Recycle Bin?") },
                text = { Text("All items in the recycle bin will be permanently erased.") },
                confirmButton = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            repository.emptyTrash()
                            showEmptyTrashConfirm = false
                            Toast.makeText(context, "Trash emptied", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Delete All", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyTrashConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
