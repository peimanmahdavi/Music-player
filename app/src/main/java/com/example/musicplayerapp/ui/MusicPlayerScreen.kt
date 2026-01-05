package com.example.musicplayerapp.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.musicplayerapp.model.MusicFile
import com.example.musicplayerapp.viewmodel.MusicViewModel
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun MusicPlayerScreen(modifier: Modifier = Modifier) {
    val viewModel: MusicViewModel = viewModel()
    val musicFiles by viewModel.musicFiles
    val currentMusic by viewModel.currentMusic
    val isPlaying by viewModel.isPlaying
    val currentPosition by viewModel.currentPosition
    val duration by viewModel.duration
    val isLooping by viewModel.isLooping
    val isShuffle by viewModel.isShuffle
    val isLoading by viewModel.isLoading

    val coroutineScope = rememberCoroutineScope()

    val context = androidx.compose.ui.platform.LocalContext.current

    val refreshLibrary = {
        viewModel.refreshMusicFiles(context)
    }

    LaunchedEffect(Unit) {
        android.util.Log.d("MusicPlayerScreen", "Starting to bind service")
        // Bind to the music service
        viewModel.bindService(context)
        android.util.Log.d("MusicPlayerScreen", "Service bound successfully")
    }

    // Update position every second using a timer
    val handler = android.os.Handler(android.os.Looper.getMainLooper())
    val updateRunnable = object : Runnable {
        override fun run() {
            try {
                android.util.Log.d("MusicPlayerScreen", "Updating position")
                viewModel.updatePosition()
            } catch (e: Exception) {
                android.util.Log.e("MusicPlayerScreen", "Error updating position", e)
            }
            handler.postDelayed(this, 1000) // Update every second
        }
    }

    DisposableEffect(Unit) {
        android.util.Log.d("MusicPlayerScreen", "Setting up position updates")
        handler.post(updateRunnable)
        onDispose {
            android.util.Log.d("MusicPlayerScreen", "Disposing - removing callbacks and unbinding service")
            handler.removeCallbacks(updateRunnable)
            viewModel.unbindService(context)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Music Player",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        currentMusic?.let { music ->
            MusicPlayerControls(
                music = music,
                isPlaying = isPlaying,
                currentPosition = currentPosition,
                duration = duration,
                isLooping = isLooping,
                isShuffle = isShuffle,
                onPlayPause = { viewModel.togglePlayPause() },
                onNext = { viewModel.playNext() },
                onPrevious = { viewModel.playPrevious() },
                onSeek = { viewModel.seekTo(it) },
                onLoopToggle = { viewModel.setLooping(!isLooping) },
                onShuffleToggle = { viewModel.setShuffle(!isShuffle) }
            )
        } ?: run {
            // Show placeholder when no music is playing
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "No music playing",
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No music playing",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Text(
                        text = "Select a song to start playing",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Music Library (${musicFiles.size})",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            if (musicFiles.isEmpty()) {
                IconButton(
                    onClick = refreshLibrary as () -> Unit
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh music library"
                    )
                }
            }
        }

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Text(
                    text = "Loading music files...",
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        } else if (musicFiles.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(vertical = 32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.LibraryMusic,
                    contentDescription = "Music Library",
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = "No music found",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                Text(
                    text = "Make sure you've granted storage permission",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Button(
                    modifier = Modifier.padding(top = 16.dp),
                    onClick = refreshLibrary
                ) {
                    Text("Refresh Library")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(musicFiles) { music ->
                    MusicItem(
                        music = music,
                        currentMusic = currentMusic,
                        onClick = {
                            val index = musicFiles.indexOf(music)
                            android.util.Log.d("MusicPlayerScreen", "Clicked on music item at index: $index, title: ${music.title}")
                            viewModel.playMusic(index)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MusicPlayerControls(
    music: MusicFile,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    isLooping: Boolean,
    isShuffle: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Int) -> Unit,
    onLoopToggle: () -> Unit,
    onShuffleToggle: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Song info
            Text(
                text = music.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            
            Text(
                text = music.artist,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Progress bar
            Column {
                Text(
                    text = "${formatTime(currentPosition)} / ${formatTime(duration)}",
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.End)
                )
                
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() else 0f,
                    onValueChange = { onSeek(it.toInt()) },
                    valueRange = 0f..if (duration > 0) duration.toFloat() else 1f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onShuffleToggle,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = if (isShuffle) Icons.Default.ShuffleOn else Icons.Default.Shuffle,
                        contentDescription = if (isShuffle) "Shuffle On" else "Shuffle Off"
                    )
                }
                
                IconButton(
                    onClick = onPrevious,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                FloatingActionButton(
                    onClick = onPlayPause,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(
                    onClick = onNext,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                IconButton(
                    onClick = onLoopToggle,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isLooping) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(
                        imageVector = if (isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = if (isLooping) "Repeat One" else "Repeat All"
                    )
                }
            }
        }
    }
}

@Composable
fun MusicItem(
    music: MusicFile,
    currentMusic: MusicFile?,
    onClick: () -> Unit
) {
    val isPlaying = currentMusic?.id == music.id

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f) // Semi-transparent to blend with background
        ),
        shape = MaterialTheme.shapes.medium, // Rounded corners
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.VolumeUp else Icons.Default.MusicNote,
                contentDescription = if (isPlaying) "Currently playing" else "Music note",
                modifier = Modifier.size(40.dp),
                tint = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = music.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${music.artist} • ${formatTime(music.duration.toInt())}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Currently playing",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


fun formatTime(milliseconds: Int): String {
    val seconds = (milliseconds / 1000) % 60
    val minutes = (milliseconds / (1000 * 60)) % 60
    val hours = (milliseconds / (1000 * 60 * 60))
    
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}