package hung.deptrai.audiorecorder.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import hung.deptrai.audiorecorder.model.Audio

class AudioRepository(application: Context) {
    private val contentResolver: ContentResolver = application.contentResolver

    fun getFileDescriptor(uri: Uri): ParcelFileDescriptor? {
        return try {
            contentResolver.openFileDescriptor(uri, "r")
        } catch (e: Exception) {
            Log.e("AudioRepository", "Error opening file descriptor: ${e.message}")
            null
        }
    }
    fun getAudioFileInfo(uri: Uri): Audio? {

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED
        )

        val cursor = contentResolver.query(
            uri,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val nameColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dateAddedColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)

                it.getLong(idColumn)
                val name = it.getString(nameColumn)
                val duration = it.getLong(durationColumn)
                val dateAdded = it.getLong(dateAddedColumn)

                return Audio(name, duration, dateAdded, uri)
            }
        }

        return null
    }
    fun deleteRecordingFile(fileUri: Uri): Boolean {
        return try {
            val rowsDeleted = contentResolver.delete(fileUri, null, null)

            rowsDeleted > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}