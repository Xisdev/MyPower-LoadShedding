package com.xis.mypower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xis.mypower.ui.theme.BackgroundDark
import com.xis.mypower.ui.theme.PrimaryGreen

@Composable
fun SettingsDrawerContent(
    onNavigateToConfig: () -> Unit,
    onNavigateToTutorial: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onLogout: () -> Unit
) {
    ModalDrawerSheet(
        drawerContainerColor = BackgroundDark,
        modifier = Modifier.width(280.dp) // Lebar spesifik agar tidak memenuhi layar
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PrimaryGreen)
                    .padding(16.dp)
                    .padding(top = 16.dp) // Padding atas untuk status bar area
            ) {
                Text(
                    text = "Pengaturan",
                    color = BackgroundDark,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            SettingsItem("Konfigurasi Blynk", "Ubah ID, Name, dan Token", onClick = onNavigateToConfig)
            HorizontalDivider(color = androidx.compose.ui.graphics.Color.DarkGray)
            
            SettingsItem("Setup Datastream", "Panduan virtual pin Blynk", onClick = onNavigateToTutorial)
            HorizontalDivider(color = androidx.compose.ui.graphics.Color.DarkGray)
            
            SettingsItem("Tentang Aplikasi", "Versi dan pembuat aplikasi", onClick = onNavigateToAbout)
            HorizontalDivider(color = androidx.compose.ui.graphics.Color.DarkGray)
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Red),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp)
            ) {
                Text("LOGOUT / HAPUS AKUN", color = androidx.compose.ui.graphics.Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(title, color = androidx.compose.ui.graphics.Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(subtitle, color = androidx.compose.ui.graphics.Color.Gray, fontSize = 12.sp)
    }
}
