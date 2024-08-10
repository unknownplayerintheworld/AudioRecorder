package hung.deptrai.audiorecorder.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import hung.deptrai.audiorecorder.data.database.AppDatabase
import hung.deptrai.audiorecorder.data.entity.Recording
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RecordingRoomViewModel(database: AppDatabase) : ViewModel(){
    private val recordingDao = database.recordingDAO()


    fun deleteRecordingByFileName(fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                recordingDao.deleteByFileName(fileName)
                Log.d("RecordingRoomViewModel", "Recording deleted: $fileName")
            } catch (e: Exception) {
                Log.e("RecordingRoomViewModel", "Error deleting recording: $fileName", e)
            }
        }
    }

    // Recording methods
    fun addRecordingIfNotExists(fileName: String, filePath: String, duration: Long, dateAdded: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = recordingDao.getRecordingByFileName(fileName) != null
            if (!exists) {
                recordingDao.insert(Recording(fileName = fileName, filePath = filePath, duration = duration, dateAdded = dateAdded))
                Log.d("MainViewModel", "Recording added: $fileName")
            } else {
                Log.d("MainViewModel", "Recording already exists: $fileName")
            }
        }
    }

    fun getAllRecording(){
        viewModelScope.launch(Dispatchers.IO){
            val recording = recordingDao.getAllRecordings()
            recording.forEach{
                recording ->
                Log.e("RecordingRoomViewModel","Recording: $recording")
            }
        }
    }
}
class RecordingRoomViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordingRoomViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordingRoomViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}