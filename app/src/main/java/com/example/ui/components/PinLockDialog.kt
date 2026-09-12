package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.ui.theme.CrimsonPrimary

@Composable
fun PinLockDialog(
    correctPin: String,
    title: String = "Security Passcode",
    subtitle: String = "Enter 4-digit PIN or use Fingerprint to unlock",
    securityQuestion: String = "What was your first project?",
    securityAnswer: String = "AUNotes",
    isDarkMode: Boolean = true,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit,
    onPinReset: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showForgotDialog by remember { mutableStateOf(false) }
    var enteredAnswer by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            isDarkMode = isDarkMode,
            strong = true
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = CrimsonPrimary
                        )
                        Text(
                            text = title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
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

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.6f) else Color.DarkGray
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 4 PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) CrimsonPrimary else if (isDarkMode) Color(0x33FFFFFF) else Color(0x22000000)
                                )
                                .border(
                                    1.dp,
                                    if (isFilled) CrimsonPrimary else Color.Gray.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Numeric keypad
                val digits = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("FP", "0", "DEL")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    digits.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            row.forEach { digit ->
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isDarkMode) Color(0x26FFFFFF) else Color(0x14FF2D55)
                                        )
                                        .border(
                                            1.dp,
                                            if (isDarkMode) Color(0x33FF8181) else Color(0x33FF2D55),
                                            CircleShape
                                        )
                                        .clickable {
                                            when (digit) {
                                                "DEL" -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                        errorMessage = null
                                                    }
                                                }
                                                "FP" -> {
                                                    val activity = context as? androidx.fragment.app.FragmentActivity
                                                    if (activity == null) {
                                                        Toast.makeText(context, "Biometric unavailable here — use your PIN", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        val availability = com.example.ui.util.BiometricAuthHelper.checkAvailability(activity)
                                                        when (availability) {
                                                            com.example.ui.util.BiometricAuthHelper.Availability.AVAILABLE -> {
                                                                com.example.ui.util.BiometricAuthHelper.authenticate(activity) { success, error ->
                                                                    if (success) {
                                                                        onUnlocked()
                                                                    } else if (error != null) {
                                                                        errorMessage = error
                                                                    }
                                                                }
                                                            }
                                                            com.example.ui.util.BiometricAuthHelper.Availability.NOT_ENROLLED -> {
                                                                Toast.makeText(context, "No fingerprint/face set up on this device yet", Toast.LENGTH_SHORT).show()
                                                            }
                                                            else -> {
                                                                Toast.makeText(context, "This device has no biometric hardware — use your PIN", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                                else -> {
                                                    if (enteredPin.length < 4) {
                                                        val next = enteredPin + digit
                                                        enteredPin = next
                                                        if (next.length == 4) {
                                                            if (next == correctPin) {
                                                                onUnlocked()
                                                            } else {
                                                                errorMessage = "Incorrect PIN. Try again."
                                                                enteredPin = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (digit == "FP") {
                                        Icon(
                                            imageVector = Icons.Default.Fingerprint,
                                            contentDescription = "Fingerprint",
                                            tint = CrimsonPrimary,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    } else if (digit == "DEL") {
                                        Text(
                                            text = "⌫",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.White else Color.Black
                                        )
                                    } else {
                                        Text(
                                            text = digit,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isDarkMode) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Forgot PIN / Security Question link
                TextButton(onClick = { showForgotDialog = true }) {
                    Text(
                        text = "Forgot PIN? Reset with Security Question",
                        fontSize = 11.sp,
                        color = CrimsonPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }

    // Security Question Recovery Dialog
    if (showForgotDialog) {
        Dialog(onDismissRequest = { showForgotDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                isDarkMode = isDarkMode,
                strong = true
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Reset PIN via Security Question",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )

                    Text(
                        text = "Question: $securityQuestion",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )

                    OutlinedTextField(
                        value = enteredAnswer,
                        onValueChange = { enteredAnswer = it },
                        placeholder = { Text("Your Answer...", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPinInput = it },
                        placeholder = { Text("New 4-digit PIN (e.g. 1234)", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showForgotDialog = false }) {
                            Text("Cancel")
                        }

                        Button(
                            onClick = {
                                if (enteredAnswer.trim().equals(securityAnswer.trim(), ignoreCase = true)) {
                                    if (newPinInput.length == 4) {
                                        onPinReset?.invoke(newPinInput)
                                        Toast.makeText(context, "PIN successfully reset!", Toast.LENGTH_SHORT).show()
                                        showForgotDialog = false
                                        onUnlocked()
                                    } else {
                                        Toast.makeText(context, "Enter a 4-digit new PIN", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Incorrect answer. Try again.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Text("Verify & Unlock")
                        }
                    }
                }
            }
        }
    }
}
