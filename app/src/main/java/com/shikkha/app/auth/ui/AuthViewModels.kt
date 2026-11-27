package com.shikkha.app.auth.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shikkha.app.auth.data.AuthRepository
import com.shikkha.app.auth.data.RegisterResult
import com.shikkha.app.auth.data.RootState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class RootViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var state by mutableStateOf<RootState>(RootState.Loading)
        private set

    init {
        viewModelScope.launch {
            state = authRepository.resolveInitialState()
        }
    }

    /** Called when the backend flips a pending account to approved. */
    fun refreshStatus() {
        viewModelScope.launch { state = authRepository.refreshStatus() }
    }

    /** Called by screens when the backend reports a new authoritative state. */
    fun updateState(newState: RootState) {
        state = newState
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            state = RootState.LoggedOut
        }
    }
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
        private set

    fun signIn(onResult: (RootState) -> Unit) {
        if (loading) return // duplicate-submission guard (Spec 19.3)
        loading = true
        error = null
        viewModelScope.launch {
            val (rootState, failure) = authRepository.signIn(email.trim(), password)
            loading = false
            if (failure != null) {
                error = failure
            }
            onResult(rootState)
        }
    }

    fun consumeError() { error = null }
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    var fullName by mutableStateOf("")
    var username by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var classLevel by mutableStateOf(6)
    var medium by mutableStateOf("bangla")
    var loading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
        private set
    var emailConfirmationRequired by mutableStateOf(false)
        private set
    var registered by mutableStateOf(false)
        private set

    fun register() {
        if (loading) return
        error = validate()
        if (error != null) return
        loading = true
        viewModelScope.launch {
            when (val result = authRepository.register(
                email = email.trim(),
                password = password,
                fullName = fullName.trim(),
                username = username.trim(),
                classLevel = classLevel,
                medium = medium,
            )) {
                RegisterResult.ProfileCreated -> registered = true
                RegisterResult.EmailConfirmationRequired -> emailConfirmationRequired = true
                is RegisterResult.Failure -> error = result.message
            }
            loading = false
        }
    }

    fun consumeError() { error = null }

    private fun validate(): String? = when {
        fullName.isBlank() -> "Name is required"
        username.isBlank() || !username.all { it.isLetterOrDigit() || it == '_' } ->
            "Username: letters, digits and underscore only"
        !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Invalid email"
        password.length < 6 -> "Password must be at least 6 characters"
        else -> null
    }
}
