package com.example.ui.screens

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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ai.AiService
import com.example.data.preferences.AppPreferences
import com.example.ui.components.GlassBackground
import com.example.ui.components.GlassCard
import com.example.ui.theme.CrimsonPrimary
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferences: AppPreferences,
    isDarkMode: Boolean,
    onBack: () -> Unit,
    onOpenRecycleBin: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val blurApis by preferences.blurApis.collectAsState()
    val syntaxHighlight by preferences.syntaxHighlight.collectAsState()
    val lockPin by preferences.lockPin.collectAsState()
    val securityQuestion by preferences.securityQuestion.collectAsState()
    val securityAnswer by preferences.securityAnswer.collectAsState()
    val useInbuiltApi by preferences.useInbuiltApi.collectAsState()
    val userApiKey by preferences.userApiKey.collectAsState()
    val selectedModel by preferences.selectedModel.collectAsState()

    var pinInput by remember(lockPin) { mutableStateOf(lockPin) }
    var pinConfirmInput by remember { mutableStateOf("") }
    var securityQuestionInput by remember(securityQuestion) { mutableStateOf(securityQuestion) }
    var securityAnswerInput by remember(securityAnswer) { mutableStateOf(securityAnswer) }
    var userKeyInput by remember(userApiKey) { mutableStateOf(userApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    val aiService = remember { AiService() }
    var detectedModels by remember { mutableStateOf<List<String>?>(null) }
    var isDetectingModels by remember { mutableStateOf(false) }
    var detectError by remember { mutableStateOf<String?>(null) }

    val models = detectedModels ?: listOf("gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-pro")

    GlassBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Top Bar: Back arrow + "AuPad Preferences Dashboard"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "AuPad Preferences Dashboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color(0xFF111111)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: ORGANIZED PREFERENCES (Screenshot 3/6)
            Text(
                text = "ORGANIZED PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Blur APIs toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Blur APIs on Note Lists",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                            Text(
                                text = "Hides/blurs long non-spaced credentials for privacy safety",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(0.6f) else Color.DarkGray
                            )
                        }

                        Switch(
                            checked = blurApis,
                            onCheckedChange = { preferences.setBlurApis(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CrimsonPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Code syntax highlight toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Code Syntax Highlight",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color.White else Color.Black
                            )
                            Text(
                                text = "Colorizes variables, tags and strings inside coding boxes",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color.White.copy(0.6f) else Color.DarkGray
                            )
                        }

                        Switch(
                            checked = syntaxHighlight,
                            onCheckedChange = { preferences.setSyntaxHighlight(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = CrimsonPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Recycle Bin button (prompt requirement 4)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDarkMode) Color(0x22FF2D55) else Color(0x14FF2D55))
                            .clickable(onClick = onOpenRecycleBin)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = CrimsonPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Recycle Bin / Trash",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CrimsonPrimary
                            )
                        }
                        Text(
                            text = "Manage →",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: SECURITY LOCKS & PROFILE (Screenshot 3/6)
            Text(
                text = "SECURITY LOCKS & PROFILE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "APIs Folder / Lock Passcode PIN",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Set a 4-digit PIN plus a security question, so you can recover access " +
                            "if you forget the PIN later (Settings -> \"Forgot PIN?\").",
                        fontSize = 11.sp,
                        color = if (isDarkMode) Color.White.copy(0.6f) else Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinInput = it
                            }
                        },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0x44FF2D55),
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Confirming the PIN here (instead of only when it's used later)
                    // catches typos immediately, before the user gets locked out of a
                    // folder by a PIN they didn't actually mean to set.
                    OutlinedTextField(
                        value = pinConfirmInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinConfirmInput = it
                            }
                        },
                        label = { Text("Confirm PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        isError = pinConfirmInput.isNotEmpty() && pinConfirmInput != pinInput,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0x44FF2D55),
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                        )
                    )
                    if (pinConfirmInput.isNotEmpty() && pinConfirmInput != pinInput) {
                        Text(
                            text = "PINs don't match yet",
                            fontSize = 11.sp,
                            color = Color(0xFFFF5252)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = securityQuestionInput,
                        onValueChange = { securityQuestionInput = it },
                        label = { Text("Security question") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0x44FF2D55),
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = securityAnswerInput,
                        onValueChange = { securityAnswerInput = it },
                        label = { Text("Your answer") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0x44FF2D55),
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            when {
                                pinInput.length != 4 -> Toast.makeText(context, "PIN must be 4 digits", Toast.LENGTH_SHORT).show()
                                pinInput != pinConfirmInput -> Toast.makeText(context, "PINs don't match — check both fields", Toast.LENGTH_SHORT).show()
                                securityQuestionInput.isBlank() -> Toast.makeText(context, "Add a security question", Toast.LENGTH_SHORT).show()
                                securityAnswerInput.isBlank() -> Toast.makeText(context, "Add an answer so you can recover your PIN later", Toast.LENGTH_SHORT).show()
                                else -> {
                                    preferences.setSecurityDetails(pinInput, securityQuestionInput, securityAnswerInput)
                                    pinConfirmInput = ""
                                    Toast.makeText(context, "Security settings saved", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Security Settings", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Locked Folders — the user picks which folders require the PIN above
            // to open. A folder is never locked automatically; it only prompts for
            // a PIN if it's toggled on here.
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Locked Folders",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Choose which folders ask for the PIN above before opening. Off by default.",
                        fontSize = 11.sp,
                        color = if (isDarkMode) Color.White.copy(0.6f) else Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val lockableFolders = listOf("APIs Keys", "Code", "Media", "Personal")
                    val lockedFolders by preferences.lockedFolders.collectAsState()

                    lockableFolders.forEach { folderName ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = folderName,
                                fontSize = 13.sp,
                                color = if (isDarkMode) Color.White.copy(0.85f) else Color(0xFF222222)
                            )
                            Switch(
                                checked = lockedFolders.contains(folderName),
                                onCheckedChange = { preferences.toggleFolderLock(folderName) },
                                colors = SwitchDefaults.colors(checkedThumbColor = CrimsonPrimary)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 3: AI ASSISTANT SETTINGS (Screenshot 3/6 & prompt requirement 4)
            Text(
                text = "AI ASSISTANT SETTINGS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                isDarkMode = isDarkMode
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "API Selection",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Radio 1: Use Inbuilt API
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { preferences.setUseInbuiltApi(true) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = useInbuiltApi,
                            onClick = { preferences.setUseInbuiltApi(true) },
                            colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Use Inbuilt API (AU Pre-configured)",
                            fontSize = 13.sp,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }

                    // Radio 2: Use Your Own API
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { preferences.setUseInbuiltApi(false) }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = !useInbuiltApi,
                            onClick = { preferences.setUseInbuiltApi(false) },
                            colors = RadioButtonDefaults.colors(selectedColor = CrimsonPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Use Your Own API",
                            fontSize = 13.sp,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val activeDisplayKey = if (useInbuiltApi) AppPreferences.DEFAULT_API_KEY else userKeyInput

                    // Gemini API Key Input with Eye and Copy Buttons (prompt spec 4)
                    OutlinedTextField(
                        value = if (useInbuiltApi) activeDisplayKey else userKeyInput,
                        onValueChange = {
                            if (!useInbuiltApi) {
                                userKeyInput = it
                                preferences.setUserApiKey(it)
                            }
                        },
                        readOnly = useInbuiltApi,
                        label = { Text("Gemini API Key") },
                        singleLine = true,
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Eye toggle button
                                IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                    Icon(
                                        imageVector = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (isApiKeyVisible) "Hide API Key" else "Show API Key",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                // Copy API button
                                IconButton(onClick = {
                                    clipboard.setText(AnnotatedString(activeDisplayKey))
                                    Toast.makeText(context, "API Key copied to clipboard!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy API Key",
                                        tint = CrimsonPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary,
                            unfocusedBorderColor = Color(0x44FF2D55),
                            focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Model Selector Chips (prompt spec 4: "if user paste his api so models should be shown")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active AI Model:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CrimsonPrimary
                        )

                        TextButton(
                            onClick = {
                                val keyToUse = if (useInbuiltApi) AppPreferences.DEFAULT_API_KEY else userKeyInput
                                isDetectingModels = true
                                detectError = null
                                coroutineScope.launch {
                                    val result = aiService.listAvailableModels(keyToUse)
                                    isDetectingModels = false
                                    result.onSuccess { list ->
                                        detectedModels = list
                                        Toast.makeText(context, "Found ${list.size} model(s)", Toast.LENGTH_SHORT).show()
                                    }.onFailure { error ->
                                        detectError = error.message ?: "Could not detect models"
                                    }
                                }
                            },
                            enabled = !isDetectingModels
                        ) {
                            Text(
                                if (isDetectingModels) "Detecting…" else "Auto Detect Models",
                                fontSize = 11.sp,
                                color = CrimsonPrimary
                            )
                        }
                    }

                    if (detectError != null) {
                        Text(
                            text = detectError ?: "",
                            fontSize = 11.sp,
                            color = Color(0xFFFF5252)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        models.forEach { model ->
                            val isSelected = model == selectedModel
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) CrimsonPrimary else if (isDarkMode) Color(0x22FFFFFF) else Color(0x14000000)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) CrimsonPrimary else Color(0x33FF2D55),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        preferences.setSelectedModel(model)
                                        Toast.makeText(context, "Selected model: $model", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = model.replace("gemini-", ""),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else if (isDarkMode) Color.White.copy(0.8f) else Color.DarkGray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
