package com.bisu.chickcare.frontend.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Sync status indicator that shows when operations are queued for sync
 */
@Composable
fun SyncStatusIndicator(
    isQueued: Boolean,
    queuedCount: Int = 0,
    isSyncing: Boolean = false,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isQueued || isSyncing,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSyncing) {
                        Color(0xFF1B5E20) // Dark green for syncing
                    } else {
                        Color(0xFF341919) // Dark red for queued
                    }
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSyncing) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = "Syncing",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Syncing data...",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Queued",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (queuedCount > 0) {
                            "$queuedCount item${if (queuedCount > 1) "s" else ""} queued for sync"
                        } else {
                            "Data will sync when you're back online"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

/**
 * Small sync status badge for showing in result screen
 */
@Composable
fun SyncStatusBadge(
    isQueued: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isQueued,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(
                    Color(0xFF341919).copy(alpha = 0.8f),
                    MaterialTheme.shapes.small
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudQueue,
                contentDescription = "Queued",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Queued for sync",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontSize = 12.sp
            )
        }
    }
}

