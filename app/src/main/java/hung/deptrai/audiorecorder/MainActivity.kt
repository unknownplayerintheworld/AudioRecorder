package hung.deptrai.audiorecorder

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.android.gms.auth.api.identity.Identity
import hung.deptrai.audiorecorder.data.database.AppDatabase
import hung.deptrai.audiorecorder.model.Audio
import hung.deptrai.audiorecorder.model.Tag
import hung.deptrai.audiorecorder.presentation.sign_in.GoogleAuthUIClient
import hung.deptrai.audiorecorder.presentation.sign_in.SignInState
import hung.deptrai.audiorecorder.presentation.sign_in.UserData
import hung.deptrai.audiorecorder.ui.theme.AudioRecorderTheme
import hung.deptrai.audiorecorder.ui.view.DropdownTypeOfRecordingMenu
import hung.deptrai.audiorecorder.ui.view.PlayingScreen
import hung.deptrai.audiorecorder.ui.view.RecordingScreen
import hung.deptrai.audiorecorder.ui.view.SettingScreen
import hung.deptrai.audiorecorder.viewmodel.AudioPlayerViewModel
//import hung.deptrai.audiorecorder.viewmodel.AudioPlayerViewModelFactory
import hung.deptrai.audiorecorder.viewmodel.LoginViewModel
import hung.deptrai.audiorecorder.viewmodel.RecordingRoomViewModel
import hung.deptrai.audiorecorder.viewmodel.RecordingRoomViewModelFactory
import hung.deptrai.audiorecorder.viewmodel.RecordingViewModel
import hung.deptrai.audiorecorder.viewmodel.RecordingViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Log.e("STATUS", "GRANTED")
            } else {
                Toast.makeText(this, "Permission required to record audio", Toast.LENGTH_LONG)
                    .show()
                Log.e("STATUS", "UNGRANTED")
            }

        }

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AudioRecorderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MyApp()
                }
            }
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }


    private val googleAuthUIClient by lazy {
        GoogleAuthUIClient(
            context = applicationContext,
            oneTapClient = Identity.getSignInClient(applicationContext)
        )
    }


    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @Composable
    fun MyApp() {
        val navController = rememberNavController()
        val viewmodel = viewModel<LoginViewModel>()
        val database = AppDatabase.getInstance(this) // Khởi tạo database của bạn
        val roomViewModel: RecordingRoomViewModel by viewModels {
            RecordingRoomViewModelFactory(database)
        }
        val recordingViewModel: RecordingViewModel by viewModels {
            RecordingViewModelFactory(this)
        }
        val audioPlayerViewModel: AudioPlayerViewModel = viewModel(factory = AudioPlayerViewModel.Factory)
        val recordingTime by recordingViewModel.recordingTime.collectAsState()

        val formattedTime = remember(recordingTime) {
            // Lấy 2 số đầu của milli giây và bỏ số 0 cuối
            val formattedMilliseconds =
                (recordingTime.milliseconds / 10).toString().padStart(2, '0')

            // Định dạng chuỗi thời gian
            if (recordingTime.days > 0) {
                String.format(
                    "%d:%02d:%02d:%02d:%02d",
                    recordingTime.days,
                    recordingTime.hours % 24,
                    recordingTime.minutes % 60,
                    recordingTime.seconds % 60,
                    formattedMilliseconds.toInt()
                )
            } else if (recordingTime.hours > 0) {
                String.format(
                    "%02d:%02d:%02d:%02d",
                    recordingTime.hours % 24,
                    recordingTime.minutes % 60,
                    recordingTime.seconds % 60,
                    formattedMilliseconds.toInt()
                )
            } else {
                String.format(
                    "%02d:%02d:%02d",
                    recordingTime.minutes % 60,
                    recordingTime.seconds % 60,
                    formattedMilliseconds.toInt()
                )
            }
        }
        val state by viewmodel._state.collectAsStateWithLifecycle()
        NavHost(navController = navController, startDestination = "home") {
            composable("sign_in") {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                    onResult = { rs ->
                        if (rs.resultCode == RESULT_OK) {
                            lifecycleScope.launch {
                                val signInResult = googleAuthUIClient.signInWithIntent(
                                    intent = rs.data ?: return@launch
                                )
                                viewmodel.onSignInResult(signInResult)
                            }
                        }
                    }
                )
                LaunchedEffect(key1 = state.isSignInSuccessful) {
                    if (state.isSignInSuccessful) {
                        Toast.makeText(
                            applicationContext,
                            "Sign In successful",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.navigate("home")
                        viewmodel.resetState()
                    }
                }
                LoginScreen(state = state, onSignInClick = {
                    lifecycleScope.launch {
                        val signInIntentSender = googleAuthUIClient.signIn()
                        launcher.launch(
                            IntentSenderRequest.Builder(
                                signInIntentSender ?: return@launch
                            ).build()
                        )
                    }
                })
            }
            composable("home") {
                val context = LocalContext.current
                val isEdit by recordingViewModel.isEdit.collectAsState()
                val isSearching by recordingViewModel.isSearch.collectAsState()
                val isOptionOpen by recordingViewModel.isOption.collectAsState()

                val recordings by recordingViewModel.audioList.collectAsState()

                LaunchedEffect(Unit) {
                    recordingViewModel.loadRecordings(context = context, roomViewModel)
                }
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                    ) {
                        val audioList = recordings
                        Column(
                            modifier = Modifier.fillMaxHeight(0.1f)
                        ) {
                            Header(Modifier.fillMaxSize(), audioList, onLoginClick = {
                                navController.navigate("sign_in")
                            }, onSignoutClick = {
                                lifecycleScope.launch {
                                    googleAuthUIClient.signOut()
                                    Toast.makeText(
                                        applicationContext,
                                        "Signed out Successful!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                                viewmodel.resetState()
                                navController.navigate("home")
                            }, recordingViewModel,
                                googleAuthUIClient.getSignedInUser()
                            )
                        }
                        if (isEdit) {
                            Column(
                                modifier = Modifier.fillMaxHeight(0.9f)
                            ) {
                                AudioList(
                                    audioList = recordings,
                                    recordingViewModel = recordingViewModel,
                                    roomViewModel = roomViewModel,
                                    navController = navController,
                                    audioPlayerVM = audioPlayerViewModel,
                                    onPlayClick = {

                                    })
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxHeight(0.75f)
                            ) {
                                AudioList(
                                    audioList = recordings,
                                    recordingViewModel = recordingViewModel,
                                    roomViewModel = roomViewModel,
                                    navController = navController,
                                    audioPlayerVM = audioPlayerViewModel,
                                    onPlayClick = {

                                    })
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.9f)
                        ) {
                            Footer(
                                onRecordClick = {
                                    if (checkPermission()) {
                                        startRecording()
                                        Log.e("STATUS", "GRANTED")
                                        navController.navigate("recording")
                                        recordingViewModel.startRecording()
                                    } else {
                                        requestPermission()
                                        Log.e("STATUS", "UNGRANTED")
                                    }
                                },
                                recordingViewModel = recordingViewModel,
                                context = context,
                                recordingRoomViewModel = roomViewModel
                            )
                        }
                    }
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Transparent.copy(alpha = 0.8f))
                        ) {
                            SearchOverlay(
                                onClose = { recordingViewModel.closeSearching() },
                                audioPlayerVM = audioPlayerViewModel,
                                recordingViewModel = recordingViewModel,
                                roomViewModel = roomViewModel,
                                navController = navController,
                                onPlayClick = {}
                            )
                        }
                    }
                    if (isOptionOpen) {
                        MoreOptionsMenu(recordingViewModel = recordingViewModel, navController)
                    }
                }
            }
            composable("recording") {
                val recordings by recordingViewModel.audioList.collectAsState()
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val tagList = listOf(
                        Tag(
                            name = "ghi âm 1",
                            time_offset = 30
                        ),
                        Tag(
                            name = "ghi âm 2",
                            time_offset = 20
                        ),
                        Tag(
                            name = "ghi âm 2",
                            time_offset = 20
                        ),
                        Tag(
                            name = "ghi âm 2",
                            time_offset = 20
                        ), Tag(
                            name = "ghi âm 2",
                            time_offset = 20
                        )
                    )
                    val isRecoding by recordingViewModel.isRecording.collectAsState()
                    val isPause by recordingViewModel.isPause.collectAsState()
                    val isCancel by recordingViewModel.isCancel.collectAsState()
                    LaunchedEffect(isCancel) {
                        if (isCancel) {
                            navController.navigate("home")
                        }
                    }
                    RecordingScreen(
                        TagList = tagList,
                        onDoneClick =
                        {
                            recordingViewModel.onConfirmStop()
                        },
                        onCancelClick =
                        {
                            recordingViewModel.cancelRecording()
                        },
                        onRecordClick =
                        {
                            if (isRecoding) {
                                recordingViewModel.pauseRecording()
                            }
                            if (isPause) {
                                recordingViewModel.resumeRecording()
                            }
                        },
                        isRecording = isRecoding,
                        isPause = isPause,
                        Timer = formattedTime,
                        recordingViewModel = recordingViewModel,
                        navController = navController, recordings
                    )
                }
            }
            composable(
                "player/{uri}",
                arguments = listOf(navArgument("uri") {
                    type = NavType.StringType
                })
            ) { navBackStackEntry ->
                run {
                    val uri = Uri.parse(navBackStackEntry.arguments?.getString("uri"))
                    val context = LocalContext.current
                    val audioFile = audioPlayerViewModel.getAudioFileInfo(uri)
                    audioFile?.let { file ->
                        val tagList = listOf(
                            Tag(
                                name = "ghi âm 1",
                                time_offset = 30
                            ),
                            Tag(
                                name = "ghi âm 2",
                                time_offset = 20
                            ),
                            Tag(
                                name = "ghi âm 2",
                                time_offset = 20
                            ),
                            Tag(
                                name = "ghi âm 2",
                                time_offset = 20
                            ), Tag(
                                name = "ghi âm 2",
                                time_offset = 20
                            )
                        )

                        PlayingScreen(
                            TagList = tagList,
                            onCancelClick =
                            {
                                audioPlayerViewModel.stopAudio()
                                navController.navigate("home")
                            },
                            onClickEqualizer = {},
                            // hoặc format lại thời gian nếu cần
                            viewModel = audioPlayerViewModel
                        )
                    }
                }
            }
            composable("setting") {
                val isTypeClick by recordingViewModel.isTypeClicking.collectAsState()
                Box(modifier = Modifier.fillMaxSize()) {
                    SettingScreen(recordingViewModel = recordingViewModel, navController)
                    if (isTypeClick) {
                        Column(
                            Modifier.fillMaxSize()
                        ) {
                            DropdownTypeOfRecordingMenu(recordingViewModel = recordingViewModel)
                        }
                    }
                }
            }
        }
    }


    private fun startRecording() {
        Toast.makeText(this, "Starting record...!", Toast.LENGTH_LONG).show()
    }

    fun generateRandomFileName(extension: String): String {
        val randomUUID = UUID.randomUUID()
        return "${randomUUID.toString()}.$extension"
    }

    fun formatDuration(milliseconds: Long): String {
        Log.e("Milliseconds", milliseconds.toString())

        val seconds = milliseconds / 1000
        val days = seconds / (24 * 3600)
        val remainingSecondsAfterDays = seconds % (24 * 3600)
        val hours = remainingSecondsAfterDays / 3600
        val remainingSecondsAfterHours = remainingSecondsAfterDays % 3600
        val minutes = remainingSecondsAfterHours / 60
        val remainingSeconds = remainingSecondsAfterHours % 60

        return when {
            days > 0 -> String.format("%d:%02d:%02d:%02d", days, hours, minutes, remainingSeconds)
            hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
            else -> String.format("%02d:%02d", minutes, remainingSeconds)
        }
    }

    @Composable
    fun LoginScreen(
        state: SignInState,
        onSignInClick: () -> Unit
    ) {
        val context = LocalContext.current
        LaunchedEffect(key1 = state.signInError) {
            state.signInError?.let { error ->
                Toast.makeText(
                    context,
                    error,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xff000000))
        ) {
            Spacer(modifier = Modifier.fillMaxHeight(0.1f))
            Column(
                modifier = Modifier.fillMaxHeight(0.15f),
                verticalArrangement = Arrangement.Center
            ) {
                LoginText()
            }
            Column(
                modifier = Modifier.fillMaxHeight(0.5f)
            ) {
                LoginInputField()
            }
            Column(
                modifier = Modifier.fillMaxHeight(0.5f),
                verticalArrangement = Arrangement.Top
            ) {
                footerLogin(
                    onSignInClick
                )
            }
            Spacer(modifier = Modifier.fillMaxHeight(0.5f))
            Column(
                modifier = Modifier.fillMaxHeight(0.5f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                footerLogin2()
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SearchOverlay(
        onClose: () -> Unit,
        audioPlayerVM: AudioPlayerViewModel,
        recordingViewModel: RecordingViewModel,
        roomViewModel: RecordingRoomViewModel,
        navController: NavHostController,
        onPlayClick: (String) -> Unit
    ) {
        val searchText by recordingViewModel.searchText.collectAsState()
        val audios by recordingViewModel.audios.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent.copy(alpha = 0.5f))
                .clickable(onClick = { recordingViewModel.closeSearching() }) // Close on clicking outside
        ) {

            Box(
                modifier = Modifier
                    .background(Color.Transparent)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Column(
                    Modifier.fillMaxSize()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TextField for search input
                        TextField(
                            value = searchText,
                            onValueChange = recordingViewModel::onSearchTextChange,
                            modifier = Modifier
                                .weight(1f),
                            placeholder = {
                                Text(text = "search")
                            },
                            trailingIcon = {
                                if (searchText.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            recordingViewModel.onSearchTextChange("") // Clear text
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Close button to exit the search overlay
                        TextButton(
                            onClick = onClose
                        ) {
                            Text(
                                text = "Hủy",
                                color = Color.Gray
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    AudioList(
                        audioPlayerVM = audioPlayerVM,
                        recordingViewModel = recordingViewModel,
                        roomViewModel = roomViewModel,
                        audioList = audios,
                        navController = navController,
                        onPlayClick = onPlayClick
                    )
                }

            }
        }
    }


    @Composable
    fun MoreOptionsMenu(
        recordingViewModel: RecordingViewModel,
        navController: NavHostController
    ) {
        val isOptionOpen by recordingViewModel.isOption.collectAsState()
        DropdownMenu(
            expanded = isOptionOpen,
            onDismissRequest = {
                recordingViewModel.setOptionStatus(false)
            },
            offset = DpOffset(x = (-10).dp, y = (-170).dp),
            modifier = Modifier
                .background(Color.Gray)
                .clip(RoundedCornerShape(10.dp))

        ) {
            DropdownMenuItem(text = { Text(text = "Sửa") }, colors = MenuDefaults.itemColors(
                textColor = Color.White
            ), onClick = {
                recordingViewModel.setOptionStatus(false)
                recordingViewModel.onEditChange(true)
            },
                modifier = Modifier.clickable(
                    indication = rememberRipple(bounded = true),
                    interactionSource = remember {
                        MutableInteractionSource()
                    }
                ) {

                })
            DropdownMenuItem(
                text = { Text(text = "Cài đặt") },
                colors = MenuDefaults.itemColors(textColor = Color.White),
                onClick = {
                    recordingViewModel.setOptionStatus(false)
                    navController.navigate("setting")
                },
                modifier = Modifier.clickable(
                    indication = rememberRipple(bounded = true),
                    interactionSource = remember {
                        MutableInteractionSource()
                    }
                ) {
                })
        }
    }


    @Composable
    fun Header(
        modifier: Modifier = Modifier,
        audioList: List<Audio>,
        onLoginClick: () -> Unit = {},
        onSignoutClick: () -> Unit = {},
        recordingViewModel: RecordingViewModel,
        userData: UserData?
    ) {
        val isEdit by recordingViewModel.isEdit.collectAsState()
        val isCheckAll by recordingViewModel.isCheckAll.collectAsState()
        fun toggleAllSelection(isSelected: Boolean) {
            if (isSelected) {
                recordingViewModel.selectAllItems(audioList.map { it.audioPath.toString() }.toSet())
            } else {
                recordingViewModel.deselectAllItems()
            }
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(Color.Black),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (isEdit) {
                IconButton(onClick = { recordingViewModel.onEditChange(false) }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                }
                Checkbox(checked = isCheckAll, onCheckedChange = { it ->
                    recordingViewModel.onCheckAll(it)
                    toggleAllSelection(it)
                })
            } else {
                if (userData == null) {
                    Button(
                        onClick = onLoginClick,
                        modifier = Modifier.padding(start = 5.dp, end = 5.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(text = "Log in")
                    }
                } else {
                    Button(
                        onClick = onSignoutClick,
                        modifier = Modifier.padding(start = 5.dp),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(text = "Sign out")
                    }
                    Text(
                        text = userData.username,
                        textAlign = TextAlign.Center,
                        style = TextStyle(fontWeight = FontWeight.Bold)
                    )
                    Log.e("Username", userData.username)
                }
                Row(
                    modifier = modifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = { recordingViewModel.setSearchingStatus(true) },
                        modifier = Modifier.padding(end = 5.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_search),
                            contentDescription = "Icon search",
                            tint = Color.White
                        )
                    }
                    IconButton(
                        onClick = { recordingViewModel.setOptionStatus(true) },
                        modifier = Modifier.padding(end = 5.dp),
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_option),
                            contentDescription = "Icon option",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun AudioList(
        audioPlayerVM: AudioPlayerViewModel,
        recordingViewModel: RecordingViewModel,
        roomViewModel: RecordingRoomViewModel,
        audioList: List<Audio>,
        navController: NavHostController,
        onPlayClick: (String) -> Unit
    ) {
        val context = LocalContext.current
        val isEdit by recordingViewModel.isEdit.collectAsState()
        val selectedItems by recordingViewModel.selectedItem.collectAsState()

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF111111)),
            contentPadding = PaddingValues(8.dp)
        ) {
            // Lặp qua danh sách âm thanh
            items(audioList.size) { index ->
                // Lấy đối tượng âm thanh hiện tại
                val audioData = audioList[index]
                val isSelected = selectedItems.contains(audioData.audioPath.toString())


                // Hiển thị một mục trong danh sách
                var showDialog by remember {
                    mutableStateOf(false)
                }
                val fileURL = audioList[index].audioPath
                if (showDialog) {
                    AlertDialog(onDismissRequest = { showDialog = true },
                        title = { Text(text = "Confirm Deletion") },
                        text = { Text("Do you really want to delete this recording?") },
                        confirmButton = {
                            Button(onClick = {
                                audioPlayerVM.deleteRecordingFile(fileURL)
                                roomViewModel.deleteRecordingByFileName(audioList[index].name)
                                recordingViewModel.deleteRecordingElement(index)
                                showDialog = false

                            }) {
                                Text(text = "Delete")
                            }
                        }, dismissButton = {
                            Button(onClick = {
                                showDialog = false
                            }) {
                                Text("Cancel")
                            }
                        })
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    showDialog = true
                                }, onTap = {
                                    if (!isEdit) {  // Check if isEdit is false
                                        val encodedUri =
                                            Uri.encode(audioList[index].audioPath.toString())
                                        navController.navigate("player/$encodedUri")
                                        audioPlayerVM.setAudioUri(audioList[index].audioPath)
                                        audioPlayerVM.playAudio()
                                    }
                                }
                            )
                        }
                        .padding(8.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.6f)
                        ) {
                            // Tên ghi âm
                            Text(
                                text = audioData.name,
                                color = Color(0xFFFFFFFF),
                                modifier = Modifier.weight(1f) // Chiếm toàn bộ chiều rộng còn lại
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.4f)
                        ) {
                            Text(
                                text = formatDuration(audioData.duration),
                                fontSize = 10.sp,
                                color = Color(0xFFFFFFFF),
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(end = 8.dp)
                            )

                            // Thời gian ghi âm
                            Text(
                                text = formatTimestamp(audioData.recordingTime),
                                fontSize = 10.sp,
                                color = Color(0xFFFFFFFF),
                                modifier = Modifier.wrapContentSize()
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        if (!isEdit) {
                            // Nút Play
                            IconButton(
                                onClick = {
                                    onPlayClick
                                }, // Gọi hàm xử lý khi nhấp vào nút Play
                                modifier = Modifier
                                    .padding(start = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_play),
                                    contentDescription = "Play",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Checkbox(checked = isSelected, onCheckedChange = {
                                recordingViewModel.toggleItem(
                                    audioData.audioPath.toString(),
                                    audioList[index].name
                                )
                            })
                        }
                    }
                }
                Spacer(
                    modifier = Modifier
                        .height(5.dp)
                        .fillMaxWidth()
                        .background(Color.Black)
                ) // Thêm Spacer dưới cùng của mỗi item
            }
        }
    }


    @Composable
    fun Footer(
        onRecordClick: () -> Unit,
        recordingViewModel: RecordingViewModel,
        context: Context,
        recordingRoomViewModel: RecordingRoomViewModel
    ) {
        val isEdit by recordingViewModel.isEdit.collectAsState()
        val selectedItems by recordingViewModel.selectedItem.collectAsState()
        Box(modifier = Modifier.fillMaxSize()) {
            if (isEdit) {
                Divider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(5.dp, 0.dp, 5.dp, 0.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(0.5f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = {
                            recordingViewModel.deleteSelectedFiles(recordingRoomViewModel)
                            recordingViewModel.onEditChange(false)
                        }, enabled = selectedItems.size > 0) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = if (selectedItems.size > 0) Color.White else Color.Gray
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(onClick = {
                            selectedItems.firstOrNull()
                                ?.let { recordingViewModel.addToRingtone(context = context, it) }
                            selectedItems.firstOrNull()?.let {
                                recordingViewModel.setAsRingtone(context, it)
                            }
                        }, enabled = selectedItems.size == 1) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Ringtones",
                                tint = if (selectedItems.size == 1) Color.White else Color.Gray
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp, 20.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.3f)
                            .background(Color(0xFF222222))
                            .padding(top = 5.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tiêu chuẩn",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.7f)
                            .background(Color.DarkGray),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onRecordClick() },
                            modifier = Modifier
                                .padding(5.dp)
                                .size(60.dp)
                                .clip(RoundedCornerShape(60.dp))
                                .background(Color.Red)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_round_record),
                                contentDescription = "Record",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    fun formatTimestamp(seconds: Long): String {
        // Chuyển đổi giây thành milliseconds
        val timestamp = seconds * 1000
        // Định dạng ngày giờ
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        // Chuyển đổi timestamp thành chuỗi ngày giờ định dạng
        return sdf.format(Date(timestamp))
    }

    @Composable
    fun LoginText() {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xff000000)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.fillMaxHeight(0.05f))
            Text(
                text = "LOGIN",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun LoginInputField() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            var filledText by remember {
                mutableStateOf("")
            }
            TextField(
                value = filledText, onValueChange = {
                    filledText = it
                },
                textStyle = TextStyle(
                    textAlign = TextAlign.Right
                ),
                placeholder = {
                    Text(text = "Username")
                },
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_person),
                        contentDescription = "username"
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .border(1.dp, Color.Gray, RoundedCornerShape(40.dp))
                    .clip(RoundedCornerShape(40.dp))
            )
            Spacer(modifier = Modifier.fillMaxHeight(0.2f))
            var filledText2 by remember {
                mutableStateOf("")
            }
            TextField(
                value = filledText, onValueChange = {
                    filledText2 = it
                },
                textStyle = TextStyle(
                    textAlign = TextAlign.Right
                ),
                placeholder = {
                    Text(text = "Password")
                },
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent
                ),
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_lock),
                        contentDescription = "username"
                    )
                },
                modifier = Modifier
                    .border(1.dp, Color.Gray, RoundedCornerShape(40.dp))
                    .clip(RoundedCornerShape(40.dp))
            )
            Spacer(modifier = Modifier.fillMaxHeight(0.3f))
            Text(
                text = "forgot your password?",
                color = Color.Blue
            )
        }
    }

    @Composable
    fun footerLogin(
        onSignInClick: () -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xff000000)),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            IconButton(
                onClick = onSignInClick,
                Modifier
                    .border(1.dp, Color.White, RoundedCornerShape(40.dp))
                    .padding(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.google),
                    contentDescription = "google",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.fillMaxHeight(0.2f))
            Button(
                onClick = { },
                modifier = Modifier
                    .background(Color(0xff000000))
                    .clip(RoundedCornerShape(20.dp))
            ) {
                Text(text = "Login", color = Color.White)
            }
        }
    }

    @Composable
    fun footerLogin2() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xff000000)),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.background(Color.Black),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Don't have an account?", color = Color.White)
                Spacer(modifier = Modifier.fillMaxWidth(0.08f))
                Button(
                    onClick = {  },
                    modifier = Modifier
                        .background(Color(0xff000000))
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    Text(text = "Sign Up", color = Color.White)
                }
            }
        }
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        AudioRecorderTheme {


        }
    }

    @Preview(showBackground = true)
    @Composable
    fun RcvPreview() {
        AudioRecorderTheme {

        }
    }

    @Preview(showBackground = true)
    @Composable
    fun FooterReview() {
        DropdownTypeOfRecordingMenu(recordingViewModel = RecordingViewModel(LocalContext.current))
    }
}