package hung.deptrai.audiorecorder

import android.app.Application
import hung.deptrai.audiorecorder.data.repository.AudioRepository

class MyApplication: Application() {
    val myRepository: AudioRepository by lazy {
        AudioRepository(application = applicationContext)
    }
}