package com.example.musicplayerapp.service

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.musicplayerapp.model.MusicFile
import java.io.IOException

class MusicService : Service(), MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener {

    private val binder = MusicBinder()
    private lateinit var mediaPlayer: MediaPlayer
    private var musicFiles: List<MusicFile> = emptyList()
    private var currentIndex = 0
    private var isPlaying = false
    private var isLooping = false
    private var isShuffle = false

    // Interface for notifying the UI about playback changes
    interface PlaybackListener {
        fun onDurationChanged(duration: Int)
        fun onPositionChanged(position: Int)
    }

    private var playbackListener: PlaybackListener? = null

    fun setPlaybackListener(listener: PlaybackListener?) {
        playbackListener = listener
    }

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("MusicService", "MusicService created")
        mediaPlayer = MediaPlayer().apply {
            setOnPreparedListener(this@MusicService)
            setOnErrorListener(this@MusicService)
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        android.util.Log.d("MusicService", "onBind called")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("MusicService", "onStartCommand called")
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release()
    }

    fun setMusicFiles(musicFiles: List<MusicFile>) {
        this.musicFiles = musicFiles
    }

    fun playMusic(index: Int) {
        android.util.Log.d("MusicService", "playMusic called with index: $index, musicFiles size: ${musicFiles.size}")
        if (musicFiles.isEmpty() || index < 0 || index >= musicFiles.size) {
            android.util.Log.d("MusicService", "Cannot play music - invalid index or empty list")
            return
        }

        currentIndex = index
        try {
            // Check if mediaPlayer is initialized
            if (!::mediaPlayer.isInitialized) {
                android.util.Log.e("MusicService", "MediaPlayer is not initialized")
                return
            }

            mediaPlayer.reset()
            android.util.Log.d("MusicService", "Setting data source to: ${musicFiles[currentIndex].path}")
            mediaPlayer.setDataSource(musicFiles[currentIndex].path)
            mediaPlayer.prepareAsync()
        } catch (e: IOException) {
            Log.e("MusicService", "Error setting data source for file: ${musicFiles[currentIndex].path}", e)
        } catch (e: Exception) {
            Log.e("MusicService", "Unexpected error during playback", e)
        }
    }

    fun playNext() {
        if (musicFiles.isEmpty()) return

        if (isShuffle) {
            currentIndex = (0 until musicFiles.size).random()
        } else {
            currentIndex = if (currentIndex == musicFiles.size - 1) 0 else currentIndex + 1
        }
        
        playMusic(currentIndex)
    }

    fun playPrevious() {
        if (musicFiles.isEmpty()) return

        if (isShuffle) {
            currentIndex = (0 until musicFiles.size).random()
        } else {
            currentIndex = if (currentIndex == 0) musicFiles.size - 1 else currentIndex - 1
        }
        
        playMusic(currentIndex)
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
        } else {
            mediaPlayer.start()
            isPlaying = true
        }
    }

    fun pause() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.pause()
            isPlaying = false
        }
    }

    fun resume() {
        if (!mediaPlayer.isPlaying) {
            mediaPlayer.start()
            isPlaying = true
        }
    }

    fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
    }

    fun getCurrentPosition(): Int {
        return try {
            mediaPlayer.currentPosition
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error getting current position", e)
            0
        }
    }

    fun getDuration(): Int = if (::mediaPlayer.isInitialized) {
        try {
            mediaPlayer.duration
        } catch (e: Exception) {
            android.util.Log.e("MusicService", "Error getting duration", e)
            0
        }
    } else 0

    fun isPlaying(): Boolean = isPlaying

    fun setLooping(isLooping: Boolean) {
        this.isLooping = isLooping
        mediaPlayer.isLooping = isLooping
    }

    fun setShuffle(isShuffle: Boolean) {
        this.isShuffle = isShuffle
    }

    fun getCurrentMusic(): MusicFile? {
        return if (musicFiles.isNotEmpty() && currentIndex >= 0 && currentIndex < musicFiles.size) {
            musicFiles[currentIndex]
        } else null
    }

    fun getCurrentIndex(): Int = currentIndex

    override fun onPrepared(mediaPlayer: MediaPlayer?) {
        android.util.Log.d("MusicService", "onPrepared called, starting playback")
        mediaPlayer?.start()
        isPlaying = true

        // Notify that duration might have changed
        val duration = mediaPlayer?.duration ?: 0
        android.util.Log.d("MusicService", "Duration after prepare: $duration")
        playbackListener?.onDurationChanged(duration)
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        Log.e("MusicService", "MediaPlayer error: $what, $extra")
        return false
    }
}