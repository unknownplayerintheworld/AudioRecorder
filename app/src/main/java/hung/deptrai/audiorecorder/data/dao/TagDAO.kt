package hung.deptrai.audiorecorder.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import hung.deptrai.audiorecorder.data.entity.Tag

@Dao
interface TagDAO {
    @Insert
    suspend fun insert(tag: Tag)

    @Update
    suspend fun update(tag: Tag)

    @Delete
    suspend fun delete(tag: Tag)

    @Query("DELETE FROM tag WHERE recordingId = :recordingId")
    suspend fun deleteTagsForRecording(recordingId: Int)
}