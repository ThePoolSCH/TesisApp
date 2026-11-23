package com.example.tesisapp.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesisapp.data.sync.OutboxRepository
import com.example.tesisapp.data.sync.SyncStatus
import com.example.tesisapp.domain.model.User
import com.example.tesisapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val user: User? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userRepository: UserRepository,
    outbox: OutboxRepository
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = userRepository.getSavedUser()
        .map { user -> MainUiState(user = user, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState(isLoading = true))

    /** Estado de la cola para el chip de la barra superior. */
    val sync: StateFlow<SyncStatus?> = outbox.estado
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        userRepository.logout()
        onDone()
    }
}
