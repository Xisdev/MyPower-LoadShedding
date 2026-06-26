package com.xis.mypower.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xis.mypower.ui.theme.BackgroundDark
import com.xis.mypower.ui.theme.PrimaryGreen
import com.xis.mypower.ui.theme.TextGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen() {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panduan Setup Datastream", color = BackgroundDark) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Aplikasi ini menggunakan Virtual Pin (V0-V14). Buat Datastream di Blynk Web Console sesuai panduan nilai berikut:", color = TextGray, fontSize = 14.sp)
            
            DatastreamInfo("V0", "Multiplex Data", "String", "-", "-")
            DatastreamInfo("V1", "Tegangan Master", "Double", "0", "300")
            DatastreamInfo("V2", "Arus Master", "Double", "0", "30")
            DatastreamInfo("V3", "Daya P1", "Double", "0", "2000")
            DatastreamInfo("V4", "Daya P2", "Double", "0", "2000")
            DatastreamInfo("V5", "Daya P3", "Double", "0", "2000")
            DatastreamInfo("V6", "Total Daya", "Double", "0", "5000")
            DatastreamInfo("V7", "Relay P1", "Integer", "0", "1")
            DatastreamInfo("V8", "Relay P2", "Integer", "0", "1")
            DatastreamInfo("V9", "Relay P3", "Integer", "0", "1")
            DatastreamInfo("V10", "Mode Auto/Manual", "Integer", "0", "1")
            DatastreamInfo("V11", "Tegangan P2", "Double", "0", "300")
            DatastreamInfo("V12", "Arus P2", "Double", "0", "30")
            DatastreamInfo("V13", "Tegangan P3", "Double", "0", "300")
            DatastreamInfo("V14", "Arus P3", "Double", "0", "30")
        }
    }
}

@Composable
fun DatastreamInfo(pin: String, name: String, type: String, min: String, max: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = com.xis.mypower.ui.theme.CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Pin: $pin ($name)", color = PrimaryGreen, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tipe Data: $type", color = androidx.compose.ui.graphics.Color.White, fontSize = 12.sp)
            Text("Min: $min  |  Max: $max", color = TextGray, fontSize = 12.sp)
        }
    }
}
