package com.oxymusic.app.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.oxymusic.app.model.Track
import com.oxymusic.app.model.TrackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Scans the device's MediaStore for local audio files.
 *
 * Uses MediaStore.Audio.Media (the standard Android API for music files). Returns Track
 * objects with streamUrl = content://media/external/audio/media/<id> which ExoPlayer
 * plays natively via DefaultDataSource.Factory (no special handling needed).
 *
 * Requires READ_MEDIA_AUDIO permission (Android 13+) or READ_EXTERNAL_STORAGE (older).
 * Caller is responsible for requesting permission before calling scan().
 */
@Singleton
class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * Scans MediaStore.Audio.Media and returns all music tracks on the device.
     *
     * Filters:
     * - IS_MUSIC = 1 (excludes ringtones, notifications, alarms)
     * - DURATION > 30000 (excludes very short clips, 30s minimum)
     *
     * Returns alphabetically sorted by title.
     */
    suspend fun scanLocalMusic(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.BITRATE,
            MediaStore.Audio.Media.YEAR,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} = 1 AND ${MediaStore.Audio.Media.DURATION} > 30000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection, projection, selection, null, sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val bitrateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.BITRATE)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id
                    )
                    val title = cursor.getString(titleCol) ?: "Unknown"
                    val artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "Unknown artist"
                    val album = cursor.getString(albumCol) ?: ""
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val mime = cursor.getString(mimeCol) ?: ""
                    val size = cursor.getLong(sizeCol)
                    val bitrate = if (bitrateCol >= 0) cursor.getLong(bitrateCol) else 0L
                    val year = if (yearCol >= 0) cursor.getInt(yearCol) else 0

                    tracks.add(Track(
                        id = "local:$id",
                        title = title,
                        artist = artist,
                        thumbnailUrl = getAlbumArtUri(albumId).toString(),
                        durationMs = duration,
                        streamUrl = contentUri.toString(),
                        source = TrackSource.LOCAL,
                        album = album,
                        bitrate = bitrate,
                        mimeType = mime,
                        fileSize = size,
                        year = year,
                    ))
                }
            }
            Log.i(TAG, "Scanned ${tracks.size} local tracks")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to scan local media", e)
        }
        tracks
    }

    /** Returns the content URI for an album's artwork (may or may not exist). */
    private fun getAlbumArtUri(albumId: Long): Uri {
        val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
        return ContentUris.withAppendedId(sArtworkUri, albumId)
    }

    companion object {
        private const val TAG = "LocalMediaRepository"
    }
}
