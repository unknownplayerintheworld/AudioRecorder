package hung.deptrai.audiorecorder.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recording",
    indices = [Index(value = ["fileName"], unique = true)])
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val duration: Long,
    val dateAdded: Long
)
