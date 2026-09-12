package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonPrimary

@Composable
fun InteractiveTableView(
    tableData: String,
    isEditable: Boolean = true,
    isDarkMode: Boolean = true,
    onTableChange: (String) -> Unit = {},
    onDeleteTable: () -> Unit = {}
) {
    // Parse markdown table:
    // | Header 1 | Header 2 |
    // | --- | --- |
    // | A | B |
    val parsedLines = tableData.lines().filter { it.contains("|") && !it.contains("---") }

    val initialHeaders = remember(tableData) {
        val first = parsedLines.firstOrNull()
        if (first != null) {
            first.split("|").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
        } else {
            mutableListOf("Column 1", "Column 2")
        }
    }

    val initialRows = remember(tableData) {
        val remaining = parsedLines.drop(1)
        if (remaining.isNotEmpty()) {
            remaining.map { line ->
                line.split("|").map { it.trim() }.filter { it.isNotEmpty() }.toMutableList()
            }.toMutableList()
        } else {
            mutableListOf(
                mutableListOf("Data 1", "Data 2")
            )
        }
    }

    var headers by remember(tableData) { mutableStateOf(initialHeaders) }
    var rows by remember(tableData) { mutableStateOf(initialRows) }

    fun serializeTable(hList: List<String>, rList: List<List<String>>) {
        val sb = StringBuilder()
        sb.append("| ").append(hList.joinToString(" | ")).append(" |\n")
        sb.append("| ").append(hList.map { "---" }.joinToString(" | ")).append(" |\n")
        for (r in rList) {
            val paddedRow = (0 until hList.size).map { idx -> r.getOrElse(idx) { "" } }
            sb.append("| ").append(paddedRow.joinToString(" | ")).append(" |\n")
        }
        onTableChange(sb.toString().trimEnd())
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDarkMode) Color(0x331E0510) else Color(0x14000000))
            .border(1.dp, Color(0x44FF2D55), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        // Table Header Controls (in editable mode)
        if (isEditable) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 Interactive Table",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Add Row
                    FilledTonalButton(
                        onClick = {
                            val newRow = MutableList(headers.size) { "Cell" }
                            val updated = rows.map { it.toMutableList() }.toMutableList().apply { add(newRow) }
                            rows = updated
                            serializeTable(headers, updated)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+Row", fontSize = 10.sp)
                    }

                    // Del Row
                    if (rows.size > 1) {
                        FilledTonalButton(
                            onClick = {
                                val updated = rows.dropLast(1).map { it.toMutableList() }.toMutableList()
                                rows = updated
                                serializeTable(headers, updated)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("-Row", fontSize = 10.sp)
                        }
                    }

                    // Add Col
                    FilledTonalButton(
                        onClick = {
                            val newHeaders = headers.toMutableList().apply { add("Col ${headers.size + 1}") }
                            val updatedRows = rows.map { r -> r.toMutableList().apply { add("") } }.toMutableList()
                            headers = newHeaders
                            rows = updatedRows
                            serializeTable(newHeaders, updatedRows)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("+Col", fontSize = 10.sp)
                    }

                    // Delete table
                    IconButton(
                        onClick = onDeleteTable,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Table",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Table Grid
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            Column {
                // Header Row
                Row(
                    modifier = Modifier
                        .background(if (isDarkMode) Color(0x4DFF2D55) else Color(0x26FF2D55))
                        .border(0.5.dp, Color(0x44FF2D55))
                ) {
                    headers.forEachIndexed { colIdx, headerText ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .border(0.5.dp, Color(0x44FF2D55))
                                .padding(8.dp)
                        ) {
                            if (isEditable) {
                                BasicTextField(
                                    value = headerText,
                                    onValueChange = { newVal ->
                                        val newHeaders = headers.toMutableList()
                                        newHeaders[colIdx] = newVal
                                        headers = newHeaders
                                        serializeTable(newHeaders, rows)
                                    },
                                    textStyle = TextStyle(
                                        color = if (isDarkMode) Color.White else Color.Black,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    cursorBrush = SolidColor(CrimsonPrimary)
                                )
                            } else {
                                Text(
                                    text = headerText,
                                    color = if (isDarkMode) Color.White else Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                // Data Rows
                rows.forEachIndexed { rowIdx, rowCells ->
                    Row(
                        modifier = Modifier
                            .background(
                                if (rowIdx % 2 == 0) Color.Transparent else if (isDarkMode) Color(0x14FFFFFF) else Color(0x0A000000)
                            )
                            .border(0.5.dp, Color(0x44FF2D55))
                    ) {
                        headers.forEachIndexed { colIdx, _ ->
                            val cellValue = rowCells.getOrElse(colIdx) { "" }
                            Box(
                                modifier = Modifier
                                    .width(120.dp)
                                    .border(0.5.dp, Color(0x44FF2D55))
                                    .padding(8.dp)
                            ) {
                                if (isEditable) {
                                    BasicTextField(
                                        value = cellValue,
                                        onValueChange = { newVal ->
                                            val updatedRows = rows.map { it.toMutableList() }.toMutableList()
                                            while (updatedRows[rowIdx].size <= colIdx) {
                                                updatedRows[rowIdx].add("")
                                            }
                                            updatedRows[rowIdx][colIdx] = newVal
                                            rows = updatedRows
                                            serializeTable(headers, updatedRows)
                                        },
                                        textStyle = TextStyle(
                                            color = if (isDarkMode) Color.White.copy(0.9f) else Color.DarkGray,
                                            fontSize = 12.sp
                                        ),
                                        cursorBrush = SolidColor(CrimsonPrimary)
                                    )
                                } else {
                                    Text(
                                        text = cellValue,
                                        color = if (isDarkMode) Color.White.copy(0.9f) else Color.DarkGray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
