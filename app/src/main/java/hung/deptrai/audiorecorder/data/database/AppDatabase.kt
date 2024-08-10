package hung.deptrai.audiorecorder.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import hung.deptrai.audiorecorder.data.dao.RecordingDAO
import hung.deptrai.audiorecorder.data.dao.TagDAO
import hung.deptrai.audiorecorder.data.entity.Recording
import hung.deptrai.audiorecorder.data.entity.Tag

@Database(entities = [Recording::class, Tag::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDAO(): RecordingDAO
    abstract fun tagDAO(): TagDAO
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase ?= null

        fun getInstance(context: Context): AppDatabase{
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}