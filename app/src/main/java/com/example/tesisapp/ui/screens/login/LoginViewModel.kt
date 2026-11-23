package com.example.tesisapp.ui.screens.login

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.domain.repository.AuthRepository
import com.example.tesisapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _state = mutableStateOf(LoginState())
    val state: State<LoginState> = _state

    fun onLogin(db: String, user: String, pass: String) {
        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            val result = repository.login(db, user, pass)

            result.onSuccess { userOdoo ->
                // <--- 3. IMPORTANTE: Guardamos el usuario antes de cambiar el estado a success
                userRepository.saveUser(userOdoo)

                _state.value = _state.value.copy(isLoading = false, success = true)

            }.onFailure { exception ->
                _state.value = _state.value.copy(isLoading = false, error = exception.message)
            }
        }
    }
}

data class LoginState(
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)