package com.xis.mypower.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xis.mypower.ui.theme.BackgroundDark
import com.xis.mypower.ui.theme.PrimaryGreen
import com.xis.mypower.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    initialToken: String = "",
    initialId: String = "",
    initialName: String = "",
    onConnectSuccess: () -> Unit,
    onVerify: (String, String, String, (Boolean, String?) -> Unit) -> Unit
) {
    var templateId by remember { mutableStateOf(initialId) }
    var templateName by remember { mutableStateOf(initialName) }
    var authToken by remember { mutableStateOf(initialToken) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Konfigurasi Blynk", color = BackgroundDark) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryGreen)
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Masukkan detail Blynk Anda untuk menghubungkan aplikasi dengan hardware.", color = TextGray)
            
            OutlinedTextField(
                value = templateId,
                onValueChange = { templateId = it },
                label = { Text("Template ID (Misal: TMPL1234)", color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = TextGray,
                    focusedTextColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = templateName,
                onValueChange = { templateName = it },
                label = { Text("Template Name", color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = TextGray,
                    focusedTextColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            OutlinedTextField(
                value = authToken,
                onValueChange = { authToken = it },
                label = { Text("Auth Token", color = TextGray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = TextGray,
                    focusedTextColor = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            if (errorMessage != null) {
                Text(text = errorMessage!!, color = androidx.compose.ui.graphics.Color.Red, fontSize = 14.sp)
            }
            
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    onVerify(authToken, templateId, templateName) { success, msg ->
                        isLoading = false
                        if (success) {
                            onConnectSuccess()
                        } else {
                            errorMessage = msg ?: "Koneksi gagal"
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(24.dp))
                } else {
                    Text("CONNECT / SIMPAN", color = BackgroundDark, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
