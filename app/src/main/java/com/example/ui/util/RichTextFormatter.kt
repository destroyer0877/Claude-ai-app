package com.example.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

/**
 * Span-based rich text engine.
 *
 * IMPORTANT ARCHITECTURE NOTE:
 * Formatting is stored SEPARATELY from the raw note text as a list of [TextSpan]
 * (start/end character offsets + style type). The raw text the user types never
 * contains markup characters like "**", "<u>", "<font color=...>" etc, so:
 *   - Formatting can never "leak" as visible raw text in the editor.
 *   - Applying a style only ever affects the exact selected range (or newly typed
 *     characters), never the whole note.
 *
 * When the underlying text changes (typing, deleting, pasting), call
 * [adjustSpansForEdit] FIRST to shift/trim existing spans to match the new text,
 * then optionally call [addSpan] to tag any newly inserted range with the
 * "currently active" formatting (bold/italic/color/etc that the user toggled on
 * while the cursor had no selection).
 */
object RichTextFormatter {

    data class TextSpan(
        val start: Int,
        val end: Int,
        val type: String, // "bold" | "italic" | "underline" | "strikethrough" | "code" | "highlight" | "color" | "fontSize"
        val value: String = "" // hex color for "color"/"highlight", integer sp size for "fontSize"
    )

    // ---------------------------------------------------------------------
    // Serialization — stored in NoteEntity.styleSpansJson using org.json,
    // which ships as part of the Android platform (no extra dependency).
    // ---------------------------------------------------------------------

