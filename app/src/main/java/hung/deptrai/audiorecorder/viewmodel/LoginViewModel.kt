package hung.deptrai.audiorecorder.viewmodel

import androidx.lifecycle.ViewModel
import hung.deptrai.audiorecorder.presentation.sign_in.SignInResult
import hung.deptrai.audiorecorder.presentation.sign_in.SignInState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LoginViewModel : ViewModel() {
    // State and logic for login
    private val _signInState = MutableStateFlow(SignInState())
    val _state = _signInState.asStateFlow()

    fun onSignInResult(result: SignInResult){
        _signInState.update { it.copy(
            isSignInSuccessful = result.data != null,
            signInError = result.errorMessage
        ) }
    }
    fun resetState(){
        _signInState.update {
            SignInState()
        }
    }
}
