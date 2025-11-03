package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class ChartType {
    HEALTHY_ONLY,
    UNHEALTHY_ONLY
}

@Composable
fun HealthyRatePieChart(
    healthyRate: Double,
    modifier: Modifier = Modifier,
    chartType: ChartType = ChartType.HEALTHY_ONLY
) {
    val unhealthyRate = 100.0 - healthyRate
    val healthyColor = Color(0xFF4CAF50) // Green
    val unhealthyColor = Color(0xFFF44336) // Red
    
    // Determine which type to show
    val displayRate = when (chartType) {
        ChartType.HEALTHY_ONLY -> healthyRate
        ChartType.UNHEALTHY_ONLY -> unhealthyRate
    }
    val displayColor = when (chartType) {
        ChartType.HEALTHY_ONLY -> healthyColor
        ChartType.UNHEALTHY_ONLY -> unhealthyColor
    }
    val displayTitle = when (chartType) {
        ChartType.HEALTHY_ONLY -> "Healthy"
        ChartType.UNHEALTHY_ONLY -> "Unhealthy"
    }
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = displayTitle,
            style = MaterialTheme.typography.titleSmall,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = (size.minDimension / 2f) * 0.85f
                
                // Calculate angles
                val sweepAngle = (displayRate / 100.0 * 360.0).toFloat()
                val startAngle = -90f // Start from top
                
                // Draw background (white/grey for remaining portion)
                drawArc(
                    color = Color(0xFFE0E0E0), // Light grey background
                    startAngle = startAngle,
                    sweepAngle = 360f,
                    useCenter = true,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2)
                )
                
                // Draw the main portion (green for healthy, red for unhealthy)
                if (displayRate > 0) {
                    drawArc(
                        color = displayColor,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2)
                    )
                }
                
                // Draw center circle for donut effect
                drawCircle(
                    color = Color.White,
                    radius = radius * 0.5f,
                    center = center
                )
                
                // Draw percentage text in center
                drawContext.canvas.nativeCanvas.apply {
                    val text = "%.1f%%".format(displayRate)
                    val paint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isFakeBoldText = true
                    }
                    val textY = center.y + (paint.descent() + paint.ascent()) / 2
                    drawText(text, center.x, textY, paint)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Legend - only show the relevant type
        LegendItem(
            label = displayTitle,
            color = displayColor,
            value = "%.1f%%".format(displayRate)
        )
    }
}

@Composable
private fun LegendItem(label: String, color: Color, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

