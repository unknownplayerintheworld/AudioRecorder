package hung.deptrai.audiorecorder.ui.view

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hung.deptrai.audiorecorder.R
import hung.deptrai.audiorecorder.model.Tag
import hung.deptrai.audiorecorder.viewmodel.AudioPlayerViewModel

@SuppressLint("RememberReturnType")
@Composable
fun PlayingScreen(
    TagList: List<Tag>,
    onCancelClick: () -> Unit,
    onClickEqualizer: () -> Unit,
    viewModel: AudioPlayerViewModel
){
    val isPause by viewModel.isPause.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val currentPosition by viewModel.currentPosition.collectAsState()
    val totalDuration by viewModel.totalDuration.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.fillMaxHeight(0.2f))
        Column(
            modifier = Modifier.fillMaxHeight(0.1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = formatTime(currentPosition),style = TextStyle(fontWeight = FontWeight.Bold), color = Color.White, fontSize = 46.sp)
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
                    ListItemPlaying(tag.name,tag.time_offset)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxHeight(1f)
                .fillMaxWidth(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            ProgressBarPlaying(
                currentPosition = currentPosition,
                totalDuration = totalDuration,
                onPositionChange = { newPosition ->
                    viewModel.mediaPlayer?.seekTo((newPosition * 1000).toInt())
                }
            )
            FooterPlaying(
                onFastRewind =
                {
                    viewModel.fastRewind()
                },
                onFastForward =
                {
                    viewModel.fastForward()
                },
                onPlayClick =
                {
                    if(isPlaying){
                        viewModel.pauseAudio()
                    }
                    if(isPause){
                        viewModel.resumeAudio()
                    }
                    else if(!isPause && !isPlaying){
                        viewModel.playAudio()
                    }
                },
                onCancelClick =
                {
                    onCancelClick()
                },
                onClickEqualizer = onClickEqualizer,
                isPlaying = isPlaying,
                isPause = isPause,
                viewModel = viewModel
            ) {
            }
        }
    }
}
@Composable
fun ProgressBarPlaying(
    currentPosition: Float,
    totalDuration: Float,
    onPositionChange: (Float) -> Unit
){
    var sliderPosition by remember {
        mutableFloatStateOf(currentPosition)
    }
    LaunchedEffect(currentPosition) {
        sliderPosition = currentPosition
    }

    Column(
        modifier = Modifier.padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Slider(
            value = sliderPosition,
            onValueChange = {
                sliderPosition = it
                onPositionChange(it)
            }, valueRange = 0f..totalDuration,
            modifier = Modifier.fillMaxWidth())
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Text(text = "${formatTime(sliderPosition)}", style = TextStyle(color = Color.White), fontWeight = FontWeight.Bold)
            Text(text = "${formatTime(totalDuration)}", style = TextStyle(color = Color.White), fontWeight = FontWeight.Bold)
        }
    }
}
fun formatTime(seconds: Float): String {
    val totalSeconds = seconds.toInt()
    val days = totalSeconds / (24 * 3600)
    val hours = (totalSeconds % (24 * 3600)) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val secs = totalSeconds % 60

    return when {
        days > 0 -> String.format("%d:%02d:%02d:%02d", days, hours, minutes, secs)
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
        else -> String.format("%02d:%02d", minutes, secs)
    }
}
@Composable
fun FooterPlaying(
    onFastRewind: () -> Unit,
    onFastForward: () -> Unit,
    onCancelClick: () -> Unit,
    onPlayClick: () -> Unit,
    onClickEqualizer:() -> Unit,
    isPlaying: Boolean,
    isPause: Boolean,
    viewModel: AudioPlayerViewModel,
    function: () -> Unit
){
    Row(
        modifier = Modifier.fillMaxWidth(0.9f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        Column(

        ){
            IconButton(onClick = onCancelClick) {
                Icon(painter = painterResource(id = R.drawable.ic_close),contentDescription = "Đánh dấu", tint = Color.White)
            }
        }
        Column(

        ){
            IconButton(onClick = onFastRewind) {
                Icon(painter = painterResource(id = R.drawable.ic_fast_rewind),contentDescription = "Tua ngược", tint = Color.White)
            }
        }
        Column(

        ){
            IconButton(
                onClick = { onPlayClick() },
                modifier = Modifier
                    .padding(5.dp)
                    .size(60.dp)
                    .clip(RoundedCornerShape(60.dp))
                    .background(Color.Red)
            ) {
                if(isPause || !isPlaying) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_play),
                        contentDescription = "Record",
                        tint = Color.White
                    )
                }
                if(isPlaying){
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
            IconButton(onClick = onFastForward) {
                Icon(painter = painterResource(id = R.drawable.ic_fast_forward),contentDescription = "Tua ngược", tint = Color.White)
            }
        }
        Column(

        ){
            IconButton(onClick = onClickEqualizer) {
                Icon(painter = painterResource(id = R.drawable.ic_equalizer),contentDescription = "Đánh dấu", tint = Color.White)
            }
        }
    }
}
@Composable
fun ListItemPlaying(name: String,time_offset: Long) {
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
@Preview(showBackground = true)
@Composable
fun Review1()
{

}