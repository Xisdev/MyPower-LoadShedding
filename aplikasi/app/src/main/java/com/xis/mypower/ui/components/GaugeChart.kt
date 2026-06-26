package com.xis.mypower.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.xis.mypower.ui.theme.AlertRed
import com.xis.mypower.ui.theme.GaugeTrackColor
import com.xis.mypower.ui.theme.PrimaryGreen
import com.xis.mypower.ui.theme.WarningYellow

@Composable
fun GaugeChart(
    modifier: Modifier = Modifier,
    value: Float,
    max: Float = 500f,
    thickness: Float = 30f
) {
    val sweepAngle = 180f
    val progressAngle = (value / max).coerceIn(0f, 1f) * sweepAngle

    Box(modifier = modifier.aspectRatio(2f)) {
        Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(2f)) {
            val canvasWidth = size.width
            val canvasHeight = size.height * 2
            
            val arcSize = Size(canvasWidth, canvasHeight)
            val stroke = Stroke(width = thickness, cap = StrokeCap.Round)

            // Latar Belakang Track Gauge
            drawArc(
                color = GaugeTrackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                style = stroke,
                size = arcSize
            )

            // Gradasi Warna berdasarkan nilai
            val simpleGradient = Brush.horizontalGradient(
                colors = listOf(PrimaryGreen, WarningYellow, AlertRed),
                startX = 0f,
                endX = canvasWidth
            )

            // Nilai saat ini
            if (progressAngle > 0) {
                drawArc(
                    brush = simpleGradient,
                    startAngle = 180f,
                    sweepAngle = progressAngle,
                    useCenter = false,
                    style = stroke,
                    size = arcSize
                )
            }
        }
    }
}
