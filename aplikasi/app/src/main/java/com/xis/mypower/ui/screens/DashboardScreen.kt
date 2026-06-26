package com.xis.mypower.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xis.mypower.R
import com.xis.mypower.AppState
import com.xis.mypower.ui.components.AppHeader
import com.xis.mypower.ui.components.GaugeChart
import com.xis.mypower.ui.components.InfoCard
import com.xis.mypower.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(state: AppState, onOpenDrawer: () -> Unit) {
    Scaffold(
        topBar = {
            AppHeader(
                title = "Sistem Load Shedding",
                showLogo = true,
                onOpenDrawer = onOpenDrawer
            )
        },
        containerColor = BackgroundDark
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL POWER SISTEM", color = TextLightBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        Box(contentAlignment = Alignment.Center) {
                            GaugeChart(value = state.totalPower, max = 500f)
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 24.dp)) {
                                Text(text = String.format("%.1f", state.totalPower), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                                Text(text = "WATT", color = PrimaryGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                InfoCard(modifier = Modifier.weight(1f)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("STATUS SISTEM", color = TextLightBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        
                        if (!state.isOnline) {
                            Text("OFFLINE", color = AlertRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text("ESP Mati/Terputus", color = TextGray, fontSize = 10.sp)
                        } else {
                            val statusText = when {
                                state.totalPower > 400 -> "LOAD SHEDDING"
                                state.totalPower > 350 -> "LOAD SHEDDING"
                                else -> "NORMAL"
                            }
                            val statusColor = when {
                                state.totalPower > 400 -> AlertRed
                                state.totalPower > 350 -> WarningYellow
                                else -> PrimaryGreen
                            }
                            Text(statusText, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))
                            Text("(< 350W Normal)", color = TextGray, fontSize = 10.sp)
                        }
                    }
                }
            }

            PriorityCard(title = "PRIORITAS 1 (BEBAN UTAMA)", power = state.powerP1, voltage = state.voltage, current = state.current, relayOn = state.relayP1On)
            PriorityCard(title = "PRIORITAS 2", power = state.powerP2, voltage = 0f, current = 0f, relayOn = state.relayP2On)
            PriorityCard(title = "PRIORITAS 3", power = state.powerP3, voltage = 0f, current = 0f, relayOn = state.relayP3On)
        }
    }
}

@Composable
fun PriorityCard(title: String, power: Float, voltage: Float, current: Float, relayOn: Boolean) {
    InfoCard {
        Column {
            Box(modifier = Modifier.fillMaxWidth().background(GaugeTrackColor).padding(8.dp)) {
                Text(title, color = TextLightBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("DAYA", color = TextGray, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(contentAlignment = Alignment.Center) {
                        GaugeChart(value = power, max = 500f, thickness = 20f)
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top=16.dp)) {
                            Text(String.format("%.1f", power), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("W", color = TextGray, fontSize = 10.sp)
                        }
                    }
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.5f)) {
                    Text("TEGANGAN", color = TextGray, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(String.format("%.1f", voltage), color = TextLightBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("V", color = TextLightBlue, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.5f)) {
                    Text("ARUS", color = TextGray, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(String.format("%.2f", current), color = PrimaryGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("A", color = PrimaryGreen, fontSize = 12.sp)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(0.5f)) {
                    Text("STATUS", color = TextGray, fontSize = 10.sp)
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.size(24.dp).background(if (relayOn) PrimaryGreen else AlertRed, shape = androidx.compose.foundation.shape.CircleShape)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(if(relayOn) "ON" else "OFF", color = if (relayOn) PrimaryGreen else AlertRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
