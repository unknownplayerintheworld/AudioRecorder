package hung.deptrai.audiorecorder.ui.view

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import hung.deptrai.audiorecorder.R
import hung.deptrai.audiorecorder.model.Audio
import hung.deptrai.audiorecorder.model.Tag
import hung.deptrai.audiorecorder.viewmodel.RecordingViewModel
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

@Composable
fun RecordingScreen(
    TagList: List<Tag>,
    onDoneClick: () -> Unit,
    onCancelClick: () -> Unit,
    onRecordClick: () -> Unit,
    isRecording: Boolean,
    isPause: Boolean,
    Timer: String,
    recordingViewModel: RecordingViewModel,
    navController: NavHostController,
    audioList: List<Audio>
){
    val context = LocalContext.current
    if(recordingViewModel.showDialog.value){
        StopRecordingDialog(context = context, viewModel = recordingViewModel, navController = navController,audioList)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.2f),
            verticalArrangement = Arrangement.Center) {
            WaveFormViewWrapper(context = context, isRecording = isRecording, recordingViewModel = recordingViewModel)
        }
        Column(
            modifier = Modifier.fillMaxHeight(0.1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = Timer,style = TextStyle(fontWeight = FontWeight.Bold), color = Color.White, fontSize = 46.sp)
        }
        Column(
            modifier = Modifier.fillMaxHeight(0.7f)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(6.dp)
            ){
                items(TagList.size){
                    index ->
                    val tag = TagList[index]
                    ListItem(tag.name,tag.time_offset)
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxHeight(1f)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ){
            Footer(
                modifier = Modifier.fillMaxWidth(),
                onRecord = onRecordClick,
                onCancel = onCancelClick,
                onDone = onDoneClick,
                isRecording = isRecording,
                isPause = isPause
            )
        }
    }
}

@Composable
fun WaveFormViewWrapper(context: Context, isRecording: Boolean, recordingViewModel: RecordingViewModel) {
    val waveFormView = remember { WaveFormView(context, null) }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            // Start adding amp continuously
            while (isRecording) {
                val amp = recordingViewModel.mediaRecorder?.maxAmplitude?.toFloat()

                amp?.let { waveFormView.addAmp(it) }
                delay(100) // Adjust delay as needed
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center
    ) {
        AndroidView(factory = { waveFormView }) {
            // Không cần thêm bất kỳ xử lý nào khác ở đây
        }
    }
}
@Composable
fun Footer(
    modifier:Modifier,
    onRecord: () -> Unit,
    onCancel:() -> Unit,
    onDone:() -> Unit,
    isRecording: Boolean,
    isPause: Boolean
){
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ){
        Column(

        ){
            IconButton(onClick = onCancel) {
                Icon(painter = painterResource(id = R.drawable.ic_close),contentDescription = "Đánh dấu", tint = Color.White)
            }
        }
        Column(

        ){
            IconButton(onClick = {  }) {
                Icon(painter = painterResource(id = R.drawable.ic_flag),contentDescription = "Đánh dấu", tint = Color.White)
            }
        }
        Column(

        ){
            IconButton(
                onClick = { onRecord() },
                modifier = Modifier
                    .padding(5.dp)
                    .size(60.dp)
                    .clip(RoundedCornerShape(60.dp))
                    .background(Color.Red)
            ) {
                if(isPause) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = "Record",
                        tint = Color.White
                    )
                }
                if(isRecording){
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pause),
                        contentDescription = "Record",
                        tint = Color.White
                    )
                }
            }
        }
        Column(

        ){
            IconButton(onClick = onDone) {
                Icon(painter = painterResource(id = R.drawable.ic_done),contentDescription = "Đánh dấu", tint = Color.White)
            }
        }
    }
}
@SuppressLint("NewApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopRecordingDialog(
    context: Context,
    viewModel: RecordingViewModel,
    navController: NavHostController,
    audioList: List<Audio> // Thêm tham số này để kiểm tra sự tồn tại của tên tệp
) {
    var text by remember { mutableStateOf("") }
    var isNameTaken by remember { mutableStateOf(false) }

    // Kiểm tra tên đã tồn tại mỗi khi `text` thay đổi
    LaunchedEffect(text) {
        isNameTaken = audioExists(text, audioList)
    }

    AlertDialog(
        onDismissRequest = { viewModel.showDialog.value = false },
        title = { Text(text = "Save Recording") },
        text = {
            Column {
                Text(text = "Enter the name of the recording:")
                TextField(
                    value = text,
                    onValueChange = { newText ->
                        text = newText
                        // Kiểm tra tên mới và cập nhật trạng thái
                        isNameTaken = audioExists(newText, audioList)
                    },
                    isError = isNameTaken // Hiển thị lỗi nếu tên đã tồn tại
                )
                if (isNameTaken) {
                    Text(
                        text = "Name already exists",
                        color = Color.Red
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isNameTaken) { // Chỉ lưu nếu tên chưa tồn tại
                        viewModel.renameRecording(context, text)
                        viewModel.stopRecording()
                        viewModel.showDialog.value = false
                        navController.navigate("home")
                    }
                },
                enabled = !isNameTaken // Vô hiệu hóa nút Save nếu tên đã tồn tại
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = { viewModel.showDialog.value = false }) {
                Text("Cancel")
            }
        }
    )
}
fun audioExists(fileName: String, audioList: List<Audio>): Boolean {
    // Kiểm tra xem tên fileName có tồn tại trong thuộc tính name của các đối tượng Audio không
    return audioList.any { it.name == fileName }
}
@Composable
fun ListItem(name: String,time_offset: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(color = Color(0xff111111), shape = RoundedCornerShape(15.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth()
                .background(Color(0xFF111111)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${formatTime(time_offset)}",
                    fontSize = 20.sp,
                    style = TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .padding(end = 15.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                Text(
                    text = name,
                    fontSize = 20.sp,
                    style = TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Row(
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { /* Xử lý khi nút được nhấn */ },
                        modifier = Modifier
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_option),
                            contentDescription = "More options",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
//    Spacer(modifier = Modifier.height(5.dp).fillMaxWidth().background(Color.Black))
}
fun formatTime(seconds: Long): String {
    val hours = TimeUnit.SECONDS.toHours(seconds)
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}