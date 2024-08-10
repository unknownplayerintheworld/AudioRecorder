package hung.deptrai.audiorecorder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import hung.deptrai.audiorecorder.data.entity.Recording

@Dao
interface RecordingDAO {
    @Insert
    suspend fun insert(recording: Recording): Long
    @Query("SELECT * FROM recording")
    suspend fun getAllRecordings(): List<Recording>

    @Query("SELECT * FROM recording WHERE id = :id")
    suspend fun getRecordingById(id: Int): Recording


    @Query("SELECT * FROM recording WHERE fileName = :fileName LIMIT 1")
    suspend fun getRecordingByFileName(fileName: String): Recording?

    @Query("DELETE FROM recording WHERE id = :id")
    suspend fun deleteRecording(id: Int): Int
    @Query("DELETE FROM recording WHERE fileName = :fileName")
    suspend fun deleteByFileName(fileName: String)
}