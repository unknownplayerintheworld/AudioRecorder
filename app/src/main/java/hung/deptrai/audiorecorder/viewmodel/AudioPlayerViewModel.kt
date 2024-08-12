package hung.deptrai.audiorecorder.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import hung.deptrai.audiorecorder.MyApplication
import hung.deptrai.audiorecorder.data.repository.AudioRepository
import hung.deptrai.audiorecorder.model.Audio
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AudioPlayerViewModel(
    private val repository : AudioRepository,
    private val savedStateHandle: SavedStateHandle) : ViewModel() {
    companion object{
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                // Get the Application object from extras
                val application = checkNotNull(extras[APPLICATION_KEY])
                // Create a SavedStateHandle for this ViewModel from extras
                val savedStateHandle = extras.createSavedStateHandle()

                return AudioPlayerViewModel(
                    (application as MyApplication).myRepository,
                    savedStateHandle
                ) as T
            }
        }
    }
    var mediaPlayer: MediaPlayer? = null
    private val _audioUri = MutableStateFlow<Uri?>(null)

    private val _isPlaying = MutableStateFlow(true)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isPause = MutableStateFlow(false)
    val isPause: StateFlow<Boolean> = _isPause

    private val _currentPosition = MutableStateFlow(0f)
    val currentPosition: StateFlow<Float> = _currentPosition

    private val _totalDuration = MutableStateFlow(0f)
    val totalDuration: StateFlow<Float> = _totalDuration


    private val handler = Handler(Looper.getMainLooper())
    private val updateProgressTask = object : Runnable {
        override fun run() {
            mediaPlayer?.let {
                _currentPosition.value = it.currentPosition / 1000f
                handler.postDelayed(this, 50)
                Log.e("duration:",_currentPosition.value.toString())
            }
        }
    }

    fun setAudioUri(uri: Uri){
        _audioUri.value = uri
    }
    fun getAudioFileInfo(uri: Uri): Audio? {
        return repository.getAudioFileInfo(uri)
    }

    fun deleteRecordingFile(fileUri: Uri): Boolean {
        return repository.deleteRecordingFile(fileUri)
    }

    fun playAudio() {
        _audioUri.value?.let { uri ->
            try {
                val parcelFileDescriptor = repository.getFileDescriptor(uri)
                if (parcelFileDescriptor != null) {
                    mediaPlayer = MediaPlayer()
                    mediaPlayer?.setDataSource(parcelFileDescriptor.fileDescriptor)
                    mediaPlayer?.prepare()
                    mediaPlayer?.apply {
                        setOnPreparedListener{
                            _totalDuration.value = duration /1000f
                            isLooping = false
                            setVolume(1.0f, 1.0f)
                            start()
                            handler.post(updateProgressTask)
                        }
                        setOnCompletionListener {
                            _isPlaying.value = false
                            handler.removeCallbacks(updateProgressTask)
                        }
                    }
                    _isPlaying.value = true
                    parcelFileDescriptor.close()  // Đóng file descriptor sau khi sử dụng
                } else {
                    Log.e("MediaPlayerError", "ParcelFileDescriptor is null")
                }
            } catch (e: Exception) {
                Log.e("MediaPlayerError", "Error playing audio: ${e.message}")
            }
        }
    }

    fun fastRewind() {
        mediaPlayer?.let {
            val newPosition = (it.currentPosition - 5000).coerceAtLeast(0) // tua lại 5 giây
            it.seekTo(newPosition)
            _currentPosition.value = newPosition / 1000f
        }
    }

    fun fastForward() {
        mediaPlayer?.let {
            val newPosition = (it.currentPosition + 5000).coerceAtMost(it.duration) // tua nhanh 5 giây
            it.seekTo(newPosition)
            _currentPosition.value = newPosition / 1000f
        }
    }
    fun pauseAudio(){
        mediaPlayer?.pause()
        _isPlaying.value = false
        _isPause.value = true
        handler.removeCallbacks(updateProgressTask)
    }
    fun resumeAudio() {
        mediaPlayer?.let {
            if (_isPause.value) {
                it.start()
                _isPlaying.value = true
                _isPause.value = false // Đặt lại giá trị isPause về false khi tiếp tục phát
                handler.post(updateProgressTask)
            }
        }
    }

    fun stopAudio(){
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _isPlaying.value = false
        _isPause.value = false
        _currentPosition.value = 0f // Đặt vị trí hiện tại về 0
        handler.removeCallbacks(updateProgressTask)
    }

    override fun onCleared() {
        super.onCleared()
        stopAudio()
    }
}