    fun serializeSpans(spans: List<TextSpan>): String {
        val arr = JSONArray()
        for (s in spans) {
            val obj = JSONObject()
            obj.put("s", s.start)
            obj.put("e", s.end)
            obj.put("t", s.type)
            obj.put("v", s.value)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeSpans(json: String): List<TextSpan> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                TextSpan(
                    start = obj.optInt("s", 0),
                    end = obj.optInt("e", 0),
                    type = obj.optString("t", ""),
                    value = obj.optString("v", "")
                )
            }.filter { it.type.isNotBlank() && it.end > it.start }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ---------------------------------------------------------------------
    // Rendering — turns (plain text + spans) into an AnnotatedString.
    // Used both by the live editor (via VisualTransformation) and by the
    // read-only note view.
    // ---------------------------------------------------------------------

    fun buildStyledText(
        text: String,
        spans: List<TextSpan>,
        defaultColor: Color,
        baseFontSizeSp: Int
    ): AnnotatedString {
        val len = text.length
        return buildAnnotatedString {
            append(text)
            for (span in spans) {
                val start = span.start.coerceIn(0, len)
                val end = span.end.coerceIn(0, len)
                if (end <= start) continue
                val style = when (span.type) {
                    "bold" -> SpanStyle(fontWeight = FontWeight.Bold)
                    "italic" -> SpanStyle(fontStyle = FontStyle.Italic)
                    "underline" -> SpanStyle(textDecoration = TextDecoration.Underline)
                    "strikethrough" -> SpanStyle(textDecoration = TextDecoration.LineThrough)
                    "code" -> SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x33FF2D55),
                        color = Color(0xFFFF8E9E)
                    )
                    "highlight" -> SpanStyle(
                        background = safeParseColor(span.value, Color(0x55FBBF24))
                    )
                    "color" -> SpanStyle(color = safeParseColor(span.value, defaultColor))
                    "fontSize" -> SpanStyle(fontSize = (span.value.toIntOrNull() ?: baseFontSizeSp).sp)
                    else -> SpanStyle()
                }
                addStyle(style, start, end)
            }
        }
    }

    private fun safeParseColor(hex: String, fallback: Color): Color {
        return try {
            Color(android.graphics.Color.parseColor(hex))
        } catch (e: Exception) {
            fallback
        }
    }

    // ---------------------------------------------------------------------
    // Span offset adjustment — keeps spans correctly attached to their text
    // whenever the raw text changes (typing, deleting, pasting, undo/redo).
    // ---------------------------------------------------------------------

    fun adjustSpansForEdit(spans: List<TextSpan>, oldText: String, newText: String): List<TextSpan> {
        if (oldText == newText) return spans
        val minLen = minOf(oldText.length, newText.length)

        var prefixLen = 0
        while (prefixLen < minLen && oldText[prefixLen] == newText[prefixLen]) prefixLen++

        var suffixLen = 0
        val maxSuffix = minLen - prefixLen
        while (suffixLen < maxSuffix &&
            oldText[oldText.length - 1 - suffixLen] == newText[newText.length - 1 - suffixLen]
        ) suffixLen++

        val changeStart = prefixLen
        val oldMiddleLen = oldText.length - prefixLen - suffixLen
        val newMiddleLen = newText.length - prefixLen - suffixLen
        val changeOldEnd = changeStart + oldMiddleLen
        val delta = newMiddleLen - oldMiddleLen
        val isPureInsertion = oldMiddleLen == 0 && newMiddleLen > 0

        val result = mutableListOf<TextSpan>()
        for (span in spans) {
            val origStart = span.start
            val origEnd = span.end
            var s: Int
            var e: Int

            if (isPureInsertion) {
                when {
                    origEnd <= changeStart -> {
                        s = origStart; e = origEnd
                    }
                    origStart >= changeStart -> {
                        s = origStart + delta; e = origEnd + delta
                    }
                    origStart < changeStart && changeStart < origEnd -> {
                        // insertion happened strictly inside this span -> it grows
                        s = origStart; e = origEnd + delta
                    }
                    else -> {
                        s = origStart; e = origEnd
                    }
                }
            } else {
                s = when {
                    origStart <= changeStart -> origStart
                    origStart >= changeOldEnd -> origStart + delta
                    else -> changeStart
                }
                e = when {
                    origEnd <= changeStart -> origEnd
                    origEnd >= changeOldEnd -> origEnd + delta
                    else -> changeStart
                }
            }

            if (e > s) {
                result.add(span.copy(start = s, end = e))
            }
        }
        return result
    }

    /** Tags a freshly-inserted text range [start,end) with a style, e.g. because the
     * user had "Bold" toggled on while typing with no selection. */
    fun addSpan(spans: List<TextSpan>, start: Int, end: Int, type: String, value: String = ""): List<TextSpan> {
        if (end <= start) return spans
        return spans + TextSpan(start, end, type, value)
    }

    // ---------------------------------------------------------------------
    // Query / mutate helpers used by the toolbar
    // ---------------------------------------------------------------------

    /** True only if [type] covers the ENTIRE range [start,end) with no gaps. */
    fun isPropertyActiveThroughout(spans: List<TextSpan>, type: String, start: Int, end: Int): Boolean {
        if (end <= start) return false
        var cursor = start
        val relevant = spans.filter { it.type == type && it.end > start && it.start < end }
            .sortedBy { it.start }
        for (span in relevant) {
            if (span.start > cursor) return false
            if (span.end > cursor) cursor = span.end
            if (cursor >= end) return true
        }
        return cursor >= end
    }

    /** Toggles a boolean-style property (bold/italic/underline/strikethrough/code)
     * over exactly [start,end). If the whole range already has it, it is removed;
     * otherwise it is applied to the whole range. Never touches text outside the range. */
    fun toggleBooleanProperty(spans: List<TextSpan>, type: String, start: Int, end: Int): List<TextSpan> {
        if (end <= start) return spans
        val currentlyActive = isPropertyActiveThroughout(spans, type, start, end)
        val result = mutableListOf<TextSpan>()
        for (span in spans) {
            if (span.type != type || span.end <= start || span.start >= end) {
                result.add(span)
                continue
            }
            if (span.start < start) result.add(span.copy(end = start))
            if (span.end > end) result.add(span.copy(start = end))
        }
        if (!currentlyActive) {
            result.add(TextSpan(start, end, type))
        }
        return result
    }

    /** Sets a value-style property (color/highlight/fontSize) over exactly [start,end),
     * replacing any overlapping spans of the same type in that range only. */
    fun setValueProperty(spans: List<TextSpan>, type: String, start: Int, end: Int, value: String): List<TextSpan> {
        if (end <= start) return spans
        val result = mutableListOf<TextSpan>()
        for (span in spans) {
            if (span.type != type || span.end <= start || span.start >= end) {
                result.add(span)
                continue
            }
            if (span.start < start) result.add(span.copy(end = start))
            if (span.end > end) result.add(span.copy(start = end))
        }
        result.add(TextSpan(start, end, type, value))
        return result
    }

    // ---------------------------------------------------------------------
    // Legacy fallback: some notes created before this fix may still contain
    // raw markup characters (**bold**, <u>...</u>, <font color="#..">) inside
    // their saved text. This is ONLY used to render those old notes nicely
    // instead of showing broken symbols; it is never used for new edits.
    // ---------------------------------------------------------------------

    fun parseFormattedText(text: String, defaultColor: Color): AnnotatedString {
        val cleanText = StringBuilder()
        data class StyleRange(val start: Int, val end: Int, val style: SpanStyle)
        val ranges = mutableListOf<StyleRange>()

        var i = 0
        while (i < text.length) {
            if (text.startsWith("**", i)) {
                val endIdx = text.indexOf("**", i + 2)
                if (endIdx != -1) {
                    val content = text.substring(i + 2, endIdx)
                    val start = cleanText.length
                    cleanText.append(content)
                    ranges.add(StyleRange(start, cleanText.length, SpanStyle(fontWeight = FontWeight.Bold)))
                    i = endIdx + 2
                    continue
                }
            }
            if (text[i] == '*' && (i + 1 < text.length && text[i + 1] != '*')) {
                val endIdx = text.indexOf('*', i + 1)
                if (endIdx != -1) {
                    val content = text.substring(i + 1, endIdx)
                    val start = cleanText.length
                    cleanText.append(content)
                    ranges.add(StyleRange(start, cleanText.length, SpanStyle(fontStyle = FontStyle.Italic)))
                    i = endIdx + 1
                    continue
                }
            }
            if (text.startsWith("~~", i)) {
                val endIdx = text.indexOf("~~", i + 2)
                if (endIdx != -1) {
                    val content = text.substring(i + 2, endIdx)
                    val start = cleanText.length
                    cleanText.append(content)
                    ranges.add(StyleRange(start, cleanText.length, SpanStyle(textDecoration = TextDecoration.LineThrough)))
                    i = endIdx + 2
                    continue
                }
            }
            if (text.startsWith("<u>", i, ignoreCase = true)) {
                val closeTag = "</u>"
                val endIdx = text.indexOf(closeTag, i + 3, ignoreCase = true)
                if (endIdx != -1) {
                    val content = text.substring(i + 3, endIdx)
                    val start = cleanText.length
                    cleanText.append(content)
                    ranges.add(StyleRange(start, cleanText.length, SpanStyle(textDecoration = TextDecoration.Underline)))
                    i = endIdx + closeTag.length
                    continue
                }
            }
            if (text[i] == '`') {
                val endIdx = text.indexOf('`', i + 1)
                if (endIdx != -1) {
                    val content = text.substring(i + 1, endIdx)
                    val start = cleanText.length
                    cleanText.append(content)
                    ranges.add(
                        StyleRange(
                            start,
                            cleanText.length,
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x33FF2D55),
                                color = Color(0xFFFF8E9E)
                            )
                        )
                    )
                    i = endIdx + 1
                    continue
                }
            }
            if (text.startsWith("<font color=\"", i, ignoreCase = true)) {
                val quoteEnd = text.indexOf('"', i + 13)
                if (quoteEnd != -1) {
                    val hexColor = text.substring(i + 13, quoteEnd)
                    val tagEnd = text.indexOf('>', quoteEnd)
                    val closeTag = "</font>"
                    val contentEnd = if (tagEnd != -1) text.indexOf(closeTag, tagEnd, ignoreCase = true) else -1
                    if (tagEnd != -1 && contentEnd != -1) {
                        val content = text.substring(tagEnd + 1, contentEnd)
                        val parsedColor = safeParseColor(hexColor, defaultColor)
                        val start = cleanText.length
                        cleanText.append(content)
                        ranges.add(StyleRange(start, cleanText.length, SpanStyle(color = parsedColor)))
                        i = contentEnd + closeTag.length
                        continue
                    }
                }
            }
            cleanText.append(text[i])
            i++
        }

        return buildAnnotatedString {
            append(cleanText.toString())
            for (range in ranges) {
                addStyle(range.style, range.start, range.end)
            }
        }
    }

    /** True if [text] still contains any of the legacy markup markers, used to decide
     * whether the read-view should fall back to [parseFormattedText] for old notes. */
    fun containsLegacyMarkup(text: String): Boolean {
        return text.contains("**") || text.contains("<u>", ignoreCase = true) ||
            text.contains("<font color", ignoreCase = true) || text.contains("~~")
    }

    // ---------------------------------------------------------------------
    // Real attachments — actual files copied into app-private storage and
    // linked to a note via NoteEntity.attachmentsJson. Never just a text
    // placeholder like "[Image attached]".
    // ---------------------------------------------------------------------

    data class AttachmentInfo(
        val uri: String,       // content:// or file:// path to the copied, app-owned file
        val fileName: String,  // original display name
        val mimeType: String,  // e.g. "image/jpeg", "application/pdf"
        val sizeBytes: Long = 0L
    )

    fun serializeAttachments(attachments: List<AttachmentInfo>): String {
        val arr = JSONArray()
        for (a in attachments) {
            val obj = JSONObject()
            obj.put("uri", a.uri)
            obj.put("name", a.fileName)
            obj.put("mime", a.mimeType)
            obj.put("size", a.sizeBytes)
            arr.put(obj)
        }
        return arr.toString()
    }

    fun deserializeAttachments(json: String): List<AttachmentInfo> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.optJSONObject(i) ?: return@mapNotNull null
                AttachmentInfo(
                    uri = obj.optString("uri", ""),
                    fileName = obj.optString("name", "file"),
                    mimeType = obj.optString("mime", "*/*"),
                    sizeBytes = obj.optLong("size", 0L)
                )
            }.filter { it.uri.isNotBlank() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
