    package hung.deptrai.audiorecorder.viewmodel

    import android.Manifest
    import android.annotation.SuppressLint
    import android.content.ContentResolver
    import android.content.ContentValues
    import android.content.Context
    import android.content.SharedPreferences
    import android.content.pm.PackageManager
    import android.database.Cursor
    import android.media.AudioFormat
    import android.media.AudioRecord
    import android.media.MediaMetadataRetriever
    import android.media.MediaRecorder
    import android.media.RingtoneManager
    import android.net.Uri
    import android.os.Build
    import android.os.Environment
    import android.provider.MediaStore
    import android.provider.MediaStore.Audio.Media
    import android.util.Log
    import androidx.annotation.RequiresApi
    import androidx.compose.runtime.MutableState
    import androidx.compose.runtime.mutableStateOf
    import androidx.core.app.ActivityCompat
    import androidx.lifecycle.ViewModel
    import androidx.lifecycle.viewModelScope
    import hung.deptrai.audiorecorder.model.Audio
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.Job
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.flow.MutableStateFlow
    import kotlinx.coroutines.flow.SharingStarted
    import kotlinx.coroutines.flow.StateFlow
    import kotlinx.coroutines.flow.asStateFlow
    import kotlinx.coroutines.flow.combine
    import kotlinx.coroutines.flow.stateIn
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import java.io.File
    import java.io.FileDescriptor
    import java.io.FileOutputStream
    import java.io.IOException
    import java.io.OutputStream
    data class RecordingTime(
        val days: Int = 0,
        val hours: Int = 0,
        val minutes: Int = 0,
        val seconds: Int = 0,
        val milliseconds: Int = 0
    )
    class RecordingViewModel(@SuppressLint("StaticFieldLeak") private val context: Context) :ViewModel()
     {
         var mediaRecorder: MediaRecorder ?= null
         var audioRecorder: AudioRecord ?= null

         private var outputStream: OutputStream? = null
         private var recordingJob: Job? = null

         private val _isRecording = MutableStateFlow(true)
         private val _isPause = MutableStateFlow(false)
         private val _isCancel = MutableStateFlow(false)
 
         private val _recordingTime = MutableStateFlow(RecordingTime(0, 0, 0, 0, 0))
 
         val isPause: StateFlow<Boolean> = _isPause
         val isRecording: StateFlow<Boolean> = _isRecording
         val isCancel: StateFlow<Boolean> = _isCancel
 
         val recordingTime: StateFlow<RecordingTime> = _recordingTime
 
         //
         private val sharedPreferences: SharedPreferences = context.getSharedPreferences("AppPreferences",Context.MODE_PRIVATE)

         //format Recording File
         private val _formatRecording = MutableStateFlow(loadFormatRecording())
         val formatRecording = _formatRecording.asStateFlow()

         private var outputUri: Uri? = null
         var tempUriString: String? = null
 
         private var startTime = 0L
         private var pauseTime = 0L

         // audioList home screen
         private val _audioList = MutableStateFlow(listOf<Audio>())
         val audioList = _audioList.asStateFlow()


         val showDialog: MutableState<Boolean> = mutableStateOf(false)

         // search using viewmodel
         private val _searchText = MutableStateFlow("")
         val searchText = _searchText.asStateFlow()

         private val _isSearch = MutableStateFlow(false)
         val isSearch = _isSearch.asStateFlow()

         private val _isOption = MutableStateFlow(false)
         val isOption = _isOption.asStateFlow()

         private val _isTypeClicking = MutableStateFlow(false)
         val isTypeClicking = _isTypeClicking.asStateFlow()

         private val _audioSearchList = MutableStateFlow(listOf<Audio>())

         private val _isEdit = MutableStateFlow(false)
         val isEdit = _isEdit.asStateFlow()

         private val _selectedItem = MutableStateFlow<Set<String>>(emptySet())
         val selectedItem = _selectedItem.asStateFlow()

         private val _selectedNames = MutableStateFlow<Set<String>>(emptySet())

         private val _isCheckAll = MutableStateFlow(false)
         val isCheckAll = _isCheckAll.asStateFlow()

         val audios = searchText
            .combine(_audioSearchList){
                text,audios ->
                if(text.isBlank()){
                    audios
                }
                else{
                    audios.filter {
                        it.doesMatchingSearchQuery(text)
                    }
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                _audioSearchList.value
            )

         fun toggleItem(item: String,name: String){
             _selectedItem.value = _selectedItem.value.toMutableSet().apply {
                 if (contains(item)) {
                     remove(item) // Bỏ chọn nếu đã có
                 } else {
                     add(item) // Thêm vào nếu chưa có
                 }
             }
             _selectedNames.value = _selectedNames.value.toMutableSet().apply {
                 if(contains(name)){
                     remove(name)
                 }
                 else{
                     add(name)
                 }
             }
         }
         fun deleteSelectedFiles(recordingRoomViewModel: RecordingRoomViewModel) {
             val contentResolver: ContentResolver = context.contentResolver

             val fileUris = _selectedItem.value.map { Uri.parse(it) }
             for (fileUri in fileUris) {
                 try {
                     val rowsDeleted = contentResolver.delete(fileUri, null, null)
                     if (rowsDeleted > 0) {
                         println("File $fileUri đã được xóa thành công.")
                     } else {
                         println("Không thể xóa file $fileUri hoặc file không tồn tại.")
                     }
                 } catch (e: Exception) {
                     println("Đã xảy ra lỗi khi xóa file $fileUri: ${e.message}")
                 }
             }
             val fileNames = _selectedNames.value.toList()
             fileNames.forEach { fileName ->
                 try{
                     recordingRoomViewModel.deleteRecordingByFileName(fileName)
                     deleteRecordingElement(fileNames.indexOf(fileName))
                 }catch(e: Exception){
                     Log.e("Exception","Some thing went wrong",e)
                 }
             }
             // Sau khi xóa, bỏ chọn tất cả các mục
             deselectAllItems()
         }
         fun selectAllItems(items : Set<String>){
             _selectedItem.value = items
             _isCheckAll.value = true
         }
         fun deselectAllItems() {
             _selectedItem.value = emptySet()
             _isCheckAll.value = false
         }
         fun onCheckAll(status: Boolean){
             _isCheckAll.value = status
         }

         fun onEditChange(status: Boolean){
             _isEdit.value = status
         }
         fun onSearchTextChange(text: String){
             _searchText.value = text
         }
         fun setSearchingStatus(status : Boolean){
            _isSearch.value = status
         }
         private fun loadFormatRecording(): String{
             return sharedPreferences.getString("Format","MP3") ?: "MP3"
         }

         fun setFormat(newFormat :String){
             _formatRecording.value = newFormat
             sharedPreferences.edit().putString("Format",newFormat).apply()
         }

         fun closeSearching(){
             _isSearch.value = false
         }
         fun setOptionStatus(status : Boolean){
             _isOption.value = status
         }

         fun setTypeClicking(status: Boolean){
             _isTypeClicking.value = status
         }

         fun loadRecordings(context: Context,roomViewModel: RecordingRoomViewModel) {
             viewModelScope.launch {
                 val audioList = getSavedRecordingsFromMediaStore(context.contentResolver)
                 _audioList.value = audioList
                 _audioSearchList.value = audioList
                 audioList.forEach { audio ->
                     roomViewModel.addRecordingIfNotExists(
                         audio.name,
                         audio.audioPath.toString(),
                         audio.duration,
                         audio.recordingTime
                     )
                 }
                 roomViewModel.getAllRecording()
             }
         }
         fun deleteRecordingElement(index: Int){
             val currentList = _audioList.value

             // Tạo bản sao của danh sách và xóa phần tử
             val updatedList = currentList.toMutableList().apply {
                 removeAt(index)
             }

             // Cập nhật giá trị của MutableStateFlow
             _audioList.value = updatedList
         }
 
         fun startTimer() {
             startTime = System.currentTimeMillis() - pauseTime
             viewModelScope.launch {
                 while (_isRecording.value) {
 
                     val currentTime = System.currentTimeMillis()
                     val elapsedTime = currentTime - startTime
                     pauseTime = elapsedTime
 
                     val newMilliseconds = (elapsedTime % 1000).toInt()
                     val totalSeconds = (elapsedTime / 1000).toInt()
                     val newSeconds = totalSeconds % 60
                     val totalMinutes = totalSeconds / 60
                     val newMinutes = totalMinutes % 60
                     val totalHours = totalMinutes / 60
                     val newHours = totalHours % 24
                     val newDays = totalHours / 24
 
                     _recordingTime.value = RecordingTime(newDays, newHours, newMinutes, newSeconds, newMilliseconds)
                     delay(10)
                 }
             }
         }
         private fun stopTimer(){
             viewModelScope.launch {
                 _recordingTime.value = RecordingTime()
                 startTime = 0L
                 pauseTime = 0L
             }
         }
         @RequiresApi(Build.VERSION_CODES.S)
         fun startRecording() {
             viewModelScope.launch {
                 withContext(Dispatchers.IO) {
                     val directoryMusic = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
                     val recordingDirectory = File(directoryMusic, "Recordings")
 
//                       Kiểm tra xem thư mục "Recordings" đã tồn tại chưa, nếu không thì tạo mới
                     if (!recordingDirectory.exists()) {
                         recordingDirectory.mkdirs()
                     }
                     val values = ContentValues().apply {
                         put(Media.DISPLAY_NAME, "Recording_${System.currentTimeMillis()}")
                         if(_formatRecording.value == "WAV"){
                             put(Media.MIME_TYPE, "audio/x-wav")
                         }
                         else {
                             put(Media.MIME_TYPE, "audio/mp4")
                         }
                         put(Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Recordings")
                         put(Media.DATE_ADDED, System.currentTimeMillis()/1000)
                     }
 
                     val resolver = context.contentResolver
                     outputUri = resolver.insert(Media.EXTERNAL_CONTENT_URI, values)
                     Log.d("Recording", "outputUri: $outputUri")
                     outputUri?.let { uri ->
                         val parcelFileDescriptor = resolver.openFileDescriptor(uri, "w")
                         parcelFileDescriptor?.fileDescriptor?.let { fd ->
                             _recordingTime.value = RecordingTime(0, 0, 0, 0, 0)
                             if(_formatRecording.value != "WAV") {
                                 mediaRecorder = MediaRecorder(context).apply {
                                     setAudioSource(MediaRecorder.AudioSource.MIC)

                                     if (_formatRecording.value == "MP3") {
                                         setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                         setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                                     } else if (_formatRecording.value == "AAC") {
                                         setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS)
                                         setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                     }
                                     setOutputFile(fd)
                                     prepare()
                                     start()
                                     _isRecording.value = true
                                     _isPause.value = false
                                 }
                             }
                             else{
                                setUpWAVrecording(fd,context)
                             }
                             startTimer()
                             Log.e("Record status:", "true")
                         } ?: run {
                             Log.e("Recording", "Failed to open ParcelFileDescriptor")
                         }
                     } ?: run {
                         Log.e("Recording", "Failed to create MediaStore entry")
                     }
                 }
             }
         }

         private fun setUpWAVrecording(fd: FileDescriptor, context: Context) {
             val samplerate = 44100
             val channelconfig = AudioFormat.CHANNEL_IN_MONO
             val audioFormat = AudioFormat.ENCODING_PCM_16BIT
             val buffersize = AudioRecord.getMinBufferSize(samplerate, channelconfig, audioFormat)

             if (ActivityCompat.checkSelfPermission(
                     context,
                     Manifest.permission.RECORD_AUDIO
                 ) != PackageManager.PERMISSION_GRANTED
             ) {
                 return
             }

             audioRecorder = AudioRecord(
                 MediaRecorder.AudioSource.MIC,
                 samplerate,
                 channelconfig,
                 audioFormat,
                 buffersize
             )

             if (audioRecorder?.state == AudioRecord.STATE_INITIALIZED) {
                 try {
                     outputStream = FileOutputStream(fd)

                     audioRecorder?.startRecording()
                     _isRecording.value = true
                     _isPause.value = false

                     recordingJob = CoroutineScope(Dispatchers.IO).launch {
                         val buffer = ByteArray(buffersize)
                         while (_isRecording.value) {
                             if (!_isPause.value) {
                                 val read = audioRecorder?.read(buffer, 0, buffer.size) ?: 0
                                 if (read > 0) {
                                     try {
                                         outputStream?.write(buffer, 0, read)
                                     } catch (e: IOException) {
                                         Log.e("Recording", "Error while writing to OutputStream", e)
                                         // Handle the exception if needed
                                     }
                                 }
                             }
                         }
                         // Close the OutputStream and add WAV header
                         outputStream?.close()
                         addWAVHeader(fd, samplerate, 1, 16) // Example: 1 channel, 16-bit depth
                     }
                 } catch (e: IOException) {
                     Log.e("Recording", "Error while recording with AudioRecord", e)
                 }
             } else {
                 Log.e("Recording", "AudioRecord initialization failed")
             }
         }

         private fun addWAVHeader(fd: FileDescriptor, sampleRate: Int, channels: Int, bitsPerSample: Int) {
             // Write WAV header to the beginning of the file
             val file = File(fd.toString()) // This is a placeholder, you may need to use a different method to get the file path
             if (file.exists()) {
                 val fileLength = file.length()
                 val outputStream = FileOutputStream(file, true) // Open file in append mode

                 val headerSize = 44 // Size of the WAV header
                 val totalDataLen = fileLength + headerSize - 8 // Total size of the data chunk (excluding RIFF header)
                 val byteRate = sampleRate * channels * bitsPerSample / 8

                 try {
                     val header = ByteArray(headerSize)
                     header[0] = 'R'.code.toByte() // RIFF Header
                     header[1] = 'I'.code.toByte()
                     header[2] = 'F'.code.toByte()
                     header[3] = 'F'.code.toByte()
                     // ChunkSize (36 + SubChunk2Size)
                     header[4] = (totalDataLen and 0xFF).toByte()
                     header[5] = ((totalDataLen shr 8) and 0xFF).toByte()
                     header[6] = ((totalDataLen shr 16) and 0xFF).toByte()
                     header[7] = ((totalDataLen shr 24) and 0xFF).toByte()
                     header[8] = 'W'.code.toByte() // Wave Header
                     header[9] = 'A'.code.toByte()
                     header[10] = 'V'.code.toByte()
                     header[11] = 'E'.code.toByte()
                     header[12] = 'f'.code.toByte() // 'fmt ' header
                     header[13] = 'm'.code.toByte()
                     header[14] = 't'.code.toByte()
                     header[15] = ' '.code.toByte()
                     // Subchunk1Size (16 for PCM)
                     header[16] = 16.toByte()
                     header[17] = 0
                     header[18] = 0
                     header[19] = 0
                     header[20] = 1.toByte() // AudioFormat (1 for PCM)
                     header[21] = 0
                     header[22] = channels.toByte() // Number of Channels
                     header[23] = 0
                     // SampleRate
                     header[24] = (sampleRate and 0xFF).toByte()
                     header[25] = ((sampleRate shr 8) and 0xFF).toByte()
                     header[26] = ((sampleRate shr 16) and 0xFF).toByte()
                     header[27] = ((sampleRate shr 24) and 0xFF).toByte()
                     // ByteRate
                     header[28] = (byteRate and 0xFF).toByte()
                     header[29] = ((byteRate shr 8) and 0xFF).toByte()
                     header[30] = ((byteRate shr 16) and 0xFF).toByte()
                     header[31] = ((byteRate shr 24) and 0xFF).toByte()
                     header[32] = (channels * bitsPerSample / 8).toByte() // BlockAlign
                     header[33] = 0
                     header[34] = bitsPerSample.toByte() // BitsPerSample
                     header[35] = 0
                     header[36] = 'd'.code.toByte() // 'data' header
                     header[37] = 'a'.code.toByte()
                     header[38] = 't'.code.toByte()
                     header[39] = 'a'.code.toByte()
                     // Subchunk2Size (DataSize)
                     header[40] = (fileLength.toInt() and 0xFF).toByte()
                     header[41] = ((fileLength.toInt() shr 8) and 0xFF).toByte()
                     header[42] = ((fileLength.toInt() shr 16) and 0xFF).toByte()
                     header[43] = ((fileLength.toInt() shr 24) and 0xFF).toByte()

                     outputStream.write(header)
                     outputStream.close()
                 } catch (e: IOException) {
                     e.printStackTrace()
                 }
             }
         }

         private fun getSavedRecordingsFromMediaStore(contentResolver: ContentResolver): List<Audio> {
             val recordings = mutableListOf<Audio>()
 
             val projection = arrayOf(
                 Media._ID,
                 Media.DISPLAY_NAME,
                 Media.DURATION,
                 Media.DATE_ADDED
             )
 
             val selection = "${Media.RELATIVE_PATH} LIKE ?"
             val selectionArgs = arrayOf("${Environment.DIRECTORY_MUSIC}/%Recordings%")
             val sortOrder = "${Media.DATE_ADDED} DESC"
 
             val queryUri = Media.EXTERNAL_CONTENT_URI
 
             val cursor: Cursor? = contentResolver.query(
                 queryUri,
                 projection,
                 selection,
                 selectionArgs,
                 sortOrder
             )
 
             cursor?.use {
                 val idColumn = it.getColumnIndexOrThrow(Media._ID)
                 val nameColumn = it.getColumnIndexOrThrow(Media.DISPLAY_NAME)
                 val durationColumn = it.getColumnIndexOrThrow(Media.DURATION)
                 val dateAddedColumn = it.getColumnIndexOrThrow(Media.DATE_ADDED)
 
                 while (it.moveToNext()) {
                     val id = it.getLong(idColumn)
                     val name = it.getString(nameColumn)
                     val duration = it.getLong(durationColumn)
                     val dateAdded = it.getLong(dateAddedColumn)
                     val contentUri = Uri.withAppendedPath(queryUri, id.toString())
                     val actualDuration = getAudioDuration(contentResolver,contentUri)
 
                     Log.d("Recording", "ID: $id, Name: $name, Duration: $duration,Actual Duration: $actualDuration, Date Added (seconds): $dateAdded")
                     recordings.add(Audio(name, actualDuration, dateAdded,contentUri))
                 }
             }
 
             return recordings
         }
         fun getAudioDuration(contentResolver: ContentResolver, uri: Uri): Long {
             var duration: Long = 0
             val retriever = MediaMetadataRetriever()
             try {
                 retriever.setDataSource(contentResolver.openAssetFileDescriptor(uri, "r")?.fileDescriptor)
                 val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                 if (durationString != null) {
                     duration = durationString.toLong()
                 }
             } catch (e: Exception) {
                 Log.e("Recording", "Failed to retrieve duration for uri: $uri", e)
             } finally {
                 retriever.release()
             }
             return duration
         }
 
         @RequiresApi(Build.VERSION_CODES.O)
         fun stopRecording(){
             viewModelScope.launch{
                 withContext(Dispatchers.IO){
                     mediaRecorder?.apply {
                         if(_isRecording.value) {
                             stop()
                         }
                         release()
                     }
                     audioRecorder?.apply {
                         if(_isRecording.value){
                             audioRecorder?.stop()
                             outputStream?.close()
                         }
                         audioRecorder?.release()
                     }
                     audioRecorder = null
                     outputStream = null
                     mediaRecorder = null
                     _isRecording.value = false
                     reset()
                     stopTimer()
                     _recordingTime.value = RecordingTime(0, 0, 0, 0, 0)
                     Log.e("Record status:","false")
                 }
             }
         }
         fun renameRecording(context: Context, newName: String) {
             tempUriString?.let { uriString ->
                 val resolver = context.contentResolver
                 val uri = Uri.parse(uriString)
                 val values = ContentValues().apply {
                     put(Media.DISPLAY_NAME, "$newName")
                 }
                 resolver.update(uri, values, null, null)
             }
         }
 
         fun cancelRecording(){
             Log.d("Recording", "outputUri: $outputUri")
             viewModelScope.launch {
                 withContext(Dispatchers.IO){
                     mediaRecorder?.apply {
                         stop()
                         release()
                     }
                     audioRecorder?.apply {
                         stop()
                         release()
                     }
                     audioRecorder = null
                     mediaRecorder = null
                     outputUri.let { uri ->
                         Log.e("URI",uri.toString())
                         if (uri != null) {
                             context.contentResolver.delete(uri, null, null)
                             _isCancel.value = true
                         }
                         outputUri = null
                         reset()
                     }
                     stopTimer()
                     _recordingTime.value = RecordingTime(0, 0, 0, 0, 0)
                 }
             }
         }
 
         override fun onCleared() {
             super.onCleared()
             cancelRecording()
         }
         fun pauseRecording(){
             viewModelScope.launch {
                 withContext(Dispatchers.IO){
                     mediaRecorder?.pause()
                 }
                 _isRecording.value = false
                 _isPause.value = true
             }
         }
         @SuppressLint("SuspiciousIndentation")
         fun onConfirmStop(){
             viewModelScope.launch {
                 withContext(Dispatchers.IO) {
                     mediaRecorder?.pause()
                 }
                 _isRecording.value = false
                 _isPause.value = true
                 outputUri?.let {
                     showDialog.value = true
                     tempUriString = it.toString()
                 }
             }
         }
         fun resumeRecording(){
             viewModelScope.launch{
                 withContext(Dispatchers.IO){
                     mediaRecorder?.resume()
                 }
                 _isRecording.value = true
                 _isPause.value = false
                 startTimer()
             }
         }
         fun reset(){
             _isRecording.value = false
             _isPause.value = false
             _recordingTime.value = RecordingTime(0, 0, 0, 0, 0)
             startTime = 0L
             pauseTime = 0L
         }

         fun addToRingtone(context: Context, filePath: String) {
             val file = File(filePath)
             val mimeType = when (file.extension.lowercase()) {
                 "mp3" -> "audio/mpeg"
                 "wav" -> "audio/x-wav"
                 else -> "audio/*" // Loại MIME chung cho các định dạng âm thanh khác
             }

             val values = ContentValues().apply {
                 put(MediaStore.MediaColumns.DATA, file.absolutePath)
                 put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                 put(MediaStore.MediaColumns.TITLE, file.name)
                 put(Media.IS_RINGTONE, true)
                 put(Media.IS_NOTIFICATION, false)
                 put(Media.IS_ALARM, false)
                 put(Media.IS_MUSIC, false)
             }

             val resolver = context.contentResolver
             val uri = Media.EXTERNAL_CONTENT_URI

             try {
                 val newUri = resolver.insert(uri, values)
                 if (newUri != null) {
                     Log.d("RingtoneUtils", "File added to MediaStore successfully.")
                 } else {
                     Log.e("RingtoneUtils", "Failed to insert file into MediaStore.")
                 }
             } catch (e: Exception) {
                 Log.e("RingtoneUtils", "Error adding file to MediaStore: ${e.message}")
             }
         }
         fun setAsRingtone(context: Context, filePath: String) {
             val file = File(filePath)
             val uri = Uri.fromFile(file)

             try {
                 RingtoneManager.setActualDefaultRingtoneUri(
                     context,
                     RingtoneManager.TYPE_RINGTONE,
                     uri
                 )
                 Log.d("RingtoneUtils", "File set as ringtone successfully.")
             } catch (e: Exception) {
                 Log.e("RingtoneUtils", "Error setting file as ringtone: ${e.message}")
             }
         }
     }