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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bisu.chickcare.backend.data.TrendDataPoint
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@Composable
fun HealthTrendLineGraph(
    title: String,
    dataPoints: List<TrendDataPoint>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(ThemeColorUtils.white(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = ThemeColorUtils.black(),
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
                        color = ThemeColorUtils.black(),
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
                        val gridLineColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) {
                            Color(0xFF353940)
                        } else {
                            ThemeColorUtils.lightGray(Color.Gray).copy(alpha = 0.3f)
                        }
                        drawLine(
                            color = gridLineColor,
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
                            val textColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) {
                                android.graphics.Color.WHITE
                            } else {
                                android.graphics.Color.GRAY
                            }
                            drawText(
                                text,
                                padding - 35f,
                                y + 5,
                                android.graphics.Paint().apply {
                                    color = textColor
                                    textSize = 11f
                                    textAlign = android.graphics.Paint.Align.RIGHT
                                }
                            )
                        }
                    }
                    
                    // Calculate X positions for all points (evenly spaced)
                    val xStep = if (dataPoints.size > 1) {
                        graphWidth / (dataPoints.size - 1)
                    } else {
                        0f
                    }
                    
                    val xPositions = FloatArray(dataPoints.size) { index ->
                        padding + (xStep * index)
                    }

                    // Show labels but skip some if there are too many to avoid overlap
                    val labelInterval = if (dataPoints.size > 4) {
                        (dataPoints.size / 2).coerceAtLeast(1) // Show ~2 labels max for 2 days
                    } else {
                        1 // Show all labels if 4 or fewer
                    }
                    
                    dataPoints.forEachIndexed { index, _ ->
                        val x = xPositions[index]
                        // Only draw label if it's at the interval or if it's the first/last point
                        if (index % labelInterval == 0 || index == 0 || index == dataPoints.size - 1) {
                            drawContext.canvas.nativeCanvas.apply {
                                val text = dataPoints[index].label
                                val textColor = if (com.bisu.chickcare.backend.viewmodels.ThemeViewModel.isDarkMode) {
                                    android.graphics.Color.WHITE
                                } else {
                                    android.graphics.Color.GRAY
                                }
                                drawText(
                                    text,
                                    x - 25f,
                                    size.height - padding + 20f,
                                    android.graphics.Paint().apply {
                                        color = textColor
                                        textSize = 9f
                                        textAlign = android.graphics.Paint.Align.CENTER
                                    }
                                )
                            }
                        }
                    }

                    // Collect all points (including 0% start) for both lines
                    val allHealthyOffsets = mutableListOf<Offset>()
                    val allUnhealthyOffsets = mutableListOf<Offset>()

                    dataPoints.forEachIndexed { index, point ->
                        val x = xPositions[index]
                        val healthyY = padding + graphHeight - ((point.healthyAverage.toFloat() / maxValue) * graphHeight)
                        val unhealthyY = padding + graphHeight - ((point.unhealthyAverage.toFloat() / maxValue) * graphHeight)

                        // Always add point (even if 0%) to maintain line continuity
                        allHealthyOffsets.add(Offset(x, healthyY))
                        allUnhealthyOffsets.add(Offset(x, unhealthyY))
                    }

                    // Draw healthy line (green) - connects all points including 0% start
                    allHealthyOffsets.forEachIndexed { index, offset ->
                        if (index < allHealthyOffsets.size - 1) {
                            drawLine(
                                start = offset,
                                end = allHealthyOffsets[index + 1],
                                color = Color(0xFF4CAF50),
                                strokeWidth = 2.5f
                            )
                        }
                        // Draw dot only if value > 0 (skip the starting 0% dot)
                        if (dataPoints[index].healthyAverage > 0) {
                            drawCircle(
                                color = Color(0xFF4CAF50),
                                radius = 6f,
                                center = offset
                            )
                            // Draw white inner circle for better visibility
                            drawCircle(
                                color = Color.White,
                                radius = 3f,
                                center = offset
                            )
                        }
                    }

                    // Draw unhealthy line (red) - connects all points including 0% start
                    allUnhealthyOffsets.forEachIndexed { index, offset ->
                        if (index < allUnhealthyOffsets.size - 1) {
                            drawLine(
                                start = offset,
                                end = allUnhealthyOffsets[index + 1],
                                color = Color(0xFFF44336),
                                strokeWidth = 2.5f
                            )
                        }
                        // Draw dot only if value > 0 (skip the starting 0% dot)
                        if (dataPoints[index].unhealthyAverage > 0) {
                            drawCircle(
                                color = Color(0xFFF44336),
                                radius = 6f,
                                center = offset
                            )
                            // Draw white inner circle for better visibility
                            drawCircle(
                                color = Color.White,
                                radius = 3f,
                                center = offset
                            )
                        }
                    }
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
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = ThemeColorUtils.black())
    }
}
