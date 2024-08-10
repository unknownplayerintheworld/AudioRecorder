package hung.deptrai.audiorecorder.ui.view

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import hung.deptrai.audiorecorder.viewmodel.RecordingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(recordingViewModel: RecordingViewModel,navHostController: NavHostController) {
    Box{
        Column(
            modifier = Modifier
                .fillMaxSize(1f)
                .background(Color.Black)
        ) {
            val selectedOption by recordingViewModel.formatRecording.collectAsState()
            Scaffold(
                topBar = {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(onClick = { navHostController.navigate("home") }) {
                                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        title = {
                            Text(
                                "Cài đặt",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = Color.Black
                        ) // Chỉnh màu nền thành màu đen
                    )
                }
            ) { innerPadding ->
                // Nội dung chính của giao diện
                // innerPadding chứa giá trị padding từ các thành phần trên
                Column(modifier = Modifier
                    .padding(innerPadding)
                    .background(Color.Black)
                    .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    /* Nội dung chính ở đây */
                    Column(
                        Modifier
                            .fillMaxWidth(0.9f)
                            .padding(0.dp, 10.dp, 0.dp, 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Gray)
                    ) {
                        Column(
                            Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                                    .clickable {
                                        recordingViewModel.setTypeClicking(true)
                                    }
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth(0.9f)
                                ) {
                                    Text(text = "Định dạng tệp ghi âm",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp)

                                        Text(text = selectedOption,
                                            color = Color.Red,
                                            fontSize = 15.sp)

                                }
                                Column{
                                    IconButton(onClick = {  }) {
                                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "View more")
                                    }
                                }
                            }
                        }
                    }
                    Column(
                        Modifier
                            .fillMaxWidth(0.9f)
                            .padding(0.dp, 0.dp, 0.dp, 10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Gray)
                    ) {
                        Column(
                            Modifier.fillMaxWidth()
                        ) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp, 0.dp, 10.dp, 0.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth(0.9f)
                                ) {
                                    Text(text = "Giới thiệu về trình ghi âm",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp)
                                }
                                Column{
                                    IconButton(onClick = {  }) {
                                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "View more")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DropdownTypeOfRecordingMenu(recordingViewModel: RecordingViewModel) {
    var visible by remember { mutableStateOf(false) }
    val isTypeClick by recordingViewModel.isTypeClicking.collectAsState()
    val transitionState = remember { MutableTransitionState(visible) }
    transitionState.targetState = isTypeClick


    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 450.dp,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing), label = "Offset"
    )

    LaunchedEffect(isTypeClick) {
        visible = isTypeClick
    }
    if(visible) {
        Box(
            modifier = Modifier
                .background(Color.Transparent)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            Column(
                Modifier.fillMaxSize()
            ) {
                Spacer(
                    modifier = Modifier
                        .fillMaxHeight(0.6f)
                        .fillMaxWidth()
                        .clickable {
                            recordingViewModel.setTypeClicking(false)
                        }
                )
                Column(
                    Modifier
                        .offset(y = offsetY)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF111111))
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Định dạng tệp ghi âm",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(25.dp))
                        val selectedOption by recordingViewModel.formatRecording.collectAsState()
                        Column{
                            val options = listOf( "MP3" to "Định dạng phổ dụng có tính tương thích rộng",
                                "AAC" to "Định dạng nén hiệu quả, chất lượng âm thanh cao",
                                "WAV" to "Định dạng âm thanh không nén, chất lượng cao")
                            options.forEach { (it,ite1) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            recordingViewModel.setFormat(it)
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(0.9f)
                                    ) {
                                        Text(
                                            text = it,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = ite1,
                                            fontSize = 12.sp,
                                            color = Color.White
                                        )
                                    }
                                    Column{
                                        RadioButton(selected = selectedOption == it, onClick = { recordingViewModel.setFormat(it) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = Color.Green,
                                                unselectedColor = Color.Red
                                            ))
                                    }
                                }
                                Spacer(modifier = Modifier.height(25.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun Preview() {
    DropdownTypeOfRecordingMenu(recordingViewModel = RecordingViewModel(LocalContext.current))
}