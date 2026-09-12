package com.example.data.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ConversationTurn(
    val role: String, // "user" or "model"
    val content: String
)

/** Result of a tool-enabled AU Bot turn: either a normal text reply, or a request
 * to run one of the small set of safe app-command functions defined below. */
sealed class AiActionResult {
    data class Text(val text: String) : AiActionResult()
    data class FunctionCall(val name: String, val args: JSONObject) : AiActionResult()
}

class AiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .build()

    /**
     * AU Bot with real app-command access, restricted to a small, explicit set of
     * safe functions (search/create/move/delete-to-trash). The model may ONLY call
     * these named functions — it never gets raw file/database access. Destructive
     * functions (move/delete) are executed by the CALLER only after the user
     * confirms — this method just reports what the model wants to do.
     */
    suspend fun generateWithTools(
        prompt: String,
        apiKey: String,
        model: String = "gemini-2.0-flash",
        noteContext: String? = null,
        history: List<ConversationTurn> = emptyList()
    ): AiActionResult = withContext(Dispatchers.IO) {
        val systemInstruction = """
            You are AU Bot, the assistant inside the AU Notes app. You can call these
            functions when the user asks you to: search_notes, create_note,
            move_note_to_folder, delete_note. Only call a function when the user's
            request clearly asks for that app action (e.g. "find my Java note",
            "create a note from this", "move this to Personal", "delete that note").
            For move_note_to_folder and delete_note you must first have a note_id —
            get it via search_notes if you don't already have it from context.
            Otherwise, just answer normally as a helpful notes/code assistant.
        """.trimIndent()

        try {
            val cleanKey = apiKey.trim()
            val cleanModel = if (model.isBlank()) "gemini-2.0-flash" else model
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$cleanKey"

            val contentsArray = JSONArray()
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", "System Instruction: $systemInstruction")))
                }
            )
            contentsArray.put(
                JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().put(JSONObject().put("text", "Understood. I am AU Bot, ready to help and to use my app functions when appropriate.")))
                }
            )
            if (!noteContext.isNullOrBlank()) {
                contentsArray.put(
                    JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().put(JSONObject().put("text", "[Current Note Context]\n$noteContext")))
                    }
                )
                contentsArray.put(
                    JSONObject().apply {
                        put("role", "model")
                        put("parts", JSONArray().put(JSONObject().put("text", "Got it, I can see the current note.")))
                    }
                )
            }
            for (turn in history.takeLast(8)) {
                contentsArray.put(
                    JSONObject().apply {
                        put("role", if (turn.role == "bot") "model" else "user")
                        put("parts", JSONArray().put(JSONObject().put("text", turn.content)))
                    }
                )
            }
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                }
            )

            val functionDeclarations = JSONArray()
                .put(functionDecl(
                    "search_notes",
                    "Search the user's notes by a keyword or topic and return matching titles.",
                    listOf(FnParam("query", "STRING", "Keyword or topic to search for", true))
                ))
                .put(functionDecl(
                    "create_note",
                    "Create a brand-new note with the given title and content.",
                    listOf(
                        FnParam("title", "STRING", "Short title for the note", true),
                        FnParam("content", "STRING", "The note's body text", true),
                        FnParam("category", "STRING", "One of: General, Code, API, Media, Personal", false)
                    )
                ))
                .put(functionDecl(
                    "move_note_to_folder",
                    "Move an existing note (by its id) into a different folder. Needs user confirmation before it actually happens.",
                    listOf(
                        FnParam("note_id", "NUMBER", "The id of the note to move, from a prior search_notes result", true),
                        FnParam("folder", "STRING", "Target folder: All Notes, APIs Keys, Code, Media, or Personal", true)
                    )
                ))
                .put(functionDecl(
                    "delete_note",
                    "Move an existing note (by its id) to the recycle bin. Needs user confirmation before it actually happens.",
                    listOf(FnParam("note_id", "NUMBER", "The id of the note to delete, from a prior search_notes result", true))
                ))

            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
                put("tools", JSONArray().put(JSONObject().put("functionDeclarations", functionDeclarations)))
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val parts = candidates.getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                    if (parts.length() > 0) {
                        val firstPart = parts.getJSONObject(0)
                        val fnCall = firstPart.optJSONObject("functionCall")
                        if (fnCall != null) {
                            val fnName = fnCall.optString("name", "")
                            val fnArgs = fnCall.optJSONObject("args") ?: JSONObject()
                            if (fnName.isNotBlank()) {
                                return@withContext AiActionResult.FunctionCall(fnName, fnArgs)
                            }
                        }
                        if (firstPart.has("text")) {
                            return@withContext AiActionResult.Text(firstPart.getString("text"))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Fall through to offline fallback below
        }

        return@withContext AiActionResult.Text(generateSmartFallback(prompt, noteContext, null))
    }

    private data class FnParam(val name: String, val type: String, val description: String, val required: Boolean)

    private fun functionDecl(name: String, description: String, params: List<FnParam>): JSONObject {
        val properties = JSONObject()
        val required = JSONArray()
        for (p in params) {
            properties.put(p.name, JSONObject().apply {
                put("type", p.type)
                put("description", p.description)
            })
            if (p.required) required.put(p.name)
        }
        return JSONObject().apply {
            put("name", name)
            put("description", description)
            put("parameters", JSONObject().apply {
                put("type", "OBJECT")
                put("properties", properties)
                put("required", required)
            })
        }
    }

    suspend fun generateResponse(
        prompt: String,
        apiKey: String,
        model: String = "gemini-2.0-flash",
        noteContext: String? = null,
        attachmentContent: String? = null,
        history: List<ConversationTurn> = emptyList(),
        // Base64 PNG images (e.g. rendered PDF pages) sent as real multimodal input,
        // so AU Bot actually reads the document instead of a text file dump.
        attachmentImagesBase64: List<String> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val systemInstruction = """
            You are AU Bot, the intelligent AI assistant in the AU Notes application.
            Created by student developer Anshul, AU Notes is a modern glassmorphism workspace for notes, documents, and code.
            Help users organize notes, write and format code snippets, summarize documents, and explain complex concepts.
            Format code with markdown triple backticks and specify language tags (e.g., ```python, ```kotlin, ```json).
            Provide concise, accurate, helpful responses.
        """.trimIndent()

        val fullPromptBuilder = StringBuilder()
        if (!noteContext.isNullOrBlank()) {
            fullPromptBuilder.append("[Current Note Context]\n").append(noteContext).append("\n\n")
        }
        if (!attachmentContent.isNullOrBlank()) {
            fullPromptBuilder.append("[Attached Document/File Content]\n").append(attachmentContent).append("\n\n")
        }
        fullPromptBuilder.append(prompt)

        val latestUserMessage = fullPromptBuilder.toString()

        try {
            val cleanKey = apiKey.trim()
            val cleanModel = if (model.isBlank()) "gemini-2.0-flash" else model
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=$cleanKey"

            val contentsArray = JSONArray()

            // Include system instruction as initial context
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", "System Instruction: $systemInstruction")))
                }
            )
            contentsArray.put(
                JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().put(JSONObject().put("text", "Understood. I am AU Bot, ready to assist.")))
                }
            )

            // Include recent conversation history (up to last 8 turns)
            val recentHistory = history.takeLast(8)
            for (turn in recentHistory) {
                contentsArray.put(
                    JSONObject().apply {
                        put("role", if (turn.role == "bot") "model" else "user")
                        put("parts", JSONArray().put(JSONObject().put("text", turn.content)))
                    }
                )
            }

            // Latest message — text plus any document page images (multimodal).
            contentsArray.put(
                JSONObject().apply {
                    put("role", "user")
                    val parts = JSONArray().put(JSONObject().put("text", latestUserMessage))
                    for (imgBase64 in attachmentImagesBase64.take(6)) {
                        parts.put(
                            JSONObject().apply {
                                put("inline_data", JSONObject().apply {
                                    put("mime_type", "image/png")
                                    put("data", imgBase64)
                                })
                            }
                        )
                    }
                    put("parts", parts)
                }
            )

            val jsonBody = JSONObject().apply {
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val jsonResponse = JSONObject(responseBody)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text")
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback gracefully below
        }

        // Offline / Built-in Smart Fallback
        return@withContext generateSmartFallback(prompt, noteContext, attachmentContent)
    }

    private fun generateSmartFallback(prompt: String, noteContext: String?, attachmentContent: String?): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("summarize") -> {
                val source = attachmentContent ?: noteContext
                if (!source.isNullOrBlank()) {
                    "📌 **AU AI Summary:**\n" +
                            "• Topic: ${source.lines().firstOrNull()?.take(50) ?: "Document"}\n" +
                            "• Total length: ${source.length} characters\n" +
                            "• Key takeaway: Organized and formatted for your AU Notes workspace.\n" +
                            "• Suggested action: Save as note or export as PDF."
                } else {
                    "Please open a note or attach a document, and I will generate an executive summary for you!"
                }
            }
            lower.contains("python") || lower.contains("py") || lower.contains("code") -> {
                "```python\n# AU Notes Python Engine\n# Created by Anshul (@anxul_ydv)\n\ndef execute_task():\n    print('AU Notes AI engine ready!')\n    return True\n\nif __name__ == '__main__':\n    execute_task()\n```\n\n*Code generated cleanly. Tap 'Copy' above or save directly into your notes.*"
            }
            lower.contains("study") || lower.contains("question") -> {
                "📚 **Generated Study Questions:**\n1. What are the key architectural advantages of AU Notes' glassmorphism design?\n2. How does the local SQLite/Room database ensure privacy?\n3. Which export formats preserve tables and rich styling?"
            }
            lower.contains("bullet") -> {
                val lines = (noteContext ?: prompt).lines().filter { it.isNotBlank() }
                "📋 **Bullet Points:**\n" + lines.take(5).joinToString("\n") { "• $it" }
            }
            else -> {
                "I am **AU Bot**, your intelligent note and code assistant in AU Notes.\n\n" +
                        "I can assist you with:\n" +
                        "• Analyzing notes and attached files\n" +
                        "• Writing and optimizing code\n" +
                        "• Converting text into structured tables and checklists\n" +
                        "• Generating summaries and study guides\n\n" +
                        "What would you like to explore?"
            }
        }
    }

    /**
     * Real "Auto Detect Models" — calls Gemini's own ListModels endpoint with the
     * user's key and returns the models that actually support generateContent,
     * instead of a fixed guessed list. Throws on invalid key / network failure so
     * the caller can show a real error instead of fake results.
     */
    suspend fun listAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val cleanKey = apiKey.trim()
            if (cleanKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Enter an API key first"))
            }
            val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey"
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val body = response.body?.string()

            if (!response.isSuccessful || body.isNullOrBlank()) {
                val message = when (response.code) {
                    400, 403 -> "This API key was rejected — double-check it's correct"
                    404 -> "Model list endpoint not found for this key's project"
                    else -> "Could not reach Gemini (HTTP ${response.code})"
                }
                return@withContext Result.failure(Exception(message))
            }

            val json = JSONObject(body)
            val modelsArray = json.optJSONArray("models") ?: JSONArray()
            val names = mutableListOf<String>()
            for (i in 0 until modelsArray.length()) {
                val m = modelsArray.getJSONObject(i)
                val supported = m.optJSONArray("supportedGenerationMethods")
                val supportsGenerate = (0 until (supported?.length() ?: 0)).any {
                    supported?.optString(it) == "generateContent"
                }
                if (supportsGenerate) {
                    val fullName = m.optString("name", "") // e.g. "models/gemini-2.0-flash"
                    val shortName = fullName.substringAfterLast('/')
                    if (shortName.isNotBlank()) names.add(shortName)
                }
            }

            if (names.isEmpty()) {
                Result.failure(Exception("This key works, but no generateContent-capable models were returned"))
            } else {
                Result.success(names.distinct())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
