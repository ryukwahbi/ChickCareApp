package com.bisu.chickcare.frontend.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bisu.chickcare.backend.viewmodels.ThemeViewModel
import com.bisu.chickcare.frontend.utils.ThemeColorUtils

@Composable
fun PlayAudioButton(
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onPlayClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isPlaying) ThemeColorUtils.darkGray(Color.DarkGray) else Color(0xFF7BC0F6)
        ),
        modifier = modifier
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isPlaying) "Stop Audio" else "Play Audio"
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            if (isPlaying) "Stop Audio" else "Play Audio",
            color = if (ThemeViewModel.isDarkMode) ThemeColorUtils.white() else Color.Unspecified
        )
    }
}
