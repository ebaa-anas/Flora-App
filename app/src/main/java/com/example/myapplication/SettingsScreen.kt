package com.example.myapplication

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    padding: PaddingValues,
    userName: String,
    userEmail: String,
    userAddress: String,                // NEW
    notificationsEnabled: Boolean,      // NEW
    onBack: () -> Unit,
    onLogout: () -> Unit,
    isDark: Boolean,
    onToggleDark: (Boolean) -> Unit,
    onEditProfile: () -> Unit,
    onEditAddress: () -> Unit,          // NEW
    onToggleNotifications: (Boolean) -> Unit // NEW
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1. DYNAMIC PROFILE HEADER
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (userName.isNotEmpty() && userName != "User") {
                    Text(text = userName.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                } else {
                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = userName.ifEmpty { "Plant Lover" }, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                Text(text = userEmail.ifEmpty { "Premium Member" }, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }

        // 2. ACCOUNT GROUP
        SettingSectionTitle("Account Settings")
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                SettingsItem(Icons.Rounded.Edit, "Edit Profile", "Name, email, and photo") { onEditProfile() }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(0.05f))

                // NEW: Shows actual saved address
                SettingsItem(Icons.Rounded.LocationOn, "Shipping Address", userAddress.ifEmpty { "Tap to add address" }) { onEditAddress() }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 3. PREFERENCES GROUP
        SettingSectionTitle("App Preferences")
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                // NEW: Real Notification Toggle
                SettingsToggleItem(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Notifications",
                    isChecked = notificationsEnabled,
                    onCheckedChange = onToggleNotifications
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(0.05f))

                SettingsToggleItem(
                    icon = if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                    title = "Dark Mode",
                    isChecked = isDark,
                    onCheckedChange = onToggleDark
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(40.dp))

        // 4. LOGOUT BUTTON
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE63946).copy(alpha = 0.1f), contentColor = Color(0xFFE63946)),
            shape = RoundedCornerShape(20.dp),
            elevation = null,
            border = BorderStroke(1.dp, Color(0xFFE63946).copy(alpha = 0.2f))
        ) {
            Icon(Icons.Rounded.Logout, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Text("Logout Session", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ... Keep your SettingSectionTitle, SettingsItem, and SettingsToggleItem functions exactly as they are below this point ...
@Composable
fun SettingSectionTitle(title: String) {
    Text(text = title.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.5.sp, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp))
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.background, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurface.copy(0.3f))
    }
}

@Composable
fun SettingsToggleItem(icon: ImageVector, title: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.background, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        Spacer(modifier = Modifier.width(16.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Switch(checked = isChecked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}