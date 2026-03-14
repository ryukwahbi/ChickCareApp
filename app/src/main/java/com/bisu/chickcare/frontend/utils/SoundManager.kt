package com.bisu.chickcare.frontend.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log

/**
 * Utility to manage simple sound effects.
 * Uses a fire-and-forget approach for UI feedback sounds.
 */
object SoundManager {
    
    fun playSound(context: Context, resId: Int) {
        try {
            val mediaPlayer = MediaPlayer.create(context, resId)
            mediaPlayer.setOnCompletionListener { 
                it.release() 
            }
            mediaPlayer.start()
        } catch (e: Exception) {
            Log.e("SoundManager", "Error playing sound: ${e.message}")
        }
    }
}
