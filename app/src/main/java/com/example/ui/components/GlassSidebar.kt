package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import com.example.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CrimsonPrimary

@Composable
fun GlassSidebar(
    isOpen: Boolean,
    isDarkMode: Boolean,
    onClose: () -> Unit,
    onNavigateHome: () -> Unit,
    onNavigateFolder: (String) -> Unit,
    onOpenStorageEditor: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRecycleBin: () -> Unit,
    onOpenFeaturesModal: () -> Unit
) {
    val context = LocalContext.current
    var showDeveloperModal by remember { mutableStateOf(false) }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(350)) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x80000000))
                .clickable(onClick = onClose)
        ) {
            // Main Glass Sidebar drawer
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .clip(RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp))
                    .background(
                        if (isDarkMode) {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xF018050D),
                                    Color(0xE6260814)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xF5FFFFFF),
                                    Color(0xF0FFF2F5)
                                )
                            )
                        }
                    )
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(
                                Color(0x66FF8181),
                                Color(0x22FFFFFF),
                                Color(0x44FF2D55)
                            )
                        ),
                        RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
                    )
                    .clickable(enabled = false) {}
                    .statusBarsPadding()
                    .padding(vertical = 20.dp, horizontal = 18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Top Bar with Window Dots and Close Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WindowDots(size = 11.dp, spacing = 6.dp)

                        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Sidebar",
                                tint = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color(0xFF333333)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // App Title Branding
                    Text(
                        text = "AU NOTES",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Your Premium Workspace",
                        fontSize = 12.sp,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.5f) else Color.Gray,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0x26FFFFFF) else Color(0x26FF2D55))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: APPLICATION WORKSPACE
                    Text(
                        text = "APPLICATION WORKSPACE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimsonPrimary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SidebarMenuItem(
                        icon = Icons.Default.Storage,
                        title = "Storage File Editor",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenStorageEditor()
                        }
                    )

                    SidebarMenuItem(
                        icon = Icons.Default.Workspaces,
                        title = "All Features of App",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenFeaturesModal()
                        }
                    )

                    SidebarMenuItem(
                        icon = Icons.Default.ContactMail,
                        title = "Developer Contact",
                        isDarkMode = isDarkMode,
                        onClick = { showDeveloperModal = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0x26FFFFFF) else Color(0x26FF2D55))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Section: FOLDERS & COLLECTIONS
                    Text(
                        text = "COLLECTIONS & LOGS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CrimsonPrimary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    SidebarMenuItem(
                        icon = Icons.Default.Home,
                        title = "All Notes",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onNavigateHome()
                        }
                    )

                    SidebarMenuItem(
                        icon = Icons.Default.Star,
                        title = "Favorites",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onNavigateFolder("Favorites")
                        }
                    )

                    SidebarMenuItem(
                        icon = Icons.Default.Key,
                        title = "APIs Keys",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onNavigateFolder("APIs Keys")
                        }
                    )

                    SidebarMenuItem(
                        icon = Icons.Default.Code,
                        title = "Code Snippets",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onNavigateFolder("Code")
                        }
                    )

                    SidebarMenuItem(
                        icon = Icons.Default.PermMedia,
                        title = "Media Links",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onNavigateFolder("Media")
                        }
                    )

                    SidebarMenuItem(
                        icon = Icons.Default.Person,
                        title = "Personal Notes",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onNavigateFolder("Personal")
                        }
                    )

                    SidebarMenuItem(
                        icon = Icons.Default.Delete,
                        title = "Recycle Bin",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenRecycleBin()
                        }
                    )

                    SidebarMenuItem(
                        icon = Icons.Default.Settings,
                        title = "Settings Dashboard",
                        isDarkMode = isDarkMode,
                        onClick = {
                            onClose()
                            onOpenSettings()
                        }
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = if (isDarkMode) Color(0x26FFFFFF) else Color(0x26FF2D55))
                    Spacer(modifier = Modifier.height(16.dp))

                    // User Profile Card at Bottom (from reference code: Hesala Hertz, Premium Account)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isDarkMode) Color(0x26FFFFFF) else Color(0x14FF2D55))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(CrimsonPrimary, Color(0xFFFF8E53)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.anxul_pfp),
                                contentDescription = "Developer profile picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ANXUL vfx",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkMode) Color.White else Color(0xFF111111)
                            )
                            Text(
                                text = "Premium Workspace",
                                fontSize = 11.sp,
                                color = CrimsonPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2CF95F))
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Version 1.2.8 • Premium Workspace",
                        fontSize = 10.sp,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.4f) else Color.Gray,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }
        }
    }

    // Developer Contact Modal
    if (showDeveloperModal) {
        DeveloperContactModal(
            isDarkMode = isDarkMode,
            onDismiss = { showDeveloperModal = false },
            onOpenLink = { url ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun SidebarMenuItem(
    icon: ImageVector,
    title: String,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = CrimsonPrimary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDarkMode) Color.White.copy(alpha = 0.88f) else Color(0xFF222222),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = if (isDarkMode) Color.White.copy(alpha = 0.3f) else Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun DeveloperContactModal(
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onOpenLink: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            isDarkMode = isDarkMode,
            strong = true
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Developer Contact",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = CrimsonPrimary
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = if (isDarkMode) Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Crafted by Anshul Yadav (@anxul_ydv)",
                    fontSize = 13.sp,
                    color = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color.DarkGray
                )

                Spacer(modifier = Modifier.height(16.dp))

                ContactButton(
                    platform = "Telegram",
                    handle = "anxul_ydv",
                    url = "https://t.me/anxul_ydv",
                    color = Color(0xFF2CA5E0),
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(10.dp))

                ContactButton(
                    platform = "Instagram",
                    handle = "anxul_ydv",
                    url = "https://instagram.com/anxul_ydv",
                    color = Color(0xFFE1306C),
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(10.dp))

                ContactButton(
                    platform = "GitHub",
                    handle = "anxul4ydv",
                    url = "https://github.com/anxul4ydv",
                    color = if (isDarkMode) Color(0xFFC9D1D9) else Color(0xFF24292E),
                    onOpenLink = onOpenLink
                )

                Spacer(modifier = Modifier.height(10.dp))

                ContactButton(
                    platform = "YouTube",
                    handle = "lrx.anshul",
                    url = "https://youtube.com/@lrx.anshul",
                    color = Color(0xFFFF0000),
                    onOpenLink = onOpenLink
                )
            }
        }
    }
}

@Composable
fun ContactButton(
    platform: String,
    handle: String,
    url: String,
    color: Color,
    onOpenLink: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .clickable { onOpenLink(url) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = platform,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = color
            )
            Text(
                text = handle,
                fontSize = 12.sp,
                color = color.copy(alpha = 0.9f)
            )
        }
        Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp)
        )
    }
}
