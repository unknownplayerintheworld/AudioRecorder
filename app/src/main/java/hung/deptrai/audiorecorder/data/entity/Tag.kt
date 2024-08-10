package hung.deptrai.audiorecorder.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tag",
    foreignKeys = [ForeignKey(
        entity = Recording::class,
        parentColumns = ["id"],
        childColumns = ["recordingId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val recordingId: Int,
    val tagTime: Long,
    val tagName: String
)
