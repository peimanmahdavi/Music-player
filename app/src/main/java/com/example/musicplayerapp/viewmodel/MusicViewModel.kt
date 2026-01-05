package com.example.musicplayerapp.viewmodel

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicplayerapp.model.MusicFile
import com.example.musicplayerapp.service.MusicService
import com.example.musicplayerapp.utils.MusicScanner
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel(), MusicService.PlaybackListener {
    private var musicService: MusicService? = null
    private var isBound = false
    private lateinit var serviceConnection: ServiceConnection

    private val _musicFiles = mutableStateOf<List<MusicFile>>(emptyList())
    val musicFiles: androidx.compose.runtime.State<List<MusicFile>> = _musicFiles

    private val _isLoading = mutableStateOf(true)
    val isLoading: androidx.compose.runtime.State<Boolean> = _isLoading

    private val _currentMusic = mutableStateOf<MusicFile?>(null)
    val currentMusic: androidx.compose.runtime.State<MusicFile?> = _currentMusic

    private val _isPlaying = mutableStateOf(false)
    val isPlaying: androidx.compose.runtime.State<Boolean> = _isPlaying

    private val _currentPosition = mutableStateOf(0)
    val currentPosition: androidx.compose.runtime.State<Int> = _currentPosition

    private val _duration = mutableStateOf(0)
    val duration: androidx.compose.runtime.State<Int> = _duration

    private val _isLooping = mutableStateOf(false)
    val isLooping: androidx.compose.runtime.State<Boolean> = _isLooping

    private val _isShuffle = mutableStateOf(false)
    val isShuffle: androidx.compose.runtime.State<Boolean> = _isShuffle

    fun bindService(context: Context) {
        android.util.Log.d("MusicViewModel", "Attempting to bind service")
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                android.util.Log.d("MusicViewModel", "Service connected: $name")
                val binder = service as MusicService.MusicBinder
                musicService = binder.getService()
                musicService?.setPlaybackListener(this@MusicViewModel)
                isBound = true

                // Load music files after service is connected
                loadMusicFiles(context)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                android.util.Log.d("MusicViewModel", "Service disconnected: $name")
                musicService?.setPlaybackListener(null)
                isBound = false
            }
        }

        val intent = Intent(context, MusicService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        android.util.Log.d("MusicViewModel", "Attempting to unbind service")
        if (isBound) {
            musicService?.setPlaybackListener(null)
            context.unbindService(serviceConnection)
            isBound = false
            android.util.Log.d("MusicViewModel", "Service unbound successfully")
        } else {
            android.util.Log.d("MusicViewModel", "Service was not bound, skipping unbind")
        }
    }

    private fun loadMusicFiles(context: Context) {
        viewModelScope.launch {
            _isLoading.value = true
            val files = MusicScanner.scanMusicFiles(context)
            _musicFiles.value = files
            musicService?.setMusicFiles(files)
            _isLoading.value = false
        }
    }

    fun playMusic(index: Int) {
        android.util.Log.d("MusicViewModel", "playMusic called with index: $index, isBound: $isBound, musicService: $musicService")
        if (isBound && musicService != null) {
            musicService?.playMusic(index)
            updateCurrentMusic()
        } else {
            android.util.Log.w("MusicViewModel", "Service not ready, cannot play music")
        }
    }

    fun playNext() {
        if (isBound && musicService != null) {
            musicService?.playNext()
            updateCurrentMusic()
        }
    }

    fun playPrevious() {
        if (isBound && musicService != null) {
            musicService?.playPrevious()
            updateCurrentMusic()
        }
    }

    fun togglePlayPause() {
        if (isBound && musicService != null) {
            musicService?.togglePlayPause()
            _isPlaying.value = musicService?.isPlaying() ?: false
        }
    }

    fun seekTo(position: Int) {
        if (isBound && musicService != null) {
            musicService?.seekTo(position)
        }
    }

    fun setLooping(isLooping: Boolean) {
        _isLooping.value = isLooping
        if (isBound && musicService != null) {
            musicService?.setLooping(isLooping)
        }
    }

    fun setShuffle(isShuffle: Boolean) {
        _isShuffle.value = isShuffle
        if (isBound && musicService != null) {
            musicService?.setShuffle(isShuffle)
        }
    }

    private fun updateCurrentMusic() {
        if (isBound && musicService != null) {
            _currentMusic.value = musicService?.getCurrentMusic()
            _isPlaying.value = musicService?.isPlaying() ?: false
            _duration.value = musicService?.getDuration() ?: 0
        }
    }

    fun updatePosition() {
        if (isBound && musicService != null) {
            try {
                _currentPosition.value = musicService?.getCurrentPosition() ?: 0
            } catch (e: Exception) {
                android.util.Log.e("MusicViewModel", "Error updating position", e)
            }
        }
    }

    override fun onDurationChanged(duration: Int) {
        _duration.value = duration
        android.util.Log.d("MusicViewModel", "Duration updated to: $duration")
    }

    override fun onPositionChanged(position: Int) {
        _currentPosition.value = position
        android.util.Log.d("MusicViewModel", "Position updated to: $position")
    }

    fun refreshMusicFiles(context: Context) {
        loadMusicFiles(context)
    }
}