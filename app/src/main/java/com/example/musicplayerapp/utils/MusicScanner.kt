package com.example.musicplayerapp.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.example.musicplayerapp.model.MusicFile

object MusicScanner {
    fun scanMusicFiles(context: Context): List<MusicFile> {
        val musicFiles = mutableListOf<MusicFile>()
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        // Look for all audio files, not just music
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 OR " +
                "${MediaStore.Audio.Media.IS_AUDIOBOOK} = 1 OR " +
                "${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        val cursor: Cursor? = context.contentResolver.query(
            uri,
            projection,
            selection,
            null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val nameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val displayName = it.getString(nameColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val album = it.getString(albumColumn)
                val duration = it.getLong(durationColumn)
                val path = it.getString(dataColumn)

                // Only add files with valid duration and path
                val validExtensions = listOf(".mp3", ".m4a", ".wav", ".flac", ".aac", ".ogg", ".wma")
                val hasValidExtension = validExtensions.any { path?.lowercase()?.endsWith(it) == true }

                if (duration > 1000 && !path.isNullOrEmpty() && hasValidExtension) {
                    val musicFile = MusicFile(
                        id = id,
                        title = if (title.isEmpty() || title == "<unknown>") displayName else title,
                        artist = if (artist.isNullOrEmpty() || artist == "<unknown>") "Unknown" else artist,
                        album = if (album.isNullOrEmpty() || album == "<unknown>") "Unknown" else album,
                        duration = duration,
                        path = path,
                        displayName = displayName
                    )
                    musicFiles.add(musicFile)
                }
            }
        }

        return musicFiles
    }
}