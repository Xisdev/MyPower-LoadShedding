package com.xis.mypower.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xis.mypower.AppState
import com.xis.mypower.ui.components.AppHeader
import com.xis.mypower.ui.components.InfoCard
import com.xis.mypower.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(state: AppState, onOpenDrawer: () -> Unit) {
    Scaffold(
        topBar = {
            AppHeader(
                title = "Histori Daya",
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
                    Text("GRAFIK TOTAL DAYA (30 Data Terakhir)", color = TextLightBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(24.dp))
                    
                    if (state.historyPower.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            Text("Menunggu data...", color = TextGray)
                        }
                    } else {
                        LineChart(data = state.historyPower, modifier = Modifier.fillMaxWidth().height(200.dp))
                    }
                }
            }

            InfoCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("INFORMASI", color = TextLightBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(16.dp))
                    Text("Grafik ini menampilkan jejak daya listrik (Total Power) dalam sesi aplikasi saat ini. Data diupdate setiap kali polling terjadi.", color = TextGray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun LineChart(data: List<Float>, modifier: Modifier = Modifier) {
    val maxDataValue = (data.maxOrNull() ?: 100f).coerceAtLeast(10f)
    val maxChartValue = maxDataValue * 1.2f // Add 20% headroom

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val stepX = width / (data.size.coerceAtLeast(2) - 1).toFloat()

        val path = Path()
        
        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value / maxChartValue) * height)
            
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = PrimaryGreen,
            style = Stroke(width = 4f)
        )

        // Draw dots at data points
        data.forEachIndexed { index, value ->
            val x = index * stepX
            val y = height - ((value / maxChartValue) * height)
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = Offset(x, y)
            )
        }
    }
}
