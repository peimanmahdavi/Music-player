package com.example.musicplayerapp.model

data class MusicFile(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val duration: Long, // in milliseconds
    val path: String,
    val displayName: String
)