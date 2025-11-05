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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bisu.chickcare.backend.data.TrendDataPoint

@Composable
fun HealthTrendLineGraph(
    title: String,
    dataPoints: List<TrendDataPoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            if (dataPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Text(
                        text = "No data available yet",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    val padding = 40f
                    val graphWidth = size.width - (padding * 2)
                    val graphHeight = size.height - (padding * 2)
                    val maxValue = 100.0f
                    val gridLines = 5
                    for (i in 0..gridLines) {
                        val y = padding + (graphHeight * i / gridLines)
                        drawLine(
                            color = Color.Gray.copy(alpha = 0.3f),
                            start = Offset(padding, y),
                            end = Offset(size.width - padding, y),
                            strokeWidth = 1f
                        )
                    }
                    
                    for (i in 0..gridLines) {
                        val value = 100 * (1 - i.toFloat() / gridLines)
                        val y = padding + (graphHeight * i / gridLines)
                        drawContext.canvas.nativeCanvas.apply {
                            val text = "${value.toInt()}%"
                            drawText(
                                text,
                                padding - 35f,
                                y + 5,
                                android.graphics.Paint().apply {
                                    color = android.graphics.Color.GRAY
                                    textSize = 11f
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                            )
                        }
                    }
                    
                    val xStep = graphWidth / (dataPoints.size - 1).coerceAtLeast(1)
                    // Show labels but skip some if there are too many to avoid overlap
                    val labelInterval = if (dataPoints.size > 15) {
                        (dataPoints.size / 10).coerceAtLeast(1) // Show ~10 labels max
                    } else {
                        1 // Show all labels if 15 or fewer
                    }
                    
                    dataPoints.forEachIndexed { index, _ ->
                        val x = padding + (xStep * index)
                        // Only draw label if it's at the interval or if it's the first/last point
                        if (index % labelInterval == 0 || index == 0 || index == dataPoints.size - 1) {
                            drawContext.canvas.nativeCanvas.apply {
                                val text = dataPoints[index].label
                                drawText(
                                    text,
                                    x - 20f,
                                    size.height - padding + 20f,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.GRAY
                                        textSize = 10f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                )
                            }
                        }
                    }
                    
                    val healthyPath = Path()
                    dataPoints.forEachIndexed { index, point ->
                        val x = padding + (xStep * index)
                        val y = padding + graphHeight - ((point.healthyAverage.toFloat() / maxValue) * graphHeight)
                        
                        if (index == 0) {
                            healthyPath.moveTo(x, y)
                        } else {
                            healthyPath.lineTo(x, y)
                        }
                        
                        if (point.healthyAverage > 0) {
                            drawCircle(
                                color = Color(0xFF4CAF50),
                                radius = 5f,
                                center = Offset(x, y)
                            )
                        }
                    }
                    
                    drawPath(
                        path = healthyPath,
                        color = Color(0xFF4CAF50),
                        style = Stroke(width = 3f)
                    )
                    
                    val unhealthyPath = Path()
                    dataPoints.forEachIndexed { index, point ->
                        val x = padding + (xStep * index)
                        val y = padding + graphHeight - ((point.unhealthyAverage.toFloat() / maxValue) * graphHeight)
                        
                        if (index == 0) {
                            unhealthyPath.moveTo(x, y)
                        } else {
                            unhealthyPath.lineTo(x, y)
                        }
                        
                        if (point.unhealthyAverage > 0) {
                            drawCircle(
                                color = Color(0xFFF44336),
                                radius = 5f,
                                center = Offset(x, y)
                            )
                        }
                    }
                    
                    drawPath(
                        path = unhealthyPath,
                        color = Color(0xFFF44336),
                        style = Stroke(width = 3f)
                    )
                }
                
                Row(modifier = Modifier.padding(top = 8.dp)) {
                    LegendItem("Healthy", Color(0xFF4CAF50))
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem("Unhealthy", Color(0xFFF44336))
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

