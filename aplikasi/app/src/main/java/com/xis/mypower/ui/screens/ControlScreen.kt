package com.xis.mypower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xis.mypower.AppState
import com.xis.mypower.ui.components.AppHeader
import com.xis.mypower.ui.components.InfoCard
import com.xis.mypower.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlScreen(state: AppState, onOpenDrawer: () -> Unit, onModeChange: (Boolean) -> Unit, onRelayChange: (String, Boolean) -> Unit) {
    Scaffold(
        topBar = {
            AppHeader(
                title = "Kontrol Manual",
                onOpenDrawer = onOpenDrawer
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("MODE SISTEM", color = TextLightBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(GaugeTrackColor),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ModeButton(
                            text = "AUTO",
                            subText = "(LOAD SHEDDING)",
                            isSelected = state.isAutoMode,
                            modifier = Modifier.weight(1f).clickable { onModeChange(true) }
                        )
                        ModeButton(
                            text = "MANUAL",
                            subText = "(KONTROL RELAY)",
                            isSelected = !state.isAutoMode,
                            modifier = Modifier.weight(1f).clickable { onModeChange(false) }
                        )
                    }
                }
            }

            InfoCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("KONTROL RELAY MANUAL", color = TextLightBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    
                    RelayControlRow("RELAY P1", "(BEBAN UTAMA)", state.relayP1On, !state.isAutoMode) { onRelayChange("v7", it) }
                    HorizontalDivider(color = GaugeTrackColor, modifier = Modifier.padding(vertical = 12.dp))
                    RelayControlRow("RELAY P2", "(PRIORITAS 2)", state.relayP2On, !state.isAutoMode) { onRelayChange("v8", it) }
                    HorizontalDivider(color = GaugeTrackColor, modifier = Modifier.padding(vertical = 12.dp))
                    RelayControlRow("RELAY P3", "(PRIORITAS 3)", state.relayP3On, !state.isAutoMode) { onRelayChange("v9", it) }
                }
            }
        }
    }
}

@Composable
fun ModeButton(text: String, subText: String, isSelected: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(if (isSelected) PrimaryGreen else Color.Transparent)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text, color = if(isSelected) BackgroundDark else Color.White, fontWeight = FontWeight.Bold)
        Text(subText, color = if(isSelected) BackgroundDark else TextGray, fontSize = 10.sp)
    }
}

@Composable
fun RelayControlRow(title: String, sub: String, isOn: Boolean, isEnabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, color = Color.White, fontWeight = FontWeight.Bold)
            Text(sub, color = TextGray, fontSize = 10.sp)
            Spacer(Modifier.height(4.dp))
            Text("Status Saat Ini: ${if(isOn) "ON" else "OFF"}", color = if(isOn) PrimaryGreen else AlertRed, fontSize = 12.sp)
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { onChange(true) },
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = if (isOn) PrimaryGreen else GaugeTrackColor)
            ) {
                Text("ON", color = if (isOn) BackgroundDark else Color.White)
            }
            Button(
                onClick = { onChange(false) },
                enabled = isEnabled,
                colors = ButtonDefaults.buttonColors(containerColor = if (!isOn) AlertRed else GaugeTrackColor)
            ) {
                Text("OFF", color = if (!isOn) Color.White else Color.White)
            }
        }
    }
}
