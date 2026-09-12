package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.model.NoteEntity
import com.example.ui.theme.CrimsonPrimary
import java.io.File
import java.io.FileOutputStream
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportDialog(
    note: NoteEntity,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedExtension by remember {
        mutableStateOf(
            when (note.category) {
                "Code" -> ".py"
                "API" -> ".txt"
                else -> ".pdf"
            }
        )
    }
    var fileNameWithoutExt by remember {
        mutableStateOf(
            note.title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "AU_Note_${note.id}" }
        )
    }

    val extensions = listOf(".pdf", ".docx", ".html", ".txt", ".py", ".xml", ".json")

    val isLossyFormat = selectedExtension in listOf(".txt", ".py", ".xml", ".json")
    val hasFormattingContent = note.tableData.isNotBlank() ||
        note.isBold || note.isItalic || (note.styleSpansJson.isNotBlank() && note.styleSpansJson != "[]")
    val attachments = remember(note.attachmentsJson) {
        com.example.ui.util.RichTextFormatter.deserializeAttachments(note.attachmentsJson)
    }
    val hasAttachments = attachments.isNotEmpty() || note.attachmentUri.isNotBlank()
    // No current export format actually embeds the real attachment files, so this
    // warning must show regardless of which format is picked, not just "lossy" ones.
    val showCompatibilityWarning = (isLossyFormat && hasFormattingContent) || hasAttachments

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            isDarkMode = isDarkMode,
            strong = true
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = CrimsonPrimary
                        )
                        Text(
                            text = "Export Note",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color(0xFF111111)
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Select Export Format:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    extensions.forEach { ext ->
                        val isSelected = selectedExtension == ext
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) CrimsonPrimary else if (isDarkMode) Color(0x33FFFFFF) else Color(0x14000000)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) CrimsonPrimary else Color(0x22FFFFFF),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedExtension = ext }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ext.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else if (isDarkMode) Color.White.copy(alpha = 0.8f) else Color.DarkGray
                            )
                        }
                    }
                }

                // Compatibility Warning Banner
                AnimatedVisibility(visible = showCompatibilityWarning) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x33FFAA00))
                            .border(1.dp, Color(0x88FFAA00), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Compatibility Warning",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFFFCC00)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = buildString {
                                if (hasAttachments) {
                                    append("This note's ${attachments.size.coerceAtLeast(1)} attachment(s) will NOT be included in the exported file — only their file names will be listed. ")
                                }
                                if (isLossyFormat && hasFormattingContent) {
                                    append("This format may also lose tables, colors, and other formatting.")
                                }
                            },
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = Color(0xFFFFE082)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "File Name (Editable):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CrimsonPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = fileNameWithoutExt,
                        onValueChange = { input ->
                            // Strip characters that break file creation (slashes, quotes, etc.)
                            fileNameWithoutExt = input.replace(Regex("[/\\\\:*?\"<>|]"), "")
                        },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0x44FF2D55)
                        )
                    )
                    Text(
                        text = selectedExtension,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val safeName = fileNameWithoutExt.trim().ifBlank { "AU_Note_${note.id}" }
                        val fullFileName = "$safeName$selectedExtension"
                        exportNoteFile(context, note, fullFileName, selectedExtension, attachments)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Export & Share File",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

private fun exportNoteFile(
    context: Context,
    note: NoteEntity,
    fileName: String,
    extension: String,
    attachments: List<com.example.ui.util.RichTextFormatter.AttachmentInfo>
) {
    val attachmentNamesText = if (attachments.isNotEmpty()) {
        "\n\n[Attachments not included in this export — file names only]\n" +
            attachments.joinToString("\n") { "- ${it.fileName}" }
    } else ""

    try {
        val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val targetFile = File(exportDir, fileName)

        when (extension) {
            ".pdf" -> {
                generatePdfFile(note, targetFile, attachments)
                shareFile(context, targetFile, "application/pdf")
            }
            ".docx" -> {
                // Generate Word-compatible HTML/MIME Document format
                val docContent = buildWordHtmlContent(note, attachments)
                targetFile.writeText(docContent, Charsets.UTF_8)
                shareFile(context, targetFile, "application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            }
            ".html" -> {
                val htmlContent = buildFullHtmlContent(note, attachments)
                targetFile.writeText(htmlContent, Charsets.UTF_8)
                shareFile(context, targetFile, "text/html")
            }
            ".xml" -> {
                val xmlContent = """
<?xml version="1.0" encoding="UTF-8"?>
<note>
    <title><![CDATA[${note.title}]]></title>
    <category>${note.category}</category>
    <createdAt>${Date(note.createdAt)}</createdAt>
    <content><![CDATA[${note.content}]]></content>
    ${if (note.tableData.isNotBlank()) "<table><![CDATA[${note.tableData}]]></table>" else ""}
    ${if (attachments.isNotEmpty()) "<attachments>${attachments.joinToString("") { "<file>${it.fileName}</file>" }}</attachments>" else ""}
</note>
                """.trimIndent()
                targetFile.writeText(xmlContent, Charsets.UTF_8)
                shareFile(context, targetFile, "application/xml")
            }
            ".py" -> {
                val pyContent = """
# ========================================================
# AU Notes Export: ${note.title}
# Category: ${note.category}
# ========================================================

${note.content}
$attachmentNamesText
                """.trimIndent()
                targetFile.writeText(pyContent, Charsets.UTF_8)
                shareFile(context, targetFile, "text/x-python")
            }
            ".json" -> {
                val attachmentsJsonArray = attachments.joinToString(",") { "\"${it.fileName.replace("\"", "\\\"")}\"" }
                val json = """
{
  "title": "${note.title.replace("\"", "\\\"")}",
  "category": "${note.category}",
  "createdAt": ${note.createdAt},
  "content": "${note.content.replace("\"", "\\\"").replace("\n", "\\n")}",
  "table": "${note.tableData.replace("\"", "\\\"").replace("\n", "\\n")}",
  "attachmentFileNames": [$attachmentsJsonArray]
}
                """.trimIndent()
                targetFile.writeText(json, Charsets.UTF_8)
                shareFile(context, targetFile, "application/json")
            }
            else -> {
                // .txt
                val txtContent = """
${note.title}
========================================
Category: ${note.category}
Date: ${Date(note.createdAt)}
========================================

${note.content}

${if (note.tableData.isNotBlank()) "\n[Table Data]\n${note.tableData}" else ""}
$attachmentNamesText
                """.trimIndent()
                targetFile.writeText(txtContent, Charsets.UTF_8)
                shareFile(context, targetFile, "text/plain")
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun generatePdfFile(note: NoteEntity, outputFile: File, attachments: List<com.example.ui.util.RichTextFormatter.AttachmentInfo>) {
    val document = PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas

    val titlePaint = Paint().apply {
        color = AndroidColor.rgb(255, 45, 85)
        textSize = 22f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        isAntiAlias = true
    }

    val metaPaint = Paint().apply {
        color = AndroidColor.GRAY
        textSize = 10f
        isAntiAlias = true
    }

    val bodyPaint = Paint().apply {
        color = AndroidColor.rgb(30, 30, 30)
        textSize = 12f
        isAntiAlias = true
    }

    var y = 60f
    canvas.drawText(note.title, 40f, y, titlePaint)
    y += 20f
    canvas.drawText("AU Notes  •  Category: ${note.category}  •  ${Date(note.createdAt)}", 40f, y, metaPaint)
    y += 30f

    val lineSeparator = Paint().apply {
        color = AndroidColor.rgb(220, 220, 220)
        strokeWidth = 1.5f
    }
    canvas.drawLine(40f, y, (pageWidth - 40).toFloat(), y, lineSeparator)
    y += 24f

    // Content lines
    val rawLines = note.content.lines()
    for (line in rawLines) {
        // Strip font tags for plain rendering
        val cleanLine = line.replace(Regex("<[^>]*>"), "")
        canvas.drawText(cleanLine, 40f, y, bodyPaint)
        y += 18f
        if (y > pageHeight - 60) break
    }

    // Table lines if present
    if (note.tableData.isNotBlank() && y < pageHeight - 100) {
        y += 16f
        canvas.drawText("[Table Data]", 40f, y, metaPaint)
        y += 18f
        for (tableLine in note.tableData.lines().take(15)) {
            canvas.drawText(tableLine, 40f, y, bodyPaint)
            y += 16f
            if (y > pageHeight - 50) break
        }
    }

    // Attachment file names (files themselves are not embedded in the PDF)
    if (attachments.isNotEmpty() && y < pageHeight - 60) {
        y += 20f
        canvas.drawText("Attachments (not included, names only):", 40f, y, metaPaint)
        y += 16f
        for (att in attachments.take(10)) {
            canvas.drawText("- ${att.fileName}", 40f, y, bodyPaint)
            y += 16f
            if (y > pageHeight - 40) break
        }
    }

    document.finishPage(page)
    FileOutputStream(outputFile).use { out ->
        document.writeTo(out)
    }
    document.close()
}

private fun buildFullHtmlContent(note: NoteEntity, attachments: List<com.example.ui.util.RichTextFormatter.AttachmentInfo>): String {
    val attachmentsHtml = if (attachments.isNotEmpty()) {
        "<div class=\"meta\">Attachments (not included, names only): " +
            attachments.joinToString(", ") { it.fileName } + "</div>"
    } else ""
    return """
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>${note.title}</title>
<style>
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; padding: 32px; background: #0F0307; color: #F1F1F1; max-width: 800px; margin: auto; }
h1 { color: #FF2D55; border-bottom: 2px solid #FF2D55; padding-bottom: 8px; margin-bottom: 6px; }
.meta { color: #888; font-size: 13px; margin-bottom: 24px; }
.content { white-space: pre-wrap; line-height: 1.7; font-size: 16px; }
table { border-collapse: collapse; width: 100%; margin: 24px 0; background: #1B0B13; border-radius: 8px; overflow: hidden; }
th, td { border: 1px solid #FF2D55; padding: 10px 14px; text-align: left; }
th { background: #FF2D55; color: #fff; }
</style>
</head>
<body>
<h1>${note.title}</h1>
<div class="meta">Category: ${note.category} | Created: ${Date(note.createdAt)} | AU Notes</div>
$attachmentsHtml
<div class="content">${note.content}</div>
${if (note.tableData.isNotBlank()) "<pre><code>${note.tableData}</code></pre>" else ""}
</body>
</html>
    """.trimIndent()
}

private fun buildWordHtmlContent(note: NoteEntity, attachments: List<com.example.ui.util.RichTextFormatter.AttachmentInfo>): String {
    val attachmentsHtml = if (attachments.isNotEmpty()) {
        "<p style='color: #666; font-size: 9pt;'>Attachments (not included, names only): " +
            attachments.joinToString(", ") { it.fileName } + "</p>"
    } else ""
    return """
<html xmlns:o='urn:schemas-microsoft-com:office:office' xmlns:w='urn:schemas-microsoft-com:office:word'>
<head><meta charset='utf-8'><title>${note.title}</title></head>
<body style='font-family: Arial, sans-serif; font-size: 12pt; line-height: 1.5;'>
<h1 style='color: #FF2D55;'>${note.title}</h1>
<p style='color: #666; font-size: 10pt;'>Category: ${note.category} | AU Notes</p>
$attachmentsHtml
<hr/>
<div style='white-space: pre-wrap;'>${note.content}</div>
${if (note.tableData.isNotBlank()) "<pre>${note.tableData}</pre>" else ""}
</body>
</html>
    """.trimIndent()
}

private fun shareFile(context: Context, file: File, mimeType: String) {
    val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, file.name)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    val chooser = Intent.createChooser(sendIntent, "Share ${file.name}")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
